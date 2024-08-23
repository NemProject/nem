package org.nem.nis.dbmodel;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.Supply;

import java.util.*;

public class DbMosaicDefinitionSupplyPairTest {

	@Test
	public void canCreatePair() {
		// Arrange:
		final DbMosaicDefinition mosaicDefinition = new DbMosaicDefinition();

		// Act:
		final DbMosaicDefinitionSupplyPair pair = new DbMosaicDefinitionSupplyPair(mosaicDefinition, new Supply(123L));

		// Assert:
		MatcherAssert.assertThat(pair.getMosaicDefinition(), IsEqual.equalTo(mosaicDefinition));
		MatcherAssert.assertThat(pair.getSupply(), IsEqual.equalTo(new Supply(123L)));
	}
}
