package org.nem.nis.balances;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.dbmodel.Transfer;

import java.util.Iterator;

public class BlockExecutor {
	public static void apply(final AccountAnalyzer accountLookup, final Block block) {
		// assuming AccountAnalyzer used to create block is the same as
		// the one passed in accountLookup

		for (Transaction transaction : block.getTransactions()) {
			transaction.execute();
		}

		block.getSigner().incrementForagedBlocks();
		block.getSigner().incrementBalance(block.getTotalFee());
	}

	public static void apply(final AccountLookup accountLookup, final org.nem.nis.dbmodel.Block block) {
		for (Transfer transfer : block.getBlockTransfers()) {
			final Address senderAccount = Address.fromPublicKey(transfer.getSender().getPublicKey());
			final Account sender = accountLookup.findByAddress(senderAccount);

			final Address recipientAccount = Address.fromEncoded(transfer.getRecipient().getPrintableKey());
			final Account recipient = accountLookup.findByAddress(recipientAccount);

			sender.decrementBalance(Amount.fromMicroNem(transfer.getAmount() + transfer.getFee()));
			recipient.incrementBalance(Amount.fromMicroNem(transfer.getAmount()));
		}

		final Address foragerAddress = Address.fromPublicKey(block.getForger().getPublicKey());
		final Account forager = accountLookup.findByAddress(foragerAddress);
		forager.incrementForagedBlocks();
		forager.incrementBalance(Amount.fromMicroNem(block.getTotalFee()));
	}

	public static void unapply(final AccountLookup accountLookup, final org.nem.nis.dbmodel.Block block) {
		final Address foragerAddress = Address.fromPublicKey(block.getForger().getPublicKey());
		final Account forager = accountLookup.findByAddress(foragerAddress);
		forager.decrementBalance(Amount.fromMicroNem(block.getTotalFee()));
		forager.decrementForagedBlocks();

		for (Transfer transfer : new Iterable<Transfer>() {
			@Override
			public Iterator<Transfer> iterator() {
				return new ReverseListIterator<>(block.getBlockTransfers());
			}
		}) {
			final Address senderAccount = Address.fromPublicKey(transfer.getSender().getPublicKey());
			final Account sender = accountLookup.findByAddress(senderAccount);

			final Address recipientAccount = Address.fromEncoded(transfer.getRecipient().getPrintableKey());
			final Account recipient = accountLookup.findByAddress(recipientAccount);

			recipient.decrementBalance(Amount.fromMicroNem(transfer.getAmount()));
			sender.incrementBalance(Amount.fromMicroNem(transfer.getAmount() + transfer.getFee()));
		}
	}
}
