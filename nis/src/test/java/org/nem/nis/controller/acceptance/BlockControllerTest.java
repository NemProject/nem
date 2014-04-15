package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.nis.test.LocalHostConnector;

import java.net.MalformedURLException;

public class BlockControllerTest {

	private static final String BLOCK_AT_PATH = "block/at";
	private static final String INVALID_PATH = "wrong/at";

	@Test
	public void validHeightReturnsValidBlock() throws MalformedURLException {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject input = new JSONObject();
		input.put("height", 1);

		// Act:
		final LocalHostConnector.Result result = connector.post(BLOCK_AT_PATH, input);
		final Block block = BlockFactory.VERIFIABLE.deserialize(result.getBodyAsDeserializer());

		// Assert:
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(1L));
	}

	@Test
	public void invalidHeightReturnsNotFound() throws MalformedURLException {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject input = new JSONObject();
		input.put("height", 0);

		// Act:
		final LocalHostConnector.Result result = connector.post(BLOCK_AT_PATH, input);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(404));
	}

	@Test
	public void missingHeightReturnsServerError() throws MalformedURLException {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject input = new JSONObject();

		// Act:
		final LocalHostConnector.Result result = connector.post(BLOCK_AT_PATH, input);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(500));
	}

	@Test
	public void wrongUrlReturnsNotFound() throws MalformedURLException {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject input = new JSONObject();

		// Act:
		final LocalHostConnector.Result result = connector.post(INVALID_PATH, input);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(404));
	}
}
