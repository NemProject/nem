package org.nem.peer.test;

import org.nem.peer.trust.score.Scores;

/**
 * A mock Scores implementation.
 */
public class MockScores extends Scores<MockScore> {

    @Override
    protected MockScore createScore() {
        return new MockScore();
    }
}
