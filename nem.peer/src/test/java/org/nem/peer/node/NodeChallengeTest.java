package org.nem.peer.node;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class NodeChallengeTest {

	private static final byte[] TEST_BYTES = new byte[]{
			0x22, (byte) 0xAB, 0x71
	};
	private static final byte[] MODIFIED_TEST_BYTES = new byte[]{
			0x22, (byte) 0xAB, 0x72
	};

	// region constructors

	@Test
	public void canCreateFromBytes() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(TEST_BYTES);

		// Assert:
		MatcherAssert.assertThat(challenge.getRaw(), IsEqual.equalTo(TEST_BYTES));
	}

	// endregion

	// region serializer

	@Test
	public void challengeCanBeRoundTripped() {
		// Act:
		final NodeChallenge challenge = createRoundTrippedChallenge(new NodeChallenge(TEST_BYTES));

		// Assert:
		MatcherAssert.assertThat(challenge, IsEqual.equalTo(new NodeChallenge(TEST_BYTES)));
	}

	private static NodeChallenge createRoundTrippedChallenge(final NodeChallenge originalChallenge) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalChallenge, null);
		return new NodeChallenge(deserializer);
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(TEST_BYTES);

		// Assert:
		MatcherAssert.assertThat(new NodeChallenge(TEST_BYTES), IsEqual.equalTo(challenge));
		MatcherAssert.assertThat(new NodeChallenge(MODIFIED_TEST_BYTES), IsNot.not(IsEqual.equalTo(challenge)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(challenge)));
		MatcherAssert.assertThat(TEST_BYTES, IsNot.not(IsEqual.equalTo((Object) challenge)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(TEST_BYTES);
		final int hashCode = challenge.hashCode();

		// Assert:
		MatcherAssert.assertThat(new NodeChallenge(TEST_BYTES).hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(new NodeChallenge(MODIFIED_TEST_BYTES).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsHexRepresentation() {
		// Assert:
		MatcherAssert.assertThat(new NodeChallenge(TEST_BYTES).toString(), IsEqual.equalTo("22ab71"));
	}

	// endregion
}
