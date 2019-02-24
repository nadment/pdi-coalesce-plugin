package org.kettle.trans.steps.coalesce;

import org.junit.Before;
import org.junit.Test;
import org.kettle.trans.steps.coalesce.CoalesceMeta;
import org.pentaho.di.core.injection.BaseMetadataInjectionTest;

public class CoalesceMetaInjectionTest extends BaseMetadataInjectionTest<CoalesceMeta> {
	@Before
	public void setup() {
		setup(new CoalesceMeta());
	}

	@Test
	public void test() throws Exception {

		check("EMPTY_STRING_AS_NULLS", new BooleanGetter() {
			@Override
			public boolean get() {
				return meta.isTreatEmptyStringsAsNulls();
			}
		});

		//TODO: REMOVE_INPUT_FIELDS
		
//		check("NAME", new StringGetter() {
//			@Override
//			public String get() {
//				return meta.getCoalesces()[0].getName();
//			}
//		});
	}
}
