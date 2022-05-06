package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.peer.test.*;

public class ScoresTest {

	// region basic operations

	@Test
	public void previouslyUnknownScoreCanBeRetrieved() {
		// Arrange:
		final TestContext context = new TestContext(2);

		// Act:
		final MockScore score = context.scores.getScore(context.nodes[0], context.nodes[1]);

		// Assert:
		MatcherAssert.assertThat(score.score().get(), IsEqual.equalTo(MockScore.INITIAL_SCORE));
	}

	@Test
	public void sameScoreIsReturnedForSameSourceAndPeerNode() {
		// Arrange:
		final TestContext context = new TestContext(2);

		// Act:
		final MockScore score1 = context.scores.getScore(context.nodes[0], context.nodes[1]);
		final MockScore score2 = context.scores.getScore(context.nodes[0], context.nodes[1]);

		// Assert:
		MatcherAssert.assertThat(score2, IsSame.sameInstance(score1));
	}

	@Test
	public void scoreIsDirectional() {
		// Arrange:
		final TestContext context = new TestContext(2);

		// Act:
		final MockScore score1 = context.scores.getScore(context.nodes[0], context.nodes[1]);
		final MockScore score2 = context.scores.getScore(context.nodes[1], context.nodes[0]);

		// Assert:
		MatcherAssert.assertThat(score2, IsNot.not(IsSame.sameInstance(score1)));
	}

	// endregion

	// region score vectors

	@Test
	public void scoreVectorCanBeRetrieved() {
		// Arrange:
		final TestContext context = new TestContext(3);

		context.scores.getScore(context.nodes[0], context.nodes[1]).score().set(7);
		context.scores.getScore(context.nodes[0], context.nodes[2]).score().set(2);

		// Act:
		final ColumnVector vector = context.scores.getScoreVector(context.nodes[0], context.nodes);

		// Assert:
		MatcherAssert.assertThat(vector.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(vector.getAt(0), IsEqual.equalTo(MockScore.INITIAL_SCORE));
		MatcherAssert.assertThat(vector.getAt(1), IsEqual.equalTo(7.0));
		MatcherAssert.assertThat(vector.getAt(2), IsEqual.equalTo(2.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void smallerTrustVectorCannotBeSet() {
		// Arrange:
		final TestContext context = new TestContext(3);

		final ColumnVector vector = new ColumnVector(2);

		// Act:
		context.scores.setScoreVector(context.nodes[0], context.nodes, vector);
	}

	@Test(expected = IllegalArgumentException.class)
	public void largerTrustVectorCannotBeSet() {
		// Arrange:
		final TestContext context = new TestContext(3);

		final ColumnVector vector = new ColumnVector(4);

		// Act:
		context.scores.setScoreVector(context.nodes[0], context.nodes, vector);
	}

	@Test
	public void trustVectorCanBeSet() {
		// Arrange:
		final TestContext context = new TestContext(3);

		final ColumnVector trustVector = new ColumnVector(3);
		trustVector.setAt(0, 3.0);
		trustVector.setAt(1, 7.0);
		trustVector.setAt(2, 4.0);

		// Act:
		context.scores.setScoreVector(context.nodes[1], context.nodes, trustVector);
		final ColumnVector vector = context.scores.getScoreVector(context.nodes[1], context.nodes);

		// Assert:
		MatcherAssert.assertThat(vector.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(vector.getAt(0), IsEqual.equalTo(3.0));
		MatcherAssert.assertThat(vector.getAt(1), IsEqual.equalTo(7.0));
		MatcherAssert.assertThat(vector.getAt(2), IsEqual.equalTo(4.0));
	}

	// endregion

	// region score matrix

	@Test
	public void scoreMatrixCanBeCalculated() {
		// Arrange:
		final TestContext context = new TestContext(3, new MockScores(0));

		context.scores.getScore(context.nodes[0], context.nodes[1]).score().set(7);
		context.scores.getScore(context.nodes[0], context.nodes[2]).score().set(2);
		context.scores.getScore(context.nodes[1], context.nodes[0]).score().set(5);
		context.scores.getScore(context.nodes[1], context.nodes[2]).score().set(4);
		context.scores.getScore(context.nodes[2], context.nodes[0]).score().set(11);
		context.scores.getScore(context.nodes[2], context.nodes[1]).score().set(6);

		// Act:
		final Matrix matrix = context.scores.getScoreMatrix(context.nodes);

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(5.0));
		MatcherAssert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(11.0));
		MatcherAssert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(7.0));
		MatcherAssert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(6.0));
		MatcherAssert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(2.0));
		MatcherAssert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(4.0));
		MatcherAssert.assertThat(matrix.getAt(2, 2), IsEqual.equalTo(0.0));
	}

	// endregion

	// region normalizeLocalTrust

	@Test
	public void localTrustValuesCanBeNormalized() {
		// Arrange:
		final TestContext context = new TestContext(3, new MockScores(0));

		context.scores.getScore(context.nodes[0], context.nodes[1]).score().set(7);
		context.scores.getScore(context.nodes[0], context.nodes[2]).score().set(2);
		context.scores.getScore(context.nodes[1], context.nodes[0]).score().set(5);
		context.scores.getScore(context.nodes[1], context.nodes[2]).score().set(4);
		context.scores.getScore(context.nodes[2], context.nodes[0]).score().set(11);
		context.scores.getScore(context.nodes[2], context.nodes[1]).score().set(6);

		// Act:
		context.scores.normalize(context.nodes);
		final Matrix matrix = context.scores.getScoreMatrix(context.nodes);

		// Assert:
		MatcherAssert.assertThat(matrix.getRowCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getColumnCount(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(matrix.getAt(0, 0), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(matrix.getAt(0, 1), IsEqual.equalTo(5.0 / 9));
		MatcherAssert.assertThat(matrix.getAt(0, 2), IsEqual.equalTo(11.0 / 17));
		MatcherAssert.assertThat(matrix.getAt(1, 0), IsEqual.equalTo(7.0 / 9));
		MatcherAssert.assertThat(matrix.getAt(1, 1), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(matrix.getAt(1, 2), IsEqual.equalTo(6.0 / 17));
		MatcherAssert.assertThat(matrix.getAt(2, 0), IsEqual.equalTo(2.0 / 9));
		MatcherAssert.assertThat(matrix.getAt(2, 1), IsEqual.equalTo(4.0 / 9));
		MatcherAssert.assertThat(matrix.getAt(2, 2), IsEqual.equalTo(0.0));
	}

	// endregion

	private static class TestContext {

		public final MockScores scores;
		public final Node[] nodes;

		public TestContext(final int numNodes) {
			this(numNodes, new MockScores());
		}

		public TestContext(final int numNodes, final MockScores scores) {
			this.scores = scores;

			this.nodes = new Node[numNodes];
			for (int i = 0; i < numNodes; ++i) {
				this.nodes[i] = NodeUtils.createNodeWithName(String.format("bob #%d", i));
			}
		}
	}
}
