/*******************************************************************************
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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * The Coalesce Transformation step selects the first non null value from a
 * group of input fields and passes it down the stream or returns null if all
 * the fields are null.
 * 
 * @author Nicolas ADMENT
 * @since 18-mai-2016
 *
 */

public class CoalesceStep extends BaseStep implements StepInterface {

	private static final Class<?> PKG = CoalesceMeta.class;

	public CoalesceStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
			Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	@Override
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		// Casting to step-specific implementation classes is safe
		CoalesceMeta meta = (CoalesceMeta) smi;
		CoalesceData data = (CoalesceData) sdi;

		first = true;

		return super.init(meta, data);
	}

	@Override
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

		// safely cast the step settings (meta) and runtime info (data) to
		// specific implementations
		CoalesceMeta meta = (CoalesceMeta) smi;
		CoalesceData data = (CoalesceData) sdi;

		// get incoming row, getRow() potentially blocks waiting for more rows,
		// returns null if no more rows expected
		Object[] row = getRow();

		// if no more rows are expected, indicate step is finished and
		// processRow() should not be called again
		if (row == null) {
			setOutputDone();
			return false;
		}

		// the "first" flag is inherited from the base step implementation
		// it is used to guard some processing tasks, like figuring out field
		// indexes
		// in the row structure that only need to be done once
		if (first) {
			if (log.isDebug()) {
				logDebug(BaseMessages.getString(PKG, "CoalesceStep.Log.StartedProcessing")); //$NON-NLS-1$
			}

			first = false;
			// clone the input row structure and place it in our data object
			data.outputRowMeta = getInputRowMeta().clone();
			// use meta.getFields() to change it, so it reflects the output row
			// structure
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this, null, null);

			checkInputFieldsExist(meta);
		}

		RowMetaInterface inputRowMeta = getInputRowMeta();

		// Create a new output row
		Object[] outputRowValues = new Object[data.outputRowMeta.size()];

		// Checks if fields from the input stream are present in the output and
		// if so passes down the values
		int outputIndex = 0;
		for (int inputIndex = 0; inputIndex < inputRowMeta.size(); inputIndex++) {
			ValueMetaInterface vm = inputRowMeta.getValueMeta(inputIndex);

			if (data.outputRowMeta.indexOfValue(vm.getName()) == -1)
				continue;

			outputRowValues[outputIndex++] = row[inputIndex];
		}

		// Calculates the coalesce value for each extra output field and also
		// converts its value to reflect the Value Type option,
		// or in case it was None to reflect on the default data type logic.
		for (Coalesce coalesce : meta.getCoalesces()) {

			int inputIndex = getFirstNonNullValueIndex(meta, inputRowMeta, row, coalesce);

			// Resolve variable name
			String name = this.environmentSubstitute(coalesce.getName());		
			outputIndex = data.outputRowMeta.indexOfValue(name);

			ValueMetaInterface vm = data.outputRowMeta.getValueMeta(outputIndex);
			try {
				Object result = null;
				if (inputIndex >= 0) {
					result = vm.convertData(inputRowMeta.getValueMeta(inputIndex), row[inputIndex]);
				}
				outputRowValues[outputIndex++] = result;
			} catch (KettleValueException e) {
				logError(BaseMessages.getString(PKG, "CoalesceStep.Log.DataIncompatibleError", //$NON-NLS-1$
						row[inputIndex].toString(), inputRowMeta.getValueMeta(inputIndex).toString(), vm.toString()));
				throw e;
			}
		}

		// put the row to the output row stream
		putRow(data.outputRowMeta, outputRowValues);

		if (log.isRowLevel()) {
			logRowlevel(BaseMessages.getString(PKG, "CoalesceStep.Log.WroteRowToNextStep", outputRowValues)); //$NON-NLS-1$
		}

		// log progress if it is time to to so
		if (checkFeedback(getLinesRead())) {
			logBasic("Line nr " + getLinesRead()); // Some basic logging
		}

		// indicate that processRow() should be called again
		return true;
	}

	private void checkInputFieldsExist(final CoalesceMeta meta) throws KettleException {
		RowMetaInterface prev = getInputRowMeta();

		for (Coalesce coalesce : meta.getCoalesces()) {
			List<String> missingFields = new ArrayList<String>();

			for (String field : coalesce.getInputFields()) {

				if (!Utils.isEmpty(field)) {
					ValueMetaInterface vmi = prev.searchValueMeta(field);
					if (vmi == null) {
						missingFields.add(field);
					}
				}
			}
			if (!missingFields.isEmpty()) {
				String errorText = BaseMessages.getString(PKG, "CoalesceStep.Log.MissingInStreamFields", //$NON-NLS-1$
						StringUtils.join(missingFields, ','));
				throw new KettleException(errorText);
			}
		}
	}

	/**
	 * The actual coalesce logic, returns the index of the first non null value
	 */
	private int getFirstNonNullValueIndex(final CoalesceMeta meta, final RowMetaInterface inputRowMeta, Object[] row,
			Coalesce coalesce) {

		for (String field : coalesce.getInputFields()) {

			int index = inputRowMeta.indexOfValue(field);
			if (index >= 0) {
				if (!meta.isTreatEmptyStringsAsNulls() && row[index] != null) {
					return index;
				} else if (meta.isTreatEmptyStringsAsNulls() && row[index] != null
						&& !Utils.isEmpty(row[index].toString())) {
					return index;
				}
			}
		}

		// signifies a null value
		return -1;
	}

	/**
	 * This method is called by PDI once the step is done processing.
	 *
	 * The dispose() method is the counterpart to init() and should release any
	 * resources acquired for step execution like file handles or database
	 * connections.
	 */
	@Override
	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {

		// Casting to step-specific implementation classes is safe
		CoalesceMeta meta = (CoalesceMeta) smi;
		CoalesceData data = (CoalesceData) sdi;

		data.outputRowMeta = null;

		super.dispose(meta, data);
	}
}