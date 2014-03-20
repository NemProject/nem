package org.nem.core.model;

import org.nem.core.crypto.KeyPair;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.HexEncoder;

import java.math.BigInteger;

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
    private final static BigInteger CREATOR_PRIVATE_KEY = new BigInteger(
        HexEncoder.getBytesSilent("aa761e0715669beb77f71de0ce3c29b792e8eb3130d21f697f59070665100c04"));

    private final static int GENESIS_HEIGHT = 1;
    private final static int HASH_LENGTH = 32;

    private final static String[] GENESIS_RECIPIENT_ACCOUNT_IDS = new String[] {
        "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI",
        "NCBWD3TSIMFRHV67PQUQPRL5SZ5CEE6MUL2ANOON",
        "NBI5SUNZOYBHM3D6Q7BOHP6K327EIJ6EETIIRTS2",
        "NAUULYJ4MSHXON3GDQVUN4WFRTAQNADYL5KYTTX7",
        "NBT7M43C4X25VDNSL34IRQO5IRKO6WXSMSJ4PCFP",
        "NAXNGGK5JEU7EXXFLV4L2NCGNJAWBGEOPEI4XHUN",
        "NCVRRAC4GIGMY5BIHDQZO3K6HLAJIDKYZDF7RO5H",
        "NBMSVDI52MR3KSO7RGIJEGGMGZAGSKV4A3ZNJJSM"
    };

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
        super(ACCOUNT, new byte[HASH_LENGTH], timestamp, GENESIS_HEIGHT);

        // TODO: as a placeholder distribute amounts equally
        final Amount shareAmount = new Amount(AMOUNT.getNumMicroNem() / GENESIS_RECIPIENT_ACCOUNT_IDS.length);
        for (final String id : GENESIS_RECIPIENT_ACCOUNT_IDS) {
            final Address address = Address.fromEncoded(id);
            final Account account = new Account(address);
            final TransferTransaction transaction = new TransferTransaction(timestamp, ACCOUNT, account, shareAmount, null);

			transaction.sign();
            this.addTransaction(transaction);
        }

        this.sign();
    }
}
