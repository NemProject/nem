/**
 * 
 */
package org.nem.peer;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.spi.JsonProvider;

import org.nem.NEM;
import org.nem.util.NEMLogger;

/**
 * Reflects a node within a peer network A node is classified by its URI.
 * Connection to peers are always done via REST connections. The node collects
 * some statistics like connection time, whether connected, error requests /
 * responses
 * 
 * @author Thies1965
 * 
 */
public class Node implements Serializable {
	private static final long serialVersionUID = -5710930110703963436L;

	private URL address;
	private NodeStatus state;
	private String myPlatform;
	private String myAddress;
	private int myPort;

	// The resources and their URI
	private URL nodeURL;
	private URL networkURL;
	private URL chainURL;

	private long startTime;
	private long totalTime;
	private long successfulCalls;
	private long failedCalls;

	public Node(String addrStr, int port) {
		super();

		if (addrStr == null) {
			throw new IllegalArgumentException("Node requires an address. Null as address is not supported.");
		}

		addrStr = addrStr.trim();
		if (addrStr.length() == 0) {
			throw new IllegalArgumentException("Node requires an address. An empty address is not supported.");

		}

		try {
			address = new URL("http", addrStr, NEM.NEM_PORT, NEM.APP_CONTEXT);
			nodeURL = new URL(address, "node");
			networkURL = new URL(address, "network");
			chainURL = new URL(address, "chain");
		} catch (MalformedURLException e) {
			NEMLogger.LOG.warning("Peer address not valid: <" + address.toString() + ">");

			throw new IllegalArgumentException("Peer address not valid: <" + address.toString() + ">");
		}

		// Hope all addressing issues are identified,
		// so the instance is valid.
		state = NodeStatus.INACTIVE;

	}

	public String getMyPlatform() {
		return myPlatform;
	}

	public void setMyPlatform(String myPlatform) {
		this.myPlatform = myPlatform;
	}

	public String getMyAddress() {
		return myAddress;
	}

	public void setMyAddress(String myAddress) {
		this.myAddress = myAddress;
	}

	public int getMyPort() {
		return myPort;
	}

	public void setMyPort(int myPort) {
		this.myPort = myPort;
	}

	// TODO: Does this belong to this class???
	public boolean verifyNEM() {
		boolean result = false;

		// We verify by getting the Information from the peer
		try {
			JsonObject response = getNodeInfo();
			response.isEmpty();

			// Okay we are connected with the peer node
			startTime = System.currentTimeMillis();

			result = true;
		} catch (MalformedURLException e) {
			// Connection error, so we do not consider the node for the next
			// time
			NEMLogger.LOG.log(Level.WARNING, toString(), e);

			// set to not connected
			setState(NodeStatus.FAILURE);
		} catch (SocketTimeoutException e) {
			NEMLogger.LOG.log(Level.WARNING, toString() + " timed out.");

			// set to not connected
			setState(NodeStatus.INACTIVE);
		} catch (UnknownHostException e) {
			// Connection error, so we do not consider the node for the next
			// time
			NEMLogger.LOG.log(Level.WARNING, toString() + " unknown host.");

			// set to not connected
			setState(NodeStatus.FAILURE);
		} catch (IOException e) {
			// Connection error, so we do not consider the node for the next
			// time
			NEMLogger.LOG.log(Level.WARNING, toString(), e);

			// set to not connected
			setState(NodeStatus.INACTIVE);
		}

		return result;
	}

	public NodeStatus getState() {
		return state;
	}

	public void setState(NodeStatus status) {
		NEMLogger.LOG.log(Level.INFO, toString() + " changed to " + status.toString());
		state = status;
	}

	private JsonObject getResponse(URL url) throws MalformedURLException, IOException {
		// Now request verification via defined protocol
		HttpURLConnection connection = null;
		JsonReader reader = null;
		JsonObject response = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setConnectTimeout(1000); // TODO: configuration
			connection.setReadTimeout(1000); // TODO: configuration

			JsonProvider provider = JsonProvider.provider();
			reader = provider.createReader(connection.getInputStream());
			response = reader.readObject();
			reader.close();
		} finally {
			// Do housekeeping
			if (reader != null) {
				reader.close();
			}
		}

		return response;

	}

	private JsonObject putResponse(URL url, JsonObject nodeInfo) throws MalformedURLException, IOException {
		// Now request verification via defined protocol
		HttpURLConnection connection = null;
		JsonWriter writer = null;
		JsonReader reader = null;
		JsonObject response = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			connection.setConnectTimeout(1000); // TODO: configuration
			connection.setReadTimeout(1000); // TODO: configuration

			JsonProvider provider = JsonProvider.provider();
			writer = provider.createWriter(connection.getOutputStream());
			writer.write(nodeInfo);
			reader = provider.createReader(connection.getInputStream());
			response = reader.readObject();
		} finally {
			// Do housekeeping
			if (writer != null) {
				writer.close();
			}
			if (reader != null) {
				reader.close();
			}
		}

		return response;

	}

	public String toString() {
		StringBuilder strB = new StringBuilder();
		strB.append("Node ").append(address.toString());
		strB.append(" [").append(state).append("]");

		return strB.toString();
	}

	public JsonObject getNodeInfo() throws MalformedURLException, IOException {
		JsonObject response = getResponse(nodeURL);

		return response;
	}

	public JsonObject extendNetworkBy(Node node) throws MalformedURLException, IOException {
		JsonProvider provider = JsonProvider.provider();
		JsonObjectBuilder builder = provider.createObjectBuilder();
		builder.add("application", NEM.APP_NAME);
		builder.add("version", NEM.VERSION);
		builder.add("platform", node.getMyPlatform());
		builder.add("address", node.getMyAddress());
		JsonObject response = null;

		response = putResponse(networkURL, builder.build());

		return response;
	}

}
