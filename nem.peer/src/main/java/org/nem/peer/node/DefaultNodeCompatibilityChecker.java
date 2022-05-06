package org.nem.peer.node;

import org.nem.core.node.*;

public class DefaultNodeCompatibilityChecker implements NodeCompatibilityChecker {

	@Override
	public boolean check(final NodeMetaData local, final NodeMetaData remote) {
		return local.getNetworkId() == remote.getNetworkId() && this.check(local.getVersion(), remote.getVersion());
	}

	private boolean check(final NodeVersion local, final NodeVersion remote) {
		// always communicate with 0-version builds (likely developer builds) to facilitate testing
		// otherwise, require matching major and minor versions
		return isZero(local) || isZero(remote) || majorMinorMatches(local, remote);
	}

	private static boolean isZero(final NodeVersion version) {
		return new NodeVersion(0, 0, 0, version.getTag()).equals(version);
	}

	private static boolean majorMinorMatches(final NodeVersion lhs, final NodeVersion rhs) {
		if (lhs.getMajorVersion() != rhs.getMajorVersion()) {
			return false;
		}

		if (lhs.getMinorVersion() == rhs.getMinorVersion()) {
			return true;
		}

		if (0 != lhs.getMajorVersion()) {
			return false;
		}

		// allow 0.6.*.* and 0.7.*.* nodes to connect with each other
		final int minMinorVersion = Math.min(lhs.getMinorVersion(), rhs.getMinorVersion());
		final int maxMinorVersion = Math.max(lhs.getMinorVersion(), rhs.getMinorVersion());
		return 6 == minMinorVersion && 7 == maxMinorVersion;
	}
}
