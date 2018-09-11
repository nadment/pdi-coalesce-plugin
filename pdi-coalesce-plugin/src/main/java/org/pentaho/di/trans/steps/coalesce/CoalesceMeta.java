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

package org.pentaho.di.trans.steps.coalesce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
import org.pentaho.di.core.util.Utils;
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
@Step(id = "Coalesce", image = "coalesce.svg", i18nPackageName = "org.pentaho.di.trans.steps.coalesce", name = "Coalesce.Name", description = "Coalesce.Description", categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Experimental", // TODO:
																																																																// Transform
																																																																// step
																																																																// category
		documentationUrl = "https://github.com/nadment/pdi-coalesce/wiki")
@InjectionSupported(localizationPrefix = "CoalesceMeta.Injection.", groups = { "FIELDS" })
public class CoalesceMeta extends BaseStepMeta implements StepMetaInterface {

	private static Class<?> PKG = CoalesceMeta.class; // for i18n purposes

	/**
	 * Constants:
	 */
	private static final int STRING_AS_DEFAULT = -1;

	private static final String TAG_OUTPUT_FIELD = "output_field";

	private static final String TAG_VALUE_TYPE = "value_type";

	private static final String TAG_EMPTY_IS_NULL = "empty_is_null";

	private static final String TAG_REMOVE = "remove";

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

	@Override
	public String getXML() throws KettleValueException {

		StringBuilder xml = new StringBuilder(500);

		xml.append("    ").append(XMLHandler.addTagValue(TAG_EMPTY_IS_NULL, emptyStringsAsNulls));

		xml.append("    <fields>").append(Const.CR);
		for (Coalesce coalesce : coalesces) {
			xml.append("      <field>").append(Const.CR);
			xml.append("        ").append(XMLHandler.addTagValue(TAG_OUTPUT_FIELD, coalesce.getName()));
			xml.append("        ").append(
					XMLHandler.addTagValue(TAG_VALUE_TYPE, ValueMetaFactory.getValueMetaName(coalesce.getType())));
			xml.append("        ")
					.append(XMLHandler.addTagValue(TAG_REMOVE, getStringFromBoolean(coalesce.isRemoveInputFields())));

			for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
				xml.append("        ").append(XMLHandler.addTagValue(getInputFieldTag(j), coalesce.getInputField(j)));
			}
			xml.append("      </field>").append(Const.CR);
		}
		xml.append("    </fields>").append(Const.CR);

