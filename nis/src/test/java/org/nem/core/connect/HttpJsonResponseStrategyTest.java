package org.nem.core.connect;

import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.*;

public class HttpJsonResponseStrategyTest {

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionOnHttpError() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(null);
		final HttpResponse response = Mockito.mock(HttpResponse.class);
		ConnectUtils.mockStatusCode(response, 500);

		// Act:
		strategy.coerce(Mockito.mock(HttpRequestBase.class), response);
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionOnIoError() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpDeserializerResponseStrategy(null);

		// Act:
		ConnectUtils.coerceStreamWithIoError(strategy);
	}
}
