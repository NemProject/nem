package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.test.ExceptionAssert;

import static org.nem.nis.controller.requests.HistoricalDataRequest.MAX_DATA_POINTS;

public class HistoricalDataRequestTest {
	private static JSONObject createValidJsonObject(long startHeight, long endHeight, long increment) {
		final JsonSerializer serializer = new JsonSerializer();
		BlockHeight.writeTo(serializer, "startHeight", new BlockHeight(startHeight));
		BlockHeight.writeTo(serializer, "endHeight", new BlockHeight(endHeight));
		serializer.writeLong("incrementBy", increment);

		return serializer.getObject();
	}

	@Test
	public void canCreateHistoricalDataRequestFromValidParameters() {
		// Act:
		final HistoricalDataRequest request = new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(20), 5L);

		// Assert:
		MatcherAssert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		MatcherAssert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(20)));
		MatcherAssert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void canCreateHistoricalDataRequestFromValidDeserializer() {
		// Arrange:
		final JSONObject jsonObject = createValidJsonObject(10L, 20L, 5L);

		// Act:
		final HistoricalDataRequest request = new HistoricalDataRequest(new JsonDeserializer(jsonObject, null));

		// Assert:
		MatcherAssert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		MatcherAssert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(20)));
		MatcherAssert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void canCreateHistoricalDataRequestWithEndHeightEqualToStartHeight() {
		// Act:
		final HistoricalDataRequest request = new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(10), 5L);

		// Assert:
		MatcherAssert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		MatcherAssert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(10)));
		MatcherAssert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void canCreateHistoricalDataRequestWithMaxDataPoints() {
		// Act:
		final long endHeight = MAX_DATA_POINTS + 1;
		final HistoricalDataRequest request = new HistoricalDataRequest(new BlockHeight(1), new BlockHeight(endHeight), 1L);

		// Assert:
		MatcherAssert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(1)));
		MatcherAssert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(endHeight)));
		MatcherAssert.assertThat(request.getIncrement(), IsEqual.equalTo(1L));
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithNullStartHeight() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new HistoricalDataRequest(null, new BlockHeight(20), 5L), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithNullEndHeight() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new HistoricalDataRequest(new BlockHeight(10), null, 5L), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithNullIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(20), null),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithStartHeightLargerThanEndHeight() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(9), 5L),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithZeroIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(20), 0L),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithNegativeIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new HistoricalDataRequest(new BlockHeight(10), new BlockHeight(20), -1L),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateHistoricalDataRequestWithTooManyDataPoints() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new HistoricalDataRequest(new BlockHeight(1), new BlockHeight(MAX_DATA_POINTS + 2), 1L),
				IllegalArgumentException.class);
	}
}
