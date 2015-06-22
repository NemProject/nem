package org.nem.core.model.namespace;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;

/**
 * Represents a namespace that is owned by an account.
 * The ownership is temporary and therefore associated with a block height.
 */
public class Namespace {
	private static final long BLOCKS_PER_YEAR = 1440 * 365;

	private final NamespaceId id;
	private final Account owner;
	private final BlockHeight height;

	/**
	 * Creates a new namespace.
	 *
	 * @param id The namespace id.
	 * @param owner The owner address.
	 * @param height The block height at which the ownership begins.
	 */
	public Namespace(
			final NamespaceId id,
			final Account owner,
			final BlockHeight height) {
		this.id = id;
		this.owner = owner;
		this.height = height;
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
	 * Gets the height at which the ownership begins.
	 *
	 * @return The height at which ownership begins.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Returns a value indicating whether ownership has expired at a given block height.
	 *
	 * @param height The height to test.
	 * @return true if the ownership has not expired, false otherwise.
	 */
	public boolean isActive(final BlockHeight height) {
		if (!this.getId().isRoot()) {
			throw new UnsupportedOperationException("call to isActive is only allowed for root namespaces");
		}

		return new BlockHeight(this.height.getRaw() + BLOCKS_PER_YEAR).compareTo(height) > 0 &&
				this.height.compareTo(height) <= 0;
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
