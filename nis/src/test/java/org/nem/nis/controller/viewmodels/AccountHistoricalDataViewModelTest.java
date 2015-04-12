package org.nem.nis.controller.viewmodels;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;

public class AccountHistoricalDataViewModelTest {

	@Test
	public void canCreateAccountHistoricalDataViewModel() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountHistoricalDataViewModel viewModel = new AccountHistoricalDataViewModel(
				new BlockHeight(123),
				address,
				Amount.fromNem(234),
				Amount.fromNem(345),
				Amount.fromNem(456),
				0.567,
				0.678);

		// Assert:
		Assert.assertThat(viewModel.getHeight(), IsEqual.equalTo(new BlockHeight(123L)));
		Assert.assertThat(viewModel.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(viewModel.getBalance(), IsEqual.equalTo(Amount.fromNem(234)));
		Assert.assertThat(viewModel.getVestedBalance(), IsEqual.equalTo(Amount.fromNem(345)));
		Assert.assertThat(viewModel.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(456)));
		Assert.assertThat(viewModel.getImportance(), IsEqual.equalTo(0.567));
		Assert.assertThat(viewModel.getPageRank(), IsEqual.equalTo(0.678));
	}

	@Test
	public void canSerializeViewModel() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountHistoricalDataViewModel viewModel = new AccountHistoricalDataViewModel(
				new BlockHeight(123),
				address,
				Amount.fromNem(234),
				Amount.fromNem(345),
				Amount.fromNem(456),
				0.567,
				0.678);

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(7));
		Assert.assertThat(jsonObject.get("height"), IsEqual.equalTo(123L));
		Assert.assertThat(jsonObject.get("address"), IsEqual.equalTo(address.toString()));
		Assert.assertThat(jsonObject.get("balance"), IsEqual.equalTo(234000000L));
		Assert.assertThat(jsonObject.get("vestedBalance"), IsEqual.equalTo(345000000L));
		Assert.assertThat(jsonObject.get("unvestedBalance"), IsEqual.equalTo(456000000L));
		Assert.assertThat(jsonObject.get("importance"), IsEqual.equalTo(0.567));
		Assert.assertThat(jsonObject.get("pageRank"), IsEqual.equalTo(0.678));
	}
}
