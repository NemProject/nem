package org.nem.nis.controller.interceptors;

import org.nem.core.time.TimeProvider;
import org.nem.nis.audit.*;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor that audits requests.
 */
public class AuditInterceptor extends HandlerInterceptorAdapter {

	private final AuditCollection auditCollection;
	private final TimeProvider timeProvider;
	private final AtomicInteger counter = new AtomicInteger(0);

	/**
	 * Creates a new audit interceptor.
	 *
	 * @param auditCollection The audit collection.
	 * @param timeProvider The time provider.
	 */
	public AuditInterceptor(final AuditCollection auditCollection, final TimeProvider timeProvider) {
		this.auditCollection = auditCollection;
		this.timeProvider = timeProvider;
	}

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {
		this.auditCollection.add(this.createAuditEntry(this.counter.incrementAndGet(), request));
		return true;
	}

	@Override
	public void afterCompletion(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler,
			final Exception ex)
			throws Exception {
		this.auditCollection.remove(this.createAuditEntry(-1, request));
	}

	private AuditEntry createAuditEntry(final int id, final HttpServletRequest request) {
		return new AuditEntry(id, request.getRemoteAddr(), request.getServletPath(), this.timeProvider);
	}
}
