/**
 * 
 */
package org.nem.peer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

//import javax.json.JsonObject;
//import javax.json.JsonObjectBuilder;
//import javax.json.JsonReader;
//import javax.json.JsonWriter;
//import javax.json.spi.JsonProvider;





import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;




//import org.nem.NEM;
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
			JSONObject response = getNodeInfo();
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

	private JSONObject getResponse(URL url) {
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
				
				NEMLogger.LOG.log(Level.FINE, "server returned: " + res.getStatus() + " " + res.getReason());
    			retObj = (JSONObject) JSONValue.parse(responseContent);
			}
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return retObj;
	}

	private JSONObject putResponse(URL url, JSONObject request) {
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
				
				NEMLogger.LOG.log(Level.FINE, "server returned: " + res.getStatus() + " " + res.getReason());
    			retObj = (JSONObject) JSONValue.parse(responseContent);
			}
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return retObj;
	}

	public String toString() {
		StringBuilder strB = new StringBuilder();
		strB.append("Node ").append(address.toString());
		strB.append(" [").append(state).append("]");

		return strB.toString();
	}

	public JSONObject getNodeInfo() throws MalformedURLException, IOException {
		NEMLogger.LOG.log(Level.WARNING, "node/info url: " + nodeInfoURL);
		JSONObject response = getResponse(nodeInfoURL);

		NEMLogger.LOG.log(Level.WARNING, "node/info response: " + response.toString());
		return response;
	}

	public static JSONObject sendNodeInfo(Node node) {
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
	
	public JSONObject extendNetworkBy(Node node) throws MalformedURLException, IOException {
		NEMLogger.LOG.log(Level.WARNING, "node/info url: " + nodeInfoURL);
		JSONObject response =  putResponse(peerNewURL, sendNodeInfo(node));
		
		NEMLogger.LOG.log(Level.WARNING, "peer/new response: " + response.toString());
		return response;
	}

}
