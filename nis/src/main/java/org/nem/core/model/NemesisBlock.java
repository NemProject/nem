package org.nem.core.model;

import net.minidev.json.*;
import org.apache.commons.io.IOUtils;
import org.nem.core.crypto.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;

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
	public final static Amount AMOUNT = Amount.fromNem(8000000000L);

	/**
	 * The nemesis generation hash.
	 */
	public final static Hash GENERATION_HASH = Hash.fromHexString(
			"16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4");

	private final static PublicKey CREATOR_PUBLIC_KEY = PublicKey.fromHexString(
			"e59ef184a612d4c3c4d89b5950eb57262c69862b2f96e59c5043bf41765c482f");

	private final static String NEMESIS_BLOCK_FILE = "nemesis-block.bin";

	static {
		final KeyPair nemesisKeyPair = new KeyPair(CREATOR_PUBLIC_KEY);
		ADDRESS = Address.fromPublicKey(nemesisKeyPair.getPublicKey());
	}

	private NemesisBlock(final Deserializer deserializer) {
		super(BlockTypes.NEMESIS, DeserializationOptions.VERIFIABLE, deserializer);
		this.setGenerationHash(GENERATION_HASH);

		fixTransactions(this.getTransactions());
	}

	private static void fixTransactions(final List<Transaction> transactions) {
		for (final Transaction transaction : transactions) {
			final Field field;
			try {
				field = transaction.getClass().getSuperclass().getDeclaredField("nemesisTransaction");
				field.setAccessible(true);
				field.set(transaction, true);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
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
		} catch (final IOException e) {
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
			//return fromJsonObject((JSONObject)JSONValue.parseStrict(fin), context);
			final ByteArrayOutputStream blob = new ByteArrayOutputStream(65536);
			IOUtils.copy(fin, blob);
			return fromBlobObject(blob.toByteArray(), context);
		} catch (IOException e) {
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

	/**
	 * Loads the nemesis block from a binaryData.
	 *
	 * @param binObject The binary data.
	 * @param context The deserialization context to use.
	 * @return The nemesis block.
	 */
	public static NemesisBlock fromBlobObject(final byte[] binObject, final DeserializationContext context) {
		final Deserializer deserializer = new BinaryDeserializer(binObject, context);
		if (BlockTypes.NEMESIS != deserializer.readInt("type")) {
			throw new IllegalArgumentException("json object does not have correct type set");
		}

		return new NemesisBlock(deserializer);
	}
}