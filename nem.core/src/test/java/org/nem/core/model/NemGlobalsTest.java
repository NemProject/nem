package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.test.Utils;

public class NemGlobalsTest {

	@After
	public void resetGlobals() {
		Utils.resetGlobals();
	}

	//region transaction fee calculator

	@Test
	public void defaultTransactionFeeCalculatorIsNotNull() {
		// Assert:
		Assert.assertThat(
				NemGlobals.getTransactionFeeCalculator(),
				IsInstanceOf.instanceOf(TransactionFeeCalculatorBeforeFork.class));
	}

	@Test
	public void defaultTransactionFeeCalculatorCanBeChanged() {
		// Arrange:
		final TransactionFeeCalculator calculator = new TransactionFeeCalculatorBeforeFork();

		// Act:
		NemGlobals.setTransactionFeeCalculator(calculator);

		// Assert:
		Assert.assertThat(
				NemGlobals.getTransactionFeeCalculator(),
				IsEqual.equalTo(calculator));
	}

	//endregion

	//region block chain configuration

	@Test
	public void defaultBlockChainConfigurationIsNotNull() {
		// Assert:
		Assert.assertThat(
				NemGlobals.getBlockChainConfiguration(),
				IsInstanceOf.instanceOf(BlockChainConfiguration.class));
	}

	@Test
	public void defaultBlockChainConfigurationCanBeChanged() {
		// Arrange:
		final BlockChainConfiguration configuration = Utils.createBlockChainConfiguration(1000, 100, 30, 200);

		// Act:
		NemGlobals.setBlockChainConfiguration(configuration);

		// Assert:
		Assert.assertThat(
				NemGlobals.getBlockChainConfiguration(),
				IsEqual.equalTo(configuration));
	}

	//endregion
}