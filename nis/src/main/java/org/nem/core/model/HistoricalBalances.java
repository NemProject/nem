package org.nem.core.model;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.nem.nis.mappers.BlockMapper;
import org.springframework.beans.factory.annotation.Autowired;

public class HistoricalBalances {

	/**
	 * Limit of history of balances (just not to let the list grow infinitely)
	 */
	public final long MAX_HISTORY = 1440;
	
	private final ArrayList<HistoricalBalance> balances = new ArrayList<HistoricalBalance>();
	
	// TODO: Is there any better way to get the height of the current last block?
	@Autowired
	private BlockChain blockChain;
	private AccountAnalyzer accountAnalyzer;

	/**
	 * Gets the historical balance at a given block height
	 * 
	 * @param height the number of blocks to go back in history
	 * 
	 * @return the historical balance
	 */
	public HistoricalBalance getHistoricalBalance(final BlockHeight height) {
		if (height.getRaw() > MAX_HISTORY || height.getRaw() < 0) {
			throw new InvalidParameterException("Historical balances are only available for the last " + MAX_HISTORY + " blocks");
		}
		if (balances.size() == 0) {
			return new HistoricalBalance(new BlockHeight(height.getRaw()), Amount.ZERO);
		}

		// Collections.binarySearch returns an index.
		// If index >= 0 a historical balance with the same block height was found.
		// If index < 0 then index = -(insertion point)-1 where insertion point is the point where the historical balance would be inserted.
		final org.nem.nis.dbmodel.Block dbLastBlock = blockChain.getLastDbBlock();
		final Block lastBlock = BlockMapper.toModel(dbLastBlock, this.accountAnalyzer);
		int index = Collections.binarySearch(balances, new HistoricalBalance(new BlockHeight(lastBlock.getHeight().subtract(height)), null));
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
	}
	
	/**
	 * Eliminate all entries that have a block height smaller than the given height.
	 * Note: height should be at least MAX_HISTORY + REWRITE_LIMIT smaller than the height of the last block in the chain.
	 * 
	 * @param height the height to compare to
	 */
	public void trim(final BlockHeight height) {
		int index = Collections.binarySearch(balances, new HistoricalBalance(height, null));
		if (index < 0) {
			index = -index - 2;
		} else {
			index--;
		}
		if (index > 0) {
			Iterator<HistoricalBalance> iter = balances.listIterator();
			while (iter.hasNext() && index-- > 0) {
				iter.next();
				iter.remove();
			}
		}
	}
}
