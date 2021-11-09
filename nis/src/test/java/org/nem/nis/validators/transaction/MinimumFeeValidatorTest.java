package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.Collection;
import java.util.stream.Collectors;

public class MinimumFeeValidatorTest {

	final static NetworkInfo createNetworkInfo(final Address nemesisSignerAddress) {
		return new NetworkInfo(NetworkInfos.getTestNetworkInfo().getVersion(), NetworkInfos.getTestNetworkInfo().getAddressStartChar(),
				new NemesisBlockInfo(Hash.ZERO, nemesisSignerAddress, Amount.ZERO, ""));
	}

	@Test
	public void transactionWithValidFeePassesValidation() {
		assertValidationResultForMockTransactionFee(MockTransaction.DEFAULT_FEE, false, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithInvalidFeeFailsValidation_NotNemesis() {
		assertValidationResultForMockTransactionFee(MockTransaction.DEFAULT_FEE.subtract(Amount.fromNem(1)), false,
				ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	@Test
	public void transactionWithInvalidFeePassesValidation_Nemesis() {
		assertValidationResultForMockTransactionFee(MockTransaction.DEFAULT_FEE.subtract(Amount.fromNem(1)), true,
				ValidationResult.SUCCESS);
	}

	private static void assertValidationResultForMockTransactionFee(final Amount fee, final boolean isSignerNemesisAccount,
			final ValidationResult expectedResult) {
		// Arrange:
		final MockTransaction transaction = new MockTransaction();
		transaction.setFee(fee);

		final Address signerAddress = isSignerNemesisAccount ? transaction.getSigner().getAddress() : Utils.generateRandomAddress();
		final SingleTransactionValidator validator = new MinimumFeeValidator(createNetworkInfo(signerAddress), new DefaultNamespaceCache(),
				false);

		// Act:
		final ValidationResult result = validator.validate(transaction,
				new ValidationContext(new BlockHeight(511000), ValidationStates.Throw));

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	@Test
	public void transferTransactionIncludingMosaicsWithInvalidFeeFailsValidation() {
		assertValidationResultForTransferTransactionWithMosaicFee(Amount.fromNem(192), ValidationResult.FAILURE_INSUFFICIENT_FEE);
	}

	@Test
	public void transferTransactionIncludingMosaicsWithValidFeePassesValidation() {
		assertValidationResultForTransferTransactionWithMosaicFee(Amount.fromNem(193), ValidationResult.SUCCESS);
	}

	private static void assertValidationResultForTransferTransactionWithMosaicFee(final Amount fee, final ValidationResult expectedResult) {
		// Arrange:
		final Account namespaceOwner = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(1);
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(namespaceOwner, mosaicId,
				Utils.createMosaicProperties(10000L, 0, null, null));
		final NamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		namespaceCache.add(new Namespace(mosaicId.getNamespaceId(), namespaceOwner, BlockHeight.ONE));
		namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().add(mosaicDefinition);

		final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
		attachment.addMosaic(mosaicId, new Quantity(1000));
		final TransferTransaction transaction = new TransferTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(),
				Utils.generateRandomAccount(), Amount.fromNem(1), attachment);
		transaction.setFee(fee);
		transaction.setDeadline(new TimeInstant(1));

		final SingleTransactionValidator validator = new MinimumFeeValidator(createNetworkInfo(Utils.generateRandomAddress()),
				namespaceCache, false);

		// Act:
		final ValidationResult result = validator.validate(transaction,
				new ValidationContext(new BlockHeight(511000), ValidationStates.Throw));

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	@Test
	public void transactionValidationWithNonZeroFeesPassesValidationIfIgnoreFeesIsSet() {
		assertValidationPassesForFeeWhenIgnoreFeesSet(Amount.fromNem(1234));
	}

	@Test
	public void transactionValidationWithZeroFeesPassesValidationIfIgnoreFeesIsSet() {
		assertValidationPassesForFeeWhenIgnoreFeesSet(Amount.ZERO);
	}

	private static void assertValidationPassesForFeeWhenIgnoreFeesSet(final Amount fee) {
		// Arrange:
		final SingleTransactionValidator validator = new MinimumFeeValidator(createNetworkInfo(Utils.generateRandomAddress()),
				new DefaultNamespaceCache(), true);
		final Collection<Transaction> transactions = TestTransactionRegistry.stream().map(entry -> {
			final Transaction transaction = entry.createModel.get();
			transaction.setFee(fee);
			return transaction;
		}).collect(Collectors.toList());

		// Test a transaction with mosaics and message too
		final Message message = new PlainMessage("Hi there".getBytes());
		final TransferTransactionAttachment attachment = new TransferTransactionAttachment(message);
		attachment.addMosaic(Utils.createMosaicId(1), new Quantity(12));
		final TransferTransaction transaction = RandomTransactionFactory.createTransferWithAttachment(attachment);
		transaction.setFee(fee);
		transactions.add(transaction);

		// Assert:
		transactions.forEach(t -> MatcherAssert.assertThat(validator.validate(t, new ValidationContext(ValidationStates.Throw)),
				IsEqual.equalTo(ValidationResult.SUCCESS)));
	}
}
