package org.nem.core.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicTransferFeeCalculator;

public class TransactionExecutionStateTest {

	@Test
	public void canCreateState() {
		// Arrange:
		final MosaicTransferFeeCalculator calculator = mosaic -> null;

		// Act:
		final TransactionExecutionState state = new TransactionExecutionState(calculator);

		// Assert:
		MatcherAssert.assertThat(state.getMosaicTransferFeeCalculator(), IsSame.sameInstance(calculator));
	}
}
