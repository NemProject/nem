package org.nem.peer.node;

public class DefaultNodeVersionCheck implements NodeVersionCheck {

	@Override
	public boolean check(final NodeVersion local, final NodeVersion remote) {
		return true;
	}
}
