package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.*;

public class TransactionRegistryTest {

	@Test
	public void allExpectedTransactionTypesAreSupported() {
		// Assert:
		Assert.assertThat(TransactionRegistry.size(), IsEqual.equalTo(4));
	}

	@Test
	public void allExpectedMultisigEmbeddableTypesAreSupported() {
		// Assert:
		Assert.assertThat(TransactionRegistry.multisigEmbeddableSize(), IsEqual.equalTo(3));
	}

	@Test
	public void transactionRegistryIsConsistentWithTransactionFactory() {
		// Assert:
		// (the transaction factory includes transactions that are not stored directly in blocks,
		// so it is aware of more transactions than the registry)
		Assert.assertThat(TransactionRegistry.size(), IsEqual.equalTo(TransactionFactory.size() - 1));
	}

	@Test
	public void allExpectedEntriesAreReturnedViaIterator() {
		// Act:
		final Collection<Class> modelClasses = StreamSupport.stream(TransactionRegistry.iterate().spliterator(), false)
				.map(e -> e.modelClass)
				.collect(Collectors.toList());

		// Assert:
		final Collection<Class> expectedModelClasses = Arrays.asList(
				TransferTransaction.class,
				ImportanceTransferTransaction.class,
				MultisigAggregateModificationTransaction.class,
				MultisigTransaction.class);
		Assert.assertThat(modelClasses, IsEquivalent.equivalentTo(expectedModelClasses));
		Assert.assertThat(expectedModelClasses.size(), IsEqual.equalTo(TransactionRegistry.size()));
	}

	@Test
	public void findByTypeCanReturnAllRegisteredTypes() {
		// Arrange:
		final List<Integer> expectedRegisteredTypes = Arrays.asList(
				TransactionTypes.TRANSFER,
				TransactionTypes.IMPORTANCE_TRANSFER,
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
				TransactionTypes.MULTISIG);

		// Act:
		for (final Integer type : expectedRegisteredTypes) {
			// Act:
			final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(type);

			// Assert:
			Assert.assertThat(entry.type, IsEqual.equalTo(type));
		}

		Assert.assertThat(expectedRegisteredTypes.size(), IsEqual.equalTo(TransactionRegistry.size()));
	}

	@Test
	public void findByTypeReturnsNullForUnregisteredType() {
		// Act:
		final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(TransactionTypes.ASSET_ASK);

		// Assert:
		Assert.assertThat(entry, IsNull.nullValue());
	}

	@Test
	public void findByDbModelClassCanReturnAllRegisteredTypes() {
		// Arrange:
		final List<Class<? extends AbstractBlockTransfer>> expectedRegisteredClasses = Arrays.asList(
				DbTransferTransaction.class,
				DbImportanceTransferTransaction.class,
				DbMultisigAggregateModificationTransaction.class,
				DbMultisigTransaction.class);

		// Act:
		for (final Class<? extends AbstractBlockTransfer> clazz : expectedRegisteredClasses) {
			// Act:
			final TransactionRegistry.Entry<AbstractBlockTransfer, ?> entry = TransactionRegistry.findByDbModelClass(clazz);

			// Assert:
			Assert.assertThat(entry.dbModelClass, IsEqual.equalTo(clazz));
		}

		Assert.assertThat(expectedRegisteredClasses.size(), IsEqual.equalTo(TransactionRegistry.size()));
	}

	@Test
	public void findByDbModelClassReturnsNullForUnregisteredType() {
		// Act:
		final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByDbModelClass(MyDbModelClass.class);

		// Assert:
		Assert.assertThat(entry, IsNull.nullValue());
	}

	private class MyDbModelClass extends AbstractBlockTransfer<MyDbModelClass> {
		private MyDbModelClass() {
			super(null);
		}
	}

	// region getInnerTransaction

