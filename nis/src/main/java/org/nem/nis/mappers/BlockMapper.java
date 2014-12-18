package org.nem.nis.mappers;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.*;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.MultisigTransaction;

import java.util.*;
import java.util.stream.Collectors;

// TODO 20141201 J-J: i will need to look at this a bit closer

/**
 * Static class that contains functions for converting to and from
 * db-model Block and model Block.
 */
public class BlockMapper {

	private static class BlockTransactionDbMapper {
		private final AccountDaoLookup accountDao;
		private final org.nem.nis.dbmodel.Block dbBlock;

		final List<ImportanceTransfer> importanceTransferTransactions = new ArrayList<>(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK / 10);
		final List<MultisigSignerModification> multisigSignerModificationsTransactions = new ArrayList<>(
				BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK / 10);
		final List<MultisigTransaction> multisigTransactions = new ArrayList<>(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK / 10);
		final List<Transfer> transferTransactions;

		int i = 0;
		int multisigSignerModificationsIndex = 0;
		int multisigTransactionsIndex = 0;
		int importanceTransferIndex = 0;
		int transferIndex = 0;

		private BlockTransactionDbMapper(final AccountDaoLookup accountDao, final org.nem.nis.dbmodel.Block dbBlock, int initialCapacity) {
			this.accountDao = accountDao;
			this.dbBlock = dbBlock;
			this.transferTransactions = new ArrayList<>(initialCapacity);
		}

		private MultisigTransaction handleMultisig(Transaction transaction) {
			final MultisigTransaction dbTransfer = MultisigTransactionMapper.toDbModel(
					(org.nem.core.model.MultisigTransaction)transaction,
					i,
					multisigTransactionsIndex++,
					accountDao);
			dbTransfer.setBlock(dbBlock);
			multisigTransactions.add(dbTransfer);

			return dbTransfer;
		}

		private void handleTransaction(final Transaction transaction) {
			handleTransaction(transaction, null);
		}

		private void handleTransaction(final Transaction transaction, final MultisigTransaction multisig) {
			switch (transaction.getType()) {
				case TransactionTypes.TRANSFER: {
					final Transfer dbTransfer = TransferMapper.toDbModel(
							(TransferTransaction)transaction,
							i,
							importanceTransferIndex++,
							accountDao);
					dbTransfer.setBlock(dbBlock);
					transferTransactions.add(dbTransfer);

					if (multisig != null) {
						multisig.setTransfer(dbTransfer);
					}
				}
				break;
				case TransactionTypes.IMPORTANCE_TRANSFER: {
					final ImportanceTransfer dbTransfer = ImportanceTransferMapper.toDbModel(
							(ImportanceTransferTransaction)transaction,
							i,
							transferIndex++,
							accountDao);
					dbTransfer.setBlock(dbBlock);
					importanceTransferTransactions.add(dbTransfer);

					if (multisig != null) {
						multisig.setImportanceTransfer(dbTransfer);
					}
				}
				break;
				case TransactionTypes.MULTISIG_SIGNER_MODIFY: {
					final MultisigSignerModification dbTransfer = MultisigSignerModificationMapper.toDbModel(
							(MultisigSignerModificationTransaction)transaction,
							i,
							multisigSignerModificationsIndex++,
							accountDao);
					dbTransfer.setBlock(dbBlock);
					multisigSignerModificationsTransactions.add(dbTransfer);

					if (multisig != null) {
						multisig.setMultisigSignerModification(dbTransfer);
					}
				}
				break;
				case TransactionTypes.MULTISIG: {
					if (multisig != null) {
						throw new RuntimeException("multisig inside multisig");
					}

					final MultisigTransaction multisigTransaction = this.handleMultisig(transaction);
					// recursive call
					this.handleTransaction(((org.nem.core.model.MultisigTransaction)transaction).getOtherTransaction(), multisigTransaction);
				}
				break;
				default:
					throw new RuntimeException("trying to map block with unknown transaction type");
			}

			// if current transaction is part of multisig transaction we will NOT increment blockindex
			if (multisig == null) {
				i++;
			}
		}

		public void saveTransfers() {
			this.dbBlock.setBlockTransfers(transferTransactions);
			this.dbBlock.setBlockImportanceTransfers(importanceTransferTransactions);
			this.dbBlock.setBlockMultisigSignerModifications(multisigSignerModificationsTransactions);
			this.dbBlock.setBlockMultisigTransactions(multisigTransactions);
		}
	}

