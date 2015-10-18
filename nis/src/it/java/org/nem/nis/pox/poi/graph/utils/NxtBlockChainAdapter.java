package org.nem.nis.pox.poi.graph.utils;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.nis.pox.poi.graph.repository.GraphClusteringTransaction;
import org.nem.nis.state.*;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class NxtBlockChainAdapter implements BlockChainAdapter {
	private static final Logger LOGGER = Logger.getLogger(NxtBlockChainAdapter.class.getName());

	@Override
	public int getDefaultEndHeight() {
		return 300000;
	}

	@Override
	public String getBlockChainType() {
		return "Nxt";
	}

	@Override
	public Map<Address, AccountState> createAccountStatesFromTransactionData(
			final Collection<GraphClusteringTransaction> transactions,
			final Function<Long, Long> normalizeAmount) {
		LOGGER.info("Creating PoiAccountStates from Nxt transaction data...");

		final Map<Address, AccountState> accountStateMap = new HashMap<>();

		// 1. Create accounts in the genesis block.
		final Amount genesisAmount = Amount.fromNem(normalizeAmount.apply(1000000000L));
		final AccountState genesis = GraphAnalyzerTestUtils.createAccountWithBalance(Address.fromEncoded("1739068987193023818"), 1, genesisAmount);
		accountStateMap.put(genesis.getAddress(), genesis);

		// 2. Iterate through transactions, creating new accounts as needed.
		for (final GraphClusteringTransaction trans : transactions) {
			final Amount amount = Amount.fromMicroNem(normalizeAmount.apply(trans.getAmount()));
			final Address sender = Address.fromEncoded(Long.toString(trans.getSenderId()));
			final Address recipient = Address.fromEncoded(Long.toString(trans.getRecipientId()));
			final BlockHeight blockHeight = new BlockHeight(trans.getHeight() + 1); // NXT blocks start at 0 but NEM blocks start at 1

			if (!accountStateMap.containsKey(recipient)) {
				accountStateMap.put(recipient, new AccountState(recipient));
			}

			final AccountState senderAccountState = accountStateMap.get(sender);
			final AccountState recipientAccountState = accountStateMap.get(recipient);
			final long balance = GraphAnalyzerTestUtils.mapPoiAccountStateToBalance(senderAccountState, blockHeight).getNumMicroNem();

			// We need to add some balance sometimes because the transactions don't account for fees earned from harvested blocks
			final long remainingBalance = balance - amount.getNumMicroNem();
			if (remainingBalance < 0) {
				senderAccountState.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight.getRaw()), Amount.fromMicroNem(amount.getNumMicroNem()));
			}

			senderAccountState.getWeightedBalances().addSend(blockHeight, amount);
			senderAccountState.getImportanceInfo().addOutlink(
					new AccountLink(blockHeight, amount, recipientAccountState.getAddress()));

			recipientAccountState.getWeightedBalances().addReceive(blockHeight, amount);
		}

		LOGGER.info("Creating PoiAccountStates finished...");
		return accountStateMap;
	}

	@Override
	public long getSupplyUnits() {
		return 100000000000L; // Convert from 1 billion Nxt (10^8 precision);
	}

	@Override
	public long getMarketCap() {
		return 12000000; // Nxt mkt cap
	}
}
