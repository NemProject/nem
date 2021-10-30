package org.nem.nis.controller.viewmodels;

import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.peer.trust.score.*;

/**
 * A NodeExperience pair with additional information.
 */
public class ExtendedNodeExperiencePair extends NodeExperiencePair {

	private final int numSyncAttempts;

	/**
	 * Creates a new node experience pair.
	 *
	 * @param node The node.
	 * @param experience The node experience.
	 * @param numSyncAttempts The number of sync attempts.
	 */
	public ExtendedNodeExperiencePair(final Node node, final NodeExperience experience, final int numSyncAttempts) {
		super(node, experience);
		this.numSyncAttempts = numSyncAttempts;
	}

	/**
	 * Deserializes a node experience pair.
	 *
	 * @param deserializer The deserializer.
	 */
	public ExtendedNodeExperiencePair(final Deserializer deserializer) {
		super(deserializer);
		this.numSyncAttempts = deserializer.readInt("syncs");
	}

	/**
	 * Gets the number of sync attempts.
	 *
	 * @return The number of sync attempts.
	 */
	public int getNumSyncAttempts() {
		return this.numSyncAttempts;
	}

	@Override
	public void serialize(final Serializer serializer) {
		super.serialize(serializer);
		serializer.writeInt("syncs", this.numSyncAttempts);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ this.numSyncAttempts;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ExtendedNodeExperiencePair)) {
			return false;
		}

		final ExtendedNodeExperiencePair rhs = (ExtendedNodeExperiencePair) obj;
		return super.equals(rhs) && this.numSyncAttempts == rhs.numSyncAttempts;
	}
}
