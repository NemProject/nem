package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

import java.util.*;

public class SmartTileBagValidatorTest {
	private static final long INITIAL_SUPPLY = 10000;
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);

	//region unknown mosaic

	@Test
	public void transactionIsInvalidIfNamespaceIdIsUnknown() {
		// Assert:
		assertUnknownMosaic(Utils.createMosaic("foo", "tokens").getId(), Utils.createMosaic("bar", "tokens").getId());
	}

	@Test
	public void transactionIsInvalidIfMosaicIdIsUnknown() {
		// Assert:
		assertUnknownMosaic(Utils.createMosaic("foo", "tokens").getId(), Utils.createMosaic("foo", "coins").getId());
	}

	private static void assertUnknownMosaic(final MosaicId idInCache, final MosaicId idInTransaction) {
		// Arrange:
		final TestContext context = new TestContext();
		context.addMosaic(context.createMosaic(idInCache));
		final TransferTransaction transaction = context.createTransaction(5, idInTransaction, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_MOSAIC_UNKNOWN);
	}

	//endregion

	//region expired namespace

	@Test
	public void transactionIsInvalidIfNamespaceIsNotActive() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId), VALIDATION_HEIGHT.next());
		final TransferTransaction transaction = context.createTransaction(5, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_NAMESPACE_EXPIRED);
	}

	//endregion

	@Test
	public void transactionIsValidIfMosaicIsNotTransferableAndSenderIsTheMosaicCreator() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account recipient = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId, createMosaicProperties(false)));
		final TransferTransaction transaction = context.createTransaction(context.signer, recipient, 5, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionIsValidIfMosaicIsNotTransferableAndRecipientIsTheMosaicCreator() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId, createMosaicProperties(false)));
		context.transfer(mosaicId, context.signer.getAddress(), sender.getAddress(), new Quantity(7500));
		final TransferTransaction transaction = context.createTransaction(sender, context.signer, 5, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionIsInvalidIfMosaicIsNotTransferableNeitherSignerNorRecipientIsTheMosaicCreator() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId, createMosaicProperties(false)));
		final TransferTransaction transaction = context.createTransaction(sender, recipient, 5, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE);
	}

	//endregion

	//region insufficient balance

	@Test
	public void transactionIsInvalidIfForNonXemMosaicIfSignerHasNotEnoughSmartTileQuantity() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId));
		final TransferTransaction transaction = context.createTransaction(5, mosaicId, INITIAL_SUPPLY / 5 + 1);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	//endregion

	//region valid

	@Test
	public void transactionWithNoSmartTilesValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = context.createTransaction(5, null);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithValidSmartTilesValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId));
		final TransferTransaction transaction = context.createTransaction(5, mosaicId, 1234);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionWithValidSmartTilesValidatesWhenCompleteBalanceIsTransferred() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicId mosaicId = Utils.createMosaicId(111);
		context.addMosaic(context.createMosaic(mosaicId));
		final TransferTransaction transaction = context.createTransaction(5, mosaicId, INITIAL_SUPPLY / 5);

		// Assert:
		context.assertValidationResult(transaction, ValidationResult.SUCCESS);
	}

	//endregion

	private static MosaicProperties createMosaicProperties(final boolean transferable) {
		final Properties properties = new Properties();
		properties.put("quantity", String.valueOf(INITIAL_SUPPLY));
		properties.put("transferable", transferable ? "true" : "false");
		properties.put("divisibility", "4");
		return new DefaultMosaicProperties(properties);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final Account recipient = Utils.generateRandomAccount();
		private final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		private final SmartTileBagValidator validator = new SmartTileBagValidator(this.namespaceCache);

		public void addMosaic(final Mosaic mosaic) {
			this.addMosaic(mosaic, VALIDATION_HEIGHT);
		}

		public void addMosaic(final Mosaic mosaic, final BlockHeight namespaceHeight) {
			final Namespace namespace = new Namespace(mosaic.getId().getNamespaceId(), mosaic.getCreator(), namespaceHeight);
			this.namespaceCache.add(namespace);
			this.namespaceCache.get(namespace.getId()).getMosaics().add(mosaic);
		}

		public void transfer(final MosaicId mosaicId, final Address sender, final Address recipient, final Quantity quantity) {
			final MosaicEntry entry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
			entry.getBalances().decrementBalance(sender, quantity);
			entry.getBalances().incrementBalance(recipient, quantity);
		}

		private ValidationResult validate(final TransferTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, DebitPredicates.Throw));
		}

		private Mosaic createMosaic(final MosaicId mosaicId) {
			return this.createMosaic(mosaicId, createMosaicProperties(true));
		}

		private Mosaic createMosaic(final MosaicId mosaicId, final MosaicProperties properties) {
			return Utils.createMosaic(this.signer, mosaicId, properties);
		}

		//region createTransaction

		private TransferTransaction createTransaction(
				final Account signer,
				final Account recipient,
				final long amount,
				final MosaicId mosaicId,
				final long quantity) {
			// Arrange: add three mosaics with the "interesting" one in the middle
			final Mosaic firstMosaic = this.addTestMosaic(signer.getAddress(), Utils.createMosaicId(1));
			final Mosaic lastMosaic = this.addTestMosaic(signer.getAddress(), Utils.createMosaicId(3));

			final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
			attachment.addMosaicTransfer(firstMosaic.getId(), new Quantity(111));
			attachment.addMosaicTransfer(mosaicId, new Quantity(quantity));
			attachment.addMosaicTransfer(lastMosaic.getId(), new Quantity(333));
			return createTransaction(signer, recipient, amount, attachment);
		}

		private Mosaic addTestMosaic(final Address senderAddress, final MosaicId mosaicId) {
			final Mosaic mosaic = this.createMosaic(mosaicId);
			this.addMosaic(mosaic);

			// if the test mosaic signer is different from the sender, transfer the full supply to the sender
			if (!senderAddress.equals(this.signer.getAddress())) {
				this.transfer(mosaicId, this.signer.getAddress(), senderAddress, new Quantity(INITIAL_SUPPLY));
			}

			return mosaic;
		}

		private TransferTransaction createTransaction(
				final long amount,
				final MosaicId mosaicId,
				final long quantity) {
			return this.createTransaction(this.signer, this.recipient, amount, mosaicId, quantity);
		}

		private TransferTransaction createTransaction(
				final long amount,
				final TransferTransactionAttachment attachment) {
			return createTransaction(this.signer, this.recipient, amount, attachment);
		}

		private static TransferTransaction createTransaction(
				final Account signer,
				final Account recipient,
				final long amount,
				final TransferTransactionAttachment attachment) {
			return new TransferTransaction(
					TimeInstant.ZERO,
					signer,
					recipient,
					Amount.fromNem(amount),
					attachment);
		}

		private void assertValidationResult(final TransferTransaction transaction, final ValidationResult expectedResult) {
			// Act:
			final ValidationResult result = this.validate(transaction);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}

		//endregion
	}
}
