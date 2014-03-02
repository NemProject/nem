/**
 * 
 */
package org.nem.peer;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;
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

	private String address;
	private String platform;
	private Integer protocol;
	private String version;

	// The resources and their URI
	private transient URL baseURL;
	private transient Dictionary<NodeRestIDs, URL> restURLs;

	private transient NodeStatus state;
	private transient long startTime;
	private transient long totalTime;
	private transient long successfulCalls;
	private transient long failedCalls;

	/**
	 * Deserializes a node.
	 * 
	 * @param deserializer
	 *            The deserializer.
	 */
	public Node(final Deserializer deserializer) {
		this.address = deserializer.readString("address");
		this.platform = deserializer.readString("platform");
		this.protocol = deserializer.readInt("protocol");
		this.version = deserializer.readString("version");

		populateURLs(address);

		// Hope all addressing issues are identified,
		// so the instance is valid.
		state = NodeStatus.INACTIVE;
	}

	//TODO: Work-around as long as J's serializer is not extended to have arrays serialized.
	public Node(JSONObject jsonNode) {
		this.address = (String) jsonNode.get("address");
		this.platform = (String) jsonNode.get("platform");
		this.protocol = (Integer) jsonNode.get("protocol");
		this.version = (String) jsonNode.get("version");

		populateURLs(address);

		// Hope all addressing issues are identified,
		// so the instance is valid.
		state = NodeStatus.INACTIVE;
	}

	public Node(String addrStr) {
		super();

		if (addrStr == null) {
			throw new IllegalArgumentException("Node requires an address. Null as address is not supported.");
		}

		addrStr = addrStr.trim();
		if (addrStr.length() == 0) {
			throw new IllegalArgumentException("Node requires an address. An empty address is not supported.");
		}

		populateURLs(addrStr);

		// Hope all addressing issues are identified,
		// so the instance is valid.
		state = NodeStatus.INACTIVE;
		protocol = CommonStarter.NEM_PROTOCOL;
		platform = "";
		version = "";

	}

	private void populateURLs(String addrStr) {
		try {
			baseURL = new URL("http", addrStr, CommonStarter.NEM_PORT, "/");
			InetAddress.getByName(addrStr); // For verification purposes

			address = addrStr;
			restURLs = new Hashtable<NodeRestIDs, URL>();
			restURLs.put(NodeRestIDs.REST_NODE_INFO, new URL(baseURL, "node/info"));
			restURLs.put(NodeRestIDs.REST_ADD_PEER, new URL(baseURL, "peer/new"));
			restURLs.put(NodeRestIDs.REST_NODE_PEER_LIST, new URL(baseURL, "node/peerlist"));
			restURLs.put(NodeRestIDs.REST_CHAIN, new URL(baseURL, "chain"));

		} catch (MalformedURLException e) {
			logger.warning("Peer address not valid: <" + addrStr + ">");
			throw new IllegalArgumentException("Peer address not valid: <" + addrStr + ">");
		} catch (UnknownHostException e) {
			logger.warning("Peer address not valid: <" + addrStr + ">");
			throw new IllegalArgumentException("Peer address unknown: <" + addrStr + ">");
		}
	}

	public String getAddress() {
		return address;
	}

	public String getVersion() {
		return version;
	}

	public Integer getProtocol() {
		return protocol;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String myPlatform) {
		this.platform = myPlatform;
	}

	public NodeStatus getState() {
		return state;
	}

	public void setState(NodeStatus status) {
		logger.info(toString() + " changed to " + status.toString());
		state = status;
	}

	public String toString() {
		StringBuilder strB = new StringBuilder();
		strB.append("Node ").append(address.toString());
		strB.append(" [").append(state).append("]");

		return strB.toString();
	}

	/**
	 * Serializes this object.
	 * 
	 * @param serializer
	 *            The serializer to use.
	 */
	public void serialize(final Serializer serializer) {
		serializer.writeInt("protocol", protocol);
		serializer.writeString("application", CommonStarter.APP_NAME);
		serializer.writeString("version", version);
		serializer.writeString("platform", platform);
		serializer.writeString("address", address);
	}

	/**
	 * Short cut for creating JsonSerializer and serialize this.
	 * 
	 */
	public JSONObject asJsonObject() {
		JsonSerializer serializer = new JsonSerializer();
		serialize(serializer);
		return serializer.getObject();
    }

	//
	public boolean verifyNEM() {
		boolean result = false;

		// We verify by getting the Information from the peer
		try {
			PeerConnector connector = new PeerConnector();

			JSONObject response = connector.requestNodeInfo(this);

			result = true;
			// Let's check
			Object value = response.get("application");
			if (!CommonStarter.APP_NAME.equals(value)) {
				logger.warning("Returned application <" + value + "> differs from requested: <" + CommonStarter.APP_NAME + ">. No NEM node.");
				result = false;
			}

			value = response.get("address"); // null value is also not allowed!
			if (value == null) {
				logger.warning("Returned address is empty. No NEM node.");
				result = false;
			} else {
				// What if address is different to initial address?
				if (!address.equals(value)) {
					// At least we protocol it
					logger.warning("Returned address <" + value + "> differs from requested: <" + address + ">. Keeping requested.");
				}
			}

			value = response.get("protocol"); // null value is also not allowed!
			if (value == null) {
				logger.warning("Returned protocol is empty. No NEM node.");
				result = false;
			} else {
				try {
					protocol = (Integer) value;
				} catch (ClassCastException e) {
					logger.warning("Returned protocol <" + value + "> not a valid integer. No NEM node.");
					result = false;
				}
			}

			value = response.get("version"); // null value is also not allowed!
			if (value == null) {
				logger.warning("Returned version is empty. No NEM node.");
				result = false;
			} else {
				version = (String) value;
			}

			// Okay we are connected with the peer node
			startTime = System.currentTimeMillis();

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

		}

		return result;
	}

	public URL getRestURL(NodeRestIDs restID) {
		return restURLs.get(restID);
	}

}
