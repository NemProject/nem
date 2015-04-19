package org.nem.core.serialization;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class SerializationContextTest {

	@Test
	public void contextConstantsAreInitializedCorrectly() {
		// Arrange:
		final SerializationContext context = new SerializationContext();

		// Assert:
		Assert.assertThat(context.getDefaultMaxBytesLimit(), IsEqual.equalTo(1024));
		Assert.assertThat(context.getDefaultMaxCharsLimit(), IsEqual.equalTo(128));
	}
}