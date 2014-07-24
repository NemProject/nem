package org.nem.peer;

import org.nem.core.node.*;
import org.nem.core.serialization.*;

/**
 * A deserializer for local node data.
 */
public class LocalNodeDeserializer implements ObjectDeserializer<Node> {

	@Override
	public Node deserialize(final Deserializer deserializer) {
		final NodeIdentity identity = deserializer.readObject("identity", deser -> NodeIdentity.deserializeWithPrivateKey(deser));
		final NodeEndpoint endpoint = deserializer.readObject("endpoint", obj -> new NodeEndpoint(obj));
		final NodeMetaData metaData = deserializer.readObject("metaData", obj -> new NodeMetaData(obj));
		return new Node(identity, endpoint, metaData);
	}
}