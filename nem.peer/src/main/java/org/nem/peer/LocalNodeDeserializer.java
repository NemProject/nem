package org.nem.peer;

import org.nem.core.node.*;
import org.nem.core.serialization.*;

/**
 * A deserializer for local node data.
 */
public class LocalNodeDeserializer implements ObjectDeserializer<Node> {

	@Override
	public Node deserialize(final Deserializer deserializer) {
		final NodeIdentity identity = deserializer.readObject("identity", NodeIdentity::deserializeWithPrivateKey);
		final NodeEndpoint endpoint = deserializer.readObject("endpoint", NodeEndpoint::new);
		final NodeMetaData metaData = deserializer.readObject("metaData", LocalNodeDeserializer::deserializeLocalNodeMetaData);
		return new Node(identity, endpoint, metaData);
	}

	private static NodeMetaData deserializeLocalNodeMetaData(final Deserializer deserializer) {
		// the only metadata property we care about is application; platform and version will get populated automatically
		// (this works because NCC only sends the application metadata when booting)
		final String application = deserializer.readOptionalString("application");
		return new NodeMetaData(null, application);
	}
}
