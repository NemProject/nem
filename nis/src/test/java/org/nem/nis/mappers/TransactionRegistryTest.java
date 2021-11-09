package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.dao.retrievers.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.*;

@RunWith(Enclosed.class)
@SuppressWarnings("rawtypes")
public class TransactionRegistryTest {

	public static class All {
		private static final Collection<Class<?>> EXPECTED_MODEL_CLASSES = Arrays.asList(TransferTransaction.class,
				ImportanceTransferTransaction.class, MultisigAggregateModificationTransaction.class, MultisigTransaction.class,
				ProvisionNamespaceTransaction.class, MosaicDefinitionCreationTransaction.class, MosaicSupplyChangeTransaction.class);

		@Test
		public void allExpectedTransactionTypesAreSupported() {
			// Assert:
			MatcherAssert.assertThat(TransactionRegistry.size(), IsEqual.equalTo(TransactionTypes.getBlockEmbeddableTypes().size()));
		}

		@Test
		public void allExpectedMultisigEmbeddableTypesAreSupported() {
			// Assert:
			MatcherAssert.assertThat(TransactionRegistry.multisigEmbeddableSize(),
					IsEqual.equalTo(TransactionTypes.getMultisigEmbeddableTypes().size()));
		}

		@Test
		public void allExpectedEntriesAreReturnedViaIterator() {
			// Act:
			final Collection<Class<?>> modelClasses = StreamSupport.stream(TransactionRegistry.iterate().spliterator(), false)
					.map(e -> e.modelClass).collect(Collectors.toList());

			// Assert:
			MatcherAssert.assertThat(modelClasses, IsEquivalent.equivalentTo(EXPECTED_MODEL_CLASSES));
			MatcherAssert.assertThat(modelClasses.size(), IsEqual.equalTo(TransactionRegistry.size()));
		}

		@Test
		public void allExpectedEntriesAreReturnedViaStream() {
			// Act:
			final Collection<Class<?>> modelClasses = TransactionRegistry.stream().map(e -> e.modelClass).collect(Collectors.toList());

			// Assert:
			MatcherAssert.assertThat(modelClasses, IsEquivalent.equivalentTo(EXPECTED_MODEL_CLASSES));
			MatcherAssert.assertThat(modelClasses.size(), IsEqual.equalTo(TransactionRegistry.size()));
		}

		@Test
		public void findByTypeCanReturnAllRegisteredTypes() {
			// Arrange:
			final Collection<Integer> expectedRegisteredTypes = TransactionTypes.getBlockEmbeddableTypes();

			// Act:
			for (final Integer type : expectedRegisteredTypes) {
				// Act:
				final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(type);

				// Assert:
				assert null != entry;
				MatcherAssert.assertThat(entry.type, IsEqual.equalTo(type));
			}

			MatcherAssert.assertThat(expectedRegisteredTypes.size(), IsEqual.equalTo(TransactionRegistry.size()));
		}

		@Test
		public void findByTypeReturnsNullForUnregisteredType() {
			// Act:
			final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(TransactionTypes.ASSET_ASK);

			// Assert:
			MatcherAssert.assertThat(entry, IsNull.nullValue());
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void findByDbModelClassCanReturnAllRegisteredTypes() {
			// Arrange:
			final List<Class<? extends AbstractBlockTransfer>> expectedRegisteredClasses = Arrays.asList(DbTransferTransaction.class,
					DbImportanceTransferTransaction.class, DbMultisigAggregateModificationTransaction.class, DbMultisigTransaction.class,
					DbProvisionNamespaceTransaction.class, DbMosaicDefinitionCreationTransaction.class,
					DbMosaicSupplyChangeTransaction.class);

			// Act:
			for (final Class<? extends AbstractBlockTransfer> clazz : expectedRegisteredClasses) {
				// Act:
				final TransactionRegistry.Entry<AbstractBlockTransfer, ?> entry = TransactionRegistry.findByDbModelClass(clazz);

				// Assert:
				assert null != entry;
				MatcherAssert.assertThat(entry.dbModelClass, IsEqual.equalTo(clazz));
			}

			MatcherAssert.assertThat(expectedRegisteredClasses.size(), IsEqual.equalTo(TransactionRegistry.size()));
		}

		@Test
		public void findByDbModelClassReturnsNullForUnregisteredType() {
			// Act:
			final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByDbModelClass(MyDbModelClass.class);

			// Assert:
			MatcherAssert.assertThat(entry, IsNull.nullValue());
		}

		private class MyDbModelClass extends AbstractBlockTransfer<MyDbModelClass> {
		}
	}

