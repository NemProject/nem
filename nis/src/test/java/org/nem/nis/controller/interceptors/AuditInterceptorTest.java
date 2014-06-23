package org.nem.nis.controller.interceptors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.time.TimeProvider;
import org.nem.nis.audit.*;

import javax.servlet.http.*;

public class AuditInterceptorTest {

	@Test
	public void preHandleAddsToAuditCollection() throws Exception {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final boolean result = context.interceptor.preHandle(context.request, context.response, null);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(context.collection, Mockito.times(1))
				.add(new AuditEntry(1, "127.0.0.10", "/foo/bar", context.timeProvider));
	}

	@Test
	public void afterCompletionRemovesFromAuditCollection() throws Exception {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.interceptor.afterCompletion(context.request, context.response, null, null);

		// Assert:
		Mockito.verify(context.collection, Mockito.times(1))
				.remove(new AuditEntry(1, "127.0.0.10", "/foo/bar", context.timeProvider));
	}

	@Test
	public void interceptorGeneratesUniqueAuditEntryIds() throws Exception {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.interceptor.preHandle(createRequest("a", "a"), context.response, null);
		context.interceptor.preHandle(createRequest("b", "b"), context.response, null);
		context.interceptor.preHandle(createRequest("c", "c"), context.response, null);
		context.interceptor.afterCompletion(createRequest("b", "b"), context.response, null, null);
		context.interceptor.preHandle(createRequest("d", "d"), context.response, null);
		context.interceptor.afterCompletion(createRequest("d", "d"), context.response, null, null);
		context.interceptor.preHandle(createRequest("e", "e"), context.response, null);

		final boolean result = context.interceptor.preHandle(context.request, context.response, null);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Mockito.verify(context.collection, Mockito.times(1)).add(new AuditEntry(1, "a", "a", context.timeProvider));
		Mockito.verify(context.collection, Mockito.times(1)).add(new AuditEntry(2, "b", "b", context.timeProvider));
		Mockito.verify(context.collection, Mockito.times(1)).add(new AuditEntry(3, "c", "c", context.timeProvider));
		Mockito.verify(context.collection, Mockito.times(1)).add(new AuditEntry(4, "d", "d", context.timeProvider));
		Mockito.verify(context.collection, Mockito.times(1)).add(new AuditEntry(5, "e", "e", context.timeProvider));
	}

	private static HttpServletRequest createRequest(final String address, final String path) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(address);
		Mockito.when(request.getServletPath()).thenReturn(path);
		return request;
	}

	private static class TestContext {
		private final AuditCollection collection = Mockito.mock(AuditCollection.class);
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final AuditInterceptor interceptor = new AuditInterceptor(collection, timeProvider);
		private final HttpServletRequest request = createRequest("127.0.0.10", "/foo/bar");
		private final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
	}
}