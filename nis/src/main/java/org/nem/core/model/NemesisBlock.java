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

	private final static PublicKey CREATOR_PUBLIC_KEY = PublicKey.fromHexString(
			"038ecc7a57d3d932330ad1521a4afbd36beacd8d7bb885537350f79f7f834f51c8");

	private final static Hash NEMESIS_GENERATION_HASH = Hash.fromHexString(
			"c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8");

	private final static String NEMESIS_BLOCK_FILE = "nemesis-block.json";

	static {
		final KeyPair nemesisKeyPair = new KeyPair(CREATOR_PUBLIC_KEY);
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
		} catch (IOException e) {
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
		} catch (IOException | net.minidev.json.parser.ParseException e) {
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
		if (BlockTypes.NEMESIS != deserializer.readInt("type")) {
			throw new IllegalArgumentException("json object does not have correct type set");
		}

		return new NemesisBlock(deserializer);
	}
}