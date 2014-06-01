package org.nem.core.model;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.nis.AccountAnalyzer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents the genesis block.
 */
public class GenesisBlock extends Block {

	private final static int GENESIS_BLOCK_TYPE = -1;

	/**
	 * The genesis account address.
	 */
	public final static Address ADDRESS;

	/**
	 * The amount of NEM in the genesis block.
	 */
	public final static Amount AMOUNT = Amount.fromNem(4000000000L);

	// this will be removed later, only public key will be present in the code
	// all signatures will be pre-generated and placed in-code
	private final static PrivateKey CREATOR_PRIVATE_KEY = PrivateKey.fromHexString(
			"aa761e0715669beb77f71de0ce3c29b792e8eb3130d21f697f59070665100c04");

	private final static PublicKey CREATOR_PUBLIC_KEY = PublicKey.fromHexString(
			"03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");

	//private final static Hash GENESIS_GENERATION_HASH = new Hash(Hashes.sha3("If ever I to the moment shall say: Beautiful moment, do not pass away! Then you may forge your chains to bind me,".getBytes()));
	private final static Hash GENESIS_GENERATION_HASH = Hash.fromHexString(
			"c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8");

	private final static String GENESIS_BLOCK_FILE = "genesis-block.json";

	static {
		final KeyPair genesisKeyPair = new KeyPair(CREATOR_PRIVATE_KEY);
		ADDRESS = Address.fromPublicKey(genesisKeyPair.getPublicKey());
	}

	private GenesisBlock(final Deserializer deserializer) {
		super(GENESIS_BLOCK_TYPE, DeserializationOptions.VERIFIABLE, deserializer);
		this.setGenerationHash(GENESIS_GENERATION_HASH);
	}

	/**
	 * Loads the genesis block from the default project resource.
	 */
	public static GenesisBlock fromResource() {
		try (final InputStream fin = GenesisBlock.class.getClassLoader().getResourceAsStream(GENESIS_BLOCK_FILE)) {
			return fromStream(fin);
		}
		catch (IOException e) {
			throw new IllegalStateException("unable to parse genesis block stream");
		}
	}

	/**
	 * Loads the genesis block from an input stream.
	 *
	 * @param fin The input stream.
	 */
	public static GenesisBlock fromStream(final InputStream fin) {
		try {
			return fromJsonObject((JSONObject)JSONValue.parseStrict(fin));
		}
		catch (IOException|net.minidev.json.parser.ParseException e) {
			throw new IllegalArgumentException("unable to parse genesis block stream");
		}
	}

	/**
	 * Loads the genesis block from a json object.
	 *
	 * @param jsonObject The json object.
	 */
	public static GenesisBlock fromJsonObject(final JSONObject jsonObject) {
		final DeserializationContext context = new DeserializationContext((new AccountAnalyzer(null)).asAutoCache());
		final Deserializer deserializer = new JsonDeserializer(jsonObject, context);
		if (GENESIS_BLOCK_TYPE != deserializer.readInt("type"))
			throw new IllegalArgumentException("json object does not have correct type set");

		return new GenesisBlock(deserializer);

	}
}