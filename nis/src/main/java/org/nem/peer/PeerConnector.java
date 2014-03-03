/**
 * 
 */
package org.nem.peer;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.peer.v2.NodeApiId;

/**
 * Access remote nodes and knows about the various REST APIs
 * 
 * Maybe a rework after sync with @Gimre is required.
 * 
 * @author Thies1965
 * 
 */
public class PeerConnector {
	private static final Logger LOGGER = Logger.getLogger(PeerNetwork.class.getName());

	private HttpClient httpClient;

	public PeerConnector() {
		httpClient = new HttpClient();
		httpClient.setFollowRedirects(false);
		try {
			httpClient.start();
		} catch (Exception e1) {
			LOGGER.log(Level.SEVERE, "HTTP CLient could not be started.", e1);
		}
	}

	//TODO: Add Interceptor pattern
	public JSONObject requestNodeInfo(Node node) throws URISyntaxException, InterruptedException, TimeoutException, ExecutionException {
		JSONObject response = getResponse(node.getRestURL(NodeApiId.REST_NODE_INFO));
		return response;
	}

	//TODO: Add Interceptor pattern
	public JSONObject requestPeerList(Node node) throws URISyntaxException, InterruptedException, TimeoutException, ExecutionException {
		JSONObject response = getResponse(node.getRestURL(NodeApiId.REST_NODE_PEER_LIST));
		return response;
	}

	//TODO: Add Interceptor pattern
	public JSONObject postNewPeer(Node node, Node peer) throws URISyntaxException, InterruptedException, TimeoutException, ExecutionException {
		JSONObject response = postResponse(node.getRestURL(NodeApiId.REST_ADD_PEER), peer.asJsonObject());
		return response;
	}

	private JSONObject getResponse(URL url) throws URISyntaxException, InterruptedException, TimeoutException, ExecutionException {
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

//	private JSONObject putResponse(URL url, JSONObject request) throws URISyntaxException, InterruptedException, TimeoutException,
//			ExecutionException {
//		JSONObject retObj = null;
//		try {
//			InputStreamResponseListener listener = new InputStreamResponseListener();
//
//			URI uri = url.toURI();
//			Request req = httpClient.newRequest(uri);
//
//			req.method(HttpMethod.PUT);
//			req.content(new BytesContentProvider(request.toString().getBytes()), "text/plain");
//			req.send(listener);
//
//			Response res = listener.get(30, TimeUnit.SECONDS);
//			if (res.getStatus() == 200) {
//				InputStream responseContent = listener.getInputStream();
//				retObj = new JSONObject(responseContent);
//			}
//
//		} finally {
//		}
//
//		return retObj;
//	}
//
	private JSONObject postResponse(URL url, JSONObject request) throws URISyntaxException, InterruptedException, TimeoutException,
			ExecutionException {
		JSONObject retObj = null;
		try {
			InputStreamResponseListener listener = new InputStreamResponseListener();

			URI uri = url.toURI();
			Request req = httpClient.newRequest(uri);

			req.method(HttpMethod.POST);
			req.content(new BytesContentProvider(request.toString().getBytes()), "text/plain");
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
}
