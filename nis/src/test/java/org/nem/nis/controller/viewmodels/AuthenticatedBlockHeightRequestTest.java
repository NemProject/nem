package org.nem.nis.controller.viewmodels;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.peer.node.AuthenticatedRequest;
import org.nem.peer.node.NodeChallenge;

public class AuthenticatedBlockHeightRequestTest {

	@Test
	public void requestCanBeCreated() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final BlockHeight height = new BlockHeight(14);

		// Act:
		final AuthenticatedBlockHeightRequest request = new AuthenticatedBlockHeightRequest(height, challenge);

		// Assert:
		Assert.assertThat(request.getChallenge(), IsEqual.equalTo(challenge));
		Assert.assertThat(request.getEntity(), IsEqual.equalTo(height));
	}

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final BlockHeight height = new BlockHeight(14);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new AuthenticatedRequest<>(height, challenge),
				null);
		final AuthenticatedBlockHeightRequest request = new AuthenticatedBlockHeightRequest(deserializer);

		// Assert:
		Assert.assertThat(request.getChallenge(), IsEqual.equalTo(challenge));
		Assert.assertThat(request.getEntity(), IsEqual.equalTo(height));
	}
}