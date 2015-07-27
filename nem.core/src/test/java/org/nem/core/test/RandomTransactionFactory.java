package org.nem.core.test;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;

import java.util.Collections;
import java.util.stream.IntStream;

/**
 * Factory class used to create random (concrete) transactions.
 */
public class RandomTransactionFactory {

	/**
	 * Creates a transfer transaction.
	 *
	 * @return The transfer.
	 */
	public static TransferTransaction createTransfer() {
		return createTransfer(Utils.generateRandomAccount());
	}

	/**
	 * Creates a transfer transaction.
	 *
	 * @param signer The signer.
	 * @return The transfer.
	 */
	public static TransferTransaction createTransfer(final Account signer) {
		return new TransferTransaction(
				TimeInstant.ZERO,
				signer,
				Utils.generateRandomAccount(),
				Amount.fromNem(111),
				null);
	}

	/**
	 * Creates an importance transfer transaction.
	 *
	 * @return The importance transfer.
	 */
	public static ImportanceTransferTransaction createImportanceTransfer() {
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				ImportanceTransferMode.Activate,
				Utils.generateRandomAccount());
	}

	/**
	 * Creates a multisig aggregate modification.
	 *
	 * @return The multisig aggregate modification.
	 */
	public static MultisigAggregateModificationTransaction createMultisigModification() {
		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Collections.singletonList(new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, Utils.generateRandomAccount())));
	}

	/**
	 * Creates a multisig transfer.
	 *
	 * @param multisig The multisig account.
	 * @param cosigner The cosigner account.
	 * @return A multisig transfer.
	 */
	public static MultisigTransaction createMultisigTransfer(final Account multisig, final Account cosigner) {
		return new MultisigTransaction(
				TimeInstant.ZERO,
				cosigner,
				createTransfer(multisig));
	}

	/**
	 * Creates a multisig transfer.
	 *
	 * @return A multisig transfer.
	 */
	public static MultisigTransaction createMultisigTransfer() {
		return createMultisigTransfer(Utils.generateRandomAccount(), Utils.generateRandomAccount());
	}

	/**
	 * Creates a multisig transfer with three signatures.
	 *
	 * @return A multisig transfer.
	 */
	public static MultisigTransaction createMultisigTransferWithThreeSignatures() {
		final MultisigTransaction multisig = createMultisigTransfer();
		IntStream.range(0, 3).forEach(i -> {
			final MultisigSignatureTransaction signature = createSignatureWithHash(multisig.getDebtor(), multisig.getOtherTransactionHash());
			multisig.addSignature(signature);
		});

		return multisig;
	}

	/**
	 * Creates a signature transaction with the specified hash.
	 *
	 * @param multisig The multisig account.
	 * @param hash The desired hash.
	 * @return The signature transaction.
	 */
	public static MultisigSignatureTransaction createSignatureWithHash(final Account multisig, final Hash hash) {
		return createSignature(Utils.generateRandomAccount(), multisig, hash);
	}

	private static MultisigSignatureTransaction createSignature(final Account cosigner, final Account multisig, final Hash hash) {
		final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				cosigner,
				multisig,
				hash);
		transaction.sign();
		return transaction;
	}

	/**
	 * Creates a multisig signature transaction.
	 *
	 * @return The multisig signature transaction.
	 */
	public static MultisigSignatureTransaction createMultisigSignature() {
		return new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Utils.generateRandomHash());
	}

	/**
	 * Creates a provision namespace transaction.
	 *
	 * @return The provision namespace transaction.
	 */
	public static ProvisionNamespaceTransaction createProvisionNamespaceTransaction() {
		return new ProvisionNamespaceTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(25000),
				new NamespaceIdPart("bar"),
				new NamespaceId("foo"));
	}

	/**
	 * Creates a mosaic definition creation transaction.
	 *
	 * @return The mosaic definition creation transaction.
	 */
	public static MosaicDefinitionCreationTransaction createMosaicDefinitionCreationTransaction() {
		return createMosaicDefinitionCreationTransaction(TimeInstant.ZERO, Utils.generateRandomAccount());
	}

	/**
	 * Creates a mosaic definition creation transaction.
	 *
	 * @param timeStamp The timestamp.
	 * @param signer The signer.
	 * @return The mosaic definition creation transaction.
	 */
	public static MosaicDefinitionCreationTransaction createMosaicDefinitionCreationTransaction(final TimeInstant timeStamp, final Account signer) {
		return new MosaicDefinitionCreationTransaction(
				timeStamp,
				signer,
				Utils.createMosaicDefinition(signer));
	}

	/**
	 * Creates a mosaic supply change transaction.
	 *
	 * @return The mosaic supply change transaction.
	 */
	public static MosaicSupplyChangeTransaction createMosaicSupplyChangeTransaction() {
		return createMosaicSupplyChangeTransaction(TimeInstant.ZERO, Utils.generateRandomAccount());
	}

	/**
	 * Creates a mosaic supply change transaction.
	 *
	 * @param timeStamp The timestamp.
	 * @param signer The signer.
	 * @return The mosaic supply change transaction.
	 */
	public static MosaicSupplyChangeTransaction createMosaicSupplyChangeTransaction(final TimeInstant timeStamp, final Account signer) {
		return new MosaicSupplyChangeTransaction(
				timeStamp,
				signer,
				Utils.createMosaicDefinition(signer).getId(),
				MosaicSupplyType.Create,
				Supply.fromValue(123));
	}
}
