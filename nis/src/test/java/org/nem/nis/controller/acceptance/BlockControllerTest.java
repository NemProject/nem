package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.serialization.Deserializer;
import org.nem.nis.test.MockPeerConnector;

import java.net.MalformedURLException;

public class BlockControllerTest {

	@Test
	public void validHeightReturnsValidBlock() throws MalformedURLException {
		// Arrange:
		final MockPeerConnector pc = new MockPeerConnector();
		final JSONObject input = new JSONObject();
		input.put("height", 1);

		// Act:
		final Block block = BlockFactory.VERIFIABLE.deserialize(pc.blockAt(input));

		// Assert:
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(1L));
	}

	@Test
	public void invalidHeightReturnsNotFound() throws MalformedURLException {
		// Arrange:
		final MockPeerConnector pc = new MockPeerConnector();
		final JSONObject input = new JSONObject();
		input.put("height", 0);

		// Act:
		final Deserializer deserializer = pc.blockAt(input);

		// Assert:
		Assert.assertThat(deserializer.readInt("status"), IsEqual.equalTo(404));
	}

	@Test
	public void missingHeightReturnsServerError() throws MalformedURLException {
		// Arrange:
		final MockPeerConnector pc = new MockPeerConnector();
		final JSONObject input = new JSONObject();

		// Act:
		final Deserializer deserializer = pc.blockAt(input);

		// Assert:
		Assert.assertThat(deserializer.readInt("status"), IsEqual.equalTo(500));
	}

	@Test
	public void wrongUrlReturnsNotFound() throws MalformedURLException {

		// TODO: does it make sense to return HTTP status codes for errors
		// TODO: i think so, but that means additional changes need to be made to MockPeerConnector

		// Arrange:
		final MockPeerConnector pc = new MockPeerConnector();
		final JSONObject input = new JSONObject();

		// Act:
		final Deserializer deserializer = pc.wrongUrl(input);

		// Assert:
		Assert.assertThat(deserializer.readInt("code"), IsEqual.equalTo(404));
	}
}
