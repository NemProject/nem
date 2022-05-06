package org.nem.nis.controller.interceptors;

import org.nem.core.utils.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.*;
import java.util.*;

/**
 * Predicate for determining whether a request is local or remote.
 */
@SuppressWarnings("serial")
public class LocalHostDetector {
	private final List<AddressMatcher> matchers = new ArrayList<AddressMatcher>() {
		{
			this.add(new DefaultAddressMatcher(parseAddress("127.0.0.1")));
			this.add(new DefaultAddressMatcher(parseAddress("0:0:0:0:0:0:0:1")));
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
			this.matchers.add(createMatcher(ipAddress));
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
		return this.matchers.stream().anyMatch(matcher -> matcher.match(remoteAddress));
	}

	private static InetAddress parseAddress(final String address) {
		return ExceptionUtils.propagate(() -> InetAddress.getByName(address), IllegalArgumentException::new);
	}

	private static AddressMatcher createMatcher(final String ipAddress) {
		if (ipAddress.contains("*")) {
			return ipAddress.contains(".") ? new Ipv4WildcardAddressMatcher(ipAddress) : new Ipv6WildcardAddressMatcher(ipAddress);
		}

		return new DefaultAddressMatcher(parseAddress(ipAddress));
	}

	// region AddressMatcher

	private interface AddressMatcher {
		boolean match(final InetAddress address);
	}

	private static class DefaultAddressMatcher implements AddressMatcher {
		private final InetAddress address;

		public DefaultAddressMatcher(final InetAddress address) {
			this.address = address;
		}

		@Override
		public boolean match(final InetAddress address) {
			return this.address.equals(address);
		}
	}

	private static abstract class WildCardAddressMapper implements AddressMatcher {
		private final String partSeparator;
		private final int numParts;
		private final Class<? extends InetAddress> addressClass;
		private final String[] parts;

		public WildCardAddressMapper(final String partSeparator, final int numParts, final Class<? extends InetAddress> addressClass,
				final String address) {
			this.partSeparator = partSeparator;
			this.numParts = numParts;
			this.addressClass = addressClass;
			this.parts = this.getParts(address);
		}

		@Override
		public boolean match(final InetAddress address) {
			return this.addressClass.isAssignableFrom(address.getClass()) && this.match(this.getParts(address.getHostAddress()));
		}

		private boolean match(final String[] parts) {
			for (int i = 0; i < this.numParts; ++i) {
				if (!this.parts[i].equals("*") && !this.parts[i].equals(parts[i])) {
					return false;
				}
			}

			return true;
		}

		private String[] getParts(final String address) {
			final String[] parts = address.split(this.partSeparator);
			if (this.numParts != parts.length) {
				final String message = String.format("%s address must have %d parts", this.addressClass, this.numParts);
				throw new IllegalArgumentException(message);
			}

			return parts;
		}
	}

	private static class Ipv4WildcardAddressMatcher extends WildCardAddressMapper {

		public Ipv4WildcardAddressMatcher(final String address) {
			super("\\.", 4, Inet4Address.class, address);
		}
	}

	private static class Ipv6WildcardAddressMatcher extends WildCardAddressMapper {

		public Ipv6WildcardAddressMatcher(final String address) {
			super(":", 8, Inet6Address.class, address);
		}
	}

	// endregion
}
