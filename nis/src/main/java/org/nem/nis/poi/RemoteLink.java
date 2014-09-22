package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;

import java.util.Objects;

/**
 * Represents a link between an account and a remote account.
 */
public class RemoteLink {
	/**
	 * The remote link owner.
	 */
	public enum Owner {
		/**
		 * The owning account is a remote harvester.
		 */
		RemoteHarvester,

		/**
		 * The owning account is a harvesting remotely.
		 */
		HarvestingRemotely
	}

	private final Address address;
	private final BlockHeight height;
	private final int mode;
	private final Owner owner;

	/**
	 * Creates a new link.
	 *
	 * @param address The linked account address.
	 * @param height The effective height of the link.
	 * @param mode The link mode.
	 * @param owner The link owner.
	 */
	public RemoteLink(final Address address, final BlockHeight height, final int mode, final Owner owner) {
		this.address = address;
		this.height = height;
		this.mode = mode;
		this.owner = owner;
	}

	/**
	 * Gets the link owner.
	 *
	 * @return The link owner.
	 */
	public Owner getOwner() {
		return this.owner;
	}

	/**
	 * Gets the address of the linked account.
	 *
	 * @return The address of the linked account.
	 */
	public Address getLinkedAddress() {
		return this.address;
	}

	/**
	 * Gets the height at which the link is effective.
	 *
	 * @return The effective height of the link.
	 */
	public BlockHeight getEffectiveHeight() {
		return this.height;
	}

	/**
	 * Gets the link mode.
	 *
	 * @return The link mode.
	 */
	public int getMode() {
		return this.mode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.owner, this.address, this.height, this.mode);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof RemoteLink)) {
			return false;
		}

		final RemoteLink rhs = (RemoteLink)obj;
		return this.owner == rhs.owner &&
				this.address.equals(rhs.address) &&
				this.height.equals(rhs.height) &&
				this.mode == rhs.mode;
	}
}
