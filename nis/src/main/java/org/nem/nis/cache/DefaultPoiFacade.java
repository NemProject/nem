package org.nem.nis.cache;

import org.nem.core.model.NemesisBlock;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.*;
import org.nem.nis.state.AccountState;

import java.math.BigInteger;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A repository of all mutable NEM account state.
 */
public class DefaultPoiFacade implements PoiFacade, CopyableCache<DefaultPoiFacade> {
	/**
	 * Number of blocks that should be treated as a group for POI purposes.
	 * In other words, POI importances will only be calculated at blocks that
	 * are a multiple of this grouping number.
	 */
	private static final int POI_GROUPING = 359;

	/**
	 * BigInteger constant 2^64
	 */
	public static final BigInteger TWO_TO_THE_POWER_OF_64 = new BigInteger("18446744073709551616");

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
	public void recalculateImportances(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
		this.recalculateImportancesAtGroupedHeight(GroupedHeight.fromHeight(blockHeight), accountStates);
	}

	private void recalculateImportancesAtGroupedHeight(final BlockHeight blockHeight, Collection<AccountState> accountStates) {
		if (null != this.lastPoiRecalculationHeight && 0 == this.lastPoiRecalculationHeight.compareTo(blockHeight)) {
			return;
		}

		accountStates = accountStates.stream()
				.filter(a -> shouldIncludeInImportanceCalculation(a, blockHeight))
				.collect(Collectors.toList());

		this.lastPoiVectorSize = accountStates.size();
		this.importanceCalculator.recalculate(blockHeight, accountStates);
		this.lastPoiRecalculationHeight = blockHeight;
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
