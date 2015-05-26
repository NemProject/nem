package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;
import org.nem.nis.validators.ValidationContext;
import org.nem.nis.validators.transaction.*;

import java.util.*;

public class MultisigTestContext {
	private final AccountStateCache accountCache = Mockito.mock(AccountStateCache.class);
	private final MultisigAggregateModificationTransactionValidator multisigAggregateModificationTransactionValidator =
			new MultisigAggregateModificationTransactionValidator(this.accountCache);
	private final MultisigTransactionSignerValidator multisigTransactionSignerValidator = new MultisigTransactionSignerValidator(this.accountCache);
	private final MultisigNonOperationalValidator validator = new MultisigNonOperationalValidator(this.accountCache);
	private final MultisigSignaturesPresentValidator multisigSignaturesPresentValidator;

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private final List<Transaction> transactionList = new ArrayList<>();

	public final Account signer = Utils.generateRandomAccount();
	public final Account multisig = Utils.generateRandomAccount();
	private final Account recipient = Utils.generateRandomAccount();
	public final Account dummy = Utils.generateRandomAccount();

	public MultisigTestContext() {
		this.multisigSignaturesPresentValidator = new MultisigSignaturesPresentValidator(this.accountCache);
		this.addState(this.signer);
		this.addState(this.multisig);
		this.addState(this.dummy);
	}

	public MultisigTransaction createMultisigModificationTransaction(final List<MultisigCosignatoryModification> modifications) {
		final MultisigAggregateModificationTransaction otherTransaction = new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				this.multisig,
				modifications);

		final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, this.signer, otherTransaction);
		transaction.sign();

		this.transactionList.add(transaction);
		return transaction;
	}

	public MultisigAggregateModificationTransaction createTypedMultisigModificationTransaction(final List<MultisigCosignatoryModification> modifications) {
		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				this.multisig,
				modifications);
	}

	public MultisigTransaction createMultisigTransferTransaction() {
		return this.createMultisigTransferTransaction(this.signer);
	}

	public MultisigTransaction createMultisigTransferTransaction(final Account multisigSigner) {
		final TransferTransaction otherTransaction = new TransferTransaction(TimeInstant.ZERO, this.multisig, this.recipient, Amount.fromNem(123), null);
		final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, multisigSigner, otherTransaction);
		transaction.sign();

		this.transactionList.add(transaction);

		return transaction;
	}

	public void addSignature(final Account signatureSigner, final MultisigTransaction multisigTransaction) {
		multisigTransaction.addSignature(new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				signatureSigner,
				this.multisig,
				multisigTransaction.getOtherTransaction()));
	}

	public AccountState addState(final Account account) {
		final Address address = account.getAddress();
		final AccountState state = new AccountState(address);
		Mockito.when(this.accountCache.findStateByAddress(address)).thenReturn(state);
		return state;
	}

	public Collection<Address> getCosignatories(final Account multisig) {
		return this.accountCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().getCosignatories();
	}

	public void makeCosignatory(final Account signer, final Account multisig) {
		this.accountCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
		this.accountCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress());
	}

	public boolean debitPredicate(final Account account, final Amount amount) {
		final Amount balance = this.accountCache.findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
		return balance.compareTo(amount) >= 0;
	}

	// forward to validators
	public ValidationResult validateSignaturePresent(final MultisigTransaction transaction) {
		return this.multisigSignaturesPresentValidator.validate(transaction, new ValidationContext(this::debitPredicate));
	}

	public ValidationResult validateNonOperational(final Transaction transaction) {
		return this.validator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
	}

	public ValidationResult validateMultisigModification(final MultisigAggregateModificationTransaction transaction) {
		return this.multisigAggregateModificationTransactionValidator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
	}

	public ValidationResult validateTransaction(final MultisigTransaction transaction) {
		return this.multisigTransactionSignerValidator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
	}
}
