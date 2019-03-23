/******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.kettle.trans.steps.coalesce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.kettle.ui.trans.steps.coalesce.CoalesceDialog;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.injection.InjectionDeep;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * Lets you combine multiple fields into one, selecting the first value that is
 * non-null.
 * 
 * @author Nicolas ADMENT
 *
 */
@Step(id = "Coalesce", name = "Coalesce.Name", description = "Coalesce.Description", image = "coalesce.svg", i18nPackageName = "org.kettle.trans.steps.coalesce", categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Transform")
@InjectionSupported(localizationPrefix = "CoalesceMeta.Injection.", groups = { "FIELDS" })
public class CoalesceMeta extends BaseStepMeta implements StepMetaInterface {

	private static final Class<?> PKG = CoalesceMeta.class; // for i18n purposes

	/**
	 * Constants:
	 */
	private static final String TAG_NAME = "name"; //$NON-NLS-1$
	private static final String TAG_FIELD = "field"; //$NON-NLS-1$
	private static final String TAG_FIELDS = "fields"; //$NON-NLS-1$
	private static final String TAG_VALUE_TYPE = "value_type"; //$NON-NLS-1$
	private static final String TAG_EMPTY_IS_NULL = "empty_is_null"; //$NON-NLS-1$
	private static final String TAG_REMOVE = "remove"; //$NON-NLS-1$

	/** The fields to coalesce */
	@InjectionDeep
	private List<Coalesce> coalesces = new ArrayList<>();

	/**
	 * additional options
	 */
	@Injection(name = "EMPTY_STRING_AS_NULLS")
	private boolean emptyStringsAsNulls;

	public CoalesceMeta() {
		super();
	}

	/**
	 * Called by PDI to get a new instance of the step implementation. A
	 * standard implementation passing the arguments to the constructor of the
	 * step class is recommended.
	 *
	 * @param stepMeta
	 *            description of the step
	 * @param stepDataInterface
	 *            instance of a step data class
	 * @param cnr
	 *            copy number
	 * @param transMeta
	 *            description of the transformation
	 * @param disp
	 *            runtime implementation of the transformation
	 * @return the new instance of a step implementation
	 */
	@Override
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
			Trans disp) {
		return new CoalesceStep(stepMeta, stepDataInterface, cnr, transMeta, disp);
	}

	/**
	 * Called by PDI to get a new instance of the step data class.
	 */
	@Override
	public StepDataInterface getStepData() {
		return new CoalesceData();
	}

	/**
	 * This method is called every time a new step is created and should
	 * allocate/set the step configuration to sensible defaults. The values set
	 * here will be used by Spoon when a new step is created.
	 */
	@Override
	public void setDefault() {
		this.coalesces = new ArrayList<>();
		this.emptyStringsAsNulls = false;
	}

	public boolean isTreatEmptyStringsAsNulls() {
		return this.emptyStringsAsNulls;
	}

	public void setEmptyStringsAsNulls(boolean treatEmptyStringsAsNulls) {
		this.emptyStringsAsNulls = treatEmptyStringsAsNulls;
	}

	@Override
	public Object clone() {
		CoalesceMeta clone = (CoalesceMeta) super.clone();

		clone.coalesces = new ArrayList<>(coalesces);

		return clone;
	}
	
	// For compatibility with 7.x
	@Override
	public String getDialogClassName() {
		return CoalesceDialog.class.getName();
	}

	@Override
	public String getXML() throws KettleValueException {

		StringBuilder xml = new StringBuilder(500);

		xml.append(XMLHandler.addTagValue(TAG_EMPTY_IS_NULL, emptyStringsAsNulls));

		xml.append("<fields>"); //$NON-NLS-1$
		for (Coalesce coalesce : coalesces) {
			xml.append("<field>"); //$NON-NLS-1$

			xml.append(XMLHandler.addTagValue(TAG_NAME, coalesce.getName()));
			xml.append(XMLHandler.addTagValue(TAG_VALUE_TYPE, ValueMetaFactory.getValueMetaName(coalesce.getType())));
			xml.append(XMLHandler.addTagValue(TAG_REMOVE, coalesce.isRemoveFields()));

			xml.append("<input>"); //$NON-NLS-1$
			for (String field : coalesce.getInputFields()) {
				xml.append(XMLHandler.addTagValue(TAG_FIELD, field));
			}
			xml.append("</input>"); //$NON-NLS-1$

			xml.append("</field>"); //$NON-NLS-1$
		}
		xml.append("</fields>"); //$NON-NLS-1$

		return xml.toString();
	}

	@Override
	public void loadXML(Node stepNode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {

		try {
			this.emptyStringsAsNulls = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_EMPTY_IS_NULL)); //$NON-NLS-1$

			Node fields = XMLHandler.getSubNode(stepNode, "fields"); //$NON-NLS-1$
			int count = XMLHandler.countNodes(fields, "field"); //$NON-NLS-1$
			coalesces = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				Node line = XMLHandler.getSubNodeByNr(fields, "field", i); //$NON-NLS-1$

				Coalesce coalesce = new Coalesce();
				coalesce.setName(Const.NVL(XMLHandler.getTagValue(line, TAG_NAME), ""));
				coalesce.setType(XMLHandler.getTagValue(line, TAG_VALUE_TYPE));
				coalesce.setRemoveFields("Y".equalsIgnoreCase(XMLHandler.getTagValue(line, TAG_REMOVE)));

				Node input = XMLHandler.getSubNode(line, "input"); //$NON-NLS-1$
				if (input != null) {
					Node field = input.getFirstChild();
					while (field != null) {
						coalesce.addInputField(XMLHandler.getNodeValue(field));
						field = field.getNextSibling();
					}
				}

				coalesces.add(coalesce);
			}
		} catch (Exception e) {
			throw new KettleXMLException(
					BaseMessages.getString(PKG, "CoalesceMeta.Exception.UnableToReadStepInfoFromXML"), e);  //$NON-NLS-1$
		}

	}

	@Override
	public void saveRep(Repository repository, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
			throws KettleException {
		try {
			repository.saveStepAttribute(id_transformation, id_step, TAG_EMPTY_IS_NULL, emptyStringsAsNulls);

			for (int i = 0; i < this.coalesces.size(); i++) {
				Coalesce coalesce = this.coalesces.get(i);
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_NAME, coalesce.getName());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_VALUE_TYPE,
						ValueMetaFactory.getValueMetaName(coalesce.getType()));
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_REMOVE, coalesce.isRemoveFields());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_FIELDS, String.join(",", coalesce.getInputFields()));
			}
		} catch (Exception e) {
			throw new KettleException(
					BaseMessages.getString(PKG, "CoalesceMeta.Exception.UnableToSaveRepository", id_step), e); //$NON-NLS-1$
		}
	}

	@Override
	public void readRep(Repository repository, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
			throws KettleException {
		try {
			this.emptyStringsAsNulls = repository.getStepAttributeBoolean(id_step, TAG_EMPTY_IS_NULL);

			int count = repository.countNrStepAttributes(id_step, TAG_NAME);

			this.coalesces = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				Coalesce coalesce = new Coalesce();
				coalesce.setName(repository.getStepAttributeString(id_step, i, TAG_NAME));
				coalesce.setType(repository.getStepAttributeString(id_step, i, TAG_VALUE_TYPE));
				coalesce.setRemoveFields(repository.getStepAttributeBoolean(id_step, i, TAG_REMOVE));
				coalesce.setInputFields(repository.getStepAttributeString(id_step, i, TAG_FIELDS));
				this.coalesces.add(coalesce);
			}
		} catch (Exception e) {

			throw new KettleException(
					BaseMessages.getString(PKG, "CoalesceMeta.Exception.UnableToReadRepository", id_step), e); //$NON-NLS-1$
		}
	}

	/**
	 * This method is called to determine the changes the step is making to the
	 * row-stream.
	 *
	 * @param inputRowMeta
	 *            the row structure coming in to the step
	 * @param stepName
	 *            the name of the step making the changes
	 * @param info
	 *            row structures of any info steps coming in
	 * @param nextStep
	 *            the description of a step this step is passing rows to
	 * @param space
	 *            the variable space for resolving variables
	 * @param repository
	 *            the repository instance optionally read from
	 * @param metaStore
	 *            the metaStore to optionally read from
	 */
	@Override
	public void getFields(RowMetaInterface inputRowMeta, String stepName, RowMetaInterface[] info, StepMeta nextStep,
			VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
		try {
			// store the input stream meta
			RowMetaInterface unalteredInputRowMeta = inputRowMeta.clone();

			// first remove all unwanted input fields from the stream
			for (Coalesce coalesce : this.getCoalesces()) {

				if (coalesce.isRemoveFields()) {

					// Resolve variable name
					String name = space.environmentSubstitute(coalesce.getName());

					for (String fieldName : coalesce.getInputFields()) {

						// If input field name is recycled for output, don't
						// remove
						if (inputRowMeta.indexOfValue(name) != -1 && name.equals(fieldName))
							continue;

						if (inputRowMeta.indexOfValue(fieldName) != -1) {
							inputRowMeta.removeValueMeta(fieldName);
						}
					}
				}
			}

			// then add the output fields
			for (Coalesce coalesce : this.getCoalesces()) {
				int type = coalesce.getType();
				if (type == ValueMetaInterface.TYPE_NONE) {
					type = findDefaultValueType(unalteredInputRowMeta, coalesce);
				}

				String name = space.environmentSubstitute(coalesce.getName());
				ValueMetaInterface valueMeta = ValueMetaFactory.createValueMeta(name, type);
				valueMeta.setOrigin(stepName);

				int index = inputRowMeta.indexOfValue(name);
				if (index >= 0) {
					inputRowMeta.setValueMeta(index, valueMeta);
				} else {
					inputRowMeta.addValueMeta(valueMeta);
				}
			}
		} catch (Exception e) {
			throw new KettleStepException(e);
		}
	}

	/**
	 * This method is called when the user selects the "Verify Transformation"
	 * option in Spoon.
	 *
	 * @param remarks
	 *            the list of remarks to append to
	 * @param transMeta
	 *            the description of the transformation
	 * @param stepMeta
	 *            the description of the step
	 * @param prev
	 *            the structure of the incoming row-stream
	 * @param input
	 *            names of steps sending input to the step
	 * @param output
	 *            names of steps this step is sending output to
	 * @param info
	 *            fields coming in from info steps
	 * @param metaStore
	 *            metaStore to optionally read from
	 */
	@Override
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev,
			String input[], String output[], RowMetaInterface info, VariableSpace space, Repository repository,
			IMetaStore metaStore) {

		// See if we have fields from previous steps
		if (prev == null || prev.size() == 0) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.NotReceivingFieldsFromPreviousSteps"), //$NON-NLS-1$
					stepMeta));
		} else {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG,
					"CoalesceMeta.CheckResult.ReceivingFieldsFromPreviousSteps", prev.size()), stepMeta)); //$NON-NLS-1$
		}

		// See if there are input streams leading to this step!
		if (input.length > 0) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.ReceivingInfoFromOtherSteps"), stepMeta)); //$NON-NLS-1$
		} else {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.NotReceivingInfoFromOtherSteps"), stepMeta)); //$NON-NLS-1$
		}

		// See if there are missing, duplicate or not enough input streams
		boolean missing = false;
		for (Coalesce coalesce : this.getCoalesces()) {

			Set<String> fields = new HashSet<String>();
			List<String> missingFields = new ArrayList<String>();
			List<String> duplicateFields = new ArrayList<String>();

			for (String fieldName : coalesce.getInputFields()) {

				if (fields.contains(fieldName))
					duplicateFields.add(fieldName);
				else
					fields.add(fieldName);

				ValueMetaInterface vmi = prev.searchValueMeta(fieldName);
				if (vmi == null) {
					missingFields.add(fieldName);
				}

			}

			if (!missingFields.isEmpty()) {
				String message = BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.MissingInStreamFields", //$NON-NLS-1$
						coalesce.getName(), StringUtils.join(missingFields, ','));
				remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, message, stepMeta));
				missing = true;
			} else if (!duplicateFields.isEmpty()) {
				String message = BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.DuplicateInStreamFields", //$NON-NLS-1$
						coalesce.getName(), StringUtils.join(duplicateFields, ','));
				remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, message, stepMeta));
				missing = true;
			} else if (fields.size() == 0) {
				String message = BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.EmptyInStreamFields", //$NON-NLS-1$
						coalesce.getName());
				remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, message, stepMeta));
				missing = true;
			} else if (fields.size() < 2) {
				String message = BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.NotEnoughInStreamFields", //$NON-NLS-1$
						coalesce.getName());
				remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING, message, stepMeta));
			}

		}

		// See if there something to coalesce
		if (this.getCoalesces().isEmpty()) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.NothingToCoalesce"), stepMeta)); //$NON-NLS-1$
		} else if (!missing) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.FoundInStreamFields"), stepMeta)); //$NON-NLS-1$
		}

	}

	/**
	 * If all fields are of the same data type then the output field should
	 * mirror this otherwise return a more generic String type
	 */
	private int findDefaultValueType(final RowMetaInterface inputRowMeta, final Coalesce coalesce) throws Exception {

		int type = ValueMetaInterface.TYPE_NONE;
		boolean first = true;

		for (String field : coalesce.getInputFields()) {

			if (first) {
				type = getInputFieldValueType(inputRowMeta, field);
				first = false;
			} else {
				int otherType = getInputFieldValueType(inputRowMeta, field);

				if (type != otherType) {

					switch (type) {
					case ValueMetaInterface.TYPE_STRING:
						// keep TYPE_STRING
						break;
					case ValueMetaInterface.TYPE_INTEGER:
						if (otherType == ValueMetaInterface.TYPE_NUMBER) {
							type = ValueMetaInterface.TYPE_NUMBER;
						} else if (otherType == ValueMetaInterface.TYPE_BIGNUMBER) {
							type = ValueMetaInterface.TYPE_BIGNUMBER;
						} else {
							type = ValueMetaInterface.TYPE_STRING;
						}
						break;
					case ValueMetaInterface.TYPE_NUMBER:
						if (otherType == ValueMetaInterface.TYPE_INTEGER) {
							// keep TYPE_NUMBER
						} else if (otherType == ValueMetaInterface.TYPE_BIGNUMBER) {
							type = ValueMetaInterface.TYPE_BIGNUMBER;
						} else {
							type = ValueMetaInterface.TYPE_STRING;
						}
						break;

					case ValueMetaInterface.TYPE_DATE:
						if (otherType == ValueMetaInterface.TYPE_TIMESTAMP) {
							type = ValueMetaInterface.TYPE_TIMESTAMP;
						} else {
							type = ValueMetaInterface.TYPE_STRING;
						}
						break;
					case ValueMetaInterface.TYPE_TIMESTAMP:
						if (otherType == ValueMetaInterface.TYPE_DATE) {
							// keep TYPE_TIMESTAMP
						} else {
							type = ValueMetaInterface.TYPE_STRING;
						}
						break;
					case ValueMetaInterface.TYPE_BIGNUMBER:
						if (otherType == ValueMetaInterface.TYPE_INTEGER) {
							// keep TYPE_BIGNUMBER
						} else if (otherType == ValueMetaInterface.TYPE_NUMBER) {
							// keep TYPE_BIGNUMBER
						} else {
							type = ValueMetaInterface.TYPE_STRING;
						}
						break;
					case ValueMetaInterface.TYPE_BOOLEAN:
					case ValueMetaInterface.TYPE_INET:
					case ValueMetaInterface.TYPE_SERIALIZABLE:
					case ValueMetaInterface.TYPE_BINARY:
					default:
						return ValueMetaInterface.TYPE_STRING;
					}
				}
			}
		}

		if (type == ValueMetaInterface.TYPE_NONE) {
			type = ValueMetaInterface.TYPE_STRING;
		}

		return type;
	}

	/**
	 * Extracts the ValueMeta type of an input field, returns
	 * {@link ValueMetaInterface.TYPE_NONE} if the field is not present in the
	 * input stream
	 */
	private int getInputFieldValueType(final RowMetaInterface inputRowMeta, final String field) {
		int index = inputRowMeta.indexOfValue(field);
		if (index >= 0) {
			return inputRowMeta.getValueMeta(index).getType();
		}
		return ValueMetaInterface.TYPE_NONE;
	}

	public List<Coalesce> getCoalesces() {
		return coalesces;
	}

	public void setCoalesces(List<Coalesce> coalesces) {
		this.coalesces = (coalesces == null) ? Collections.emptyList() : coalesces;
	}
}