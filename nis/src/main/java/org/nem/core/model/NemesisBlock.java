package org.nem.core.model;

import net.minidev.json.*;
import org.nem.core.crypto.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;

import java.io.*;

/**
 * Represents the nemesis block.
 */
public class NemesisBlock extends Block {

	/**
	 * The nemesis account address.
	 */
	public final static Address ADDRESS;

	/**
	 * The amount of NEM in the nemesis block.
	 */
	public final static Amount AMOUNT = Amount.fromNem(4000000000L);

	// this will be removed later, only public key will be present in the code
	// all signatures will be pre-generated and placed in-code
	private final static PrivateKey CREATOR_PRIVATE_KEY = PrivateKey.fromHexString(
			"aa761e0715669beb77f71de0ce3c29b792e8eb3130d21f697f59070665100c04");

	private final static PublicKey CREATOR_PUBLIC_KEY = PublicKey.fromHexString(
			"03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");

	//private final static Hash NEMESIS_GENERATION_HASH = new Hash(Hashes.sha3("If ever I to the moment shall say: Beautiful moment, do not pass away! Then you may forge your chains to bind me,".getBytes()));
	private final static Hash NEMESIS_GENERATION_HASH = Hash.fromHexString(
			"c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8");

	private final static String NEMESIS_BLOCK_FILE = "nemesis-block.json";

	static {
		final KeyPair nemesisKeyPair = new KeyPair(CREATOR_PRIVATE_KEY);
		ADDRESS = Address.fromPublicKey(nemesisKeyPair.getPublicKey());
	}

	private NemesisBlock(final Deserializer deserializer) {
		super(BlockTypes.NEMESIS, DeserializationOptions.VERIFIABLE, deserializer);
		this.setGenerationHash(NEMESIS_GENERATION_HASH);
	}

	/**
	 * Loads the nemesis block from the default project resource.
	 *
	 * @param context The deserialization context to use.
	 * @return The nemesis block.
	 */
	public static NemesisBlock fromResource(final DeserializationContext context) {
		try (final InputStream fin = NemesisBlock.class.getClassLoader().getResourceAsStream(NEMESIS_BLOCK_FILE)) {
			return fromStream(fin, context);
		}
		catch (IOException e) {
			throw new IllegalStateException("unable to parse nemesis block stream");
		}
	}

	/**
	 * Loads the nemesis block from an input stream.
	 *
	 * @param fin The input stream.
	 * @param context The deserialization context to use.
	 * @return The nemesis block.
	 */
	public static NemesisBlock fromStream(final InputStream fin, final DeserializationContext context) {
		try {
			return fromJsonObject((JSONObject)JSONValue.parseStrict(fin), context);
		}
		catch (IOException|net.minidev.json.parser.ParseException e) {
			throw new IllegalArgumentException("unable to parse nemesis block stream");
		}
	}

	/**
	 * Loads the nemesis block from a json object.
	 *
	 * @param jsonObject The json object.
	 * @param context The deserialization context to use.
	 * @return The nemesis block.
	 */
	public static NemesisBlock fromJsonObject(final JSONObject jsonObject, final DeserializationContext context) {
		final Deserializer deserializer = new JsonDeserializer(jsonObject, context);
		if (BlockTypes.NEMESIS != deserializer.readInt("type"))
			throw new IllegalArgumentException("json object does not have correct type set");

		return new NemesisBlock(deserializer);
	}
}