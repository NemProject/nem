package org.nem.core.connect;

import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.*;

import java.io.*;

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
		final HttpResponse response = Mockito.mock(HttpResponse.class);
		ConnectUtils.mockStatusCode(response, 200);

		final HttpEntity entity = Mockito.mock(HttpEntity.class);
		Mockito.when(response.getEntity()).thenReturn(entity);
		Mockito.when(entity.getContent()).thenThrow(new IOException());

		// Act:
		strategy.coerce(Mockito.mock(HttpRequestBase.class), response);
	}
}
