package org.nem.peer;

import org.nem.core.serialization.*;
import org.nem.peer.node.*;

/**
 * A deserializer for local node data.
 */
public class LocalNodeDeserializer implements ObjectDeserializer<Node> {

	@Override
	public Node deserialize(final Deserializer deserializer) {
		final NodeIdentity identity = deserializer.readObject("identity", NodeIdentity::deserializeWithPrivateKey);
		final NodeEndpoint endpoint = deserializer.readObject("endpoint", NodeEndpoint::new);
		final NodeMetaData metaData = deserializer.readObject("metaData", NodeMetaData::new);
		return new Node(identity, endpoint, metaData);
	}
}