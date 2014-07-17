package org.nem.nis.controller.interceptors;

import org.nem.nis.audit.*;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.*;
import java.util.logging.Logger;

/**
 * Interceptor that audits requests.
 */
public class AuditInterceptor extends HandlerInterceptorAdapter {
	private static final Logger LOGGER = Logger.getLogger(AuditInterceptor.class.getName());

	private final AuditCollection auditCollection;

	/**
	 * Creates a new audit interceptor.
	 *
	 * @param auditCollection The audit collection.
	 */
	public AuditInterceptor(final AuditCollection auditCollection) {
		this.auditCollection = auditCollection;
	}

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {
		if (! request.getServletPath().equals("/heartbeat")) {
			LOGGER.info(String.format("entering %s [%s]", request.getServletPath(), request.getRemoteAddr()));
			this.auditCollection.add(request.getRemoteAddr(), request.getServletPath());
		}
		return true;
	}

	@Override
	public void afterCompletion(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler,
			final Exception ex)
			throws Exception {
		if (! request.getServletPath().equals("/heartbeat")) {
			this.auditCollection.remove(request.getRemoteAddr(), request.getServletPath());
			LOGGER.info(String.format("exiting %s [%s]", request.getServletPath(), request.getRemoteAddr()));
		}
	}
}
