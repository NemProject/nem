package org.nem.peer.node;

import org.nem.core.crypto.Signature;
import org.nem.core.node.NodeIdentity;
import org.nem.core.serialization.*;

import java.util.logging.Logger;

/**
 * Authenticated NIS response that can be used to authenticate the local node.
 *
 * @param <T> The type of entity.
 */
public class AuthenticatedResponse<T extends SerializableEntity> implements SerializableEntity {
	private static final Logger LOGGER = Logger.getLogger(AuthenticatedResponse.class.getName());

	private final T entity;
	private final Signature signature;

	/**
	 * Creates an authenticated response.
	 *
	 * @param entity The entity.
	 * @param identity The identity.
	 * @param challenge The challenge data.
	 */
	public AuthenticatedResponse(final T entity, final NodeIdentity identity, final NodeChallenge challenge) {
		this.entity = entity;
		this.signature = identity.sign(challenge.getRaw());
	}

	/**
	 * Deserializes a response.
	 *
	 * @param deserializer The deserializer.
	 * @param entityDeserializer The deserializer for the underlying entity.
	 */
	public AuthenticatedResponse(final Deserializer deserializer, final ObjectDeserializer<T> entityDeserializer) {
		this.entity = deserializer.readObject("entity", entityDeserializer);
		this.signature = Signature.readFrom(deserializer, "signature");
	}

	/**
	 * Gets the entity.
	 *
	 * @param identity The identity of the responding node.
	 * @param challenge The challenge data.
	 * @return The entity.
	 */
	public T getEntity(final NodeIdentity identity, final NodeChallenge challenge) {
		if (!identity.verify(challenge.getRaw(), this.signature)) {
			LOGGER.info(String.format("couldn't verify response from node '%s'", identity));
			throw new ImpersonatingPeerException("entity source cannot be verified");
		}

		return this.entity;
	}

	/**
	 * Gets the signature.
	 *
	 * @return The signature.
	 */
	public Signature getSignature() {
		return this.signature;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("entity", this.entity);
		Signature.writeTo(serializer, "signature", this.signature);
	}
}
