package org.nem.peer.node;

import org.nem.core.serialization.*;
import org.nem.core.utils.HexEncoder;

import java.util.Arrays;

/**
 * A challenge that can be used for authenticating nodes.
 */
public class NodeChallenge implements SerializableEntity {

	private final byte[] data;

	/**
	 * Creates a new challenge.
	 *
	 * @param bytes The raw challenge bytes.
	 */
	public NodeChallenge(final byte[] bytes) {
		this.data = bytes;
	}

	/**
	 * Deserializes a challenge.
	 *
	 * @param deserializer The challenge.
	 */
	public NodeChallenge(final Deserializer deserializer) {
		this.data = deserializer.readBytes("data");
	}

	/**
	 * Gets the raw challenge value.
	 *
	 * @return The raw challenge value.
	 */
	public byte[] getRaw() {
		return this.data;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeBytes("data", this.data);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.data);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeChallenge)) {
			return false;
		}

		final NodeChallenge rhs = (NodeChallenge) obj;
		return Arrays.equals(this.data, rhs.data);
	}

	@Override
	public String toString() {
		return HexEncoder.getString(this.data);
	}
}
