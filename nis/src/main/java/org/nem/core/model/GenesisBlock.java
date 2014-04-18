package org.nem.core.model;

import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.time.TimeInstant;

/**
 * Represents the genesis block.
 */
public class GenesisBlock extends Block {

	/**
	 * The genesis account.
	 */
	public final static Account ACCOUNT;

	/**
	 * The amount of NEM in the genesis block.
	 */
	public final static Amount AMOUNT = Amount.fromNem(4000000000L);

	// this will be removed later, only public key will be present in the code
	// all signatures will be pre-generated and placed in-code
	private final static PrivateKey CREATOR_PRIVATE_KEY = PrivateKey.fromHexString(
			"aa761e0715669beb77f71de0ce3c29b792e8eb3130d21f697f59070665100c04");

	private final static BlockHeight GENESIS_HEIGHT = BlockHeight.ONE;

	static {
		final KeyPair genesisKeyPair = new KeyPair(CREATOR_PRIVATE_KEY);
		ACCOUNT = new Account(genesisKeyPair);
	}

	/**
	 * Creates a genesis block.
	 *
	 * @param timestamp The block timestamp.
	 */
	public GenesisBlock(final TimeInstant timestamp) {
		super(ACCOUNT, Hash.ZERO, timestamp, GENESIS_HEIGHT);

		// TODO: as a placeholder distribute amounts equally
		final String[] recipientIds = NetworkInfo.getDefault().getGenesisRecipientAccountIds();
		final Amount shareAmount = new Amount(AMOUNT.getNumMicroNem() / recipientIds.length);
		for (final String id : recipientIds) {
			final Address address = Address.fromEncoded(id);
			final Account account = new Account(address);
			final TransferTransaction transaction = new TransferTransaction(timestamp, ACCOUNT, account, shareAmount, null);

			transaction.sign();
			this.addTransaction(transaction);
		}

		this.sign();
	}
}
