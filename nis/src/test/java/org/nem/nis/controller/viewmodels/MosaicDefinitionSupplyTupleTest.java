package org.nem.nis.controller.viewmodels;

import java.util.*;
import net.minidev.json.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.state.*;

public class MosaicDefinitionSupplyTupleTest {
	@Test
	public void canCreate() {
		// Act:
		final MosaicDefinitionSupplyTuple tuple = new MosaicDefinitionSupplyTuple(Utils.createMosaicDefinition("foo", "bar"),
				new Supply(123), new BlockHeight(999));

		// Assert:
		MatcherAssert.assertThat(Utils.createMosaicId("foo", "bar"), IsEqual.equalTo(tuple.getMosaicDefinition().getId()));
		MatcherAssert.assertThat(new Supply(123), IsEqual.equalTo(tuple.getSupply()));
		MatcherAssert.assertThat(new BlockHeight(999), IsEqual.equalTo(tuple.getExpirationHeight()));
	}

	@Test
	public void canSerialize() {
		// Act:
		final MosaicDefinitionSupplyTuple tuple = new MosaicDefinitionSupplyTuple(Utils.createMosaicDefinition("foo", "bar"),
				new Supply(123), new BlockHeight(999));

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(tuple);

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(3));

		final JSONObject jsonMosaicId = (JSONObject) ((JSONObject) jsonObject.get("mosaicDefinition")).get("id");
		MatcherAssert.assertThat(jsonMosaicId.get("namespaceId"), IsEqual.equalTo("foo"));
		MatcherAssert.assertThat(jsonMosaicId.get("name"), IsEqual.equalTo("bar"));

		MatcherAssert.assertThat((Long) jsonObject.get("supply"), IsEqual.equalTo(123L));

		MatcherAssert.assertThat((Long) jsonObject.get("expirationHeight"), IsEqual.equalTo(999L));
	}
}
