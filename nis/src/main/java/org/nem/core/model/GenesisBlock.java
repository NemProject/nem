package org.nem.core.model;

import org.nem.core.crypto.KeyPair;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.HexEncoder;

import java.math.BigInteger;

/**
 * Represents the genesis block.
 */
public class GenesisBlock extends Block {

    // this will be removed later, only public key will be present in the code
    // all signatures will be pre-generated and placed in-code
    private final static BigInteger CREATOR_PRIVATE_KEY = new BigInteger(
        HexEncoder.getBytesSilent("aa761e0715669beb77f71de0ce3c29b792e8eb3130d21f697f59070665100c04"));

    private final static int GENESIS_HEIGHT = 1;
    private final static int HASH_LENGTH = 32;

    public final static Account GENESIS_ACCOUNT;

	public final static byte[] GENESIS_HASH = HexEncoder.getBytesSilent("7486a763590b6220b3b275f06dc313bdffeea47359d98d2b1dfd9278ce8faf4b");

	//	Hashes.sha3(StringEncoder.getBytes("super-duper-special")),
	//	Hashes.sha3(StringEncoder.getBytes("Jaguar0625")),
	//	Hashes.sha3(StringEncoder.getBytes("BloodyRookie")),
	//	Hashes.sha3(StringEncoder.getBytes("Thies1965")),
	//	Hashes.sha3(StringEncoder.getBytes("borzalom")),
	//	Hashes.sha3(StringEncoder.getBytes("gimre")),
	//	Hashes.sha3(StringEncoder.getBytes("Makoto")),
	//	Hashes.sha3(StringEncoder.getBytes("UtopianFuture")),
	//	Hashes.sha3(StringEncoder.getBytes("minusbalancer"))
    private final static String[] GENESIS_RECIPIENT_ACCOUNT_IDS = new String[] {
        "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI",
        "NCBWD3TSIMFRHV67PQUQPRL5SZ5CEE6MUL2ANOON",
        "NBI5SUNZOYBHM3D6Q7BOHP6K327EIJ6EETIIRTS2",
        "NAUULYJ4MSHXON3GDQVUN4WFRTAQNADYL5KYTTX7",
        "NBT7M43C4X25VDNSL34IRQO5IRKO6WXSMSJ4PCFP",
        "NAXNGGK5JEU7EXXFLV4L2NCGNJAWBGEOPEI4XHUN",
        "NCVRRAC4GIGMY5BIHDQZO3K6HLAJIDKYZDF7RO5H",
        "NBMSVDI52MR3KSO7RGIJEGGMGZAGSKV4A3ZNJJSM",
        "NBZUVLKB7THC5QH5IJUJVEF66QJZUCQLMVTIFXUC"
    };

    static {
        final KeyPair genesisKeyPair = new KeyPair(CREATOR_PRIVATE_KEY);
        GENESIS_ACCOUNT = new Account(genesisKeyPair);
    }

	// 40.000.000 NEMs (* 1000000 micro nems)
    private final static long GENESIS_AMOUNT = 40000000000000L;

    /**
     * Creates a genesis block.
     *
     * @param timestamp The block timestamp.
     */
    public GenesisBlock(final int timestamp) {
        super(GENESIS_ACCOUNT, new byte[HASH_LENGTH], timestamp, GENESIS_HEIGHT);

        // TODO: as a placeholder distribute amounts equally
        final long shareAmount = GENESIS_AMOUNT / GENESIS_RECIPIENT_ACCOUNT_IDS.length;
        for (final String id : GENESIS_RECIPIENT_ACCOUNT_IDS) {
            final Address address = Address.fromEncoded(id);
            final Account account = new Account(address);
            final TransferTransaction transaction = new TransferTransaction(TransactionTypes.TRANSFER, GENESIS_ACCOUNT, account, shareAmount, null);
            transaction.setFee(0); // TODO: this won't work because of minimum fee enforcement

			transaction.sign();
            this.addTransaction(transaction);
        }

        this.sign();
    }
}
