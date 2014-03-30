package org.nem.peer.trust.score;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NodeExperienceTest {

    @Test
    public void nodeExperienceIsInitializedCorrectly() {
        // Act:
        final NodeExperience experience = new NodeExperience();

        // Assert:
        Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
        Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
        Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
    }

    @Test
    public void totalCallsReturnsTheSumOfSuccessfulAndFailedCalls() {
        // Arrange:
        final NodeExperience experience = new NodeExperience();

        // Act:
        experience.successfulCalls().set(4);
        experience.failedCalls().set(7);

        // Assert:
        Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(11L));
    }
}
