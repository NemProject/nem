package org.nem.nis.poi.graph;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.nis.state.*;

import java.util.*;

/**
 * Integration test class for analysis the Proof-of-Importance algorithm with the Bitcoin transaction graph.
 */
public class BtcGraphClusteringITCase extends GraphClusteringITCase {
	private static final int DEFAULT_END_HEIGHT = 20000; // This end height is set assuming that we only process block files blk00148.dat to blk00152.dat (inclusive)
	private static final String BLOCKCHAIN_TYPE = "Btc";
	private static final double SUPPLY_NORMALIZATION_FACTOR =
			8999999999.0 / 1300000000.0; // Convert from Satoshis (10^8 precision); current BTC supply is about 13 mil
	private static final double MKT_CAP_NORMALIZATION_FACTOR = 3235636400.0 / 4000000.0; // BTC mkt cap / NEM mkt cap (in USD)
	private static final boolean USE_RANDOMNESS = false; // Create random connections to make the transaction graph more interesting

	private final static Map<Integer, Map<Address, AccountState>> accountStateMapCache = new HashMap<>();

	/**
	 * Default constructor - where we set the parameters for
	 * analyzing the Bitcoin blockchain.
	 */
	public BtcGraphClusteringITCase() {
		super(DEFAULT_END_HEIGHT, BLOCKCHAIN_TYPE, new BtcDatabaseRepository());
	}

	//TODO: Add some tests here to verify that BtcTransactions are read in and created correctly?
	// TODO 20150223 J-M: i guess we can close this TODO?

	/**
	 * Creates account states from transaction data, which are then used for running various importance score calculation tests.
	 *
	 * @param transactions - list of transactions
	 * @return list of account states, with each account having its own state
	 */
	protected Map<Address, AccountState> createAccountStatesFromTransactionData(final Collection<GraphClusteringTransaction> transactions) {
		LOGGER.info("Creating PoiAccountStates from Btc transaction data...");

		if (!accountStateMapCache.containsKey(transactions.hashCode())) {
			final Map<Address, AccountState> accountStateMap = new HashMap<>();
			long maxBlockHeight = 0;
			// Iterate through transactions, creating new accounts as needed.
			for (final GraphClusteringTransaction trans : transactions) {
				final Amount amount = Amount.fromMicroNem(this.normalizeBtcToNemMktCap(trans.getAmount()));
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
				final long balance = mapPoiAccountStateToBalance(senderAccountState, blockHeight).getNumMicroNem();

				// We need to add some balance sometimes because the transactions don't account for fees and coins earned from mined blocks
				final long remainingBalance = balance - amount.getNumMicroNem();
				if (0 > remainingBalance) {
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
			accountStateMapCache.put(transactions.hashCode(), accountStateMap);
		}

		return accountStateMapCache.get(transactions.hashCode());
	}

	// TODO 20150223 J-M: i noticed you're not calling this; is that intentional?
	private long normalizeBtcToNemSupply(final long amt) {
		return (long)(amt * SUPPLY_NORMALIZATION_FACTOR);
	}

	private long normalizeBtcToNemMktCap(final long amt) {
		return (long)(amt * MKT_CAP_NORMALIZATION_FACTOR);
	}

	private long normalizeBtcBlockHeightsToNem(final long btcBlockHeight) {
		return btcBlockHeight * 10;
	}
}
