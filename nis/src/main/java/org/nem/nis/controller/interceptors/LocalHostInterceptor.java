package org.nem.nis.controller.interceptors;

import org.nem.nis.controller.annotations.TrustedApi;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.*;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Interceptor that rejects remote calls to client APIs.
 */
public class LocalHostInterceptor extends HandlerInterceptorAdapter {
	private static final Logger LOGGER = Logger.getLogger(AuditInterceptor.class.getName());

	private final LocalHostDetector localHostDetector;

	/**
	 * Creates a new interceptor.
	 *
	 * @param localHostDetector A predicate for determining whether or not a request is local.
	 */
	public LocalHostInterceptor(final LocalHostDetector localHostDetector) {
		this.localHostDetector = localHostDetector;
	}

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {
		final HandlerMethod handlerMethod = (HandlerMethod)handler;
		final Method method = handlerMethod.getMethod();
		final boolean isTrustedApi = method.isAnnotationPresent(TrustedApi.class);

		if (!isTrustedApi || this.localHostDetector.isLocal(request)) {
			return true;
		}

		final String message = String.format("remote %s attempted to call local %s", request.getRemoteAddr(), request.getRequestURI());
		LOGGER.warning(message);
		throw new UnauthorizedAccessException(message);
	}
}
