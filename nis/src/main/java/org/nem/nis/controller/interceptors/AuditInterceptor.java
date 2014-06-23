package org.nem.nis.controller.interceptors;

import org.nem.nis.audit.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.*;

/**
 * Interceptor that audits requests.
 */
public class AuditInterceptor extends HandlerInterceptorAdapter {

	private final AuditCollection auditCollection;

	/**
	 * Creates a new audit interceptor.
	 */
	public AuditInterceptor(final AuditCollection auditCollection) {
		this.auditCollection = auditCollection;
	}

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {
		this.auditCollection.add(createAuditEntry(request));
		return true;
	}

	@Override
	public void postHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler,
			final ModelAndView modelAndView) throws Exception {
		this.auditCollection.remove(createAuditEntry(request));
	}

	private static AuditEntry createAuditEntry(final HttpServletRequest request) {
		return new AuditEntry(request.getRemoteAddr(), request.getServletPath());
	}
}
