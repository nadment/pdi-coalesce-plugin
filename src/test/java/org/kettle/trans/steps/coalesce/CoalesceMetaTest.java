package org.kettle.trans.steps.coalesce;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.kettle.trans.steps.coalesce.CoalesceMeta;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.junit.rules.RestorePDIEngineEnvironment;
import org.pentaho.di.trans.steps.loadsave.LoadSaveTester;

public class CoalesceMetaTest  {
	  @ClassRule public static RestorePDIEngineEnvironment env = new RestorePDIEngineEnvironment();

	  @BeforeClass
	  public static void setUpBeforeClass() throws KettleException {
	    KettleEnvironment.init( false );
	  }

	  @Test
	  public void testLoadSave() throws KettleException {
	    List<String> attributes = Arrays.asList( "TreatEmptyStringsAsNulls" );

	    LoadSaveTester<CoalesceMeta> loadSaveTester =
	      new LoadSaveTester<CoalesceMeta>( CoalesceMeta.class, attributes );

	    loadSaveTester.testSerialization();
	}

//	  @Test
//	  public void testSerialization() throws KettleException {
//	    List<String> attributes =
//	      Arrays.asList( "TreatEmptyStringsAsNulls" );
//
//	    Map<String, String> getterMap = new HashMap<String, String>();
//	    Map<String, String> setterMap = new HashMap<String, String>();
//	    getterMap.put( "CheckSumType", "getTypeByDesc" );
//
//	    FieldLoadSaveValidator<String[]> stringArrayLoadSaveValidator =
//	      new ArrayLoadSaveValidator<String>( new StringLoadSaveValidator(), 5 );
//
//	    Map<String, FieldLoadSaveValidator<?>> attrValidatorMap = new HashMap<String, FieldLoadSaveValidator<?>>();
////	    attrValidatorMap.put( "FieldName", stringArrayLoadSaveValidator );
////	    attrValidatorMap.put( "CheckSumType", new IntLoadSaveValidator( CheckSumMeta.checksumtypeCodes.length ) );
////	    attrValidatorMap.put( "ResultType", new IntLoadSaveValidator( CheckSumMeta.resultTypeCode.length ) );
//
//	    Map<String, FieldLoadSaveValidator<?>> typeValidatorMap = new HashMap<String, FieldLoadSaveValidator<?>>();
//
//	    LoadSaveTester<CoalesceMeta> loadSaveTester =
//	      new LoadSaveTester<>( CoalesceMeta.class, attributes, getterMap, setterMap,
//	        attrValidatorMap, typeValidatorMap, this );
//
//	    loadSaveTester.testSerialization();
//	}
}
