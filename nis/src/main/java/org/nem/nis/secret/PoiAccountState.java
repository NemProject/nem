package org.nem.nis.secret;

/**
 * Class containing extrinsic NIS-account information that is used to calculate POI.
 */
public class PoiAccountState {
	private final AccountImportance importance;
	private final WeightedBalances weightedBalances;

	/**
	 * Creates a new NIS account state.
	 */
	public PoiAccountState() {
		this(new AccountImportance(), new WeightedBalances());
	}

	private PoiAccountState(final AccountImportance importance, final WeightedBalances weightedBalances) {
		this.importance = importance;
		this.weightedBalances = weightedBalances;
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
	 * Creates a copy of this state.
	 *
	 * @return A copy of this state.
	 */
	public PoiAccountState copy() {
		return new PoiAccountState(this.importance.copy(), this.weightedBalances.copy());
	}
}