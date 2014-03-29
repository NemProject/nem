package org.nem.peer.trust.score;

import org.hamcrest.core.*;
import org.junit.Assert;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.Node;
import org.nem.peer.test.*;
import org.nem.peer.trust.Vector;

import java.security.InvalidParameterException;

public class ScoresTest {

    //region basic operations

    @Test
    public void previouslyUnknownScoreCanBeRetrieved() {
        // Arrange:
        final TestContext context = new TestContext(2);

        // Act:
        final MockScore score = context.scores.getScore(context.nodes[0], context.nodes[1]);

        // Assert:
        Assert.assertThat(score.score().get(), IsEqual.equalTo(MockScore.INITIAL_SCORE));
    }

    @Test
    public void sameScoreIsReturnedForSameSourceAndPeerNode() {
        // Arrange:
        final TestContext context = new TestContext(2);

        // Act:
        final MockScore score1 = context.scores.getScore(context.nodes[0], context.nodes[1]);
        final MockScore score2 = context.scores.getScore(context.nodes[0], context.nodes[1]);

        // Assert:
        Assert.assertThat(score2, IsSame.sameInstance(score1));
    }

    @Test
    public void scoreIsDirectional() {
        // Arrange:
        final TestContext context = new TestContext(2);

        // Act:
        final MockScore score1 = context.scores.getScore(context.nodes[0], context.nodes[1]);
        final MockScore score2 = context.scores.getScore(context.nodes[1], context.nodes[0]);

        // Assert:
        Assert.assertThat(score2, IsNot.not(IsSame.sameInstance(score1)));
    }

    //endregion

    //region score vectors

    @Test
    public void scoreVectorCanBeRetrieved() {
        // Arrange:
        final TestContext context = new TestContext(3);

        context.scores.getScore(context.nodes[0], context.nodes[1]).score().set(7);
        context.scores.getScore(context.nodes[0], context.nodes[2]).score().set(2);

        // Act:
        final Vector vector = context.scores.getScoreVector(context.nodes[0], context.nodes);

        // Assert:
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(3));
        Assert.assertThat(vector.getAt(0), IsEqual.equalTo(MockScore.INITIAL_SCORE));
        Assert.assertThat(vector.getAt(1), IsEqual.equalTo(7.0));
        Assert.assertThat(vector.getAt(2), IsEqual.equalTo(2.0));
    }

    @Test(expected = InvalidParameterException.class)
    public void smallerTrustVectorCannotBeSet() {
        // Arrange:
        final TestContext context = new TestContext(3);

        final Vector vector = new Vector(2);

        // Act:
        context.scores.setScoreVector(context.nodes[0], context.nodes, vector);
    }

    @Test(expected = InvalidParameterException.class)
    public void largerTrustVectorCannotBeSet() {
        // Arrange:
        final TestContext context = new TestContext(3);

        final Vector vector = new Vector(4);

        // Act:
        context.scores.setScoreVector(context.nodes[0], context.nodes, vector);
    }

    @Test
    public void trustVectorCanBeSet() {
        // Arrange:
        final TestContext context = new TestContext(3);

        final Vector trustVector = new Vector(3);
        trustVector.setAt(0, 3.0);
        trustVector.setAt(1, 7.0);
        trustVector.setAt(2, 4.0);

        // Act:
        context.scores.setScoreVector(context.nodes[1], context.nodes, trustVector);
        final Vector vector = context.scores.getScoreVector(context.nodes[1], context.nodes);

        // Assert:
        Assert.assertThat(vector.getSize(), IsEqual.equalTo(3));
        Assert.assertThat(vector.getAt(0), IsEqual.equalTo(3.0));
        Assert.assertThat(vector.getAt(1), IsEqual.equalTo(7.0));
        Assert.assertThat(vector.getAt(2), IsEqual.equalTo(4.0));
    }

    //endregion

    private static class TestContext {

        public final MockScores scores;
        public final Node[] nodes;

        public TestContext(final int numNodes) {
            this.scores = new MockScores();

            this.nodes = new Node[numNodes];
            for (int i = 0; i < numNodes; ++i)
                this.nodes[i] = Utils.createNodeWithPort(80 + i);
        }
    }
}
