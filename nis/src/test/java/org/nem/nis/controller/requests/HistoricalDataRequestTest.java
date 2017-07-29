package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;

public class HistoricalDataRequestTest {
	@Test
	public void canCreateHistoricalDataRequestFromValidParameters() {
		// Act:
		final HistoricalDataRequest request = new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(20), 5L);

		// Assert:
		Assert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(20)));
		Assert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void canCreateHistoricalDataRequestWithEndHeightEqualToStartHeight() {
		// Act:
		final HistoricalDataRequest request = new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(10), 5L);

		// Assert:
		Assert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithNullStartHeight() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new HistoricalDataRequest(null, new BlockHeight(20), 5L), NullPointerException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithNullEndHeight() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new HistoricalDataRequest(new BlockHeight(10), null, 5L),
				NullPointerException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithNullIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(20), null),
				NullPointerException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithStartHeightLargerThanEndHeight() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(9), 5L),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithZeroIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(20), 0L),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithNegativeIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(20), -1L),
				IllegalArgumentException.class);
	}
}
