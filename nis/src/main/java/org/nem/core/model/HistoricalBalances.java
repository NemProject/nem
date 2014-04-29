package org.nem.core.model;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.nem.nis.BlockChain;
import org.springframework.beans.factory.annotation.Autowired;

public class HistoricalBalances {

	/**
	 * Limit of history of balances (just not to let the list grow infinitely)
	 */
	public final long MAX_HISTORY = BlockChain.ESTIMATED_BLOCKS_PER_DAY + BlockChain.REWRITE_LIMIT;
	
	private final ArrayList<HistoricalBalance> balances = new ArrayList<>();
	
	/**
	 * The block chain
	 */
	private BlockChain blockchain;

	@Autowired(required = true)
	HistoricalBalances(final BlockChain blockchain) {
		this.blockchain = blockchain;
	}
	
	/**
	 * Gets the size of the list
	 * 
	 * @return the size of the list
	 */
	public int size() {
		return balances.size();
	}

	/**
	 * Gets the historical balance at a given block height
	 * 
	 * @param height the height at which to retrieve the balance
	 * 
	 * @return the historical balance
	 */
	public HistoricalBalance getHistoricalBalance(final BlockHeight height) {
		long lastBlockHeight = blockchain.getLastDbBlock().getHeight();
		if (lastBlockHeight - height.getRaw() > MAX_HISTORY || height.getRaw() < 1) {
			throw new InvalidParameterException("Historical balances are only available for the last " + MAX_HISTORY + " blocks.");
		}
		if (lastBlockHeight < height.getRaw()) {
			throw new InvalidParameterException("Future historical balances are not known.");
		}
		if (balances.size() == 0) {
			return new HistoricalBalance(new BlockHeight(height.getRaw()), Amount.ZERO);
		}

		// Collections.binarySearch returns an index.
		// If index >= 0 a historical balance with the same block height was found.
		// If index < 0 then index = -(insertion point)-1 where insertion point is the point where the historical balance would be inserted.
		int index = Collections.binarySearch(balances, new HistoricalBalance(height, null));
		if (index == -1) {
			// Insertion point would be at the beginning of the list.
			// This can only happen if the first nem appeared on the account AFTER the given block height.
			return new HistoricalBalance(new BlockHeight(height.getRaw()), new Amount(0));
		}
		if (index < -1) {
			// index = insertion point - 1
			index = -index - 2;
		}
		HistoricalBalance balance = balances.get(index);
		return new HistoricalBalance(balance.getHeight().getRaw(), balance.getBalance().getNumMicroNem());
	}
	
	/**
	 * Add an amount at a given block height.
	 * Add the amount to all historical balances with bigger height.
	 * 
	 * @param height the height where the amount is inserted
	 * @param amount the amount to add
	 */
	public void add(final BlockHeight height, final Amount amount) {
		int startIndex = -1;
		int index = Collections.binarySearch(balances, new HistoricalBalance(height, null));
		if (index < 0) {
			long numMicroNem = index == -1? 0 : balances.get(-index-2).getBalance().getNumMicroNem();
			balances.add(-index-1, new HistoricalBalance(height.getRaw(), numMicroNem + amount.getNumMicroNem()));
			startIndex = -index;
		} else {
			balances.get(index).add(amount);
			startIndex = index + 1;
		}
		if (startIndex < balances.size()) {
			Iterator<HistoricalBalance> iter = balances.listIterator(startIndex);
			while (iter.hasNext()) {
				iter.next().add(amount);
			}
		}
		trim(new BlockHeight(Math.max(1, blockchain.getLastDbBlock().getHeight() - MAX_HISTORY)));
	}
	
	/**
	 * Subtract an amount at a given block height.
	 * Subtract the amount to all historical balances with bigger height.
	 * 
	 * @param height the height where the amount is inserted
	 * @param amount the amount to add
	 */
	public void subtract(final BlockHeight height, final Amount amount) {
		int startIndex = -1;
		int index = Collections.binarySearch(balances, new HistoricalBalance(height, null));
		if (index < 0) {
			long numMicroNem = index == -1? 0 : balances.get(-index-2).getBalance().getNumMicroNem();
			balances.add(-index-1, new HistoricalBalance(height.getRaw(), numMicroNem - amount.getNumMicroNem()));
			startIndex = -index;
		} else {
			balances.get(index).subtract(amount);
			startIndex = index + 1;
		}
		if (startIndex < balances.size()) {
			Iterator<HistoricalBalance> iter = balances.listIterator(startIndex);
			while (iter.hasNext()) {
				iter.next().subtract(amount);
			}
		}
		trim(new BlockHeight(Math.max(1, blockchain.getLastDbBlock().getHeight() - MAX_HISTORY)));
	}
	
	/**
	 * Eliminate all entries that have a block height smaller than the given height.
	 * Note: height should be at least MAX_HISTORY smaller than the height of the last block in the chain.
	 * 
	 * @param height the height to compare to
	 */
	private void trim(final BlockHeight height) {
		if (balances.size() == 0 || balances.get(0).getHeight().getRaw() >= height.getRaw()) {
			return;
		}
		// Remember the historical balance at the point we start deleting entries
		HistoricalBalance balance = getHistoricalBalance(height);
		boolean insertBalance = false;
		int index = Collections.binarySearch(balances, new HistoricalBalance(height, null));
		if (index < 0) {
			index = -index - 1;
			insertBalance = true;
		}
		if (index > 0) {
			Iterator<HistoricalBalance> iter = balances.listIterator();
			while (iter.hasNext() && index-- > 0) {
				iter.next();
				iter.remove();
			}
		}
		if (insertBalance) {
			balances.add(0, balance);
		}
	}
}
