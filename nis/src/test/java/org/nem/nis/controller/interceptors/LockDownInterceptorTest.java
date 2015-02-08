package org.nem.nis.controller.interceptors;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LockDownInterceptorTest {
	@Test
	public void interceptorWorksCorrectlyWithoutLastBlock() {
		// Act:
		final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		final LockDownInterceptor interceptor = new LockDownInterceptor(lastBlockLayer);

		// Assert:
		assertAccessGranted(interceptor, "/status");
		assertAccessGranted(interceptor, "/chain/height");
		assertAccessDenied(interceptor, "/foobar");
	}

	@Test
	public void interceptorWorksCorrectlyWithLastBlock() {
		// Act:
		final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		final LockDownInterceptor interceptor = new LockDownInterceptor(lastBlockLayer);
		final DbBlock dbBlock = Mockito.mock(DbBlock.class);
		Mockito.when(dbBlock.getHeight()).thenReturn(1234L);
		Mockito.when(lastBlockLayer.getLastDbBlock()).thenReturn(dbBlock);

		// Assert:
		assertAccessGranted(interceptor, "/status");
		assertAccessGranted(interceptor, "/chain/height");
		assertAccessGranted(interceptor, "/foobar");
	}

	private static void assertAccessGranted(final LockDownInterceptor interceptor, final String requestUri) {
		// Act:
		final boolean result = preHandle(interceptor, requestUri);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	private static void assertAccessDenied(final LockDownInterceptor interceptor, final String requestUri) {
		// Act / Assert:
		ExceptionAssert.assertThrows(v -> preHandle(interceptor, requestUri), UnauthorizedAccessException.class);
	}

	public static boolean preHandle(final LockDownInterceptor interceptor, final String requestUri) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRequestURI()).thenReturn(requestUri);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		final HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);

		// Act:
		return ExceptionUtils.propagate(() -> interceptor.preHandle(request, response, handlerMethod));
	}
}
