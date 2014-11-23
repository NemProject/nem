package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.remote.RemoteLinks;
import org.nem.nis.secret.*;

import java.util.*;

/**
 * Class containing extrinsic NIS-account information that is used to calculate POI.
 */
public class PoiAccountState {
	private final Address address;
	private final AccountImportance importance;
	private final WeightedBalances weightedBalances;
	private final RemoteLinks remoteLinks;
	private final MultisigLinks multisigLinks;
	private BlockHeight height;

	/**
	 * Creates a new NIS account state.
	 */
	public PoiAccountState(final Address address) {
		this(address, new AccountImportance(), new WeightedBalances(), new RemoteLinks(), new MultisigLinks(), null);
	}

	private PoiAccountState(
			final Address address,
			final AccountImportance importance,
			final WeightedBalances weightedBalances,
			final RemoteLinks remoteLinks,
			final MultisigLinks multisigLinks,
			final BlockHeight height) {
		this.address = address;
		this.importance = importance;
		this.weightedBalances = weightedBalances;
		this.remoteLinks = remoteLinks;
		this.multisigLinks = multisigLinks;
		this.height = height;
	}

	/**
	 * Gets the account address.
	 *
	 * @return The account address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the weighted balances.
	 *
	 * @return The weighted balances.
	 */
	public WeightedBalances getWeightedBalances() {
		return this.weightedBalances;
	}

	/**
	 * Gets the importance information.
	 *
	 * @return The importance information.
	 */
	public AccountImportance getImportanceInfo() {
		return this.importance;
	}

	/**
	 * Gets the remote link information.
	 *
	 * @return The remote link information.
	 */
	public RemoteLinks getRemoteLinks() {
		return this.remoteLinks;
	}


	public void addMultisig(final Address multisigAddress, final BlockHeight height) {
		this.multisigLinks.addMultisig(multisigAddress, height);
	}

	public void addCosignatory(final Address cosignatoryAddress, final BlockHeight height) {
		this.multisigLinks.addCosignatory(cosignatoryAddress, height);
	}

	public void removeMultisig(final Address multisigAddress, final BlockHeight height) {
		this.multisigLinks.removeMultisig(multisigAddress, height);
	}

	public void removeCosignatory(final Address cosignatoryAddress, final BlockHeight height) {
		this.multisigLinks.removeCosignatory(cosignatoryAddress, height);
	}

	/**
	 * Returns height of an account.
	 *
	 * @return The height of an account - when the account has been created.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Sets height of an account if the account does not already have a height.
	 *
	 * @param height The height.
	 */
	public void setHeight(final BlockHeight height) {
		if (null == this.height) {
			this.height = height;
		}
	}

	/**
	 * Creates a copy of this state.
	 *
	 * @return A copy of this state.
	 */
	public PoiAccountState copy() {
		return new PoiAccountState(
				this.address,
				this.importance.copy(),
				this.weightedBalances.copy(),
				this.remoteLinks.copy(),
				this.multisigLinks.copy(),
				this.height);
	}
}