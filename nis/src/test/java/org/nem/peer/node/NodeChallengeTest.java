package org.nem.peer.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class NodeChallengeTest {

	private final byte[] TEST_BYTES = new byte[]{ 0x22, (byte) 0xAB, 0x71 };
	private final byte[] MODIFIED_TEST_BYTES = new byte[]{ 0x22, (byte) 0xAB, 0x72 };

	//region constructors

	@Test
	public void canCreateFromBytes() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(this.TEST_BYTES);

		// Assert:
		Assert.assertThat(challenge.getRaw(), IsEqual.equalTo(this.TEST_BYTES));
	}

	//endregion

	//region serializer

	@Test
	public void challengeCanBeRoundTripped() {
		// Act:
		final NodeChallenge challenge = createRoundTrippedChallenge(new NodeChallenge(this.TEST_BYTES));

		// Assert:
		Assert.assertThat(challenge, IsEqual.equalTo(new NodeChallenge(this.TEST_BYTES)));
	}

	public static NodeChallenge createRoundTrippedChallenge(final NodeChallenge originalChallenge) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalChallenge, null);
		return new NodeChallenge(deserializer);
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(this.TEST_BYTES);

		// Assert:
		Assert.assertThat(new NodeChallenge(this.TEST_BYTES), IsEqual.equalTo(challenge));
		Assert.assertThat(new NodeChallenge(this.MODIFIED_TEST_BYTES), IsNot.not(IsEqual.equalTo(challenge)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(challenge)));
		Assert.assertThat(this.TEST_BYTES, IsNot.not(IsEqual.equalTo((Object) challenge)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeChallenge challenge = new NodeChallenge(this.TEST_BYTES);
		final int hashCode = challenge.hashCode();

		// Assert:
		Assert.assertThat(new NodeChallenge(this.TEST_BYTES).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeChallenge(this.MODIFIED_TEST_BYTES).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsHexRepresentation() {
		// Assert:
		Assert.assertThat(new NodeChallenge(this.TEST_BYTES).toString(), IsEqual.equalTo("22ab71"));
	}

	//endregion
}