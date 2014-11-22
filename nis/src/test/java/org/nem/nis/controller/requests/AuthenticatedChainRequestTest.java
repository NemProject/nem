package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;
import org.nem.peer.node.*;

public class AuthenticatedChainRequestTest {

	@Test
	public void requestCanBeCreated() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final ChainRequest chainRequest = new ChainRequest(new BlockHeight(100), 123, 1234);

		// Act:
		final AuthenticatedChainRequest request = new AuthenticatedChainRequest(chainRequest, challenge);

		// Assert:
		Assert.assertThat(request.getChallenge(), IsEqual.equalTo(challenge));
		assertEqual(request.getEntity(), chainRequest);
	}

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final ChainRequest chainRequest = new ChainRequest(new BlockHeight(100), 123, 1234);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new AuthenticatedRequest<>(chainRequest, challenge),
				null);
		final AuthenticatedChainRequest request = new AuthenticatedChainRequest(deserializer);

		// Assert:
		Assert.assertThat(request.getChallenge(), IsEqual.equalTo(challenge));
		assertEqual(request.getEntity(), chainRequest);

	}

	private void assertEqual(final ChainRequest lhs, final ChainRequest rhs) {
		Assert.assertThat(lhs.getHeight(), IsEqual.equalTo(rhs.getHeight()));
		Assert.assertThat(lhs.getMinBlocks(), IsEqual.equalTo(rhs.getMinBlocks()));
		Assert.assertThat(lhs.getMaxTransactions(), IsEqual.equalTo(rhs.getMaxTransactions()));
	}
}
