package org.nem.nis.controller.viewmodels;

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

import java.util.*;
import java.util.stream.Collectors;

public class ExpiredMosaicViewModelTest {
	@Test
	public void canSerializeWithoutBalances() {
		// Arrange:
		final ExpiredMosaicViewModel viewModel = new ExpiredMosaicViewModel(
			new MosaicId(new NamespaceId("alice"), "tokens"),
			new MosaicBalances());

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(((JSONObject) jsonObject.get("mosaicId")).get("namespaceId"), IsEqual.equalTo("alice"));
		MatcherAssert.assertThat(((JSONObject) jsonObject.get("mosaicId")).get("name"), IsEqual.equalTo("tokens"));
		MatcherAssert.assertThat(((JSONArray) jsonObject.get("balances")).size(), IsEqual.equalTo(0));
	}

	@Test
	public void canSerializeWithBalances() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();

		final MosaicBalances balances = new MosaicBalances();
		balances.incrementBalance(address1, new Quantity(111));
		balances.incrementBalance(address2, new Quantity(333));
		balances.incrementBalance(address3, new Quantity(222));

		final ExpiredMosaicViewModel viewModel = new ExpiredMosaicViewModel(new MosaicId(new NamespaceId("alice"), "tokens"), balances);

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(((JSONObject) jsonObject.get("mosaicId")).get("namespaceId"), IsEqual.equalTo("alice"));
		MatcherAssert.assertThat(((JSONObject) jsonObject.get("mosaicId")).get("name"), IsEqual.equalTo("tokens"));

		JSONArray jsonBalances = (JSONArray) jsonObject.get("balances");
		MatcherAssert.assertThat(jsonBalances.size(), IsEqual.equalTo(3));

		jsonBalances.forEach(obj -> {
			MatcherAssert.assertThat(((JSONObject) obj).size(), IsEqual.equalTo(2));
		});

		// - balances array is unordered, so read it into a map for further inspection
		final Map<Address, Quantity> balanceMap = jsonBalances.stream()
				.map(obj -> (JSONObject) obj)
				.collect(Collectors.toMap(
					jsonBalance -> Address.fromEncoded((String) jsonBalance.get("address")),
					jsonBalance -> new Quantity((Long) jsonBalance.get("quantity"))
			));

		MatcherAssert.assertThat(balanceMap.keySet(), IsEquivalent.equivalentTo(Arrays.asList(address1, address2, address3)));
		MatcherAssert.assertThat(balanceMap.get(address1), IsEqual.equalTo(new Quantity(111)));
		MatcherAssert.assertThat(balanceMap.get(address2), IsEqual.equalTo(new Quantity(333)));
		MatcherAssert.assertThat(balanceMap.get(address3), IsEqual.equalTo(new Quantity(222)));
	}
}
