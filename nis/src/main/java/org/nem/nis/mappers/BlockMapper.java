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

		private BlockTransactionDbMapper(final AccountDaoLookup accountDao, final org.nem.nis.dbmodel.Block dbBlock, final int initialCapacity) {
			this.accountDao = accountDao;
			this.dbBlock = dbBlock;
			this.transferTransactions = new ArrayList<>(initialCapacity);
		}

		private MultisigTransaction handleMultisig(final Transaction transaction) {
			final MultisigTransaction dbTransfer = MultisigTransactionMapper.toDbModel(
					(org.nem.core.model.MultisigTransaction)transaction,
					this.i,
					this.multisigTransactionsIndex++,
					this.accountDao);
			dbTransfer.setBlock(this.dbBlock);
			this.multisigTransactions.add(dbTransfer);

			return dbTransfer;
		}

		private void handleTransaction(final Transaction transaction) {
			this.handleTransaction(transaction, null);
		}

		private void handleTransaction(final Transaction transaction, final MultisigTransaction multisig) {
			switch (transaction.getType()) {
				case TransactionTypes.TRANSFER: {
					final Transfer dbTransfer = null; /*TransferMapper.toDbModel(
							(TransferTransaction)transaction,
							this.i,
							this.importanceTransferIndex++,
							this.accountDao);
					dbTransfer.setBlock(this.dbBlock);*/
					this.transferTransactions.add(dbTransfer);

					if (multisig != null) {
						multisig.setTransfer(dbTransfer);
					}
				}
				break;
				case TransactionTypes.IMPORTANCE_TRANSFER: {
					final ImportanceTransfer dbTransfer = /*ImportanceTransferMapper.toDbModel(
							(ImportanceTransferTransaction)transaction,
							this.i,
							this.transferIndex++,
							this.accountDao);*/
							null;
					dbTransfer.setBlock(this.dbBlock);
					this.importanceTransferTransactions.add(dbTransfer);

					if (multisig != null) {
						multisig.setImportanceTransfer(dbTransfer);
					}
				}
				break;
				case TransactionTypes.MULTISIG_SIGNER_MODIFY: {
					final MultisigSignerModification dbTransfer = MultisigSignerModificationMapper.toDbModel(
							(MultisigSignerModificationTransaction)transaction,
							this.i,
							this.multisigSignerModificationsIndex++,
							this.accountDao);
					dbTransfer.setBlock(this.dbBlock);
					this.multisigSignerModificationsTransactions.add(dbTransfer);

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
				this.i++;
			}
		}

		public void saveTransfers() {
			this.dbBlock.setBlockTransfers(this.transferTransactions);
			this.dbBlock.setBlockImportanceTransfers(this.importanceTransferTransactions);
			this.dbBlock.setBlockMultisigSignerModifications(this.multisigSignerModificationsTransactions);
			this.dbBlock.setBlockMultisigTransactions(this.multisigTransactions);
		}
	}

}