package org.nem.peer;

import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.SerializableList;
import org.nem.core.utils.MustBe;

/**
 * A pair comprised of a nis peer id and an entity list.
 */
public class NisPeerIdAndEntityListPair {
	private final NisPeerId apiId;
	private final SerializableList<?> entities;

	/**
	 * Creates a broadcastable list.
	 *
	 * @param apiId The api id.
	 * @param entities The entities.
	 */
	public NisPeerIdAndEntityListPair(final NisPeerId apiId, final SerializableList<?> entities) {
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
		if (obj == null || !(obj instanceof NisPeerIdAndEntityListPair)) {
			return false;
		}

		final NisPeerIdAndEntityListPair rhs = (NisPeerIdAndEntityListPair) obj;
		return this.apiId.equals(rhs.apiId) && this.entities.asCollection().equals(rhs.entities.asCollection());
	}
}
