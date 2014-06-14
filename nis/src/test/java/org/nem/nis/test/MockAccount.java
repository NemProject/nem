package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

import java.util.*;

/**
 * Mock Account implementation that allows the setting of coin days.
 */
public class MockAccount extends Account {

	private final Map<BlockHeight, Amount> heightToVestedBalanceMap;

	/**
	 * Creates a new mock account with a random address.
	 */
	public MockAccount(){
		super(Utils.generateRandomAddress());
		this.heightToVestedBalanceMap = new HashMap<>();
	}

	/**
	 * Creates a new mock account with the specified address.
	 *
	 * @param address The address.
	 */
	public MockAccount(final Address address){
		super(address);
		this.heightToVestedBalanceMap = new HashMap<>();
	}

	/**
	 * Sets vested balance at the specified block height.
	 *
	 * @param vestedBalance The vested balance.
	 * @param blockHeight The block height.
	 */
	public void setVestedBalanceAt(final Amount vestedBalance, final BlockHeight blockHeight) {
		this.heightToVestedBalanceMap.put(blockHeight, vestedBalance);
	}

	@Override
	public WeightedBalances getWeightedBalances() {
		return new MockWeightedBalances();
	}

	private class MockWeightedBalances extends WeightedBalances  {

		@Override
		public Amount getVested(final BlockHeight blockHeight) {
			return heightToVestedBalanceMap.getOrDefault(blockHeight, null);
		}
	}
}
