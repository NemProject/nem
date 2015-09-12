package org.nem.core.model;

import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.utils.MustBe;

/**
 * A pair that can be broadcast by a NodeBroadcaster.
 */
public class BroadcastablePair {
	private final NisPeerId apiId;
	private final SerializableEntity entity;

	/**
	 * Creates a broadcastable pair.
	 *
	 * @param apiId The api id.
	 * @param entity The entity.
	 */
	public BroadcastablePair(final NisPeerId apiId, final SerializableEntity entity) {
		this.apiId = apiId;
		this.entity = entity;
	}

	private void validate() {
		MustBe.notNull(this.apiId, "api id");
		MustBe.notNull(this.entity, "entity");
	}

	/**
	 * Gets the api id.
	 *
	 * @return The api id.
	 */
	public NisPeerId getApiId() {
		return this.apiId;
	}

	/**
	 * Gets the entity.
	 *
	 * @return The entity.
	 */
	public SerializableEntity getEntity() {
		return this.entity;
	}
}
