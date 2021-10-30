package org.nem.core.serialization;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class SerializationContextTest {

	@Test
	public void contextConstantsAreInitializedCorrectly() {
		// Arrange:
		final SerializationContext context = new SerializationContext();

		// Assert:
		MatcherAssert.assertThat(context.getDefaultMaxBytesLimit(), IsEqual.equalTo(2048));
		MatcherAssert.assertThat(context.getDefaultMaxCharsLimit(), IsEqual.equalTo(128));
	}
}
