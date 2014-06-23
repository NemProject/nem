package org.nem.nis.controller.interceptors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.audit.*;

import javax.servlet.http.*;

public class AuditInterceptorTest {

	@Test
	public void preHandleAddsToAuditCollection() throws Exception {
		// Arrange:
		final AuditCollection collection = Mockito.mock(AuditCollection.class);
		final AuditInterceptor interceptor = new AuditInterceptor(collection);
		final HttpServletRequest request = createRequest("127.0.0.10", "/foo/bar");

		// Act:
		final boolean result = interceptor.preHandle(request, Mockito.mock(HttpServletResponse.class), null);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(collection, Mockito.times(1)).add(new AuditEntry("127.0.0.10", "/foo/bar"));
	}

	@Test
	public void postHandleRemovesFromAuditCollection() throws Exception {
		// Arrange:
		final AuditCollection collection = Mockito.mock(AuditCollection.class);
		final AuditInterceptor interceptor = new AuditInterceptor(collection);
		final HttpServletRequest request = createRequest("127.0.0.10", "/foo/bar");

		// Act:
		interceptor.postHandle(request, Mockito.mock(HttpServletResponse.class), null, null);

		// Assert:
		Mockito.verify(collection, Mockito.times(1)).remove(new AuditEntry("127.0.0.10", "/foo/bar"));
	}

	private static HttpServletRequest createRequest(final String address, final String path) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(address);
		Mockito.when(request.getServletPath()).thenReturn(path);
		return request;
	}
}