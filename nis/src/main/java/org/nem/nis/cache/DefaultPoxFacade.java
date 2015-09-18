package org.nem.nis.cache;

import org.nem.core.model.Address;
import org.nem.core.model.NetworkInfos;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.pox.poi.GroupedHeight;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.state.AccountState;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A repository of all mutable NEM account state.
 */
public class DefaultPoxFacade implements PoxFacade, CopyableCache<DefaultPoxFacade> {
	private final ImportanceCalculator importanceCalculator;
	private BlockHeight lastPoxRecalculationHeight;
	private int lastPoxVectorSize;

	/**
	 * Creates a new pox facade.
	 *
	 * @param importanceCalculator The importance calculator to use.
	 */
	public DefaultPoxFacade(final ImportanceCalculator importanceCalculator) {
		this.importanceCalculator = importanceCalculator;
	}

	@Override
	public int getLastPoxVectorSize() {
		return this.lastPoxVectorSize;
	}

	@Override
	public BlockHeight getLastPoxRecalculationHeight() {
		return this.lastPoxRecalculationHeight;
	}

	@Override
	public void recalculateImportances(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
		this.recalculateImportancesAtGroupedHeight(GroupedHeight.fromHeight(blockHeight), accountStates);
	}

	private void recalculateImportancesAtGroupedHeight(final BlockHeight blockHeight, Collection<AccountState> accountStates) {
		if (null != this.lastPoxRecalculationHeight && 0 == this.lastPoxRecalculationHeight.compareTo(blockHeight)) {
			return;
		}

		accountStates = accountStates.stream()
				.filter(a -> shouldIncludeInImportanceCalculation(a, blockHeight))
				.collect(Collectors.toList());

		this.lastPoxVectorSize = accountStates.size();
		this.importanceCalculator.recalculate(blockHeight, accountStates);
		this.lastPoxRecalculationHeight = blockHeight;
	}

	private static boolean shouldIncludeInImportanceCalculation(final AccountState accountState, final BlockHeight blockHeight) {
		final Address nemesisAddress = NetworkInfos.getDefault().getNemesisBlockInfo().getAddress();
		return null != accountState.getHeight()
				&& accountState.getHeight().compareTo(blockHeight) <= 0
				&& !accountState.getAddress().equals(nemesisAddress);
	}

	@Override
	public void shallowCopyTo(final DefaultPoxFacade rhs) {
		rhs.lastPoxRecalculationHeight = this.lastPoxRecalculationHeight;
		rhs.lastPoxVectorSize = this.lastPoxVectorSize;
	}

	@Override
	public DefaultPoxFacade copy() {
		final DefaultPoxFacade copy = new DefaultPoxFacade(this.importanceCalculator);
		copy.lastPoxRecalculationHeight = this.lastPoxRecalculationHeight;
		copy.lastPoxVectorSize = this.lastPoxVectorSize;
		return copy;
	}
}
