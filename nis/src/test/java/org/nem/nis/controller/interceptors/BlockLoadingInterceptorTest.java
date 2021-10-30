package org.nem.nis.controller.interceptors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.NisIllegalStateException;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.test.NisUtils;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.*;

public class BlockLoadingInterceptorTest {

	// region without last block

	@Test
	public void preHandleAllowsIgnoredApiPathRequestsWithoutLastBlock() {
		// Arrange:
		final BlockLoadingInterceptor interceptor = createInterceptorWithoutLastBlock();

		// Assert:
		assertAccessGranted(interceptor, "/status");
		assertAccessGranted(interceptor, "/chain/height");
	}

	@Test
	public void preHandleAllowsCaseInsensitiveIgnoredApiPathRequestsWithoutLastBlock() {
		// Arrange:
		final BlockLoadingInterceptor interceptor = createInterceptorWithoutLastBlock();

		// Assert:
		assertAccessGranted(interceptor, "/StaTus");
		assertAccessGranted(interceptor, "/Chain/HeiGhT");
	}

	@Test
	public void preHandleBlocksNonIgnoredApiPathRequestsWithoutLastBlock() {
		// Arrange:
		final BlockLoadingInterceptor interceptor = createInterceptorWithoutLastBlock();

		// Assert:
		assertAccessDenied(interceptor, "/foobar");
	}

	private static BlockLoadingInterceptor createInterceptorWithoutLastBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		Mockito.when(lastBlockLayer.isLoading()).thenReturn(true);
		return new BlockLoadingInterceptor(lastBlockLayer);
	}

	// endregion

	// region with last block

	@Test
	public void preHandleAllowsIgnoredApiPathRequestsWithLastBlock() {
		// Arrange:
		final BlockLoadingInterceptor interceptor = createInterceptorWithLastBlock();

		// Assert:
		assertAccessGranted(interceptor, "/status");
		assertAccessGranted(interceptor, "/chain/height");
	}

	@Test
	public void preHandleAllowsNonIgnoredApiPathRequestsWithLastBlock() {
		// Arrange:
		final BlockLoadingInterceptor interceptor = createInterceptorWithLastBlock();

		// Assert:
		assertAccessGranted(interceptor, "/foobar");
	}

	private static BlockLoadingInterceptor createInterceptorWithLastBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		Mockito.when(lastBlockLayer.isLoading()).thenReturn(false);
		return new BlockLoadingInterceptor(lastBlockLayer);
	}

	// endregion

	private static void assertAccessGranted(final BlockLoadingInterceptor interceptor, final String requestUri) {
		// Act:
		final boolean result = preHandle(interceptor, requestUri);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(true));
	}

	private static void assertAccessDenied(final BlockLoadingInterceptor interceptor, final String requestUri) {
		// Act / Assert:
		NisUtils.assertThrowsNisIllegalStateException(v -> preHandle(interceptor, requestUri),
				NisIllegalStateException.Reason.NIS_ILLEGAL_STATE_LOADING_CHAIN);
	}

	private static boolean preHandle(final BlockLoadingInterceptor interceptor, final String requestUri) {
		// Arrange:
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getRequestURI()).thenReturn(requestUri);
		final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

		final HandlerMethod handlerMethod = Mockito.mock(HandlerMethod.class);

		// Act:
		return ExceptionUtils.propagate(() -> interceptor.preHandle(request, response, handlerMethod));
	}
}
