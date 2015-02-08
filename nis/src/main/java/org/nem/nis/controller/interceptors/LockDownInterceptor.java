package org.nem.nis.controller.interceptors;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.NisIllegalStateException;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.logging.Logger;

/**
 * An interceptor that prevents access to most NIS functions while the block chain is loading.
 * TODO 20150208 J-G: might want to rename this to something else like 'BlockLoadingInterceptor'
 */
public class LockDownInterceptor extends HandlerInterceptorAdapter {
	private static final Logger LOGGER = Logger.getLogger(LockDownInterceptor.class.getName());

	private final BlockChainLastBlockLayer lastBlockLayer;
	private final List<String> ignoredApiPaths = Arrays.asList("/chain/height", "/status");

	/**
	 * Creates a new lock down interceptor.
	 *
	 * @param lastBlockLayer The last block layer.
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
			final String uri = request.getRequestURI();
			if (this.ignoredApiPaths.stream().anyMatch(uri::equalsIgnoreCase)) {
				return true;
			}

			final DbBlock block = this.lastBlockLayer.getCurrentDbBlock();
			final String message = String.format("Can't perform any actions until db is fully loaded %s", block == null ? BlockHeight.ONE : block.getHeight());
			LOGGER.warning(message);
			throw new NisIllegalStateException(NisIllegalStateException.Reason.NIS_ILLEGAL_STATE_LOADING_CHAIN);
		}

		return true;
	}
}
