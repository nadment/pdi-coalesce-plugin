package org.kettle.trans.steps.coalesce;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * This class is part of the demo step plug-in implementation. It demonstrates
 * the basics of developing a plug-in step for PDI.
 *
 * The demo step adds a new string field to the row stream and sets its value to
 * "Hello World!". The user may select the name of the new field.
 *
 * This class is the implementation of StepDataInterface.
 *
 * Implementing classes inherit from BaseStepData, which implements the entire
 * interface completely.
 *
 * In addition classes implementing this interface usually keep track of
 * per-thread resources during step execution. Typical examples are: result
 * sets, temporary data, caching indexes, etc.
 *
 * The implementation for the demo step stores the output row structure in the
 * data class.
 *
 */
public class CoalesceData extends BaseStepData implements StepDataInterface {

	protected RowMetaInterface outputRowMeta;

	public CoalesceData() {
		super();
	}

}