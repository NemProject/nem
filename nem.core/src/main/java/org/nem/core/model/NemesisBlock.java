package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.nem.core.serialization.*;

import java.io.*;

/**
 * Represents the nemesis block.
 */
public class NemesisBlock {

	/**
	 * Loads the nemesis block from the default project resource.
	 *
	 * @param nemesisBlockInfo The nemesis block information.
	 * @param context The deserialization context to use.
	 * @return The nemesis block.
	 */
	public static Block fromResource(final NemesisBlockInfo nemesisBlockInfo, final DeserializationContext context) {
		try (final InputStream fin = NemesisBlock.class.getClassLoader().getResourceAsStream(nemesisBlockInfo.getDataFileName())) {
			final byte[] buffer = IOUtils.toByteArray(fin);
			return fromBlobObject(nemesisBlockInfo, buffer, context);
		} catch (final IOException e) {
			throw new IllegalStateException("unable to parse nemesis block stream");
		}
	}

	/**
	 * Loads the nemesis block from a json object.
	 *
	 * @param nemesisBlockInfo The nemesis block information.
	 * @param jsonObject The json object.
	 * @param context The deserialization context to use.
	 * @return The nemesis block.
	 */
	public static Block fromJsonObject(final NemesisBlockInfo nemesisBlockInfo, final JSONObject jsonObject, final DeserializationContext context) {
		final Deserializer deserializer = new JsonDeserializer(jsonObject, context);
		return deserialize(nemesisBlockInfo, deserializer);
	}

	/**
	 * Loads the nemesis block from binary data.
	 *
	 * @param nemesisBlockInfo The nemesis block information.
	 * @param buffer The binary data.
	 * @param context The deserialization context to use.
	 * @return The nemesis block.
	 */
	public static Block fromBlobObject(final NemesisBlockInfo nemesisBlockInfo, final byte[] buffer, final DeserializationContext context) {
		final Deserializer deserializer = new BinaryDeserializer(buffer, context);
		return deserialize(nemesisBlockInfo, deserializer);
	}

	private static Block deserialize(final NemesisBlockInfo nemesisBlockInfo, final Deserializer deserializer) {
		if (BlockTypes.NEMESIS != deserializer.readInt("type")) {
			throw new IllegalArgumentException("deserializer does not have correct type set");
		}

		final Block block = new Block(BlockTypes.NEMESIS, VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
		block.setGenerationHash(nemesisBlockInfo.getGenerationHash());
		return block;
	}
}