package org.nem.core.connect;

import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.test.ConnectUtils;

import java.io.ByteArrayInputStream;

public class HttpDeserializerResponseStrategyTest {

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionOnHttpError() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpJsonResponseStrategy(null);
		final HttpResponse response = Mockito.mock(HttpResponse.class);
		ConnectUtils.mockStatusCode(response, 500);

		final HttpEntity entity = Mockito.mock(HttpEntity.class);
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] {});
		Mockito.when(response.getEntity()).thenReturn(entity);
		Mockito.when(entity.getContent()).thenReturn(inputStream);

		// Act:
		strategy.coerce(Mockito.mock(HttpRequestBase.class), response);
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionOnIoError() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpJsonResponseStrategy(null);

		// Act:
		ConnectUtils.coerceStreamWithIoError(strategy);
	}
}