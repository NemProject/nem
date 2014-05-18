package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.test.Utils;

import java.util.*;

/**
 * Mock Account implementation that allows the setting of coin days.
 * TODO: we need a better way to set vested balance
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
	public Amount getVestedBalance(final BlockHeight blockHeight) {
		return this.heightToVestedBalanceMap.getOrDefault(blockHeight, null);
	}
}