	@SuppressWarnings("rawtypes")
	private static abstract class SingleTransactionTest<TDbModel extends AbstractBlockTransfer> {

		// region full tests

		@Test
		public void getTransactionRetrieverGetsRetrieverOfCorrectType() {
			// Arrange:
			final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(this.getType());

			// Assert:
			assert null != entry;
			MatcherAssert.assertThat(entry.getTransactionRetriever.get(), IsInstanceOf.instanceOf(this.getRetrieverType()));
		}

		// endregion

		// region abstract functions

		protected abstract int getType();

		protected abstract Class getRetrieverType();

		// endregion

		// region helpers

		@SuppressWarnings("unchecked")
		protected TransactionRegistry.Entry<TDbModel, ?> getEntry() {
			return (TransactionRegistry.Entry<TDbModel, ?>) TransactionRegistry.findByType(this.getType());
		}

		// endregion
	}

	@SuppressWarnings("rawtypes")
	private static abstract class NonMultisigSingleTransactionTest<TDbModel extends AbstractBlockTransfer>
			extends
				SingleTransactionTest<TDbModel> {

		@Test
		public void getTransactionCountReturnsOne() {
			final TDbModel transaction = this.createTransaction();

			// Act:
			final int count = this.getEntry().getTransactionCount.apply(transaction);

			// Assert:
			MatcherAssert.assertThat(count, IsEqual.equalTo(1));
		}

		@Test
		public void getInnerTransactionReturnsNull() {
			// Arrange:
			final TDbModel transaction = this.createTransaction();

			// Act:
			final AbstractBlockTransfer inner = this.getEntry().getInnerTransaction.apply(transaction);

			// Assert:
			MatcherAssert.assertThat(inner, IsNull.nullValue());
		}

		@Test
		public void canSetInMultisig() {
			// Arrange:
			final DbMultisigTransaction multisig = new DbMultisigTransaction();
			final TDbModel transaction = this.createTransaction();

			// Act:
			this.getEntry().setInMultisig.accept(multisig, transaction);

			// Assert:
			MatcherAssert.assertThat(this.getEntry().getFromMultisig.apply(multisig), IsEqual.equalTo(transaction));

			for (final TransactionRegistry.Entry<? extends AbstractBlockTransfer, ?> entry : TransactionRegistry.iterate()) {
				if (this.getType() == entry.type || null == entry.getFromMultisig) {
					continue;
				}

				MatcherAssert.assertThat(entry.getFromMultisig.apply(multisig), IsNull.nullValue());
			}
		}

		protected abstract TDbModel createTransaction();
	}

	public static class TransferTransactionTest extends NonMultisigSingleTransactionTest<DbTransferTransaction> {

		@Override
		protected int getType() {
			return TransactionTypes.TRANSFER;
		}

		@Override
		protected Class<?> getRetrieverType() {
			return TransferRetriever.class;
		}

		@Override
		protected DbTransferTransaction createTransaction() {
			return new DbTransferTransaction();
		}

		@Test
		public void getRecipientReturnsRecipient() {
			// Arrange:
			final DbAccount original = new DbAccount(1);
			final DbTransferTransaction t = new DbTransferTransaction();
			t.setRecipient(original);

			// Act:
			final DbAccount account = this.getEntry().getRecipient.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsSame.sameInstance(original));
		}

