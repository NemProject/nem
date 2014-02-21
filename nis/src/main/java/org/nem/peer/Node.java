/**
 * 
 */
package org.nem.peer;

import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.logging.Logger;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.HttpClient;

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
	private static final Logger logger = Logger.getLogger(Node.class.getName());
	
	private URL address;
	private NodeStatus state;
	private String myPlatform;
	private String myAddress;
	private int myPort;

	// The resources and their URI
	private URL nodeInfoURL;
	private URL peerNewURL;
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
			address = new URL("http", addrStr, PeerInitializer.NEM_PORT, "/");
			nodeInfoURL = new URL(address, "node/info"); 
			peerNewURL = new URL(address, "peer/new");
			chainURL = new URL(address, "chain");
			
		} catch (MalformedURLException e) {
			logger.warning("Peer address not valid: <" + address.toString() + ">");
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

	public NodeStatus getState() {
		return state;
	}

	public void setState(NodeStatus status) {
		logger.info(toString() + " changed to " + status.toString());
		state = status;
	}

	private JSONObject getResponse(URL url) throws URISyntaxException, InterruptedException, TimeoutException, ExecutionException {
		HttpClient httpClient = new HttpClient();
		httpClient.setFollowRedirects(false);
		try {
			httpClient.start();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		JSONObject retObj = null;
		
		try {
			InputStreamResponseListener listener = new InputStreamResponseListener();
			
			URI uri = url.toURI();
			Request req = httpClient.newRequest(uri);
			req.method(HttpMethod.GET);
			req.send(listener);
			
			Response res = listener.get(30, TimeUnit.SECONDS);
			if (res.getStatus() == 200) {
				InputStream responseContent = listener.getInputStream();
				retObj = (JSONObject) JSONValue.parse(responseContent);
			}
			
		} finally {

		}
		return retObj;
	}

	private JSONObject putResponse(URL url, JSONObject request) throws URISyntaxException, InterruptedException, TimeoutException, ExecutionException {
		HttpClient httpClient = new HttpClient();
		httpClient.setFollowRedirects(false);
		try {
			httpClient.start();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		JSONObject retObj = null;
		try {
			InputStreamResponseListener listener = new InputStreamResponseListener();
			
			URI uri = url.toURI();
			Request req = httpClient.newRequest(uri);
			
			req.method(HttpMethod.POST);
			req.content(new BytesContentProvider(request.toJSONString().getBytes()), "text/plain");
			req.send(listener);
			
			Response res = listener.get(30, TimeUnit.SECONDS);
			if (res.getStatus() == 200) {
				InputStream responseContent = listener.getInputStream();
				retObj = (JSONObject) JSONValue.parse(responseContent);
			}
			
		} finally {
		}
		
		return retObj;
	}

	public String toString() {
		StringBuilder strB = new StringBuilder();
		strB.append("Node ").append(address.toString());
		strB.append(" [").append(state).append("]");

		return strB.toString();
	}

	public JSONObject getNodeInfo() throws URISyntaxException, InterruptedException, TimeoutException, ExecutionException {
		logger.warning(toString() + "node/info url: " + nodeInfoURL);
		JSONObject response = getResponse(nodeInfoURL);

		logger.warning(toString() + "node/info response: " + response.toString());
		return response;
	}

	public static JSONObject prepareNodeInfo(Node node) {
		JSONObject obj=new JSONObject();
		obj.put("protocol",new Integer(1));
		obj.put("application", PeerInitializer.APP_NAME);
		obj.put("version", PeerInitializer.VERSION);
		obj.put("platform", node.getMyPlatform());
		obj.put("address", node.getMyAddress());
		obj.put("port","7676");
		obj.put("shareAddress", new Boolean(false));
		return obj;
	}
	
	// TODO: Does this belong to this class???
	public boolean verifyNEM() {
		boolean result = false;

		// We verify by getting the Information from the peer
		try {
			JSONObject response = getNodeInfo();
			response.isEmpty();

			// Okay we are connected with the peer node
			startTime = System.currentTimeMillis();

			result = true;
			
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
			
		} catch (ExecutionException e) {
			// Connection error, so we do not consider the node for the next
			// time
			logger.warning(toString() + e.toString());

			// set to not connected
			setState(NodeStatus.FAILURE);
			
		} catch (InterruptedException e) {
			// Connection error, so we do not consider the node for the next
			// time
			logger.warning(toString() + e.toString());

			// set to not connected
			setState(NodeStatus.INACTIVE);
		}

		return result;
	}

	public JSONObject extendNetworkBy(Node node) throws URISyntaxException, InterruptedException, TimeoutException, ExecutionException {
		logger.warning(toString() + "peer/new url: " + peerNewURL);
		JSONObject response = putResponse(peerNewURL, prepareNodeInfo(node));
			
		logger.warning(toString() + "peer/new response: " + response.toString());
		return response;
	}

}
