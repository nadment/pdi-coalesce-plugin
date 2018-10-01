package org.pentaho.di.trans.steps.coalesce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.step.StepMeta;

public class CoalesceStepTest {
	private String[] fieldNames;
	private String[] fieldTypes;
	ValueMetaInterface[] valuesMeta;
	private List<List<Object>> inputRows;

	// @Test
	// public void testCoalesce() throws Exception {
	// processInputTestFile( "phone_numbers.txt" );
	//
	// List<RowMetaAndData> transformationResults = test( true,
	// ValueMetaInterface.TYPE_STRING );
	// List<RowMetaAndData> expectedResults = createExpectedResults( 1 );
	//
	//// try {
	//// TestUtilities.checkRows( transformationResults, expectedResults, 0 );
	//// } catch ( TestFailedException tfe ) {
	//// Assert.fail( tfe.getMessage() );
	//// }
	// }

	// @Test
	// public void testDefaultValueMeta() throws Exception {
	// processInputTestFile( "phone_numbers.txt" );
	//
	// List<RowMetaAndData> transformationResults = test( true,
	// ValueMetaInterface.TYPE_NONE );
	// List<RowMetaAndData> expectedResults = createExpectedResults( 1 );
	//
	//// try {
	//// TestUtilities.checkRows( transformationResults, expectedResults, 0 );
	//// } catch ( TestFailedException tfe ) {
	//// Assert.fail( tfe.getMessage() );
	//// }
	// }

	// @Test
	// public void testNumberFromString() throws Exception {
	// processInputTestFile( "average_temperatures.txt" );
	//
	// List<RowMetaAndData> transformationResults = test( false,
	// ValueMetaInterface.TYPE_NUMBER );
	// List<RowMetaAndData> expectedResults = createExpectedResults( 2 );
	//
	//// try {
	//// TestUtilities.checkRows( transformationResults, expectedResults, 0 );
	//// } catch ( TestFailedException tfe ) {
	//// Assert.fail( tfe.getMessage() );
	//// }
	// }

	// private List<RowMetaAndData> test( boolean remove, int valueType ) throws
	// KettleException {
	//
	// KettleEnvironment.init();
	//
	// // Create a new transformation
	// TransMeta transMeta = new TransMeta();
	// transMeta.setName( "testCoalesce" );
	// PluginRegistry registry = PluginRegistry.getInstance();
	//
	// // Create Injector
	// String injectorStepName = "injector step";
	// StepMeta injectorStep = TestUtilities.createInjectorStep(
	// injectorStepName, registry );
	// transMeta.addStep( injectorStep );
	//
	// // Create a Coalesce step
	// String coalesceStepName = "coalesce step";
	// StepMeta coalesceStep = createCoalesceMeta( coalesceStepName, registry,
	// remove, valueType );
	// transMeta.addStep( coalesceStep );
	//
	// // TransHopMeta between injector step and CoalesceStep
	// TransHopMeta injectorToCoalesceHop = new TransHopMeta( injectorStep,
	// coalesceStep );
	// transMeta.addTransHop( injectorToCoalesceHop );
	//
	// // Create a dummy step
	// String dummyStepName = "dummy step";
	// StepMeta dummyStep = TestUtilities.createDummyStep( dummyStepName,
	// registry );
	// transMeta.addStep( dummyStep );
	//
	// // TransHopMeta between CoalesceStep and DummyStep
	// TransHopMeta coalesceToDummyHop = new TransHopMeta( coalesceStep,
	// dummyStep );
	// transMeta.addTransHop( coalesceToDummyHop );
	//
	// // Execute the transformation
	// Trans trans = new Trans( transMeta );
	// trans.prepareExecution( null );
	//
	// // Create a row collector and add it to the dummy step interface
	// StepInterface si = trans.getStepInterface( dummyStepName, 0 );
	// RowStepCollector dummyRowCollector = new RowStepCollector();
	// si.addRowListener( dummyRowCollector );
	//
	// // Create a row producer
	// RowProducer rowProducer = trans.addRowProducer( injectorStepName, 0 );
	// trans.startThreads();
	//
	// // create the rows
	// List<RowMetaAndData> inputList = createInputData();
	// for ( RowMetaAndData rowMetaAndData : inputList ) {
	// rowProducer.putRow( rowMetaAndData.getRowMeta(), rowMetaAndData.getData()
	// );
	// }
	// rowProducer.finished();
	//
	// trans.waitUntilFinished();
	//
	// return dummyRowCollector.getRowsWritten();
	// }