		@Test
		public void getOtherAccountsReturnsEmptyList() {
			// Arrange:
			final DbTransferTransaction t = new DbTransferTransaction();

			// Act:
			final Collection<DbAccount> accounts = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(accounts, IsEqual.equalTo(new ArrayList<>()));
		}
	}

	public static class ImportanceTransferTransactionTest extends NonMultisigSingleTransactionTest<DbImportanceTransferTransaction> {

		@Override
		protected int getType() {
			return TransactionTypes.IMPORTANCE_TRANSFER;
		}

		@Override
		protected Class<?> getRetrieverType() {
			return ImportanceTransferRetriever.class;
		}

		@Override
		protected DbImportanceTransferTransaction createTransaction() {
			return new DbImportanceTransferTransaction();
		}

		@Test
		public void getRecipientReturnsRemote() {
			// Arrange:
			final DbAccount original = new DbAccount(1);
			final DbImportanceTransferTransaction t = new DbImportanceTransferTransaction();
			t.setRemote(original);

			// Act:
			final DbAccount account = this.getEntry().getRecipient.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsSame.sameInstance(original));
		}

		@Test
		public void getOtherAccountsReturnsEmptyList() {
			// Arrange:
			final DbImportanceTransferTransaction t = new DbImportanceTransferTransaction();

			// Act:
			final Collection<DbAccount> accounts = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(accounts, IsEqual.equalTo(new ArrayList<>()));
		}
	}

	public static class MultisigAggregateModificationTransactionTest
			extends
				NonMultisigSingleTransactionTest<DbMultisigAggregateModificationTransaction> {

		@Override
		protected int getType() {
			return TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION;
		}

		@Override
		protected Class<?> getRetrieverType() {
			return MultisigModificationRetriever.class;
		}

		@Override
		protected DbMultisigAggregateModificationTransaction createTransaction() {
			return new DbMultisigAggregateModificationTransaction();
		}

		@Test
		public void getRecipientReturnsNull() {
			// Arrange:
			final DbMultisigAggregateModificationTransaction t = new DbMultisigAggregateModificationTransaction();

			// Act:
			final DbAccount account = this.getEntry().getRecipient.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsNull.nullValue());
		}

		@Test
		public void getOtherAccountsReturnsAffectedCosignatoriesForMultisigAggregateModificationTransaction() {
			// Arrange:
			final DbMultisigAggregateModificationTransaction t = new DbMultisigAggregateModificationTransaction();
			final DbAccount cosignatory1 = new DbAccount(1);
			final DbAccount cosignatory2 = new DbAccount(2);
			final Set<DbMultisigModification> modifications = new HashSet<>();
			modifications.add(this.createMultisigModification(cosignatory1));
			modifications.add(this.createMultisigModification(cosignatory2));
			t.setMultisigModifications(modifications);

			// Act:
			final Collection<DbAccount> account = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsEquivalent.equivalentTo(Arrays.asList(cosignatory1, cosignatory2)));
		}

		private DbMultisigModification createMultisigModification(final DbAccount cosignatory) {
			final DbMultisigModification modifications = new DbMultisigModification();
			modifications.setCosignatory(cosignatory);
			modifications.setModificationType(MultisigModificationType.AddCosignatory.value());
			return modifications;
		}
	}

	public static class ProvisionNamespaceTransactionTest extends NonMultisigSingleTransactionTest<DbProvisionNamespaceTransaction> {

		@Override
		protected int getType() {
			return TransactionTypes.PROVISION_NAMESPACE;
		}

		@Override
		protected Class<?> getRetrieverType() {
			return ProvisionNamespaceRetriever.class;
		}

		@Override
		protected DbProvisionNamespaceTransaction createTransaction() {
			return new DbProvisionNamespaceTransaction();
		}

		@Test
		public void getRecipientReturnsNull() {
			// Arrange:
			final DbProvisionNamespaceTransaction t = new DbProvisionNamespaceTransaction();

			// Act:
			final DbAccount account = this.getEntry().getRecipient.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsNull.nullValue());
		}

		@Test
		public void getOtherAccountsReturnsRentalFeeSink() {
			// Arrange:
			final DbAccount sink = new DbAccount(1);
			final DbProvisionNamespaceTransaction t = new DbProvisionNamespaceTransaction();
			t.setRentalFeeSink(sink);

			// Act:
			final Collection<DbAccount> accounts = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(accounts, IsEqual.equalTo(Collections.singletonList(sink)));
		}
	}

	public static class MosaicDefinitionCreationTransactionTest
			extends
				NonMultisigSingleTransactionTest<DbMosaicDefinitionCreationTransaction> {

		@Override
		protected int getType() {
			return TransactionTypes.MOSAIC_DEFINITION_CREATION;
		}

		@Override
		protected Class<?> getRetrieverType() {
			return MosaicDefinitionCreationRetriever.class;
		}

		@Override
		protected DbMosaicDefinitionCreationTransaction createTransaction() {
			return new DbMosaicDefinitionCreationTransaction();
		}

		@Test
		public void getRecipientReturnsNull() {
			// Arrange:
			final DbMosaicDefinitionCreationTransaction t = new DbMosaicDefinitionCreationTransaction();

			// Act:
			final DbAccount account = this.getEntry().getRecipient.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsNull.nullValue());
		}

		@Test
		public void getOtherAccountsReturnsCreationFeeSinkWhenNoFeeRecipient() {
			// Arrange:
			final DbMosaicDefinitionCreationTransaction t = this.createTransaction(new DbAccount(1), new DbAccount(2), null);

			// Act:
			final Collection<DbAccount> accounts = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(accounts, IsEqual.equalTo(Collections.singletonList(new DbAccount(2))));
		}

		@Test
		public void getOtherAccountsReturnsCreationFeeSinkWhenFeeRecipientSameAsCreator() {
			// Arrange:
			final DbMosaicDefinitionCreationTransaction t = this.createTransaction(new DbAccount(1), new DbAccount(2), new DbAccount(1));

			// Act:
			final Collection<DbAccount> accounts = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(accounts, IsEqual.equalTo(Collections.singletonList(new DbAccount(2))));
		}

		@Test
		public void getOtherAccountsReturnsCreationFeeSinkAndFeeRecipientWhenFeeRecipientDifferentFromCreator() {
			// Arrange:
			final DbMosaicDefinitionCreationTransaction t = this.createTransaction(new DbAccount(1), new DbAccount(2), new DbAccount(3));

			// Act:
			final Collection<DbAccount> accounts = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(accounts, IsEqual.equalTo(Arrays.asList(new DbAccount(2), new DbAccount(3))));
		}

		private DbMosaicDefinitionCreationTransaction createTransaction(final DbAccount creator, final DbAccount sink,
				final DbAccount feeRecipient) {
			final DbMosaicDefinitionCreationTransaction t = new DbMosaicDefinitionCreationTransaction();
			final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
			dbMosaicDefinition.setCreator(creator);
			dbMosaicDefinition.setFeeRecipient(feeRecipient);
			t.setMosaicDefinition(dbMosaicDefinition);
			t.setCreationFeeSink(sink);
			return t;
		}
	}

	public static class MosaicSupplyChangeTransactionTest extends NonMultisigSingleTransactionTest<DbMosaicSupplyChangeTransaction> {

		@Override
		protected int getType() {
			return TransactionTypes.MOSAIC_SUPPLY_CHANGE;
		}

		@Override
		protected Class<?> getRetrieverType() {
			return MosaicSupplyChangeRetriever.class;
		}

		@Override
		protected DbMosaicSupplyChangeTransaction createTransaction() {
			return new DbMosaicSupplyChangeTransaction();
		}

		@Test
		public void getRecipientReturnsNull() {
			// Arrange:
			final DbMosaicSupplyChangeTransaction t = new DbMosaicSupplyChangeTransaction();

			// Act:
			final DbAccount account = this.getEntry().getRecipient.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsNull.nullValue());
		}

		@Test
		public void getOtherAccountsReturnsEmptyList() {
			// Arrange:
			final DbMosaicSupplyChangeTransaction t = new DbMosaicSupplyChangeTransaction();

			// Act:
			final Collection<DbAccount> accounts = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(accounts, IsEqual.equalTo(Collections.emptyList()));
		}
	}

	public static class MultisigTransactionTest extends SingleTransactionTest<DbMultisigTransaction> {

		@Override
		protected int getType() {
			return TransactionTypes.MULTISIG;
		}

		@Override
		protected Class<?> getRetrieverType() {
			return MultisigTransactionRetriever.class;
		}

		@Test
		public void getRecipientReturnsNull() {
			// Arrange:
			final DbMultisigTransaction t = new DbMultisigTransaction();

			// Act:
			final DbAccount account = this.getEntry().getRecipient.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsNull.nullValue());
		}

		@Test
		public void getOtherAccountsReturnsMultisigSignatureTransactionSignersForMultisigTransaction() {
			// Arrange:
			final DbMultisigTransaction t = new DbMultisigTransaction();
			final DbAccount signer1 = new DbAccount(1);
			final DbAccount signer2 = new DbAccount(2);
			final Set<DbMultisigSignatureTransaction> signatureTransactions = new HashSet<>();
			signatureTransactions.add(this.createMultisigSignatureTransaction(signer1));
			signatureTransactions.add(this.createMultisigSignatureTransaction(signer2));
			t.setMultisigSignatureTransactions(signatureTransactions);

			// Act:
			final Collection<DbAccount> account = this.getEntry().getOtherAccounts.apply(t);

			// Assert:
			MatcherAssert.assertThat(account, IsEquivalent.equivalentTo(Arrays.asList(signer1, signer2)));
		}

		@Test
		public void getTransactionCountReturnsTwoPlusSignatureCountForMultisigTransaction() {
			// Arrange:
			final DbTransferTransaction t = new DbTransferTransaction();
			final Set<DbMultisigSignatureTransaction> signatureTransactions = new HashSet<>();
			signatureTransactions.add(this.createMultisigSignatureTransaction(new DbAccount()));
			signatureTransactions.add(this.createMultisigSignatureTransaction(new DbAccount()));
			signatureTransactions.add(this.createMultisigSignatureTransaction(new DbAccount()));
			final DbMultisigTransaction multisig = new DbMultisigTransaction();
			multisig.setTransferTransaction(t);
			multisig.setMultisigSignatureTransactions(signatureTransactions);

			// Act:
			final int count = this.getEntry().getTransactionCount.apply(multisig);

			// Assert:
			MatcherAssert.assertThat(count, IsEqual.equalTo(2 + 3));
		}

		@Test
		@SuppressWarnings("rawtypes")
		public void getInnerTransactionReturnsInnerTransactionForMultisigTransaction() {
			// Arrange:
			final DbTransferTransaction t = new DbTransferTransaction();
			final DbMultisigTransaction multisig = new DbMultisigTransaction();
			multisig.setTransferTransaction(t);

			// Act:
			final AbstractBlockTransfer inner = this.getEntry().getInnerTransaction.apply(multisig);

			// Assert:
			MatcherAssert.assertThat(inner, IsSame.sameInstance(t));
		}

		@Test
		public void nestedMultisigTransactionsAreNotAllowed() {
			// Assert:
			MatcherAssert.assertThat(this.getEntry().setInMultisig, IsNull.nullValue());
		}

		private DbMultisigSignatureTransaction createMultisigSignatureTransaction(final DbAccount account) {
			final DbMultisigSignatureTransaction transaction = new DbMultisigSignatureTransaction();
			transaction.setSender(account);
			return transaction;
		}
	}
}
