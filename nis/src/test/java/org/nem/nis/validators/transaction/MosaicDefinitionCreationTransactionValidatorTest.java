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
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.ValidationContext;

import java.util.*;
import java.util.function.Function;

public class MosaicDefinitionCreationTransactionValidatorTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);
	private static final Account CREATION_FEE_SINK = MosaicConstants.MOSAIC_CREATION_FEE_SINK;
	private static final Amount CREATION_FEE = Amount.fromNem(50000);

	//region valid

	@Test
	public void transactionIsValidIfMosaicDefinitionIsNew() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionPropertyChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsEntireSupply() {
		// Assert:
		assertCreatorFullSupplyModificationAllowed(mosaicDefinition -> createAlteredMosaicDefinition(mosaicDefinition, createCustomMosaicProperties()));
	}

	@Test
	public void transactionMosaicLevyEnabledChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsEntireSupply() {
		// Assert:
		assertCreatorFullSupplyModificationAllowed(null, createCustomMosaicLevy());
		assertCreatorFullSupplyModificationAllowed(createCustomMosaicLevy(), null);
	}

	@Test
	public void transactionMosaicLevyChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsEntireSupply() {
		// Assert:
		assertCreatorFullSupplyModificationAllowed(
				createCustomMosaicLevy(Utils.createMosaicId("alice", "1")),
				createCustomMosaicLevy(Utils.createMosaicId("alice", "2")));
	}

	@Test
	public void transactionDescriptionChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsEntireSupply() {
		// Assert:
		assertCreatorFullSupplyModificationAllowed(mosaicDefinition -> createAlteredMosaicDefinition(mosaicDefinition, "some desc"));
	}

	@Test
	public void transactionPropertyAndDescriptionAndMosaicLevyChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsFullSupply() {
		// Assert:
		assertCreatorFullSupplyModificationAllowed(mosaicDefinition ->
				createAlteredMosaicDefinition(
						mosaicDefinition,
						"some desc",
						createCustomMosaicProperties(),
						createCustomMosaicLevy()));
	}

	private static void assertCreatorFullSupplyModificationAllowed(final Function<MosaicDefinition, MosaicDefinition> alter) {
		// Assert:
		assertCreatorFullSupplyModificationAllowed(createTransaction(), alter);
	}

	private static void assertCreatorFullSupplyModificationAllowed(final MosaicLevy originalLevy, final MosaicLevy changedLevy) {
		// Assert:
		assertCreatorFullSupplyModificationAllowed(
				createTransactionWithLevy(changedLevy),
				mosaicDefinition -> createAlteredMosaicDefinition(mosaicDefinition, originalLevy));
	}

	private static void assertCreatorFullSupplyModificationAllowed(
			final MosaicDefinitionCreationTransaction transaction,
			final Function<MosaicDefinition, MosaicDefinition> alter) {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinition mosaicDefinition = alter.apply(transaction.getMosaicDefinition());
		context.addMosaicDefinition(mosaicDefinition);
		final Collection<MosaicId> mosaicIds = Arrays.asList(
				getMosaicFeeId(transaction.getMosaicDefinition()),
				getMosaicFeeId(mosaicDefinition));
		context.addOptionalMosaicFeeIdDefinitions(mosaicIds);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionDescriptionChangeIsValidIfMosaicDefinitionExistsAndCreatorOwnsPartialSupply() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		final MosaicDefinition mosaicDefinition = createAlteredMosaicDefinition(transaction.getMosaicDefinition(), "some desc");
		context.addMosaicDefinition(mosaicDefinition);
		context.makeOwnerHavePartialSupply(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionIsValidIfMosaicLevyMosaicIdIsEqualToMosaicDefinitionMosaicId() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicId feeMosaicId = Utils.createMosaicId("alice.vouchers", "alice's gift vouchers");
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithFeeMosaicId(feeMosaicId);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionIsValidIfCreationFeeIsMinimum() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithCreationFee(CREATION_FEE);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void transactionIsValidIfCreationFeeIsGreaterThanMinimum() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithCreationFee(CREATION_FEE.add(Amount.fromNem(100)));

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region invalid

	@Test
	public void transactionIsInvalidIfNamespaceIsInactive() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT.next());
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
	}

	@Test
	public void transactionIsInvalidIfNamespaceIsNotOwned() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(Utils.generateRandomAccount(), VALIDATION_HEIGHT);
		final MosaicDefinitionCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT));
	}

	@Test
	public void transactionPropertyChangeIsInvalidIfMosaicDefinitionExistsAndCreatorOwnsPartialSupply() {
		// Assert:
		assertCreatorPartialSupplyModificationDisallowed(mosaicDefinition -> createAlteredMosaicDefinition(mosaicDefinition, createCustomMosaicProperties()));
	}

	@Test
	public void transactionMosaicLevyEnabledChangeIsInvalidIfMosaicDefinitionExistsAndCreatorOwnsPartialSupply() {
		// Assert:
		assertCreatorPartialSupplyModificationDisallowed(null, createCustomMosaicLevy());
		assertCreatorPartialSupplyModificationDisallowed(createCustomMosaicLevy(), null);
	}

	@Test
	public void transactionMosaicLevyChangeIsInvalidIfMosaicDefinitionExistsAndCreatorOwnsPartialSupply() {
		// Assert:
		assertCreatorPartialSupplyModificationDisallowed(
				createCustomMosaicLevy(Utils.createMosaicId("alice", "1")),
				createCustomMosaicLevy(Utils.createMosaicId("alice", "2")));
	}

	@Test
	public void transactionPropertyAndDescriptionAndMosaicLevyChangeIsInvalidIfMosaicDefinitionExistsAndCreatorOwnsPartialSupply() {
		// Assert:
		assertCreatorPartialSupplyModificationDisallowed(mosaicDefinition ->
				createAlteredMosaicDefinition(
						mosaicDefinition,
						"some desc",
						createCustomMosaicProperties(),
						createCustomMosaicLevy()));
	}

	private static void assertCreatorPartialSupplyModificationDisallowed(final Function<MosaicDefinition, MosaicDefinition> alter) {
		// Assert:
		assertCreatorPartialSupplyModificationDisallowed(createTransaction(), alter);
	}

	private static void assertCreatorPartialSupplyModificationDisallowed(final MosaicLevy originalLevy, final MosaicLevy changedLevy) {
		// Assert:
		assertCreatorPartialSupplyModificationDisallowed(
				createTransactionWithLevy(changedLevy),
				mosaicDefinition -> createAlteredMosaicDefinition(mosaicDefinition, originalLevy));
	}

	private static void assertCreatorPartialSupplyModificationDisallowed(
			final MosaicDefinitionCreationTransaction transaction,
			final Function<MosaicDefinition, MosaicDefinition> alter) {
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinition mosaicDefinition = alter.apply(transaction.getMosaicDefinition());
		context.addMosaicDefinition(mosaicDefinition);
		context.makeOwnerHavePartialSupply(mosaicDefinition);
		final Collection<MosaicId> mosaicIds = Arrays.asList(
				getMosaicFeeId(transaction.getMosaicDefinition()),
				getMosaicFeeId(mosaicDefinition));
		context.addOptionalMosaicFeeIdDefinitions(mosaicIds);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED));
	}

	@Test
	public void transactionChangeIsInvalidIfMosaicDefinitionExistsAndTransactionResultsInNoChange() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransaction();
		context.addMosaicDefinition(transaction.getMosaicDefinition());

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED));
	}

	@Test
	public void transactionIsInvalidIfMosaicLevyContainsUnknownMosaicId() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithFeeMosaicId(Utils.createMosaicId(12));

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_UNKNOWN));
	}

	@Test
	public void transactionIsInvalidIfTransferablePropertyIsChanged() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinition originalMosaicDefinition = Utils.createMosaicDefinition(
				SIGNER,
				new MosaicId(new NamespaceId("alice.vouchers"), "other vouchers"),
				createCustomMosaicPropertiesWithTransferability(false),
				null);
		final MosaicDefinitionCreationTransaction transaction = createTransaction(originalMosaicDefinition);
		final MosaicDefinition mosaicDefinition = createAlteredMosaicDefinition(
				transaction.getMosaicDefinition(),
				createCustomMosaicPropertiesWithTransferability(true));
		context.addMosaicDefinition(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_MODIFICATION_NOT_ALLOWED));
	}

	@Test
	public void transactionIsInvalidIfMosaicLevyIsNotTransferable() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinition feeDefinition = Utils.createMosaicDefinition(
				SIGNER,
				new MosaicId(new NamespaceId("alice.vouchers"), "other vouchers"),
				createCustomMosaicPropertiesWithTransferability(false),
				null);
		context.addMosaicDefinition(feeDefinition);
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithFeeMosaicId(feeDefinition.getId());

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_LEVY_NOT_TRANSFERABLE));
	}

	@Test
	public void transactionIsInvalidIfMosaicLevyRefersToDefinitionAndIsNotTransferable() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.vouchers"), "other vouchers");
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(
				SIGNER,
				mosaicId,
				createCustomMosaicPropertiesWithTransferability(false),
				createCustomMosaicLevy(mosaicId));
		final MosaicDefinitionCreationTransaction transaction = createTransaction(mosaicDefinition);

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_LEVY_NOT_TRANSFERABLE));
	}

	@Test
	public void transactionIsInvalidIfCreationFeeSinkIsInvalid() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithCreationFeeSink(Utils.generateRandomAccount());

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_INVALID_CREATION_FEE_SINK));
	}

	@Test
	public void transactionIsInvalidIfCreationFeeIsLessThanMinimum() {
		// Arrange:
		final TestContext context = createContextWithValidNamespace();
		final MosaicDefinitionCreationTransaction transaction = createTransactionWithCreationFee(CREATION_FEE.subtract(Amount.fromNem(1)));

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MOSAIC_INVALID_CREATION_FEE));
	}

	//endregion

	//region createTransaction

	private static MosaicDefinitionCreationTransaction createTransaction() {
		return createTransaction(Utils.createMosaicDefinition(SIGNER));
	}

	private static MosaicDefinitionCreationTransaction createTransactionWithCreationFeeSink(final Account creationFeeSink) {
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);
		return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, SIGNER, mosaicDefinition, creationFeeSink, CREATION_FEE);
	}

	private static MosaicDefinitionCreationTransaction createTransactionWithCreationFee(final Amount creationFee) {
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(SIGNER);
		return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, SIGNER, mosaicDefinition, CREATION_FEE_SINK, creationFee);
	}

	private static MosaicDefinitionCreationTransaction createTransactionWithFeeMosaicId(final MosaicId feeMosaicId) {
		return createTransaction(Utils.createMosaicDefinition(SIGNER, createCustomMosaicLevy(feeMosaicId)));
	}

	private static MosaicDefinitionCreationTransaction createTransactionWithLevy(final MosaicLevy levy) {
		return createTransaction(Utils.createMosaicDefinition(SIGNER, levy));
	}

	private static MosaicDefinitionCreationTransaction createTransaction(final MosaicDefinition mosaicDefinition) {
		return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, SIGNER, mosaicDefinition, CREATION_FEE_SINK, CREATION_FEE);
	}

	//endregion

	//region createAlteredMosaicDefinition

	private static MosaicDefinition createAlteredMosaicDefinition(final MosaicDefinition mosaicDefinition, final String description) {
		return createAlteredMosaicDefinition(mosaicDefinition, description, mosaicDefinition.getProperties(), mosaicDefinition.getMosaicLevy());
	}

	private static MosaicDefinition createAlteredMosaicDefinition(final MosaicDefinition mosaicDefinition, final MosaicProperties properties) {
		return createAlteredMosaicDefinition(mosaicDefinition, mosaicDefinition.getProperties().toString(), properties, mosaicDefinition.getMosaicLevy());
	}

	private static MosaicDefinition createAlteredMosaicDefinition(final MosaicDefinition mosaicDefinition, final MosaicLevy levy) {
		return createAlteredMosaicDefinition(mosaicDefinition, mosaicDefinition.getProperties().toString(), mosaicDefinition.getProperties(), levy);
	}

	private static MosaicDefinition createAlteredMosaicDefinition(
			final MosaicDefinition mosaicDefinition,
			final String description,
			final MosaicProperties properties,
			final MosaicLevy levy) {
		return new MosaicDefinition(
				mosaicDefinition.getCreator(),
				mosaicDefinition.getId(),
				new MosaicDescriptor(description),
				properties,
				levy);
	}

	//endregion

	private static MosaicId getMosaicFeeId(final MosaicDefinition mosaicDefinition) {
		return null == mosaicDefinition.getMosaicLevy() ? null : mosaicDefinition.getMosaicLevy().getMosaicId();
	}

	private static MosaicProperties createCustomMosaicProperties() {
		final Properties properties = new Properties();
		properties.put("divisibility", "5");
		properties.put("quantity", "567");
		return new DefaultMosaicProperties(properties);
	}

	private static MosaicProperties createCustomMosaicPropertiesWithTransferability(final boolean isTransferable) {
		return Utils.createMosaicProperties(null, null, null, isTransferable);
	}

	private static MosaicLevy createCustomMosaicLevy() {
		return createCustomMosaicLevy(Utils.createMosaicId("alice.vouchers", "alice's gift vouchers"));
	}

	private static MosaicLevy createCustomMosaicLevy(final MosaicId mosaicId) {
		return new MosaicLevy(
				MosaicTransferFeeType.Percentile,
				Utils.generateRandomAccount(),
				mosaicId,
				Quantity.fromValue(456));
	}

	private static TestContext createContextWithValidNamespace() {
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT);
		return context;
	}

	private static class TestContext {
		final NamespaceCache namespaceCache = new DefaultNamespaceCache().copy();
		final MosaicDefinitionCreationTransactionValidator validator = new MosaicDefinitionCreationTransactionValidator(this.namespaceCache);

		public void activateNamespaceAtHeight(final Account signer, final BlockHeight height) {
			for (final String namespace : Arrays.asList("alice", "alice.vouchers")) {
				this.namespaceCache.add(new Namespace(new NamespaceId(namespace), signer, height));
			}
		}

		public void addMosaicDefinition(final MosaicDefinition mosaicDefinition) {
			this.namespaceCache.get(mosaicDefinition.getId().getNamespaceId()).getMosaics().add(mosaicDefinition);
		}

		public void addOptionalMosaicFeeIdDefinitions(final Collection<MosaicId> mosaicIds) {
			mosaicIds.stream()
					.filter(mid -> null != mid && !this.namespaceCache.get(mid.getNamespaceId()).getMosaics().contains(mid))
					.forEach(mid -> this.addMosaicDefinition(Utils.createMosaicDefinition(SIGNER, mid, Utils.createMosaicProperties())));
		}

		public void makeOwnerHavePartialSupply(final MosaicDefinition mosaicDefinition) {
			final MosaicEntry mosaicEntry = this.getMosaicEntry(mosaicDefinition.getId());
			mosaicEntry.increaseSupply(Supply.fromValue(234));
			mosaicEntry.getBalances().decrementBalance(mosaicDefinition.getCreator().getAddress(), Quantity.fromValue(123_000));
		}

		public ValidationResult validate(final MosaicDefinitionCreationTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, ValidationStates.Throw));
		}

		private MosaicEntry getMosaicEntry(final MosaicId mosaicId) {
			return this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		}
	}
}