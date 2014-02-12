package org.nem.nis;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class NxtRequests {

	public static JSONObject makeRequest(JSONObject request) {
		HttpClient httpClient = new HttpClient();
		httpClient.setFollowRedirects(false);
		try {
			httpClient.start();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println("started");
		JSONObject par = null;
		try {
			InputStreamResponseListener listener = new InputStreamResponseListener();
			
			Request req = httpClient.newRequest("http://127.0.0.1:7874/nxt");
			req.method(HttpMethod.POST);
			req.content(new BytesContentProvider(request.toJSONString().getBytes()), "text/plain");
			req.send(listener);
			
			Response res = listener.get(30, TimeUnit.SECONDS);
			if (res.getStatus() == 200) {
				InputStream responseContent = listener.getInputStream();
				
				System.out.print(res.getStatus());
    			System.out.print(" ");
    			System.out.println(res.getReason());
    			par = (JSONObject) JSONValue.parse(responseContent);
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return par;
	}
	
	public static JSONObject getInfo() {
		JSONObject obj=new JSONObject();
		obj.put("protocol",new Integer(1));
		obj.put("scheme","http");
		obj.put("application","NIS");
		obj.put("version","0.1.0");
		obj.put("platform", "PC x64");
		obj.put("port","7676");
		obj.put("shareAddress", new Boolean(false));
		obj.put("requestType","getInfo");
		return obj;
	}

	public static JSONObject getPeers() {
		JSONObject obj=new JSONObject();
		obj.put("protocol",new Integer(1));
		obj.put("requestType","getPeers");
		return obj;
	}
	
	public static JSONObject getCumulativeDifficulty() {
		JSONObject obj=new JSONObject();
		obj.put("protocol",new Integer(1));
		obj.put("requestType","getCumulativeDifficulty");
		return obj;
	}

	public static JSONObject getMilestoneBlockIds() {
		JSONObject obj=new JSONObject();
		obj.put("protocol",new Integer(1));
		obj.put("requestType","getMilestoneBlockIds");
		return obj;
	}

	public static JSONObject getNextBlockIds(long commonBlockId) {
		JSONObject obj=new JSONObject();
		obj.put("protocol",new Integer(1));
		obj.put("blockId", NxtRequests.longToUnsigedString(commonBlockId));
		obj.put("requestType","getNextBlockIds");
		return obj;
	}
	
	public static JSONObject getNextBlocks(long prevBlockId) {
		JSONObject obj=new JSONObject();
		obj.put("protocol",new Integer(1));
		obj.put("blockId", NxtRequests.longToUnsigedString(prevBlockId));
		obj.put("requestType","getNextBlocks");
		return obj;
	}
	
	private static final BigInteger Two_Pow_64 = new BigInteger("18446744073709551616");
	private static String longToUnsigedString(long value) {
		BigInteger bi = BigInteger.valueOf(value);
		if (bi.compareTo(BigInteger.ZERO) < 0) {
			bi.add(Two_Pow_64);
		}
		return bi.toString();
	}
}
