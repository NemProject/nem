package org.nem.nis.controller.interceptors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.controller.annotations.*;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.*;
import java.lang.reflect.Method;

public class LocalHostInterceptorTest {

	@Test
	public void remoteAccessIsAllowedForUnannotatedMethod() {
		// Assert:
		assertAccessGranted("unannotatedMethod", "194.66.82.11");
	}

	@Test
	public void remoteAccessIsAllowedForPublicApi() {
		// Assert:
		assertAccessGranted("publicMethod", "194.66.82.11");
	}

	@Test
	public void remoteAccessIsAllowedForPublicAndClientApi() {
		// Assert:
		assertAccessGranted("publicAndClientMethod", "194.66.82.11");
	}

	@Test
	public void remoteAccessIsNotAllowedForClientApi() {
		// Assert:
		assertAccessDenied("clientMethod", "194.66.82.11");
		assertAccessDenied("clientMethod", "127.0.0.10");
		assertAccessDenied("clientMethod", "0:0:0:0:0:0:0:10");
	}

	@Test
	public void localIpv4AccessIsAllowedForClientApi() {
		// Assert:
		assertAccessGranted("clientMethod", "127.0.0.1");
	}

	@Test
	public void localIpv6AccessIsAllowedForClientApi() {
		// Assert:
		assertAccessGranted("clientMethod", "0:0:0:0:0:0:0:1");
	}

	private static void assertAccessGranted(final String methodName, final String remoteAddress) {
		// Act:
		final boolean result = preHandle(methodName, remoteAddress);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	private static void assertAccessDenied(final String methodName, final String remoteAddress) {
		// Act / Assert:
		ExceptionAssert.assertThrows(v -> preHandle(methodName, remoteAddress), UnauthorizedAccessException.class);
	}

	public static boolean preHandle(final String methodName, final String remoteAddress) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(remoteAddress);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		final HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);
		final Method method = ExceptionUtils.propagate(() -> LocalHostInterceptorTest.class.getMethod(methodName));
		Mockito.when(handlerMethod.getMethod()).thenReturn(method);

		final LocalHostInterceptor interceptor = new LocalHostInterceptor();

		// Act:
		return ExceptionUtils.propagate(() -> interceptor.preHandle(request, response, handlerMethod));
	}

	//region annotated methods

	public static void unannotatedMethod() {
	}

	@PublicApi
	public static void publicMethod() {
	}

	@PublicApi
	@ClientApi
	public static void publicAndClientMethod() {
	}

	@ClientApi
	public static void clientMethod() {
	}

	//endregion
}