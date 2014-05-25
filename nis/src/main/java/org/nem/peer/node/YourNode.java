package org.nem.peer.node;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class YourNode implements SerializableEntity {
	private Node nodeInfo;
	private NodeEndpoint requestEndpoint;
	private String error;
	
	public YourNode(NodeEndpoint requestEndpoint, Node nodeInfo) {
		this.nodeInfo = nodeInfo;
		this.requestEndpoint = requestEndpoint;
		this.error = null;
	}

	public YourNode(NodeEndpoint requestEndpoint, String error) {
		this.nodeInfo = null;
		this.requestEndpoint = requestEndpoint;
		this.error = error;
	}

	public YourNode(Deserializer deserializer) {
		this.nodeInfo = deserializer.readObject("response", Node::new);
		this.requestEndpoint = deserializer.readObject("requestEndpoint", NodeEndpoint.DESERIALIZER);
		this.error = deserializer.readString("error");
	}

	public Node getNodeInfo() {
		return nodeInfo;
	}

	public NodeEndpoint getRequestEndpoint() {
		return requestEndpoint;
	}

	public String getError() {
		return error;
	}

	@Override
	public void serialize(Serializer serializer) {
		//
		serializer.writeObject("response", nodeInfo);
		serializer.writeObject("requestEndpoint", requestEndpoint);
		serializer.writeString("error", error);
	}
}
