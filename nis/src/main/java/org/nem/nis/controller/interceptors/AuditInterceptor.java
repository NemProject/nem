package org.nem.nis.controller.interceptors;

import org.nem.nis.audit.AuditCollection;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.*;
import java.util.logging.Logger;

/**
 * Interceptor that audits requests.
 */
public class AuditInterceptor extends HandlerInterceptorAdapter {
	private static final Logger LOGGER = Logger.getLogger(AuditInterceptor.class.getName());
	private static final String HEARTBEAT_PATH = "/heartbeat";

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
		final AuditEntry entry = new AuditEntry(request);
		if (entry.shouldIgnore()) {
			return true;
		}

		LOGGER.info(String.format("entering %s [%s]", entry.path, entry.host));
		this.auditCollection.add(entry.host, entry.path);
		return true;
	}

	@Override
	public void afterCompletion(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler,
			final Exception ex)
			throws Exception {
		final AuditEntry entry = new AuditEntry(request);
		if (entry.shouldIgnore()) {
			return;
		}

		this.auditCollection.remove(entry.host, entry.path);
		LOGGER.info(String.format("exiting %s [%s]", entry.path, entry.host));
	}

	private static class AuditEntry {
		private String host;
		private String path;

		private AuditEntry(final HttpServletRequest request) {
			this.host = request.getRemoteAddr();
			this.path = request.getRequestURI();
		}

		private boolean shouldIgnore() {
			return this.path.equalsIgnoreCase(HEARTBEAT_PATH);
		}
	}
}
