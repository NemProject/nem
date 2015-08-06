package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

public class NemGlobalsTest {

	@After
	public void resetGlobals() {
		NemGlobals.setTransactionFeeCalculator(null);
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
}