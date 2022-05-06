package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.*;

/**
 * A writable mosaic entry.
 */
public class MosaicEntry implements ReadOnlyMosaicEntry {
	private final MosaicDefinition mosaicDefinition;
	private final MosaicBalances balances;
	private Supply supply;

	/**
	 * Creates a new mosaic entry.
	 *
	 * @param mosaicDefinition The mosaic definition.
	 */
	public MosaicEntry(final MosaicDefinition mosaicDefinition) {
		this(mosaicDefinition, new Supply(mosaicDefinition.getProperties().getInitialSupply()));
	}

	/**
	 * Creates a new mosaic entry.
	 *
	 * @param mosaicDefinition The mosaic definition.
	 * @param supply The supply.
	 */
	public MosaicEntry(final MosaicDefinition mosaicDefinition, final Supply supply) {
		this.mosaicDefinition = mosaicDefinition;
		this.supply = Supply.ZERO;
		this.balances = new MosaicBalances();
		this.increaseSupplyImpl(supply);
	}

	/**
	 * Creates a new mosaic entry.
	 *
	 * @param mosaicDefinition The mosaic definition.
	 * @param supply The supply.
	 * @param balances The balances.
	 */
	public MosaicEntry(final MosaicDefinition mosaicDefinition, final Supply supply, final MosaicBalances balances) {
		this.mosaicDefinition = mosaicDefinition;
		this.supply = supply;
		this.balances = balances;
	}

	@Override
	public MosaicDefinition getMosaicDefinition() {
		return this.mosaicDefinition;
	}

	@Override
	public Supply getSupply() {
		return this.supply;
	}

	@Override
	public MosaicBalances getBalances() {
		return this.balances;
	}

	/**
	 * Increases the supply of the current mosaic.
	 *
	 * @param increase The increase.
	 */
	public void increaseSupply(final Supply increase) {
		this.increaseSupplyImpl(increase);
	}

	private void increaseSupplyImpl(final Supply increase) {
		final int divisibility = this.mosaicDefinition.getProperties().getDivisibility();
		this.supply = MosaicUtils.add(divisibility, this.supply, increase);
		this.getBalances().incrementBalance(this.getCreatorAddress(), this.toQuantity(increase));
	}

	/**
	 * Decreases the supply of the current mosaic.
	 *
	 * @param decrease The decrease.
	 */
	public void decreaseSupply(final Supply decrease) {
		this.supply = this.getSupply().subtract(decrease);
		this.getBalances().decrementBalance(this.getCreatorAddress(), this.toQuantity(decrease));
	}

	private Quantity toQuantity(final Supply supply) {
		final int divisibility = this.mosaicDefinition.getProperties().getDivisibility();
		return MosaicUtils.toQuantity(supply, divisibility);
	}

	/**
	 * Creates a copy of this entry.
	 *
	 * @return A copy of this entry.
	 */
	public MosaicEntry copy() {
		return new MosaicEntry(this.mosaicDefinition, this.supply, this.balances.copy());
	}

	private Address getCreatorAddress() {
		return this.getMosaicDefinition().getCreator().getAddress();
	}
}
