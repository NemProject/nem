package org.nem.nis.controller.interceptors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.audit.AuditCollection;

import javax.servlet.http.*;

public class AuditInterceptorTest {

	//region preHandle

	@Test
	public void preHandleAddsToAuditCollection() throws Exception {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.interceptor.preHandle(context.request, context.response, null);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(context.collection, Mockito.times(1)).add("127.0.0.10", "/foo/bar");
	}

	@Test
	public void preHandleIgnoresHeartbeatRequests() throws Exception {
		// Arrange:
		final TestContext context = new TestContext("/heartbeat");

		// Act:
		final boolean result = context.interceptor.preHandle(context.request, context.response, null);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(context.collection, Mockito.times(0)).add(Mockito.any(), Mockito.any());
	}

	//endregion

	//region afterCompletion

	@Test
	public void afterCompletionRemovesFromAuditCollection() throws Exception {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.interceptor.afterCompletion(context.request, context.response, null, null);

		// Assert:
		Mockito.verify(context.collection, Mockito.times(1)).remove("127.0.0.10", "/foo/bar");
	}

	@Test
	public void afterCompletionIgnoresHeartbeatRequests() throws Exception {
		// Arrange:
		final TestContext context = new TestContext("/heartbeat");

		// Act:
		context.interceptor.afterCompletion(context.request, context.response, null, null);

		// Assert:
		Mockito.verify(context.collection, Mockito.times(0)).add(Mockito.any(), Mockito.any());
	}

	//endregion

	private static HttpServletRequest createRequest(final String address, final String path) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(address);
		Mockito.when(request.getServletPath()).thenReturn(path);
		return request;
	}

	private static class TestContext {
		private final AuditCollection collection = Mockito.mock(AuditCollection.class);
		private final AuditInterceptor interceptor = new AuditInterceptor(this.collection);
		private final HttpServletRequest request;
		private final HttpServletResponse response;

		private TestContext() {
			this("/foo/bar");
		}

		private TestContext(final String servletPath) {
			this.request = createRequest("127.0.0.10", servletPath);
			this.response = Mockito.mock(HttpServletResponse.class);
		}
	}
}