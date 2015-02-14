package org.nem.nis.controller.interceptors;

import org.nem.core.utils.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.*;

/**
 * Predicate for determining whether a request is local or remote.
 */
public class LocalHostDetector {
	private final List<InetAddress> localAddresses = new ArrayList<InetAddress>() {
		{
			this.add(parseAddress("127.0.0.1"));
			this.add(parseAddress("0:0:0:0:0:0:0:1"));
		}
	};

	/**
	 * Creates a new detector.
	 */
	public LocalHostDetector() {
	}

	/**
	 * Creates a new detector with additional local ip addresses.
	 *
	 * @param additionalLocalIpAddresses The additional local ip addresses.
	 */
	public LocalHostDetector(final String[] additionalLocalIpAddresses) {
		for (final String ipAddress : additionalLocalIpAddresses) {
			this.localAddresses.add(parseAddress(ipAddress));
		}
	}

	/**
	 * Gets a value indicating whether or not the specified request is local.
	 *
	 * @param request The request.
	 * @return true if the request is local.
	 */
	public boolean isLocal(final HttpServletRequest request) {
		final InetAddress remoteAddress = parseAddress(request.getRemoteAddr());
		return this.localAddresses.stream().anyMatch(localAddress -> localAddress.equals(remoteAddress));
	}

	private static InetAddress parseAddress(final String address) {
		return ExceptionUtils.propagate(
				() -> InetAddress.getByName(address),
				IllegalArgumentException::new);
	}
}
