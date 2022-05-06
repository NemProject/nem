package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.test.LocalHostConnector;

import java.net.MalformedURLException;

public class BlockControllerITCase {

	private static final String BLOCK_AT_PATH = "block/at/public";
	private static final String INVALID_PATH = "wrong/at";

	@Test
	public void validHeightReturnsValidBlock() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject input = new JSONObject();
		input.put("height", 1);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(BLOCK_AT_PATH, input);
		final Block block = BlockFactory.VERIFIABLE.deserialize(result.getDeserializer());

		// Assert:
		MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(1)));
	}

	@Test
	public void invalidBlockHeightReturnsBadRequest() {
		// Assert:
		assertRequestForBlockAtHeightReturnsStatus(0, 400);
	}

	@Test
	public void futureBlockHeightReturnsNotFound() {
		// Assert:
		assertRequestForBlockAtHeightReturnsStatus(Long.MAX_VALUE, 404);
	}

	private static void assertRequestForBlockAtHeightReturnsStatus(final long height, final int expectedStatus) {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject input = new JSONObject();
		input.put("height", height);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(BLOCK_AT_PATH, input);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(expectedStatus));
	}

	@Test
	public void missingHeightReturnsServerError() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject input = new JSONObject();

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(BLOCK_AT_PATH, input);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(500));
	}

	@Test
	public void wrongUrlReturnsNotFound() throws MalformedURLException {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject input = new JSONObject();

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(INVALID_PATH, input);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(404));
	}
}
