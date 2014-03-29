package org.nem.peer.trust.score;

import org.hamcrest.core.*;
import org.junit.*;

public class ScoreTest {

    @Test
    public void derivedScoreProvidesInitialValue() {
        // Arrange:
        final Score score = new MockScore();

        // Assert:
        Assert.assertThat(score.score().get(), IsEqual.equalTo(1.4));
    }

    @Test
    public void rawScoreCanBeChanged() {
        // Arrange:
        final Score score = new MockScore();

        // Act:
        score.score().set(5.2);

        // Assert:
        Assert.assertThat(score.score().get(), IsEqual.equalTo(5.2));
    }

    private static class MockScore extends Score
    {
        public MockScore() {
            super(1.4);
        }
    }
}
