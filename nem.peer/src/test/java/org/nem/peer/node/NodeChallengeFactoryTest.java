package org.nem.peer.node;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;

public class NodeChallengeFactoryTest {

	@Test
	public void nextCreates64ByteChallenge() {
		// Arrange:
		final NodeChallengeFactory factory = new NodeChallengeFactory();

		// Act:
		final NodeChallenge challenge = factory.next();

		// Assert:
		MatcherAssert.assertThat(challenge.getRaw().length, IsEqual.equalTo(64));
	}

	@Test
	public void consecutiveChallengesAreDifferent() {
		// Arrange:
		final NodeChallengeFactory factory = new NodeChallengeFactory();

		// Act:
		final NodeChallenge challenge1 = factory.next();
		final NodeChallenge challenge2 = factory.next();

		// Assert:
		MatcherAssert.assertThat(challenge2, IsNot.not(IsEqual.equalTo(challenge1)));
	}
}
