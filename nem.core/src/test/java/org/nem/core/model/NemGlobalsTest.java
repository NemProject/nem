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
				IsInstanceOf.instanceOf(DefaultTransactionFeeCalculator.class));
	}

	@Test
	public void defaultTransactionFeeCalculatorCanBeChanged() {
		// Arrange:
		final TransactionFeeCalculator calculator = new DefaultTransactionFeeCalculator();

		// Act:
		NemGlobals.setTransactionFeeCalculator(calculator);

		// Assert:
		Assert.assertThat(
				NemGlobals.getTransactionFeeCalculator(),
				IsEqual.equalTo(calculator));
	}

	//endregion

	//region mosaic transaction fee calculator

	@Test
	public void defaultMosaicTransactionFeeCalculatorIsNotNull() {
		// Assert:
		Assert.assertThat(
				NemGlobals.getMosaicTransferFeeCalculator(),
				IsInstanceOf.instanceOf(DefaultMosaicTransferFeeCalculator.class));
	}

	@Test
	public void defaultMosaicTransactionFeeCalculatorCanBeChanged() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = new DefaultMosaicTransferFeeCalculator();

		// Act:
		NemGlobals.setMosaicTransferFeeCalculator(calculator);

		// Assert:
		Assert.assertThat(
				NemGlobals.getMosaicTransferFeeCalculator(),
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
		final BlockChainConfiguration configuration = new BlockChainConfiguration(1000, 100, 30, 200);

		// Act:
		NemGlobals.setBlockChainConfiguration(configuration);

		// Assert:
		Assert.assertThat(
				NemGlobals.getBlockChainConfiguration(),
				IsEqual.equalTo(configuration));
	}

	//endregion
}