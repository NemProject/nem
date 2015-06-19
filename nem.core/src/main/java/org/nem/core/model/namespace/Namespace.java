package org.nem.core.model.namespace;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;

/**
 * Represents a namespace that is owned by an account.
 * The ownership is temporary and therefore has an expiry block height.
 */
public class Namespace {
	private final NamespaceId id;
	private final Account owner;
	private final BlockHeight expiryHeight;

	/**
	 * Creates a new namespace.
	 *
	 * @param id The namespace id.
	 * @param owner The owner address.
	 * @param expiryHeight The block height at which the ownership expires.
	 */
	public Namespace(
			final NamespaceId id,
			final Account owner,
			final BlockHeight expiryHeight) {
		this.id = id;
		this.owner = owner;
		this.expiryHeight = expiryHeight;
	}

	/**
	 * Gets the namespace id.
	 *
	 * @return The namespace id.
	 */
	public NamespaceId getId() {
		return this.id;
	}

	/**
	 * Gets the owner of the namespace.
	 *
	 * @return The owner.
	 */
	public Account getOwner() {
		return this.owner;
	}

	/**
	 * Gets the height at which the ownership expires.
	 *
	 * @return The height at which ownership expires.
	 */
	public BlockHeight getExpiryHeight() {
		return this.expiryHeight;
	}

	/**
	 * Returns a value indicating whether ownership has expired at a given block height.
	 *
	 * @param height The height to test.
	 * @return true if the ownership has not expired, false otherwise.
	 */
	public boolean isActive(final BlockHeight height) {
		return this.expiryHeight.compareTo(height) > 0;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Namespace)) {
			return false;
		}

		final Namespace rhs = (Namespace)obj;
		return this.id.equals(rhs.id);
	}
}
