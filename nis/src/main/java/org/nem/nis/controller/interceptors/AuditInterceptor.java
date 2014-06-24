package org.nem.nis.controller.interceptors;

import org.nem.nis.audit.*;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.*;

/**
 * Interceptor that audits requests.
 */
public class AuditInterceptor extends HandlerInterceptorAdapter {

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
		this.auditCollection.add(request.getRemoteAddr(), request.getServletPath());
		return true;
	}

	@Override
	public void afterCompletion(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler,
			final Exception ex)
			throws Exception {
		this.auditCollection.remove(request.getRemoteAddr(), request.getServletPath());
	}
}
