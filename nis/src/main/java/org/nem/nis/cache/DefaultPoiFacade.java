package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A repository of all mutable NEM account state.
 */
public class DefaultPoiFacade implements PoiFacade, CopyableCache<DefaultPoiFacade> {
	private final ImportanceCalculator importanceCalculator;
	private BlockHeight lastPoiRecalculationHeight;
	private int lastPoiVectorSize;

	/**
	 * Creates a new poi facade.
	 *
	 * @param importanceCalculator The importance calculator to use.
	 */
	public DefaultPoiFacade(final ImportanceCalculator importanceCalculator) {
		this.importanceCalculator = importanceCalculator;
	}

	@Override
	public int getLastPoiVectorSize() {
		return this.lastPoiVectorSize;
	}

	@Override
	public BlockHeight getLastPoiRecalculationHeight() {
		return this.lastPoiRecalculationHeight;
	}

	@Override
	public void recalculateImportances(final BlockHeight blockHeight) {
		if (null != this.lastPoiRecalculationHeight && 0 == this.lastPoiRecalculationHeight.compareTo(blockHeight)) {
			return;
		}

		final Collection<AccountState> accountStates = this.getAccountStates(blockHeight);
		this.lastPoiVectorSize = accountStates.size();
		this.importanceCalculator.recalculate(blockHeight, accountStates);
		this.lastPoiRecalculationHeight = blockHeight;
	}

	private Collection<AccountState> getAccountStates(final BlockHeight blockHeight) {
		// TODO 20141212 figure out what to do here!
		return null;
		//return this.addressToStateMap.values().stream()
		//		.filter(a -> shouldIncludeInImportanceCalculation(a, blockHeight))
		//		.collect(Collectors.toList());
	}

	private static boolean shouldIncludeInImportanceCalculation(final AccountState accountState, final BlockHeight blockHeight) {
		return null != accountState.getHeight()
				&& accountState.getHeight().compareTo(blockHeight) <= 0
				&& !accountState.getAddress().equals(NemesisBlock.ADDRESS);
	}

	@Override
	public void shallowCopyTo(final DefaultPoiFacade rhs) {
		rhs.lastPoiRecalculationHeight = this.lastPoiRecalculationHeight;
	}

	@Override
	public DefaultPoiFacade copy() {
		final DefaultPoiFacade copy = new DefaultPoiFacade(this.importanceCalculator);
		copy.lastPoiRecalculationHeight = this.lastPoiRecalculationHeight;
		return copy;
	}
}
