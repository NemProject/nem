package org.nem.peer.trust.score;

public class CredibilityScores extends Scores<CredibilityScore> {

    @Override
    protected CredibilityScore createScore() {
        return new CredibilityScore();
    }
}
