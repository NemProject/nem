package org.nem.nis.dbmodel;

import java.util.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;

public class DbMosaicDefinitionSupplyTupleTest {

	@Test
	public void canCreateTuple() {
		// Arrange:
		final DbMosaicDefinition mosaicDefinition = new DbMosaicDefinition();

		// Act:
		final DbMosaicDefinitionSupplyTuple tuple = new DbMosaicDefinitionSupplyTuple(mosaicDefinition, new Supply(123L),
				new BlockHeight(999L));

		// Assert:
		MatcherAssert.assertThat(tuple.getMosaicDefinition(), IsEqual.equalTo(mosaicDefinition));
		MatcherAssert.assertThat(tuple.getSupply(), IsEqual.equalTo(new Supply(123L)));
		MatcherAssert.assertThat(tuple.getExpirationHeight(), IsEqual.equalTo(new BlockHeight(999L)));
	}
}
