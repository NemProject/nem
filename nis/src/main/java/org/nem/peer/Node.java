/**
 * 
 */
package org.nem.peer;

import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minidev.json.JSONObject;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.serialization.Serializer;
import org.nem.core.serialization.*;
import org.nem.deploy.CommonStarter;
import org.nem.peer.v2.NodeEndpoint;
import org.nem.peer.v2.NodeApiId;
import org.nem.peer.v2.NodeInfo;
import org.nem.peer.v2.NodeStatus;

/**
 * Reflects a node within a peer network A node is classified by its URI.
 * Connection to peers are always done via REST connections. The node collects
 * some statistics like connection time, whether connected, error requests /
 * responses
 * 
 * @author Thies1965
 * 
 */
public class Node implements SerializableEntity {
	private static final long serialVersionUID = -5710930110703963436L;
	private static final Logger logger = Logger.getLogger(Node.class.getName());

	// The resources and their URI
	private transient long startTime;
	private transient long totalTime;
	private transient long successfulCalls;
	private transient long failedCalls;

    private org.nem.peer.v2.Node node;

	/**
	 * Deserializes a node.
	 * 
	 * @param deserializer
	 *            The deserializer.
	 */
	public Node(final Deserializer deserializer) {
        NodeInfo info = new NodeInfo(deserializer);
        this.node = new org.nem.peer.v2.Node(info);
	}

	public Node(String host) {
        try {
            NodeEndpoint endpoint = new NodeEndpoint("http", host, CommonStarter.NEM_PORT);
            NodeInfo info = new NodeInfo(endpoint, "", CommonStarter.APP_NAME);
            this.node = new org.nem.peer.v2.Node(info);
        } catch (InvalidParameterException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Node(org.nem.peer.v2.Node node) {
        this.node = node;
    }


    public String getAddress() {
		return this.node.getInfo().getEndpoint().getBaseUrl().getHost();
	}

	public String getVersion() {
		return String.format("%d", this.node.getInfo().getVersion());
	}

	public Integer getProtocol() {
        return 0;
//		return this.node.getInfo().getProtocol();
	}

	public String getPlatform() {
		return this.node.getInfo().getPlatform();
	}

//	public void setPlatform(String myPlatform) {
//		this.platform = myPlatform;
//	}

	public NodeStatus getState() {
        return this.node.getStatus();
	}

	public void setState(NodeStatus status) {
		this.node.setStatus(status);
	}

	public String toString() {
        return this.node.toString();
	}

	/**
	 * Serializes this object.
	 * 
	 * @param serializer
	 *            The serializer to use.
	 */
	public void serialize(final Serializer serializer) {
        this.node.getInfo().serialize(serializer);
	}

	/**
	 * Short cut for creating JsonSerializer and serialize this.
	 * 
	 */
	public JSONObject asJsonObject() {
        return JsonSerializer.serializeToJson(this);
    }

	//
	public boolean verifyNEM() {
		boolean result = false;

		// We verify by getting the Information from the peer
		try {
			PeerConnector connector = new PeerConnector();

			JSONObject response = connector.requestNodeInfo(this);

            NodeInfo info = new NodeInfo(new JsonDeserializer(response, new DeserializationContext(null)));
            this.node = new org.nem.peer.v2.Node(info);

			// Okay we are connected with the peer node
			this.startTime = System.currentTimeMillis();

		} catch (URISyntaxException e) {
			// Connection error, so we do not consider the node for the next
			// time
			logger.warning(toString() + e.toString());

			// set to not connected
			setState(NodeStatus.FAILURE);

		} catch (TimeoutException e) {
			logger.warning(toString() + " timed out.");

			// set to not connected
			setState(NodeStatus.INACTIVE);

		} catch (InterruptedException e1) {
			// Interrupted means the thread received an interrupt signal
			// Usually we have to go down...
			logger.log(Level.INFO, "InterruptedException received during waiting on network response.", e1);
		} catch (ExecutionException e1) {
			// Connection error, so we do not consider the node for the next
			// time
			logger.warning(toString() + e1.toString());

			// set to not connected
			setState(NodeStatus.FAILURE);

		} catch (Exception e) {
            // TODO temporary
            return false;
        }

		return result;
	}

	public URL getRestURL(NodeApiId restID) {
		return this.node.getInfo().getEndpoint().getApiUrl(restID);
	}

}
