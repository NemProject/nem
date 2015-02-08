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

	//region without last block

	@Test
	public void preHandleAllowsIgnoredApiPathRequestsWithoutLastBlock() {
		// Arrange:
		final LockDownInterceptor interceptor = createInterceptorWithoutLastBlock();

		// Assert:
		assertAccessGranted(interceptor, "/status");
		assertAccessGranted(interceptor, "/chain/height");
	}

	@Test
	public void preHandleAllowsCaseInsensitiveIgnoredApiPathRequestsWithoutLastBlock() {
		// Arrange:
		final LockDownInterceptor interceptor = createInterceptorWithoutLastBlock();

		// Assert:
		assertAccessGranted(interceptor, "/StaTus");
		assertAccessGranted(interceptor, "/Chain/HeiGhT");
	}

	@Test
	public void preHandleBlocksNonIgnoredApiPathRequestsWithoutLastBlock() {
		// Arrange:
		final LockDownInterceptor interceptor = createInterceptorWithoutLastBlock();

		// Assert:
		assertAccessDenied(interceptor, "/foobar");
	}

	private static LockDownInterceptor createInterceptorWithoutLastBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		return new LockDownInterceptor(lastBlockLayer);
	}

	//endregion

	//region with last block

	@Test
	public void preHandleAllowsIgnoredApiPathRequestsWithLastBlock() {
		// Arrange:
		final LockDownInterceptor interceptor = createInterceptorWithLastBlock();

		// Assert:
		assertAccessGranted(interceptor, "/status");
		assertAccessGranted(interceptor, "/chain/height");
	}

	@Test
	public void preHandleAllowsNonIgnoredApiPathRequestsWithLastBlock() {
		// Arrange:
		final LockDownInterceptor interceptor = createInterceptorWithLastBlock();

		// Assert:
		assertAccessGranted(interceptor, "/foobar");
	}

	private static LockDownInterceptor createInterceptorWithLastBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		Mockito.when(lastBlockLayer.getLastDbBlock()).thenReturn(new DbBlock());
		return new LockDownInterceptor(lastBlockLayer);
	}

	//endregion

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
