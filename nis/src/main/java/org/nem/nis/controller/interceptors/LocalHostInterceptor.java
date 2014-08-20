package org.nem.nis.controller.interceptors;

import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.controller.annotations.TrustedApi;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.logging.Logger;

/**
 * Interceptor that rejects remote calls to client APIs.
 */
public class LocalHostInterceptor extends HandlerInterceptorAdapter {
	private static final Logger LOGGER = Logger.getLogger(AuditInterceptor.class.getName());

	final InetAddress[] localAddresses;

	/**
	 * Creates a new interceptor.
	 */
	public LocalHostInterceptor() {
		this.localAddresses = new InetAddress[] {
				parseAddress("127.0.0.1"),
				parseAddress("0:0:0:0:0:0:0:1")
		};
	}

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {
		final HandlerMethod handlerMethod = (HandlerMethod)handler;
		final Method method = handlerMethod.getMethod();
		final boolean isTrustedApi = method.isAnnotationPresent(TrustedApi.class);

		if (!isTrustedApi) {
			return true;
		}

		final InetAddress remoteAddress = parseAddress(request.getRemoteAddr());
		for (final InetAddress localAddress : this.localAddresses) {
			if (localAddress.equals(remoteAddress)) {
				return true;
			}
		}

		final String message = String.format("remote %s attempted to call local %s", request.getRemoteAddr(), request.getRequestURI());
		LOGGER.warning(message);
		throw new UnauthorizedAccessException(message);
	}

	private static InetAddress parseAddress(final String address) {
		return ExceptionUtils.propagate(
				() -> InetAddress.getByName(address),
				IllegalArgumentException::new);
	}
}
