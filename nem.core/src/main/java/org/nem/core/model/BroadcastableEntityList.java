package org.nem.core.model;

import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.SerializableList;
import org.nem.core.utils.MustBe;

/**
 * A serializable list of entities that can be broadcast by a NodeBroadcaster.
 */
public class BroadcastableEntityList {
	private final NisPeerId apiId;
	private final SerializableList<?> entities;

	/**
	 * Creates a broadcastable list.
	 *
	 * @param apiId The api id.
	 * @param entities The entities.
	 */
	public BroadcastableEntityList(final NisPeerId apiId, final SerializableList<?> entities) {
		this.apiId = apiId;
		this.entities = entities;
		this.validate();
	}

	private void validate() {
		MustBe.notNull(this.apiId, "api id");
		MustBe.notNull(this.entities, "entities");
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
	 * Gets the list of entities.
	 *
	 * @return The entities.
	 */
	public SerializableList<?> getEntities() {
		return this.entities;
	}

	@Override
	public int hashCode() {
		return this.apiId.hashCode() ^ this.entities.asCollection().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof BroadcastableEntityList)) {
			return false;
		}

		final BroadcastableEntityList rhs = (BroadcastableEntityList)obj;
		return this.apiId.equals(rhs.apiId) && this.entities.asCollection().equals(rhs.entities.asCollection());
	}

}
