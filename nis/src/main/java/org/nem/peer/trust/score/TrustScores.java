package org.nem.peer.trust.score;

/**
 * A collection of TrustScore objects.
 */
public class TrustScores extends Scores<TrustScore> {

    @Override
    protected TrustScore createScore() {
        return new TrustScore();
    }
}
