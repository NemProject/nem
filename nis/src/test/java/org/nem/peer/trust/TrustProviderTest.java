package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class TrustProviderTest {

    @Test
    public void uniformProviderReturnsSameScoreForAllInputs() {
        // Arrange:
        final TrustProvider provider = new UniformTrustProvider();

        // Assert:
        Assert.assertThat(provider.calculateScore(1000, 1), IsEqual.equalTo(1.0));
        Assert.assertThat(provider.calculateScore(1, 1000), IsEqual.equalTo(1.0));
        Assert.assertThat(provider.calculateScore(1000, 980), IsEqual.equalTo(1.0));
        Assert.assertThat(provider.calculateScore(21, 1), IsEqual.equalTo(1.0));
    }

    @Test
    public void eigenTrustProviderProviderReturnsDifferenceOfSuccessfulAndFailureCalls() {
        // Arrange:
        final TrustProvider provider = new EigenTrustProvider();

        // Assert:
        Assert.assertThat(provider.calculateScore(1000, 1), IsEqual.equalTo(999.0));
        Assert.assertThat(provider.calculateScore(1, 1000), IsEqual.equalTo(0.0));
        Assert.assertThat(provider.calculateScore(1000, 980), IsEqual.equalTo(20.0));
        Assert.assertThat(provider.calculateScore(21, 1), IsEqual.equalTo(20.0));
    }

    @Test
    public void eigenTrustPlusPlusProviderProviderReturnsNumberOfSuccessfulCalls() {
        // Arrange:
        final TrustProvider provider = new EigenTrustPlusPlusProvider();

        // Assert:
        Assert.assertThat(provider.calculateScore(1000, 1), IsEqual.equalTo(1000.0));
        Assert.assertThat(provider.calculateScore(1, 1000), IsEqual.equalTo(1.0));
        Assert.assertThat(provider.calculateScore(1000, 980), IsEqual.equalTo(1000.0));
        Assert.assertThat(provider.calculateScore(21, 1), IsEqual.equalTo(21.0));
    }
}
