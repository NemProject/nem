package org.nem.peer.node;

import org.nem.core.model.NisInfo;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class NisNodeInfo implements SerializableEntity {
	private Node node;
	private NisInfo nisInfo;

	public NisNodeInfo(Node node, NisInfo nisInfo) {
		this.node = node;
		this.nisInfo = nisInfo;
	}

	public Node getNode() {
		return node;
	}

	public NisInfo getNisInfo() {
		return nisInfo;
	}

	@Override
	public void serialize(Serializer serializer) {
		// 
		serializer.writeObject("node", node);
		serializer.writeObject("nisInfo", nisInfo);
	}

}
