package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

public class UnconfirmedTransactionsCacheTest {

	//region construction

	@Test
	public void cacheIsInitiallyEmpty() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));
	}

	//endregion

	//region add

	@Test
	public void canAddNewTransactionWithoutChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		final ValidationResult result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void canAddNewTransactionWithNewChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));

		// Act:
		final ValidationResult result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(3));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(innerTransaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(innerTransaction2), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddExistingTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction);

		// Act:
		final ValidationResult result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddExistingTransactionAsChildTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction);

		final MockTransaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		transaction2.setChildTransactions(Arrays.asList(transaction));

		// Act:
		final ValidationResult result = cache.add(transaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddExistingChildTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction));
		cache.add(transaction);

		final MockTransaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		transaction2.setChildTransactions(Arrays.asList(innerTransaction));

		// Act:
		final ValidationResult result = cache.add(transaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(2));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddExistingChildTransactionAsTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction));
		cache.add(transaction);

		// Act:
		final ValidationResult result = cache.add(innerTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(2));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	//endregion

	//region add (with validation)

	@Test
	public void canAddNewTransactionWithSuccessValidation() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache(t -> ValidationResult.SUCCESS, (mst, mt) -> true);
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		final ValidationResult result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddNewTransactionWithNeutralValidation() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache(t -> ValidationResult.NEUTRAL, (mst, mt) -> true);
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		final ValidationResult result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(false));
	}

	@Test
	public void cannotAddNewTransactionWithFailureValidation() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache(t -> ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER, (mst, mt) -> true);
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		final ValidationResult result = cache.add(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(false));
	}

	//endregion

	//region add (multisig signature)

	@Test
	public void cannotAddMultisigSignatureWithNoExistingTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache(t -> ValidationResult.SUCCESS, (mst, mt) -> true);
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(context.cosigner1);

		// Act:
		final ValidationResult result = cache.add(signatureTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG));
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));
		Assert.assertThat(cache.contains(signatureTransaction), IsEqual.equalTo(false));
	}

	@Test
	public void cannotAddMultisigSignatureWithNonMatchingNonMultisigTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache(t -> ValidationResult.SUCCESS, (mst, mt) -> true);
		cache.add(new MockTransaction(context.cosigner2));
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(context.cosigner1);

		// Act:
		final ValidationResult result = cache.add(signatureTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(signatureTransaction), IsEqual.equalTo(false));
	}

	@Test
	public void cannotAddMultisigSignatureWithNonMatchingMultisigTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache(t -> ValidationResult.SUCCESS, (mst, mt) -> false);
		cache.add(context.createMultisigTransaction(context.cosigner2));
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(context.cosigner1);

		// Act:
		final ValidationResult result = cache.add(signatureTransaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(2)); // the multisig transaction has an inner transaction
		Assert.assertThat(cache.contains(signatureTransaction), IsEqual.equalTo(false));
	}

	@Test
	public void canAddMultisigSignatureWithMatchingMultisigTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache(t -> ValidationResult.SUCCESS, (mst, mt) -> true);
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(context.cosigner2);
		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(context.cosigner1);
		cache.add(multisigTransaction);

		// Act:
		final ValidationResult result = cache.add(signatureTransaction);

		// Assert:
		// - check the cache state
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(2 + 1)); // the multisig transaction has an inner transaction and a signature
		Assert.assertThat(cache.contains(multisigTransaction), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(multisigTransaction.getOtherTransaction()), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(signatureTransaction), IsEqual.equalTo(true));

		// - check the cache multisig transaction signatures
		Assert.assertThat(multisigTransaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
		Assert.assertThat(multisigTransaction.getCosignerSignatures(), IsEquivalent.equivalentTo(signatureTransaction));
	}

	@Test
	public void canAddMultisigSignatureWithMatchingMultisigTransactionAndNonMatchingMultisigTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final MultisigTransaction multisigTransaction = context.createMultisigTransaction(context.cosigner2);

		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache(t -> ValidationResult.SUCCESS, (mst, mt) -> mt.equals(multisigTransaction));
		cache.add(context.createRandomMultisigTransaction());
		cache.add(context.createRandomMultisigTransaction());
		cache.add(multisigTransaction);
		cache.add(context.createRandomMultisigTransaction());

		final MultisigSignatureTransaction signatureTransaction = context.createSignatureTransaction(context.cosigner1);

		// Act:
		final ValidationResult result = cache.add(signatureTransaction);

		// Assert:
		// - check the cache state
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(cache.size(), IsEqual.equalTo(4));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(4 * 2 + 1)); // each multisig transaction has an inner transaction
		Assert.assertThat(cache.contains(multisigTransaction), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(multisigTransaction.getOtherTransaction()), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(signatureTransaction), IsEqual.equalTo(true));

		// - check the cache multisig transaction signatures
		Assert.assertThat(multisigTransaction.getCosignerSignatures().size(), IsEqual.equalTo(1));
		Assert.assertThat(multisigTransaction.getCosignerSignatures(), IsEquivalent.equivalentTo(signatureTransaction));
	}

	private static class MultisigTestContext {
		private final Account cosigner1 = Utils.generateRandomAccount();
		private final Account cosigner2 = Utils.generateRandomAccount();
		private final Account multisig = Utils.generateRandomAccount();
		private final Transaction otherTransaction = new MockTransaction(this.multisig);

		public MultisigSignatureTransaction createSignatureTransaction(final Account signer) {
			return new MultisigSignatureTransaction(TimeInstant.ZERO, signer, this.multisig, this.otherTransaction);
		}

		public MultisigTransaction createMultisigTransaction(final Account signer) {
			return new MultisigTransaction(TimeInstant.ZERO, signer, this.otherTransaction);
		}

		private MultisigTransaction createRandomMultisigTransaction() {
			return new MultisigTransaction(TimeInstant.ZERO, this.cosigner1, new MockTransaction(this.multisig, new Random().nextInt()));
		}
	}

	//endregion

	//region add (deterministic)

	@Test
	public void addedTransactionsAreStoredInOrder() {
		// Arrange:
		final int numTransactions = 10;
		final List<Transaction> transactions = new ArrayList<>();
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		for (int i = 0; i < numTransactions; ++i) {
			final Transaction transaction = new MockTransaction(Utils.generateRandomAccount(), i);
			cache.add(transaction);
			transactions.add(transaction);
		}

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(numTransactions));
		Assert.assertThat(cache.stream().collect(Collectors.toList()), IsEqual.equalTo(transactions));
	}

	//endregion

	//region remove

	@Test
	public void canRemoveExistingTransactionWithoutChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction1);

		final Transaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction2);

		// Act:
		final boolean result = cache.remove(transaction1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction1), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(transaction2), IsEqual.equalTo(true));
	}

	@Test
	public void canRemoveExistingTransactionWithNewChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		transaction1.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));
		cache.add(transaction1);

		final Transaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction2);

		// Act:
		final boolean result = cache.remove(transaction1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction1), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(transaction2), IsEqual.equalTo(true));
	}

	@Test
	public void canRemoveExistingMiddleTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction transaction3 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction1);
		cache.add(transaction2);
		cache.add(transaction3);

		// Act:
		final boolean result = cache.remove(transaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(cache.size(), IsEqual.equalTo(2));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(2));
		Assert.assertThat(cache.contains(transaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(transaction2), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(transaction3), IsEqual.equalTo(true));
	}

	@Test
	public void cannotRemoveNewTransactionWithoutChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction1);

		final Transaction transaction2 = new MockTransaction(Utils.generateRandomAccount());

		// Act:
		final boolean result = cache.remove(transaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction1), IsEqual.equalTo(true));
	}

	@Test
	public void cannotRemoveNewTransactionWithChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction1);

		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		transaction2.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));

		// Act:
		final boolean result = cache.remove(transaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(transaction1), IsEqual.equalTo(true));
	}

	@Test
	public void cannotRemoveTransactionFirstSeenAsChildTransaction() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));
		cache.add(transaction);

		// Act:
		final boolean result = cache.remove(innerTransaction2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(3));
		Assert.assertThat(cache.contains(innerTransaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(innerTransaction2), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(true));
	}

	//endregion

	//region clear

	@Test
	public void clearRemovesAllTransactions() {
		// Arrange:
		final int numTransactions = 5;
		final List<Transaction> transactions = new ArrayList<>();
		for (int i = 0; i < numTransactions; ++i) {
			transactions.add(new MockTransaction(Utils.generateRandomAccount()));
		}

		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		transactions.forEach(cache::add);

		// Act:
		cache.clear();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));

		for (final Transaction transaction : transactions) {
			Assert.assertThat(cache.contains(transaction), IsEqual.equalTo(false));
		}
	}

	@Test
	public void clearRemovesAllChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		cache.add(createTransactionWithTwoChildTransaction());
		cache.add(createTransactionWithTwoChildTransaction());
		cache.add(createTransactionWithTwoChildTransaction());

		// Act:
		cache.clear();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.flatSize(), IsEqual.equalTo(0));
	}

	//endregion

	//region contains

	@Test
	public void containsReturnsTrueForRootTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction1);
		cache.add(transaction2);

		// Assert:
		Assert.assertThat(cache.contains(transaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(transaction2), IsEqual.equalTo(true));
	}

	@Test
	public void containsReturnsTrueForChildTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount());
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));
		cache.add(transaction);

		// Assert:
		Assert.assertThat(cache.contains(innerTransaction1), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(innerTransaction2), IsEqual.equalTo(true));
	}

	@Test
	public void containsReturnsFalseForOtherTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		final Transaction transaction2 = new MockTransaction(Utils.generateRandomAccount());
		final Transaction transaction1 = new MockTransaction(Utils.generateRandomAccount());
		cache.add(transaction1);

		// Assert:
		Assert.assertThat(cache.contains(transaction2), IsEqual.equalTo(false));
	}

	//endregion

	//region stream / flatStream

	@Test
	public void streamReturnsAllRootTransactions() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		cache.add(createTransactionWithTwoChildTransaction(10));
		cache.add(createTransactionWithTwoChildTransaction(20));
		cache.add(createTransactionWithTwoChildTransaction(30));

		// Assert:
		Assert.assertThat(
				cache.stream().map(t -> ((MockTransaction)t).getCustomField()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(10, 20, 30));
	}

	@Test
	public void flatStreamReturnsAllRootTransactionsAndChildren() {
		// Arrange:
		final UnconfirmedTransactionsCache cache = new UnconfirmedTransactionsCache();
		cache.add(createTransactionWithTwoChildTransaction(10));
		cache.add(createTransactionWithTwoChildTransaction(20));
		cache.add(createTransactionWithTwoChildTransaction(30));

		// Assert:
		Assert.assertThat(
				cache.streamFlat().map(t -> ((MockTransaction)t).getCustomField()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(10, 11, 12, 20, 21, 22, 30, 31, 32));
	}

	//endregion

	private static Transaction createTransactionWithTwoChildTransaction() {
		return createTransactionWithTwoChildTransaction(0);
	}

	private static Transaction createTransactionWithTwoChildTransaction(final int seed) {
		final Transaction innerTransaction1 = new MockTransaction(Utils.generateRandomAccount(), seed + 1);
		final Transaction innerTransaction2 = new MockTransaction(Utils.generateRandomAccount(), seed + 2);
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), seed);
		transaction.setChildTransactions(Arrays.asList(innerTransaction1, innerTransaction2));
		return transaction;
	}
}