	/**
	 * Converts a Block model to a Block db-model.
	 *
	 * @param block The block model.
	 * @param accountDao The account dao lookup object.
	 * @return The Block db-model.
	 */
	public static org.nem.nis.dbmodel.Block toDbModel(final Block block, final AccountDaoLookup accountDao) {
		final org.nem.nis.dbmodel.Account harvester = accountDao.findByAddress(block.getSigner().getAddress());
		final org.nem.nis.dbmodel.Account lessor = block.getLessor() != null ? accountDao.findByAddress(block.getLessor().getAddress()) : null;

		final Hash blockHash = HashUtils.calculateHash(block);
		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block(
				blockHash,
				block.getVersion(),
				block.getGenerationHash(),
				block.getPreviousBlockHash(),
				block.getTimeStamp().getRawTime(),
				harvester,
				block.getSignature().getBytes(),
				block.getHeight().getRaw(),
				0L,
				block.getTotalFee().getNumMicroNem(),
				block.getDifficulty().getRaw(),
				lessor);

		final BlockTransactionDbMapper blockTransactionDbMapper = new BlockTransactionDbMapper(accountDao, dbBlock, block.getTransactions().size());
		for (final Transaction transaction : block.getTransactions()) {
			blockTransactionDbMapper.handleTransaction(transaction);
		}

		blockTransactionDbMapper.saveTransfers();
		return dbBlock;
	}

	/**
	 * Converts a Block db-model to a Block model.
	 *
	 * @param dbBlock The block db-model.
	 * @param accountLookup The account lookup object.
	 * @return The Block model.
	 */
	public static Block toModel(final org.nem.nis.dbmodel.Block dbBlock, final AccountLookup accountLookup) {
		if (1 == dbBlock.getHeight()) {
			return NemesisBlock.fromResource(new DeserializationContext(accountLookup));
		}

		final Address foragerAddress = AccountToAddressMapper.toAddress(dbBlock.getForger());
		final Account forager = accountLookup.findByAddress(foragerAddress);
		final Address lessorAddress = dbBlock.getLessor() != null ? AccountToAddressMapper.toAddress(dbBlock.getLessor()) : null;
		final Account lessor = lessorAddress != null ? accountLookup.findByAddress(lessorAddress) : null;

		final Block block = new org.nem.core.model.Block(
				forager,
				dbBlock.getPrevBlockHash(),
				dbBlock.getGenerationHash(),
				new TimeInstant(dbBlock.getTimeStamp()),
				new BlockHeight(dbBlock.getHeight()));

		final Long difficulty = dbBlock.getDifficulty();
		block.setDifficulty(new BlockDifficulty(null == difficulty ? 0L : difficulty));
		block.setLessor(lessor);
		block.setSignature(new Signature(dbBlock.getForgerProof()));

		// note: we must NOT add getBlockMultisigTransactions here
		final int count =
				dbBlock.getBlockMultisigSignerModifications().size() +
						dbBlock.getBlockImportanceTransfers().size() +
						dbBlock.getBlockTransfers().size();
		final ArrayList<Transaction> transactions = new ArrayList<>(Arrays.asList(new Transaction[count]));

		final Set<Integer> transactionsToSkip = dbBlock.getBlockMultisigTransactions().stream()
				.map(t -> t.getBlkIndex())
				.collect(Collectors.toSet());
		for (final MultisigTransaction dbTransfer : dbBlock.getBlockMultisigTransactions()) {
			final org.nem.core.model.MultisigTransaction transaction = MultisigTransactionMapper.toModel(dbTransfer, accountLookup);
			transactions.set(dbTransfer.getBlkIndex(), transaction);
		}

		for (final MultisigSignerModification dbTransfer : dbBlock.getBlockMultisigSignerModifications()) {
			if (transactionsToSkip.contains(dbTransfer.getBlkIndex())) {
				continue;
			}
			final MultisigSignerModificationTransaction transaction = MultisigSignerModificationMapper.toModel(dbTransfer, accountLookup);
			transactions.set(dbTransfer.getBlkIndex(), transaction);
		}

		for (final ImportanceTransfer dbTransfer : dbBlock.getBlockImportanceTransfers()) {
			if (transactionsToSkip.contains(dbTransfer.getBlkIndex())) {
				continue;
			}
			final ImportanceTransferTransaction transaction = ImportanceTransferMapper.toModel(dbTransfer, accountLookup);
			transactions.set(dbTransfer.getBlkIndex(), transaction);
		}

		for (final Transfer dbTransfer : dbBlock.getBlockTransfers()) {
			if (transactionsToSkip.contains(dbTransfer.getBlkIndex())) {
				continue;
			}
			final TransferTransaction transaction = TransferMapper.toModel(dbTransfer, accountLookup);
			transactions.set(dbTransfer.getBlkIndex(), transaction);
		}

		block.addTransactions(transactions);
		return block;
	}
}