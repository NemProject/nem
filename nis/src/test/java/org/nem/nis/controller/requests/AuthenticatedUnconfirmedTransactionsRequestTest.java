package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.peer.node.*;
import org.nem.peer.requests.UnconfirmedTransactionsRequest;

import java.util.*;

public class AuthenticatedUnconfirmedTransactionsRequestTest {

	// region construction

	@Test
	public void requestCanBeCreatedWhenUnconfirmedTransactionsRequestIsSupplied() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final UnconfirmedTransactionsRequest request = new UnconfirmedTransactionsRequest(Collections.singletonList(new MockTransaction()));

		// Act:
		final AuthenticatedUnconfirmedTransactionsRequest authenticatedRequest = new AuthenticatedUnconfirmedTransactionsRequest(request, challenge);

		// Assert:
		Assert.assertThat(authenticatedRequest.getChallenge(), IsEqual.equalTo(challenge));
		assertEquivalent(authenticatedRequest.getEntity(), request);
	}

	@Test
	public void requestCanBeCreatedWhenOnlyNodeChallengeIsSupplied() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Act:
		final AuthenticatedUnconfirmedTransactionsRequest authenticatedRequest = new AuthenticatedUnconfirmedTransactionsRequest(challenge);

		// Assert:
		Assert.assertThat(authenticatedRequest.getChallenge(), IsEqual.equalTo(challenge));
		assertEquivalent(authenticatedRequest.getEntity(), new UnconfirmedTransactionsRequest());
	}

	// endregion

	// region serialization

	@Test
	public void requestCanBeRoundTripped() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());
		final UnconfirmedTransactionsRequest original = new UnconfirmedTransactionsRequest(Collections.singletonList(new MockTransaction()));

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntityWithBinarySerializer(
				new AuthenticatedRequest<>(original, challenge),
				null);
		final AuthenticatedUnconfirmedTransactionsRequest request = new AuthenticatedUnconfirmedTransactionsRequest(deserializer);

		// Assert:
		Assert.assertThat(request.getChallenge(), IsEqual.equalTo(challenge));
		assertEquivalent(request.getEntity(), original);
	}

	// endregion

	private static void assertEquivalent(final UnconfirmedTransactionsRequest lhs, final UnconfirmedTransactionsRequest rhs) {
		Assert.assertThat(lhs.getHashShortIds(), IsEquivalent.equivalentTo(rhs.getHashShortIds()));
	}
}
