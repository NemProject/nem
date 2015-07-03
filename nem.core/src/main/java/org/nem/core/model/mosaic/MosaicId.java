package org.nem.core.model.mosaic;

import org.nem.core.serialization.*;

import java.util.regex.Pattern;

/**
 * Helper class wrapping the mosaic id.
 */
public class MosaicId {
	private static final Pattern IsValidPattern = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9 '_-]*");

	private final String id;

	public MosaicId(final String id) {
		this.id = id;
		this.validate();
	}

	private void validate() {
		final int maxNameLength = 32;
		if (!IsValidPattern.matcher(this.id).matches() || maxNameLength < this.id.length() || this.id.isEmpty()) {
			throw new IllegalArgumentException(String.format("'%s' is not a valid mosaic id", this.id));
		}
	}

	// region inline serialization

	/**
	 * Writes a mosaic id.
	 *
	 * @param serializer The serializer to use.
	 * @param label The label.
	 * @param mosaicId The mosaic id.
	 */
	public static void writeTo(final Serializer serializer, final String label, final MosaicId mosaicId) {
		serializer.writeString(label, mosaicId.id);
	}

	/**
	 * Reads a mosaic id.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The label.
	 * @return The mosaic id.
	 */
	public static MosaicId readFrom(final Deserializer deserializer, final String label) {
		return new MosaicId(deserializer.readString(label));
	}

	// endregion

	@Override
	public String toString() {
		return this.id;
	}

	@Override
	public int hashCode() {
		return this.id.toLowerCase().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MosaicId)) {
			return false;
		}

		final MosaicId rhs = (MosaicId)obj;

		// should not be case sensitive
		return this.id.toLowerCase().equals(rhs.id.toLowerCase());
	}
}
