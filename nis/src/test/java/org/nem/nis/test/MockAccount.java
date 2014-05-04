package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.test.Utils;

import java.util.*;

/**
 * Mock Account implementation that allows the setting of coin days.
 * TODO: we need a better way to set coin days
 */
public class MockAccount extends Account {

	private final Map<BlockHeight, Amount> heightToCoinDaysMap;

	/**
	 * Creates a new mock account with a random address.
	 */
	public MockAccount(){
		super(Utils.generateRandomAddress());
		this.heightToCoinDaysMap = new HashMap<>();
	}

	/**
	 * Sets coin days at the specified block height.
	 *
	 * @param coinDays The coin days.
	 * @param blockHeight The block height.
	 */
	public void setCoinDaysAt(final Amount coinDays, final BlockHeight blockHeight) {
		this.heightToCoinDaysMap.put(blockHeight, coinDays);
	}

	@Override
	public Amount getCoinDayWeightedBalance(final BlockHeight blockHeight) {
		return this.heightToCoinDaysMap.getOrDefault(blockHeight, null);
	}
}