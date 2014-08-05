package org.nem.peer.test;

import org.nem.core.crypto.KeyPair;
import org.nem.core.node.NodeIdentity;

/**
 * A weak node identity that uses a name as an identifier.
 */
public class WeakNodeIdentity extends NodeIdentity {

	private final String name;

	/**
	 * Creates a new weak node identity with the specified name.
	 *
	 * @param name The name.
	 */
	public WeakNodeIdentity(final String name) {
		super(new KeyPair(), name);
		this.name = name;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof WeakNodeIdentity))
			return false;

		final WeakNodeIdentity rhs = (WeakNodeIdentity)obj;
		return this.name.equals(rhs.name);
	}

	@Override
	public String toString() {
		return String.format("(Weak Id) %s", this.name);
	}
}