	private StepMeta createCoalesceMeta(String name, PluginRegistry registry, boolean removeInputFields,
			int valueType) {

		CoalesceMeta coalesceMeta = new CoalesceMeta();

		Coalesce coalesce = new Coalesce();
		coalesce.setType(valueType);
		coalesce.setName("out");
		coalesce.setRemoveFields(removeInputFields);
		//coalesce.setInputFields(fieldNames);

		String pluginId = registry.getPluginId(StepPluginType.class, coalesceMeta);

		return new StepMeta(pluginId, name, coalesceMeta);
	}

	private List<RowMetaAndData> createInputData() {
		List<RowMetaAndData> list = new ArrayList<RowMetaAndData>();
		RowMetaInterface rowMeta = createRowMetaInterface(valuesMeta);

		for (List<Object> r : inputRows) {
			list.add(new RowMetaAndData(rowMeta, r.toArray()));
		}
		return list;
	}

	private RowMetaInterface createRowMetaInterface(ValueMetaInterface[] valuesMeta) {
		RowMetaInterface rowMeta = new RowMeta();
		for (ValueMetaInterface aValuesMeta : valuesMeta) {
			rowMeta.addValueMeta(aValuesMeta);
		}

		return rowMeta;
	}

//	private void processInputTestFile(String file) throws Exception {
//		BufferedReader reader = new BufferedReader(
//				new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(file)));
//
//		fieldNames = reader.readLine().split(",");
//		fieldTypes = reader.readLine().split(",");
//		valuesMeta = new ValueMetaInterface[Coalesce.MAX_INPUT_FIELD];
//		for (int i = 0; i < fieldNames.length; i++) {
//			valuesMeta[i] = ValueMetaFactory.createValueMeta(fieldNames[i], Integer.parseInt(fieldTypes[i]));
//		}
//
//		inputRows = new ArrayList<List<Object>>();
//		String line;
//		ValueMetaInterface stringValueMeta = new ValueMetaString("forConversionOnly");
//		while ((line = reader.readLine()) != null) {
//			String[] stringValues = parseValuesFromStringRow(line);
//			List<Object> objectValues = new ArrayList<Object>();
//			for (int i = 0; i < stringValues.length; i++) {
//				objectValues.add(valuesMeta[i].convertData(stringValueMeta, stringValues[i]));
//			}
//			inputRows.add(objectValues);
//		}
//
//		reader.close();
//	}

	/**
	 * Creates result data.
	 *
	 * @return list of metadata/data couples of how the result should look.
	 */
	private List<RowMetaAndData> createExpectedResults(int testCase) {
		List<RowMetaAndData> list = new ArrayList<RowMetaAndData>();
		List<ValueMetaInterface> valuesMeta = new ArrayList<ValueMetaInterface>();
		Object[][] resultRows = new Object[inputRows.size()][];

		switch (testCase) {
		case 1:
			valuesMeta.add(new ValueMetaString("out"));
			resultRows[0] = new Object[] { "248-0532" };
			resultRows[1] = new Object[] { "125-2044" };
			resultRows[2] = new Object[] { "216-9620" };
			resultRows[3] = new Object[] { null };
			break;

		case 2:
			valuesMeta.addAll(Arrays.asList(this.valuesMeta));
			valuesMeta.add(new ValueMetaNumber("temperature"));
			resultRows[0] = new Object[] { 10.5d, "6", 8d };
			resultRows[1] = new Object[] { null, "7.5", 9d };
			resultRows[2] = new Object[] { null, null, 10.5d };
			resultRows[3] = new Object[] { null, null, null };
			break;
		}

		RowMetaInterface rowMeta = createRowMetaInterface(
				valuesMeta.toArray(new ValueMetaInterface[valuesMeta.size()]));
		for (Object[] r : resultRows) {
			list.add(new RowMetaAndData(rowMeta, r));
		}

		return list;
	}

	private String[] parseValuesFromStringRow(String line) {
		String[] values = line.split(",");

		for (int i = 0; i < values.length; i++) {
			if (values[i].equals("\"\"")) {
				values[i] = "";
			} else if (values[i].isEmpty()) {
				values[i] = null;
			}
		}

		// for a ",," input String.split returns 0 size array
		if (values.length == 0) {
			return new String[line.length() + 1];
		}

		return values;
	}
}
