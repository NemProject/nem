package org.nem.nis.pox.poi.graph.utils;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.nis.pox.poi.graph.repository.GraphClusteringTransaction;
import org.nem.nis.state.*;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class BtcBlockChainAdapter implements BlockChainAdapter {
	protected static final Logger LOGGER = Logger.getLogger(BtcBlockChainAdapter.class.getName());

	private static final boolean USE_RANDOMNESS = false; // Create random connections to make the transaction graph more interesting

	private static final Map<Integer, Map<Address, AccountState>> ACCOUNT_STATE_MAP_CACHE = new HashMap<>();

	@Override
	public int getDefaultEndHeight() {
		return 20000; // This end height is set assuming that we only process block files blk00148.dat to blk00152.dat (inclusive)
	}

	@Override
	public String getBlockChainType() {
		return "Btc";
	}

	@Override
	public Map<Address, AccountState> createAccountStatesFromTransactionData(
			final Collection<GraphClusteringTransaction> transactions,
			final Function<Long, Long> normalizeAmount) {
		LOGGER.info("Creating PoiAccountStates from Btc transaction data...");

		if (!ACCOUNT_STATE_MAP_CACHE.containsKey(transactions.hashCode())) {
			final Map<Address, AccountState> accountStateMap = new HashMap<>();
			long maxBlockHeight = 0;
			// Iterate through transactions, creating new accounts as needed.
			for (final GraphClusteringTransaction trans : transactions) {
				final Amount amount = Amount.fromMicroNem(normalizeAmount.apply(trans.getAmount()));
				final Address sender = Address.fromEncoded(Long.toString(trans.getSenderId()));
				final Address recipient = Address.fromEncoded(Long.toString(trans.getRecipientId()));
				final BlockHeight blockHeight = new BlockHeight(this.normalizeBtcBlockHeightsToNem(trans.getHeight()));

				if (maxBlockHeight < trans.getHeight()) {
					maxBlockHeight = trans.getHeight(); // This is used to keep track of the maximum height found, for logging purposes
				}

				if (!accountStateMap.containsKey(sender)) {
					accountStateMap.put(sender, new AccountState(sender));
				}
				if (!accountStateMap.containsKey(recipient)) {
					accountStateMap.put(recipient, new AccountState(recipient));
				}

				final AccountState senderAccountState = accountStateMap.get(sender);
				final AccountState recipientAccountState = accountStateMap.get(recipient);
				final long balance = GraphAnalyzerTestUtils.mapPoiAccountStateToBalance(senderAccountState, blockHeight).getNumMicroNem();

				// We need to add some balance sometimes because the transactions don't account for fees and coins earned from mined blocks
				final long remainingBalance = balance - amount.getNumMicroNem();
				if (0 > remainingBalance) {
					//noinspection PointlessBooleanExpression,ConstantConditions
					if (USE_RANDOMNESS && 0.5 > Math.random()) {
						senderAccountState.getWeightedBalances().addReceive(new BlockHeight(blockHeight.getRaw()),
								Amount.fromMicroNem(amount.getNumMicroNem()));
					} else {
						senderAccountState.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight.getRaw()),
								Amount.fromMicroNem(amount.getNumMicroNem()));
					}
				}

				senderAccountState.getWeightedBalances().addSend(blockHeight, amount);
				senderAccountState.getImportanceInfo().addOutlink(
						new AccountLink(blockHeight, amount, recipientAccountState.getAddress()));

				recipientAccountState.getWeightedBalances().addReceive(blockHeight, amount);
			}

			LOGGER.info("Max block height processed: " + maxBlockHeight);
			ACCOUNT_STATE_MAP_CACHE.put(transactions.hashCode(), accountStateMap);
		}

		return ACCOUNT_STATE_MAP_CACHE.get(transactions.hashCode());
	}

	@Override
	public long getSupplyUnits() {
		return 1300000000; // Convert from Satoshis (10^8 precision); current BTC supply is about 13 mil;
	}

	@Override
	public long getMarketCap() {
		return 3235636400L;
	}

	private long normalizeBtcBlockHeightsToNem(final long btcBlockHeight) {
		return btcBlockHeight * 10;
	}
}
