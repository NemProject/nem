package org.nem.peer.node;

import org.nem.core.node.NodeVersion;

public class DefaultNodeCompatibilityCheck implements NodeCompatibilityCheck {

	@Override
	public boolean check(final NodeVersion local, final NodeVersion remote) {
		// always communicate with 0-version builds (likely developer builds) to facilitate testing
		// otherwise, require matching major and minor versions
		return isZero(local) || isZero(remote) || majorMinorMatches(local, remote);
	}

	private static boolean isZero(final NodeVersion version) {
		return new NodeVersion(0, 0, 0, version.getTag()).equals(version);
	}

	private static boolean majorMinorMatches(final NodeVersion lhs, final NodeVersion rhs) {
		return lhs.getMajorVersion() == rhs.getMajorVersion() && lhs.getMinorVersion() == rhs.getMinorVersion();
	}
}
