package org.nem.core.connect;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.ConnectUtils;

public class HttpVoidResponseStrategyTest {

	@Test
	public void getSupportedContentTypeReturnsCorrectContentType() throws Exception {
		// Arrange:
		final HttpVoidResponseStrategy strategy = new HttpVoidResponseStrategy();

		// Assert:
		Assert.assertThat(strategy.getSupportedContentType(), IsNull.nullValue());
	}

	@Test
	public void nullIsReturnedIfRequestSucceedsAndNoDataIsReturned() throws Exception {
		// Arrange:
		final HttpVoidResponseStrategy strategy = new HttpVoidResponseStrategy();

		// Act:
		final Deserializer deserializer = ConnectUtils.coerceDeserializer(new byte[] {}, strategy);

		// Assert:
		Assert.assertThat(deserializer, IsNull.nullValue());
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionIfPeerReturnsDataWhenNoneIsExpected() throws Exception {
		// Arrange:
		final HttpVoidResponseStrategy strategy = new HttpVoidResponseStrategy();

		// Act:
		ConnectUtils.coerceDeserializer("some data".getBytes(), strategy);
	}
}