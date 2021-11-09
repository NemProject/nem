package org.nem.nis.validators.transaction;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.test.ValidationStates;
import org.nem.nis.validators.*;

import java.util.Collection;

@RunWith(Enclosed.class)
public class VersionTransactionValidatorTest {
	private static final long MULTISIG_M_OF_N_FORK = BlockMarkerConstants
			.MULTISIG_M_OF_N_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24);
	private static final long MOSAICS_FORK = BlockMarkerConstants.MOSAICS_FORK(NetworkInfos.getTestNetworkInfo().getVersion() << 24);

	public static class Fork {

		// region MULTISIG_M_OF_N_FORK

		@Test
		public void v1MultisigModificationTransactionIsAlwaysAllowed() {
			// Assert:
			assertAlwaysAllowed(createModificationTransaction(1), MULTISIG_M_OF_N_FORK);
		}

		@Test
		public void v2MultisigModificationTransactionIsOnlyAllowedAtAndAfterFork() {
			// Assert:
			assertOnlyAllowedAtAndAfterFork(createModificationTransaction(2), MULTISIG_M_OF_N_FORK,
					ValidationResult.FAILURE_MULTISIG_V2_AGGREGATE_MODIFICATION_BEFORE_FORK);
		}

		@Test
		public void v3MultisigModificationTransactionIsNotAllowed() {
			// Assert:
			assertNotAllowed(createModificationTransaction(3), MULTISIG_M_OF_N_FORK);
		}

		// endregion

		// region MOSAICS_FORK

		@Test
		public void provisionNamespaceTransactionIsOnlyAllowedAtAndAfterFork() {
			// Assert:
			assertOnlyAllowedAtAndAfterFork(RandomTransactionFactory.createProvisionNamespaceTransaction(), MOSAICS_FORK,
					ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK);
		}

		@Test
		public void v2ProvisionNamespaceTransactionIsNotAllowed() {
			// Assert:
			assertNotAllowed(changeTransactionVersion(RandomTransactionFactory.createProvisionNamespaceTransaction(), 2), MOSAICS_FORK);
		}

		@Test
		public void mosaicDefinitionCreationTransactionIsOnlyAllowedAtAndAfterFork() {
			// Assert:
			assertOnlyAllowedAtAndAfterFork(RandomTransactionFactory.createMosaicDefinitionCreationTransaction(), MOSAICS_FORK,
					ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK);
		}

		@Test
		public void v2MosaicDefinitionCreationTransactionIsNotAllowed() {
			// Assert:
			assertNotAllowed(changeTransactionVersion(RandomTransactionFactory.createMosaicDefinitionCreationTransaction(), 2),
					MOSAICS_FORK);
		}

		@Test
		public void mosaicSupplyChangeTransactionIsOnlyAllowedAtAndAfterFork() {
			// Assert:
			assertOnlyAllowedAtAndAfterFork(RandomTransactionFactory.createMosaicSupplyChangeTransaction(), MOSAICS_FORK,
					ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK);
		}

		@Test
		public void v2MosaicSupplyChangeTransactionIsNotAllowed() {
			// Assert:
			assertNotAllowed(changeTransactionVersion(RandomTransactionFactory.createMosaicSupplyChangeTransaction(), 2), MOSAICS_FORK);
		}

		@Test
		public void v2TransferTransactionIsOnlyAllowedAtAndAfterFork() {
			// Assert:
			assertOnlyAllowedAtAndAfterFork(createTransferTransaction(2), MOSAICS_FORK,
					ValidationResult.FAILURE_TRANSACTION_BEFORE_SECOND_FORK);
		}

		@Test
		public void v1TransferTransactionIsAlwaysAllowed() {
			// Assert:
			assertAlwaysAllowed(createTransferTransaction(1), MOSAICS_FORK);
		}

		@Test
		public void v3TransferTransactionIsNotAllowed() {
			// Assert:
			assertNotAllowed(createTransferTransaction(3), MOSAICS_FORK);
		}

		// endregion
	}

	// region PerTransaction

	@RunWith(Parameterized.class)
	public static class PerTransaction {
		private final TestTransactionRegistry.Entry<?> entry;

		public PerTransaction(final int type) {
			this.entry = TestTransactionRegistry.findByType(type);
		}

		@Parameterized.Parameters
		public static Collection<Object[]> data() {
			return ParameterizedUtils.wrap(TransactionTypes.getActiveTypes());
		}

		@Test
		public void versionOneTransactionIsAllowed() {
			// Arrange:
			final Transaction transaction = changeTransactionVersion(this.entry.createModel.get(), 1);

			// Assert:
			assertValidation(transaction, Long.MAX_VALUE, ValidationResult.SUCCESS);
		}

		@Test
		public void versionOneHundredTransactionIsNotAllowed() {
			// Arrange:
			final Transaction transaction = changeTransactionVersion(this.entry.createModel.get(), 100);

			// Assert:
			assertValidation(transaction, Long.MAX_VALUE, ValidationResult.FAILURE_ENTITY_INVALID_VERSION);
		}
	}

	private static Transaction createTransferTransaction(final int version) {
		return changeTransactionVersion(RandomTransactionFactory.createTransfer(), version);
	}

	private static Transaction createModificationTransaction(final int version) {
		return changeTransactionVersion(RandomTransactionFactory.createMultisigModification(), version);
	}

	private static void assertOnlyAllowedAtAndAfterFork(final Transaction transaction, final long forkHeight,
			final ValidationResult expectedFailure) {
		assertValidation(transaction, forkHeight - 100, expectedFailure);
		assertValidation(transaction, forkHeight - 1, expectedFailure);
		assertValidation(transaction, forkHeight, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight + 1, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight + 100, ValidationResult.SUCCESS);
	}

	private static void assertAlwaysAllowed(final Transaction transaction, final long forkHeight) {
		assertValidation(transaction, 1, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight - 100, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight - 1, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight + 1, ValidationResult.SUCCESS);
		assertValidation(transaction, forkHeight + 100, ValidationResult.SUCCESS);
	}

	private static void assertNotAllowed(final Transaction transaction, final long blockHeight) {
		assertValidation(transaction, blockHeight, ValidationResult.FAILURE_ENTITY_INVALID_VERSION);
	}

	private static void assertValidation(final Transaction transaction, final long blockHeight, final ValidationResult expectedResult) {
		// Arrange:
		final SingleTransactionValidator validator = new VersionTransactionValidator();
		final ValidationContext validationContext = new ValidationContext(new BlockHeight(blockHeight), ValidationStates.Throw);

		// Act:
		final ValidationResult result = validator.validate(transaction, validationContext);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static Transaction changeTransactionVersion(final Transaction transaction, final int version) {
		final JSONObject jsonObject = JsonSerializer.serializeToJson(transaction.asNonVerifiable());
		jsonObject.put("version", version | (NetworkInfos.getDefault().getVersion() << 24));
		return TransactionFactory.NON_VERIFIABLE.deserialize(Utils.createDeserializer(jsonObject));
	}
}