	@Test
	@SuppressWarnings("unchecked")
	public void getInnerTransactionReturnsNullForNonMultisgTransactions() {
		// Arrange:
		final DbTransferTransaction t1 = new DbTransferTransaction();
		final DbImportanceTransferTransaction t2 = new DbImportanceTransferTransaction();
		final DbMultisigAggregateModificationTransaction t3 = new DbMultisigAggregateModificationTransaction();
		final TransactionRegistry.Entry<DbTransferTransaction, ?> entry1
				= (TransactionRegistry.Entry<DbTransferTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.TRANSFER);
		final TransactionRegistry.Entry<DbImportanceTransferTransaction, ?> entry2
				= (TransactionRegistry.Entry<DbImportanceTransferTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.IMPORTANCE_TRANSFER);
		final TransactionRegistry.Entry<DbMultisigAggregateModificationTransaction, ?> entry3
				= (TransactionRegistry.Entry<DbMultisigAggregateModificationTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION);

		// Act:
		final AbstractBlockTransfer inner1 = entry1.getInnerTransaction.apply(t1);
		final AbstractBlockTransfer inner2 = entry2.getInnerTransaction.apply(t2);
		final AbstractBlockTransfer inner3 = entry3.getInnerTransaction.apply(t3);

		// Assert:
		Assert.assertThat(inner1, IsNull.nullValue());
		Assert.assertThat(inner2, IsNull.nullValue());
		Assert.assertThat(inner3, IsNull.nullValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getInnerTransactionReturnsInnerTransactionForMultisgTransaction() {
		// Arrange:
		final DbTransferTransaction t = new DbTransferTransaction();
		final DbMultisigTransaction multisig = new DbMultisigTransaction();
		multisig.setTransferTransaction(t);
		final TransactionRegistry.Entry<DbMultisigTransaction, ?> entry
				= (TransactionRegistry.Entry<DbMultisigTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG);

		// Act:
		final AbstractBlockTransfer inner = entry.getInnerTransaction.apply(multisig);

		// Assert:
		Assert.assertThat(inner, IsSame.sameInstance(t));
	}

	// endregion

	// region getTransactionCount

	@Test
	@SuppressWarnings("unchecked")
	public void getTransactionCountReturnsOneForNonMultisgTransactions() {
		final DbTransferTransaction t1 = new DbTransferTransaction();
		final DbImportanceTransferTransaction t2 = new DbImportanceTransferTransaction();
		final DbMultisigAggregateModificationTransaction t3 = new DbMultisigAggregateModificationTransaction();
		final TransactionRegistry.Entry<DbTransferTransaction, ?> entry1
				= (TransactionRegistry.Entry<DbTransferTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.TRANSFER);
		final TransactionRegistry.Entry<DbImportanceTransferTransaction, ?> entry2
				= (TransactionRegistry.Entry<DbImportanceTransferTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.IMPORTANCE_TRANSFER);
		final TransactionRegistry.Entry<DbMultisigAggregateModificationTransaction, ?> entry3
				= (TransactionRegistry.Entry<DbMultisigAggregateModificationTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION);

		// Act:
		final int count1 = entry1.getTransactionCount.apply(t1);
		final int count2 = entry2.getTransactionCount.apply(t2);
		final int count3 = entry3.getTransactionCount.apply(t3);

		// Assert:
		Assert.assertThat(count1, IsEqual.equalTo(1));
		Assert.assertThat(count2, IsEqual.equalTo(1));
		Assert.assertThat(count3, IsEqual.equalTo(1));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getInnerTransactionReturnsTwoPlusSignatureCountForMultisgTransaction() {
		// Arrange:
		final DbTransferTransaction t = new DbTransferTransaction();
		final DbAccount signer1 = new DbAccount();
		final DbAccount signer2 = new DbAccount();
		final Set<DbMultisigSignatureTransaction> signatureTransactions = new HashSet<>();
		signatureTransactions.add(this.createMultisigSignatureTransaction(signer1));
		signatureTransactions.add(this.createMultisigSignatureTransaction(signer2));
		final DbMultisigTransaction multisig = new DbMultisigTransaction();
		multisig.setTransferTransaction(t);
		multisig.setMultisigSignatureTransactions(signatureTransactions);
		final TransactionRegistry.Entry<DbMultisigTransaction, ?> entry
				= (TransactionRegistry.Entry<DbMultisigTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG);

		// Act:
		final int count = entry.getTransactionCount.apply(multisig);

		// Assert:
		Assert.assertThat(count, IsEqual.equalTo(2 + 2));
	}

	// endregion

	// region getRecipient

	@Test
	@SuppressWarnings("unchecked")
	public void getRecipientReturnsRecipientForTransfer() {
		// Arrange:
		final DbAccount original = new DbAccount();
		final DbTransferTransaction t = new DbTransferTransaction();
		t.setRecipient(original);
		final TransactionRegistry.Entry<DbTransferTransaction, ?> entry
				= (TransactionRegistry.Entry<DbTransferTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.TRANSFER);

		// Act:
		final DbAccount account = entry.getRecipient.apply(t);

		// Assert:
		Assert.assertThat(account, IsSame.sameInstance(original));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getRecipientReturnsRemoteForImportanceTransfer() {
		// Arrange:
		final DbAccount original = new DbAccount();
		final DbImportanceTransferTransaction t = new DbImportanceTransferTransaction();
		t.setRemote(original);
		final TransactionRegistry.Entry<DbImportanceTransferTransaction, ?> entry
				= (TransactionRegistry.Entry<DbImportanceTransferTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.IMPORTANCE_TRANSFER);

		// Act:
		final DbAccount account = entry.getRecipient.apply(t);

		// Assert:
		Assert.assertThat(account, IsSame.sameInstance(original));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getRecipientReturnsNullForMultisigAggregateModificationTransaction() {
		// Arrange:
		final DbMultisigAggregateModificationTransaction t = new DbMultisigAggregateModificationTransaction();
		final TransactionRegistry.Entry<DbMultisigAggregateModificationTransaction, ?> entry
				= (TransactionRegistry.Entry<DbMultisigAggregateModificationTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION);

		// Act:
		final DbAccount account = entry.getRecipient.apply(t);

		// Assert:
		Assert.assertThat(account, IsNull.nullValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getRecipientReturnsNullForMultisigTransaction() {
		// Arrange:
		final DbMultisigTransaction t = new DbMultisigTransaction();
		final TransactionRegistry.Entry<DbMultisigTransaction, ?> entry
				= (TransactionRegistry.Entry<DbMultisigTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG);

		// Act:
		final DbAccount account = entry.getRecipient.apply(t);

		// Assert:
		Assert.assertThat(account, IsNull.nullValue());
	}

	// endregion

	// region getOtherAccounts

	@Test
	@SuppressWarnings("unchecked")
	public void getOtherAccountsReturnsEmptyListForTransfer() {
		// Arrange:
		final DbTransferTransaction t = new DbTransferTransaction();
		final TransactionRegistry.Entry<DbTransferTransaction, ?> entry
				= (TransactionRegistry.Entry<DbTransferTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.TRANSFER);

		// Act:
		final Collection<DbAccount> accounts = entry.getOtherAccounts.apply(t);

		// Assert:
		Assert.assertThat(accounts, IsEqual.equalTo(new ArrayList<>()));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getOtherAccountsReturnsEmptyListForImportanceTransfer() {
		// Arrange:
		final DbImportanceTransferTransaction t = new DbImportanceTransferTransaction();
		final TransactionRegistry.Entry<DbImportanceTransferTransaction, ?> entry
				= (TransactionRegistry.Entry<DbImportanceTransferTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.IMPORTANCE_TRANSFER);

		// Act:
		final Collection<DbAccount> accounts = entry.getOtherAccounts.apply(t);

		// Assert:
		Assert.assertThat(accounts, IsEqual.equalTo(new ArrayList<>()));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getOtherAccountsReturnsAffectedCosignatoriesForMultisigAggregateModificationTransaction() {
		// Arrange:
		final DbMultisigAggregateModificationTransaction t = new DbMultisigAggregateModificationTransaction();
		final DbAccount cosignatory1 = NisUtils.createDbAccount(1L);
		final DbAccount cosignatory2 = NisUtils.createDbAccount(2L);
		final Set<DbMultisigModification> modifications = new HashSet<>();
		modifications.add(this.createMultisigModification(cosignatory1));
		modifications.add(this.createMultisigModification(cosignatory2));
		t.setMultisigModifications(modifications);
		final TransactionRegistry.Entry<DbMultisigAggregateModificationTransaction, ?> entry
				= (TransactionRegistry.Entry<DbMultisigAggregateModificationTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION);

		// Act:
		final Collection<DbAccount> account = entry.getOtherAccounts.apply(t);

		// Assert:
		Assert.assertThat(account, IsEquivalent.equivalentTo(Arrays.asList(cosignatory1, cosignatory2)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getOtherAccountsReturnsMultisigSignatureTransactionSignersForMultisigTransaction() {
		// Arrange:
		final DbMultisigTransaction t = new DbMultisigTransaction();
		final DbAccount signer1 = NisUtils.createDbAccount(1L);
		final DbAccount signer2 = NisUtils.createDbAccount(2L);
		final Set<DbMultisigSignatureTransaction> signatureTransactions = new HashSet<>();
		signatureTransactions.add(this.createMultisigSignatureTransaction(signer1));
		signatureTransactions.add(this.createMultisigSignatureTransaction(signer2));
		t.setMultisigSignatureTransactions(signatureTransactions);
		final TransactionRegistry.Entry<DbMultisigTransaction, ?> entry
				= (TransactionRegistry.Entry<DbMultisigTransaction, ?>)TransactionRegistry.findByType(TransactionTypes.MULTISIG);

		// Act:
		final Collection<DbAccount> account = entry.getOtherAccounts.apply(t);

		// Assert:
		Assert.assertThat(account, IsEquivalent.equivalentTo(Arrays.asList(signer1, signer2)));
	}

	private DbMultisigModification createMultisigModification(final DbAccount cosignatory) {
		final DbMultisigModification modifications = new DbMultisigModification();
		modifications.setCosignatory(cosignatory);
		modifications.setModificationType(MultisigModificationType.Add.value());
		return modifications;
	}

	private DbMultisigSignatureTransaction createMultisigSignatureTransaction(final DbAccount account) {
		final DbMultisigSignatureTransaction transaction = new DbMultisigSignatureTransaction();
		transaction.setSender(account);
		return transaction;
	}

	// endregion

	// region getFromDb

	@Test
	public void getFromDbCallsGetTransfersForAccountForTransferType() {
		assertGetFromDbCallsExpectedMethodForGivenType(TransactionTypes.TRANSFER, 0x1);
	}

	@Test
	public void getFromDbCallsGetImportanceTransfersForAccountForImportanceTransferType() {
		assertGetFromDbCallsExpectedMethodForGivenType(TransactionTypes.IMPORTANCE_TRANSFER, 0x2);
	}

	@Test
	public void getFromDbCallsGetMultisigSignerModificationsForMultisigType() {
		assertGetFromDbCallsExpectedMethodForGivenType(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, 0x4);
	}

	@Test
	public void getFromDbCallsGetMultisigTransactionsForAccountForMultisigType() {
		assertGetFromDbCallsExpectedMethodForGivenType(TransactionTypes.MULTISIG, 0x8);
	}

	private static void assertGetFromDbCallsExpectedMethodForGivenType(final int type, final int callPattern) {
		// Arrange:
		final TransferDao transferDao = Mockito.mock(TransferDao.class);
		final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(type);

		// Act:
		entry.getFromDb.apply(transferDao, 1L, 2L, 3, ReadOnlyTransferDao.TransferType.OUTGOING);

		// Assert:
		Mockito.verify(transferDao, Mockito.times(callPattern & 0x01))
				.getTransfersForAccount(1L, 2L, 3, ReadOnlyTransferDao.TransferType.OUTGOING);
		Mockito.verify(transferDao, Mockito.times((callPattern & 0x02) >> 1))
				.getImportanceTransfersForAccount(1L, 2L, 3, ReadOnlyTransferDao.TransferType.OUTGOING);
		Mockito.verify(transferDao, Mockito.times((callPattern & 0x04) >> 2))
				.getMultisigSignerModificationsForAccount(1L, 2L, 3, ReadOnlyTransferDao.TransferType.OUTGOING);
		Mockito.verify(transferDao, Mockito.times((callPattern & 0x08) >> 3))
				.getMultisigTransactionsForAccount(1L, 2L, 3, ReadOnlyTransferDao.TransferType.OUTGOING);
	}

	// endregion
}