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
		final NodeMetaData metaData = deserializer.readObject("metaData", obj -> deserializeLocalNodeMetaData(obj));
		return new Node(identity, endpoint, metaData);
	}

	private static NodeMetaData deserializeLocalNodeMetaData(final Deserializer deserializer) {
		// TODO 20150113 J-J HACK that should be removed before block chain restart
		// > also should fix the serialization of NodeMetaData so all required properties are
		// > serialized before all optional properties
		String application;
		try {
			// the only property we care about is application; platform and version will get populated automatically
			final NodeMetaData metaData = new NodeMetaData(deserializer);
			application = metaData.getApplication();
		} catch (final IllegalArgumentException ex) {
			application = deserializer.readOptionalString("application");
		}

		return new NodeMetaData(null, application, null);
	}
}