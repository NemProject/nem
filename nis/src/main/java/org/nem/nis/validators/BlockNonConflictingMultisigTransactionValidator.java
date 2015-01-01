package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.*;
import java.util.stream.Collectors;

public class BlockNonConflictingMultisigTransactionValidator implements BlockValidator {
	final ReadOnlyNisCache nisCache;

	public BlockNonConflictingMultisigTransactionValidator(final ReadOnlyNisCache nisCache) {
		this.nisCache = nisCache;
	}

	private static class NonConflictingMultisigTransactionValidator {
		final Set<Address> multisigModificationSenders = new HashSet<>();
		final ReadOnlyNisCache nisCache;

		private NonConflictingMultisigTransactionValidator(final ReadOnlyNisCache nisCache) {
			this.nisCache = nisCache;
		}

		final boolean validate(final Transaction transaction) {
			if (transaction.getType() == TransactionTypes.MULTISIG_SIGNER_MODIFY) {
				return this.multisigModificationSenders.add(transaction.getSigner().getAddress());
			}

			if (transaction.getType() != TransactionTypes.MULTISIG) {
				return true;
			}

			final MultisigTransaction multisigTransaction = (MultisigTransaction)transaction;
			final Account multisigAccount = multisigTransaction.getOtherTransaction().getSigner();
			final ReadOnlyAccountState multisigState = this.nisCache.getAccountStateCache().findStateByAddress(multisigAccount.getAddress());

			if (multisigState.getMultisigLinks().getCosignatories().size() == 1) {
				return true;
			}

			int expectedNumberOfCosignatories = multisigState.getMultisigLinks().getCosignatories().size() - 1;
			if (multisigTransaction.getOtherTransaction().getType() == TransactionTypes.MULTISIG_SIGNER_MODIFY) {
				final MultisigSignerModificationTransaction modification = (MultisigSignerModificationTransaction)multisigTransaction.getOtherTransaction();
				// TODO test if there is multisigmodification inside and if it's type is Del
				// (N-2 cosignatories "exception")

				// we only allow single multisig modification from given block at given height
				if (! this.multisigModificationSenders.add(modification.getSigner().getAddress())) {
					return false;
				}

				long deletions = modification.getModifications().stream()
						.filter(m -> m.getModificationType() == MultisigModificationType.Del)
						.count();
				if (deletions > 1) {
					return false;
				}

				if (deletions == 1) {
					expectedNumberOfCosignatories -= 1;
				}
			}

			if (expectedNumberOfCosignatories == multisigTransaction.getCosignerSignatures().size()) {
				return true;
			}

			return false;
		}
	}

	public static List<Transaction> multisigSignatureFilter(final ReadOnlyNisCache nisCache, final List<Transaction> transactions) {
		final NonConflictingMultisigTransactionValidator validator = new NonConflictingMultisigTransactionValidator(nisCache);

		return transactions.stream()
				.filter(t -> validator.validate(t))
				.collect(Collectors.toList());
	}

	@Override
	public ValidationResult validate(final Block block) {
		final NonConflictingMultisigTransactionValidator validator = new NonConflictingMultisigTransactionValidator(this.nisCache);
		boolean result = block.getTransactions().stream()
				.anyMatch(t -> ! validator.validate(t));
		return result ? ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG : ValidationResult.SUCCESS;
	}
}
