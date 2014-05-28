package org.nem.core.model;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;

import java.io.IOException;
import java.io.InputStream;

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

	private final static PublicKey CREATOR_PUBLIC_KEY = new PublicKey(
			HexEncoder.getBytes("03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c")
	);

	//private final static Hash GENESIS_GENERATION_HASH = new Hash(Hashes.sha3("If ever I to the moment shall say: Beautiful moment, do not pass away! Then you may forge your chains to bind me,".getBytes()));
	private final static Hash GENESIS_GENERATION_HASH = new Hash(
			HexEncoder.getBytes("c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8")
	);

	private final static String GENESIS_BLOCK_FILE = "genesis-block.json";

	static {
		final KeyPair genesisKeyPair = new KeyPair(CREATOR_PRIVATE_KEY);
		ACCOUNT = new Account(genesisKeyPair);
	}

	private GenesisBlock(final Deserializer deserializer) {
		super(Block.BLOCK_TYPE, DeserializationOptions.VERIFIABLE, deserializer);
		this.setPrevious(GENESIS_GENERATION_HASH, Hash.ZERO);
	}

	/**
	 * Creates the genesis block.
	 */
	public static GenesisBlock create() {

		final InputStream fin = GenesisBlock.class.getClassLoader().getResourceAsStream(GENESIS_BLOCK_FILE);
		if (null == fin) {
			throw new IllegalArgumentException(
					String.format("Genesis block file <%s> not available", GENESIS_BLOCK_FILE));
		}


//				return new Config((JSONObject)JSONValue.parse(fin));
		// TODO: this really needs to be passed in
		// TODO: needs cleanup
		try {
			final JSONObject jsonGenesisBlock = (JSONObject)JSONValue.parseStrict(fin);
			final DeserializationContext context = new DeserializationContext((new AccountAnalyzer(null)).asAutoCache());
			final Deserializer deserializer = new JsonDeserializer(jsonGenesisBlock, context);
			deserializer.readInt("type");

			return new GenesisBlock(deserializer);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (net.minidev.json.parser.ParseException e) {
			e.printStackTrace();
		}

		throw new IllegalStateException("can't continue");
	}
}