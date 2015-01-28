package org.nem.core.test;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

/**
 * A simple multisig context for creating signatures and multisig transactions that can be used in model tests.
 */
public class SimpleMultisigContext {
	private final Transaction innerTransaction;
	private final Hash innerTransactionHash;
	private final Account multisig;

	/**
	 * Creates a multisig context around an inner transaction.
	 *
	 * @param innerTransaction The inner transaction.
	 */
	public SimpleMultisigContext(final Transaction innerTransaction) {
		this.innerTransaction = innerTransaction;
		this.innerTransactionHash = HashUtils.calculateHash(this.innerTransaction);
		this.multisig = innerTransaction.getSigner();
	}

	/**
	 * Creates a default (compatible) signature transaction.
	 *
	 * @return The signature transaction.
	 */
	public MultisigSignatureTransaction createSignature() {
		return this.createSignature(Utils.generateRandomAccount());
	}

	/**
	 * Creates a default (compatible) signature transaction with the specified cosigner.
	 *
	 * @param cosigner The cosigner account.
	 * @return The signature transaction.
	 */
	public MultisigSignatureTransaction createSignature(final Account cosigner) {
		return createSignature(cosigner, this.multisig, this.innerTransactionHash);
	}

	/**
	 * Creates a default (compatible) multisig transaction.
	 *
	 * @return The multisig transaction.
	 */
	public MultisigTransaction createMultisig() {
		return new MultisigTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), this.innerTransaction);
	}

	/**
	 * Creates a default (compatible) multisig transaction with the specified cosigner.
	 *
	 * @param cosigner The cosigner account.
	 * @return The multisig transaction.
	 */
	public MultisigTransaction createMultisig(final Account cosigner) {
		return new MultisigTransaction(TimeInstant.ZERO, cosigner, this.innerTransaction);
	}

	/**
	 * Creates a signature transaction with the specified hash.
	 *
	 * @param hash The desired hash.
	 * @return The signature transaction.
	 */
	public MultisigSignatureTransaction createSignatureWithHash(final Hash hash) {
		return createSignature(Utils.generateRandomAccount(), this.multisig, hash);
	}

	/**
	 * Creates a signature transaction with the specified multisig account.
	 *
	 * @param multisig The desired multisig.
	 * @return The signature transaction.
	 */
	public MultisigSignatureTransaction createSignatureWithMultisig(final Account multisig) {
		return createSignature(Utils.generateRandomAccount(), multisig, this.innerTransactionHash);
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
}
