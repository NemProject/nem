package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

import static org.nem.nis.controller.requests.HistoricalDataRequest.MAX_DATA_POINTS;

public class AccountBatchHistoricalDataRequestTest {

	private static JSONObject createValidJsonObject(final Collection<SerializableAccountId> accountIds, long startHeight, long endHeight,
			long increment) {
		final JsonSerializer serializer = new JsonSerializer();
		serializer.writeObjectArray("accounts", accountIds);
		BlockHeight.writeTo(serializer, "startHeight", new BlockHeight(startHeight));
		BlockHeight.writeTo(serializer, "endHeight", new BlockHeight(endHeight));
		serializer.writeLong("incrementBy", increment);

		return serializer.getObject();
	}

	@Test
	public void canCreateAccountBatchHistoricalDataRequestFromValidDeserializer() {
		// Arrange:
		final Collection<SerializableAccountId> accountIds = Arrays.asList(new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress()), new SerializableAccountId(Utils.generateRandomAddress()));

		final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 10L, 20L, 5L), null);

		// Act:
		final AccountBatchHistoricalDataRequest request = new AccountBatchHistoricalDataRequest(deserializer);

		// Assert:
		MatcherAssert.assertThat(request.getAccountIds(), IsEquivalent.equivalentTo(accountIds));
		MatcherAssert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		MatcherAssert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(20)));
		MatcherAssert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void canCreateAccountBatchHistoricalDataRequestForRequestingMaxDataPoints() {
		// Arrange:
		final long endHeight = MAX_DATA_POINTS / 2 + 1;
		final Collection<SerializableAccountId> accountIds = Arrays.asList(new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress()));

		final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 1L, endHeight, 1L), null);

		// Act: 2 * 5000 = MAX_DATA_POINTS
		final AccountBatchHistoricalDataRequest request = new AccountBatchHistoricalDataRequest(deserializer);

		// Assert:
		MatcherAssert.assertThat(request.getAccountIds(), IsEquivalent.equivalentTo(accountIds));
		MatcherAssert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(1)));
		MatcherAssert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(endHeight)));
		MatcherAssert.assertThat(request.getIncrement(), IsEqual.equalTo(1L));
	}

	@Test
	public void cannotCreateAccountBatchHistoricalDataRequestWithMissingRequiredParameter() {
		for (final String missingPropertyName : Arrays.asList("accounts", "startHeight", "endHeight", "incrementBy")) {
			assertCannotCreateAccountBatchHistoricalDataRequestWhenPropertyIsMissing(missingPropertyName);
		}
	}

	private static void assertCannotCreateAccountBatchHistoricalDataRequestWhenPropertyIsMissing(final String missingPropertyName) {
		// Arrange:
		final Collection<SerializableAccountId> accountIds = Arrays.asList(new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress()), new SerializableAccountId(Utils.generateRandomAddress()));

		final JSONObject jsonObject = createValidJsonObject(accountIds, 10L, 20L, 5L);
		jsonObject.remove(missingPropertyName);
		final Deserializer deserializer = new JsonDeserializer(jsonObject, null);

		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountBatchHistoricalDataRequest(deserializer), MissingRequiredPropertyException.class);
	}

	@Test
	public void cannotCreateAccountBatchHistoricalDataRequestForTooManyDataPoints() {
		// Arrange:
		final Collection<SerializableAccountId> accountIds = Arrays.asList(new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress()), new SerializableAccountId(Utils.generateRandomAddress()));

		final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 1L, 4000L, 1L), null);

		// Assert: 3 * 4000 > MAX_DATA_POINTS
		ExceptionAssert.assertThrows(v -> new AccountBatchHistoricalDataRequest(deserializer), IllegalArgumentException.class);
	}
}
