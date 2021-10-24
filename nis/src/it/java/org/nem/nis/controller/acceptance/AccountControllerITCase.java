package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.serialization.JsonSerializer;
import org.nem.nis.test.LocalHostConnector;

public class AccountControllerITCase {
	private static final String ACCOUNT_UNLOCK_PATH = "account/unlock";
	private static final String ACCOUNT_LOCK_PATH = "account/lock";
	private static final PrivateKey REAL_PRIVATE_KEY = AcceptanceTestConstants.PRIVATE_KEY;

	@After
	public void lockAll() {
		lock();
	}

	@Test
	public void unlockSuccessReturnsOk() {
		// Act:
		final ErrorResponseDeserializerUnion result = unlock();

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(200));
		MatcherAssert.assertThat(result.hasBody(), IsEqual.equalTo(false));
	}

	@Test
	public void lockSuccessReturnsOk() {
		// Act:
		unlock();
		final ErrorResponseDeserializerUnion result = lock();

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(200));
		MatcherAssert.assertThat(result.hasBody(), IsEqual.equalTo(false));
	}

	private static ErrorResponseDeserializerUnion lock() {
		// Act:
		return lockOrUnlock(ACCOUNT_LOCK_PATH);
	}

	private static ErrorResponseDeserializerUnion unlock() {
		// Act:
		return lockOrUnlock(ACCOUNT_UNLOCK_PATH);
	}

	private static ErrorResponseDeserializerUnion lockOrUnlock(final String apiPath) {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(REAL_PRIVATE_KEY);

		// Act:
		return connector.post(apiPath, jsonObject);
	}
}
