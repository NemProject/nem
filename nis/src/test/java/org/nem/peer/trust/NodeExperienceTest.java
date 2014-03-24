package org.nem.peer.trust;

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
        Assert.assertThat(experience.localTrust().get(), IsEqual.equalTo(0.0));
        Assert.assertThat(experience.localTrustSum().get(), IsEqual.equalTo(0.0));
        Assert.assertThat(experience.feedbackCredibility().get(), IsEqual.equalTo(1.0));
    }
}