		return xml.toString();
	}

	@Override
	public void loadXML(Node stepNode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {

		try {
			this.emptyStringsAsNulls = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepNode, TAG_EMPTY_IS_NULL));

			Node fields = XMLHandler.getSubNode(stepNode, "fields");
			int count = XMLHandler.countNodes(fields, "field");
			coalesces = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				Node line = XMLHandler.getSubNodeByNr(fields, "field", i);

				Coalesce coalesce = new Coalesce();
				coalesce.setName(Const.NVL(XMLHandler.getTagValue(line, TAG_OUTPUT_FIELD), ""));
				coalesce.setType(XMLHandler.getTagValue(line, TAG_VALUE_TYPE));
				coalesce.setRemoveInputFields(getBooleanFromString(XMLHandler.getTagValue(line, TAG_REMOVE)));
				for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
					coalesce.setInputField(j, Const.NVL(XMLHandler.getTagValue(line, getInputFieldTag(j)), ""));
				}

				coalesces.add(coalesce);
			}
		} catch (Exception e) {
			throw new KettleXMLException(
					BaseMessages.getString(PKG, "CoalesceMeta.Exception.UnableToReadStepInfoFromXML"), e);
		}

	}

	@Override
	public void saveRep(Repository repository, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
			throws KettleException {
		try {
			repository.saveStepAttribute(id_transformation, id_step, TAG_EMPTY_IS_NULL, emptyStringsAsNulls);

			for (int i = 0; i < this.coalesces.size(); i++) {
				Coalesce coalesce = this.coalesces.get(i);
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_OUTPUT_FIELD, coalesce.getName());
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_VALUE_TYPE,
						ValueMetaFactory.getValueMetaName(coalesce.getType()));
				repository.saveStepAttribute(id_transformation, id_step, i, TAG_REMOVE, coalesce.isRemoveInputFields());

				for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
					repository.saveStepAttribute(id_transformation, id_step, i, getInputFieldTag(j),
							coalesce.getInputField(j));
				}
			}
		} catch (Exception e) {
			throw new KettleException(
					BaseMessages.getString(PKG, "CoalesceMeta.Exception.UnableToSaveRepository", id_step), e);
		}
	}

	@Override
	public void readRep(Repository repository, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
			throws KettleException {
		try {
			this.emptyStringsAsNulls = repository.getStepAttributeBoolean(id_step, TAG_EMPTY_IS_NULL);

			int count = repository.countNrStepAttributes(id_step, TAG_OUTPUT_FIELD);

			coalesces = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {

				Coalesce coalesce = new Coalesce();
				coalesce.setName(repository.getStepAttributeString(id_step, i, TAG_OUTPUT_FIELD));
				coalesce.setType(repository.getStepAttributeString(id_step, i, TAG_VALUE_TYPE));
				coalesce.setRemoveInputFields(repository.getStepAttributeBoolean(id_step, i, TAG_REMOVE));

				for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
					String name = repository.getStepAttributeString(id_step, i, getInputFieldTag(j));
					if (!Utils.isEmpty(name))
						coalesce.setInputField(j, name);
				}

				coalesces.add(coalesce);
			}
		} catch (Exception e) {

			throw new KettleException(
					BaseMessages.getString(PKG, "CoalesceMeta.Exception.UnableToReadRepository", id_step), e);
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
				if (coalesce.isRemoveInputFields()) {

					String outputFieldName = space.environmentSubstitute(coalesce.getName());

					for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {

						String inputFieldName = coalesce.getInputField(j);

						// If input field name is recyled for output, don't
						// remove
						if (inputRowMeta.indexOfValue(outputFieldName) != -1 && outputFieldName.equals(inputFieldName))
							continue;

						if (inputRowMeta.indexOfValue(inputFieldName) != -1) {
							inputRowMeta.removeValueMeta(inputFieldName);
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

				String outputFieldName = space.environmentSubstitute(coalesce.getName());
				ValueMetaInterface valueMeta = ValueMetaFactory.createValueMeta(outputFieldName, type);
				valueMeta.setOrigin(stepName);

				int index = inputRowMeta.indexOfValue(outputFieldName);
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
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.NotReceivingFieldsFromPreviousSteps"),
					stepMeta));
		} else {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG,
					"CoalesceMeta.CheckResult.ReceivingFieldsFromPreviousSteps", prev.size()), stepMeta));
		}

		// See if there are input streams leading to this step!
		if (input.length > 0) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.ReceivingInfoFromOtherSteps"), stepMeta));
		} else {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.NotReceivingInfoFromOtherSteps"), stepMeta));
		}

		// See if there are missing, duplicate or not enough input streams
		boolean missing = false;
		for (Coalesce coalesce : this.getCoalesces()) {

			Set<String> fields = new HashSet<String>();
			List<String> missingFields = new ArrayList<String>();
			List<String> duplicateFields = new ArrayList<String>();

			for (int j = 0; j < Coalesce.MAX_INPUT_FIELD; j++) {
				String fieldName = coalesce.getInputField(j);

				if (!Utils.isEmpty(fieldName)) {

					if (fields.contains(fieldName))
						duplicateFields.add(fieldName);
					else
						fields.add(fieldName);

					ValueMetaInterface vmi = prev.searchValueMeta(fieldName);
					if (vmi == null) {
						missingFields.add(fieldName);
					}
				}
			}

			if (!missingFields.isEmpty()) {
				String message = BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.MissingInStreamFields",
						coalesce.getName(), StringUtils.join(missingFields, ','));
				remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, message, stepMeta));
				missing = true;
			} else if (!duplicateFields.isEmpty()) {
				String message = BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.DuplicateInStreamFields",
						coalesce.getName(), StringUtils.join(duplicateFields, ','));
				remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, message, stepMeta));
				missing = true;
			} else if (fields.size() <= 1) {
				String message = BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.NotEnoughInStreamFields",
						coalesce.getName());
				remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING, message, stepMeta));
			}

		}

		// See if there something to coalesce
		if (this.getCoalesces().isEmpty()) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.EmptyInStreamFields"), stepMeta));
		} else if (!missing) {
			remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK,
					BaseMessages.getString(PKG, "CoalesceMeta.CheckResult.FoundInStreamFields"), stepMeta));
		}

	}

	// TODO: Find a global function
	public static String getStringFromBoolean(boolean b) {
		return b ? BaseMessages.getString(PKG, "System.Combo.Yes") : BaseMessages.getString(PKG, "System.Combo.No");
	}

	// TODO: Find a global function
	public static boolean getBooleanFromString(final String s) {

		if (Utils.isEmpty(s))
			return false;

		return BaseMessages.getString(PKG, "System.Combo.Yes").equals(s);
	}

	/**
	 * If all 3 fields are of the same data type then the output field should
	 * mirror this otherwise return a more generic String type
	 */
	private int findDefaultValueType(final RowMetaInterface inputRowMeta, final Coalesce coalesce) throws Exception {

		Integer valueType = null;
		int i = 0;
		do {
			if (i == 0) {
				valueType = getInputFieldValueType(inputRowMeta, coalesce, i++);
			}
			Integer type = getInputFieldValueType(inputRowMeta, coalesce, i);

			if ((valueType = getResultingType(valueType, type)) == STRING_AS_DEFAULT) {
				return ValueMetaInterface.TYPE_STRING;
			}
		} while (++i < Coalesce.MAX_INPUT_FIELD);

		return valueType;
	}

	/**
	 * extracts the ValueMeta type of an input field, returns null if the field
	 * is not present in the input stream
	 */
	private Integer getInputFieldValueType(RowMetaInterface inputRowMeta, Coalesce coalesce, int inputIndex) {
		int index = inputRowMeta.indexOfValue(coalesce.getInputField(inputIndex));
		if (index > 0) {
			return inputRowMeta.getValueMeta(index).getType();
		}
		return null;
	}

	private Integer getResultingType(Integer typeA, Integer typeB) {
		if (typeA == null) {
			return typeB;
		} else {
			if (typeB == null) {
				return typeA;
			}
			return typeA.equals(typeB) ? typeA : STRING_AS_DEFAULT;
		}
	}

	private String getInputFieldTag(int index) {
		return "input_field_" + (char) ('a' + index);
	}

	public Coalesce getCoalesce(final String name) {
		if (name != null) {
			for (Coalesce coalesce : this.getCoalesces()) {
				if (name.equals(coalesce.getName()))
					return coalesce;

			}
		}
		return null;
	}

	public List<Coalesce> getCoalesces() {
		return coalesces;
	}

	public void setCoalesces(List<Coalesce> coalesces) {		
		this.coalesces =  ( coalesces==null) ? Collections.emptyList():coalesces;
	}
}