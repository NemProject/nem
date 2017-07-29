package org.nem.nis.controller.requests;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class AccountBatchHistoricalDataRequestTest {

	private static JSONObject createValidJsonObject(
			final Collection<SerializableAccountId> accountIds,
			long startHeight,
			long endHeight,
			long increment) {
		final JsonSerializer serializer = new JsonSerializer();
		serializer.writeObjectArray("accounts", accountIds);
		BlockHeight.writeTo(serializer, "startHeight", new BlockHeight(startHeight));
		BlockHeight.writeTo(serializer, "endHeight", new BlockHeight(endHeight));
		serializer.writeLong("incrementBy", increment);

		return serializer.getObject();
	}

	private static JSONObject createJsonObjectWithMissingProperty(
			final Collection<SerializableAccountId> accountIds,
			long startHeight,
			long endHeight,
			long increment,
			final String missingPropertyName) {
		final JSONObject jsonObject = createValidJsonObject(accountIds, startHeight, endHeight, increment);
		jsonObject.remove(missingPropertyName);
		return jsonObject;
	}

	@Test
	public void canCreateAccountBatchHistoricalDataRequestFromValidDeserializer() {
		// Arrange:
		final Collection<SerializableAccountId> accountIds = Arrays.asList(
				new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress())
		);

		final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 10L, 20L, 5L), null);

		// Act:
		final AccountBatchHistoricalDataRequest request = new AccountBatchHistoricalDataRequest(deserializer);

		// Assert:
		Assert.assertThat(request.getAccountIds(), IsEquivalent.equivalentTo(accountIds));
		Assert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(20)));
		Assert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void cannotCreateAccountBatchHistoricalDataRequestWithMissingRequiredParameter() {
		assertCannotCreateAccountBatchHistoricalDataRequestWhenPropertyIsMissing("accounts");
		assertCannotCreateAccountBatchHistoricalDataRequestWhenPropertyIsMissing("startHeight");
		assertCannotCreateAccountBatchHistoricalDataRequestWhenPropertyIsMissing("endHeight");
		assertCannotCreateAccountBatchHistoricalDataRequestWhenPropertyIsMissing("incrementBy");
	}

	private static void assertCannotCreateAccountBatchHistoricalDataRequestWhenPropertyIsMissing(final String missingPropertyName) {
		// Arrange:
		final Collection<SerializableAccountId> accountIds = Arrays.asList(
				new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress())
		);

		final Deserializer deserializer = new JsonDeserializer(createJsonObjectWithMissingProperty(accountIds, 10L, 20L, 5L, missingPropertyName), null);

		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountBatchHistoricalDataRequest(deserializer), MissingRequiredPropertyException.class);
	}

	@Test
	public void cannotCreateAccountBatchHistoricalDataRequestForTooManyDataPoints() {
		// Arrange:
		final Collection<SerializableAccountId> accountIds = Arrays.asList(
				new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress()),
				new SerializableAccountId(Utils.generateRandomAddress())
		);

		final Deserializer deserializer = new JsonDeserializer(createValidJsonObject(accountIds, 1L, 4000L, 1L), null);

		// Assert: 3 * 4000 > 10000
		ExceptionAssert.assertThrows(v -> new AccountBatchHistoricalDataRequest(deserializer), IllegalArgumentException.class);
	}
}
