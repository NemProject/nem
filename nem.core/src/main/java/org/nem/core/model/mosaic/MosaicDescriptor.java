package org.nem.core.model.mosaic;

import org.nem.core.serialization.*;

import java.util.regex.Pattern;

/**
 * Helper class wrapping the mosaic description.
 */
public class MosaicDescriptor {
	private static final Pattern IsValidPattern = Pattern.compile("^[a-zA-Z0-9].*");
	private final String description;

	public MosaicDescriptor(final String description) {
		this.description = description;
		this.validate();
	}

	// TODO 20150703 BR -> all: limit to same pattern as id?
	private void validate() {
		final int maxDescriptionLength = 512;
		if (null == this.description) {
			throw new IllegalArgumentException("mosaic description cannot be null");
		}

		if (!IsValidPattern.matcher(this.description).matches() ||
			maxDescriptionLength < this.description.length() ||
			this.description.isEmpty()) {
			throw new IllegalArgumentException(String.format("'%s' is not a valid mosaic description", this.description));
		}
	}

	// region inline serialization

	/**
	 * Writes a mosaic descriptor.
	 *
	 * @param serializer The serializer to use.
	 * @param label The label.
	 * @param descriptor The mosaic descriptor.
	 */
	public static void writeTo(final Serializer serializer, final String label, final MosaicDescriptor descriptor) {
		serializer.writeString(label, descriptor.description);
	}

	/**
	 * Reads a mosaic descriptor.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The label.
	 * @return The mosaic descriptor.
	 */
	public static MosaicDescriptor readFrom(final Deserializer deserializer, final String label) {
		return new MosaicDescriptor(deserializer.readString(label));
	}

	// endregion

	@Override
	public String toString() {
		return this.description;
	}

	@Override
	public int hashCode() {
		return this.description.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MosaicDescriptor)) {
			return false;
		}

		final MosaicDescriptor rhs = (MosaicDescriptor)obj;

		// should not be case sensitive
		return this.description.equals(rhs.description);
	}
}
