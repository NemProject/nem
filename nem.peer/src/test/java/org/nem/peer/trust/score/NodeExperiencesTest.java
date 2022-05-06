package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.Matrix;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.core.time.TimeInstant;
import org.nem.peer.test.PeerUtils;

import java.util.*;

public class NodeExperiencesTest {
	private static final int RETENTION_TIME = 24;

	// region basic operations

	@Test
	public void previouslyUnknownNodeExperienceCanBeRetrieved() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(2);
		final NodeExperiences experiences = new NodeExperiences();

		// Act:
		final NodeExperience experience = experiences.getNodeExperience(nodes[0], nodes[1]);

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
	}

	@Test
	public void sameExperienceIsReturnedForSameSourceAndPeerNode() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(2);
		final NodeExperiences experiences = new NodeExperiences();

		// Act:
		final NodeExperience experience1 = experiences.getNodeExperience(nodes[0], nodes[1]);
		final NodeExperience experience2 = experiences.getNodeExperience(nodes[0], nodes[1]);

		// Assert:
		MatcherAssert.assertThat(experience2, IsSame.sameInstance(experience1));
	}

	@Test
	public void experienceIsDirectional() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(2);
		final NodeExperiences experiences = new NodeExperiences();

		// Act:
		final NodeExperience experience1 = experiences.getNodeExperience(nodes[0], nodes[1]);
		final NodeExperience experience2 = experiences.getNodeExperience(nodes[1], nodes[0]);

		// Assert:
		MatcherAssert.assertThat(experience2, IsNot.not(IsSame.sameInstance(experience1)));
	}

	// endregion

	// region shared experiences matrix

	@Test
	public void sharedExperiencesMatrixHasZeroRowForLocalNode() {
		// Act:
		final Matrix matrix = createTotalSharedExperienceMatrix();

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(0.0));
	}

	@Test
	public void sharedExperiencesMatrixHasZeroDiagonal() {
		// Act:
		final Matrix matrix = createTotalSharedExperienceMatrix();

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(matrix.getAt(2, 2), IsEqual.equalTo(0.0));
	}

	@Test
	public void sharedExperiencesCanHaveOneInOtherCells() {
		// Act:
		final Matrix matrix = createTotalSharedExperienceMatrix();

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.absSum(), IsEqual.equalTo(4.0));
		MatcherAssert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(1.0));
		MatcherAssert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(1.0));
		MatcherAssert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(1.0));
		MatcherAssert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(1.0));
	}

	private static Matrix createTotalSharedExperienceMatrix() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(3);
		final NodeExperiences experiences = new NodeExperiences();

		for (final Node nodeI : nodes) {
			for (final Node nodeJ : nodes) {
				experiences.getNodeExperience(nodeI, nodeJ).successfulCalls().set(1);
			}
		}

		// Act:
		return experiences.getSharedExperienceMatrix(nodes[1], nodes);
	}

	@Test
	public void sharedExperiencesMatrixHasZeroForLocalOnlyInteraction() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(3);
		final NodeExperiences experiences = new NodeExperiences();

		for (final Node nodeI : nodes) {
			experiences.getNodeExperience(nodes[1], nodeI).successfulCalls().set(1);
		}

		// Act:
		final Matrix matrix = experiences.getSharedExperienceMatrix(nodes[1], nodes);

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.absSum(), IsEqual.equalTo(0.0));
	}

	@Test
	public void sharedExperiencesMatrixHasZeroForExternalOnlyInteraction() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(3);
		final NodeExperiences experiences = new NodeExperiences();

		experiences.getNodeExperience(nodes[0], nodes[2]).successfulCalls().set(7);
		experiences.getNodeExperience(nodes[2], nodes[0]).failedCalls().set(7);

		// Act:
		final Matrix matrix = experiences.getSharedExperienceMatrix(nodes[1], nodes);

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.absSum(), IsEqual.equalTo(0.0));
	}

	@Test
	public void sharedExperiencesMatrixHasOneForLocalAndExternalInteraction() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(3);
		final NodeExperiences experiences = new NodeExperiences();

		experiences.getNodeExperience(nodes[0], nodes[2]).successfulCalls().set(2);
		experiences.getNodeExperience(nodes[1], nodes[2]).failedCalls().set(8);

		// Act:
		final Matrix matrix = experiences.getSharedExperienceMatrix(nodes[1], nodes);

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.absSum(), IsEqual.equalTo(1.0));
		MatcherAssert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(1.0));
	}

	// endregion

	// region getNodeExperiences / setNodeExperiences

	@Test
	public void getExperiencesReturnsAllNodeExperiences() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(4);
		final NodeExperiences experiences = new NodeExperiences();

		experiences.getNodeExperience(nodes[0], nodes[3]).successfulCalls().set(2);
		experiences.getNodeExperience(nodes[1], nodes[2]).successfulCalls().set(6);
		experiences.getNodeExperience(nodes[0], nodes[1]).successfulCalls().set(7);

		// Act:
		final List<NodeExperiencePair> pairs = experiences.getNodeExperiences(nodes[0]);

		// Assert:
		MatcherAssert.assertThat(pairs.size(), IsEqual.equalTo(2));
		NodeExperiencePair pair1 = pairs.get(0);
		NodeExperiencePair pair2 = pairs.get(1);
		if (pair1.getNode().equals(nodes[3])) {
			final NodeExperiencePair temp = pair1;
			pair1 = pair2;
			pair2 = temp;
		}

		MatcherAssert.assertThat(pair1.getNode(), IsEqual.equalTo(nodes[1]));
		MatcherAssert.assertThat(pair1.getExperience().successfulCalls().get(), IsEqual.equalTo(7L));

		MatcherAssert.assertThat(pair2.getNode(), IsEqual.equalTo(nodes[3]));
		MatcherAssert.assertThat(pair2.getExperience().successfulCalls().get(), IsEqual.equalTo(2L));
	}

	@Test
	public void setExperiencesUpdatesAllNodeExperiences() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(4);
		final NodeExperiences experiences = new NodeExperiences();

		experiences.getNodeExperience(nodes[0], nodes[3]).successfulCalls().set(6);

		final List<NodeExperiencePair> pairs = new ArrayList<>();
		pairs.add(new NodeExperiencePair(nodes[3], PeerUtils.createNodeExperience(2)));
		pairs.add(new NodeExperiencePair(nodes[1], PeerUtils.createNodeExperience(11)));

		// Act:
		experiences.setNodeExperiences(nodes[0], pairs, new TimeInstant(123));

		// Assert:
		final NodeExperience experience01 = experiences.getNodeExperience(nodes[0], nodes[1]);
		final NodeExperience experience03 = experiences.getNodeExperience(nodes[0], nodes[3]);
		MatcherAssert.assertThat(experience01.successfulCalls().get(), IsEqual.equalTo(11L));
		MatcherAssert.assertThat(experience03.successfulCalls().get(), IsEqual.equalTo(2L));
		MatcherAssert.assertThat(experience01.getLastUpdateTime(), IsEqual.equalTo(new TimeInstant(123)));
		MatcherAssert.assertThat(experience03.getLastUpdateTime(), IsEqual.equalTo(new TimeInstant(123)));
	}

	@Test
	public void setExperiencesUsesNodeCache() {
		// Arrange:
		final Node[] nodes = PeerUtils.createNodeArray(2);
		final NodeExperiences experiences = new NodeExperiences();
		final List<NodeExperiencePair> pairs = new ArrayList<>();
		pairs.add(new NodeExperiencePair(nodes[1], PeerUtils.createNodeExperience(11)));
		experiences.setNodeExperiences(nodes[0], pairs, new TimeInstant(123)); // both nodes are in the cache now
		pairs.clear();
		Node node0Copy = createCopy(nodes[0]);
		Node node1Copy = createCopy(nodes[1]);
		pairs.add(new NodeExperiencePair(node1Copy, PeerUtils.createNodeExperience(22)));

		// Act:
		experiences.setNodeExperiences(node0Copy, pairs, new TimeInstant(123));
		final List<NodeExperiencePair> nodeExperiences = experiences.getNodeExperiences(nodes[0]);

		// Assert (can only check node[1]):
		MatcherAssert.assertThat(nodeExperiences.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(nodes[1], IsSame.sameInstance(nodeExperiences.get(0).getNode()));
		MatcherAssert.assertThat(22L, IsEqual.equalTo(nodeExperiences.get(0).getExperience().successfulCalls().get()));
	}

	private static Node createCopy(final Node node) {
		return new Node(node.getIdentity(), node.getEndpoint(), node.getMetaData());
	}

	// endregion

	// region prune

	@Test
	public void prunePreservesAllExperiencesWithTimeStampAtLeastAsOldAsGivenTimeStampMinusRetentionTime() {
		// Arrange:
		final int timeOffsetSecs = 1234;
		final NodeExperiences experiences = createNodeExperiences(timeOffsetSecs);
		final TimeInstant currentTime = TimeInstant.ZERO.addHours(RETENTION_TIME).addSeconds(timeOffsetSecs);

		// Act:
		experiences.prune(currentTime);

		// Assert:
		for (int i = 0; i <= 5; ++i) {
			final Node node = NodeUtils.createNodeWithName("alice" + String.valueOf(i));
			final List<NodeExperiencePair> nodeExperiencePairs = experiences.getNodeExperiences(node);
			MatcherAssert.assertThat(nodeExperiencePairs.size(), IsEqual.equalTo(1));
			final NodeExperiencePair pair = nodeExperiencePairs.get(0);
			MatcherAssert.assertThat(pair.getNode(), IsEqual.equalTo(NodeUtils.createNodeWithName("bob" + String.valueOf(i))));
			MatcherAssert.assertThat(pair.getExperience().successfulCalls().get(), IsEqual.equalTo(10L + i));
			MatcherAssert.assertThat(pair.getExperience().getLastUpdateTime(), IsEqual.equalTo(new TimeInstant(1234 + i)));
		}
	}

	@Test
	public void pruneRemovesAllExperiencesWithEarlierTimeStampThanGivenTimeStampMinusRetentionTime() {
		// Arrange:
		final int timeOffsetSecs = 1234;
		final NodeExperiences experiences = createNodeExperiences(timeOffsetSecs);
		final TimeInstant currentTime = TimeInstant.ZERO.addHours(RETENTION_TIME).addSeconds(timeOffsetSecs);

		// Act:
		experiences.prune(currentTime);

		// Assert:
		for (int i = -5; i < 0; ++i) {
			final Node node = NodeUtils.createNodeWithName("alice" + String.valueOf(i));
			final List<NodeExperiencePair> nodeExperiencePairs = experiences.getNodeExperiences(node);
			MatcherAssert.assertThat(nodeExperiencePairs.size(), IsEqual.equalTo(0));
		}
	}

	private NodeExperiences createNodeExperiences(final int timeOffsetSecs) {
		final NodeExperiences experiences = new NodeExperiences();
		for (int delta = -5; delta <= 5; ++delta) {
			final Node node = NodeUtils.createNodeWithName("alice" + String.valueOf(delta));
			final NodeExperiencePair pair = new NodeExperiencePair(NodeUtils.createNodeWithName("bob" + String.valueOf(delta)),
					PeerUtils.createNodeExperience(10 + delta));
			experiences.setNodeExperiences(node, Collections.singletonList(pair), new TimeInstant(timeOffsetSecs + delta));
		}

		return experiences;
	}

	// endregion
}
