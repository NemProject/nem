package org.nem.nis.controller.interceptors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.controller.annotations.TrustedApi;
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
	public void remoteAccessIsNotAllowedForTrustedApi() {
		// Assert:
		assertAccessDenied("trustedMethod", "194.66.82.11");
		assertAccessDenied("trustedMethod", "127.0.0.10");
		assertAccessDenied("trustedMethod", "0:0:0:0:0:0:0:10");
	}

	@Test
	public void localIpv4AccessIsAllowedForTrustedApi() {
		// Assert:
		assertAccessGranted("trustedMethod", "127.0.0.1");
	}

	@Test
	public void localIpv6AccessIsAllowedForTrustedApi() {
		// Assert:
		assertAccessGranted("trustedMethod", "0:0:0:0:0:0:0:1");
	}

	@Test
	public void interceptorCannotBeCreatedAroundInvalidAdditionalLocalIpAddresses() {
		// Act:
		ExceptionAssert.assertThrows(
				v -> new LocalHostInterceptor(new String[] { "not a host" }),
				IllegalArgumentException.class);
	}

	@Test
	public void interceptorCanBeCreatedWithAdditionalLocalIpAddresses() {
		// Act:
		final LocalHostInterceptor interceptor = new LocalHostInterceptor(new String[] { "194.66.82.11", "0:0:0:0:0:0:0:10" });

		// Assert:
		assertAccessGranted(interceptor, "trustedMethod", "194.66.82.11");
		assertAccessDenied(interceptor, "trustedMethod", "127.0.0.10");
		assertAccessGranted(interceptor, "trustedMethod", "0:0:0:0:0:0:0:10");
	}

	private static void assertAccessGranted(final String methodName, final String remoteAddress) {
		// Assert:
		assertAccessGranted(new LocalHostInterceptor(), methodName, remoteAddress);
	}

	private static void assertAccessGranted(final LocalHostInterceptor interceptor, final String methodName, final String remoteAddress) {
		// Act:
		final boolean result = preHandle(interceptor, methodName, remoteAddress);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	private static void assertAccessDenied(final String methodName, final String remoteAddress) {
		// Assert:
		assertAccessDenied(new LocalHostInterceptor(), methodName, remoteAddress);
	}

	private static void assertAccessDenied(final LocalHostInterceptor interceptor, final String methodName, final String remoteAddress) {
		// Act / Assert:
		ExceptionAssert.assertThrows(v -> preHandle(interceptor, methodName, remoteAddress), UnauthorizedAccessException.class);
	}

	public static boolean preHandle(final LocalHostInterceptor interceptor, final String methodName, final String remoteAddress) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRemoteAddr()).thenReturn(remoteAddress);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		final HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);
		final Method method = ExceptionUtils.propagate(() -> LocalHostInterceptorTest.class.getMethod(methodName));
		Mockito.when(handlerMethod.getMethod()).thenReturn(method);

		// Act:
		return ExceptionUtils.propagate(() -> interceptor.preHandle(request, response, handlerMethod));
	}

	//region annotated methods

	public static void unannotatedMethod() {
	}

	@TrustedApi
	public static void trustedMethod() {
	}

	//endregion
}