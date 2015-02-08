package org.nem.nis.controller.interceptors;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.logging.Logger;

public class LockDownInterceptor extends HandlerInterceptorAdapter {
	private static final Logger LOGGER = Logger.getLogger(LockDownInterceptor.class.getName());

	private BlockChainLastBlockLayer lastBlockLayer;

	/**
	 * Creates a new audit interceptor.
	 */
	@Autowired
	public LockDownInterceptor(final BlockChainLastBlockLayer lastBlockLayer) {
		this.lastBlockLayer = lastBlockLayer;
	}

	@Override
	public boolean preHandle(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final Object handler) throws Exception {

		if (this.lastBlockLayer.getLastDbBlock() == null) {
			String uri = request.getRequestURI();
			if (uri.equals("/chain/height") || uri.equals("/status")) {
				return true;
			}

			final DbBlock block = this.lastBlockLayer.getCurrentDbBlock();
			final String message = String.format("Can't perform any actions until db is fully loaded %s", block == null ? BlockHeight.ONE : block.getHeight());
			LOGGER.warning(message);
			throw new UnauthorizedAccessException(message);
		}

		return true;
	}
}
