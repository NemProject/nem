package org.nem.nis.controller;

import net.minidev.json.JSONObject;

public class Utils {
	public static String jsonError(int num, String errorMessage) {
		JSONObject obj = new JSONObject();
		obj.put("error", new Integer(num));
		obj.put("reason", errorMessage);
		return obj.toJSONString() + "\r\n";
	}

	public static String jsonOk() {
		JSONObject obj = new JSONObject();
		obj.put("ok", 42);
		return obj.toJSONString() + "\r\n";
	}
}
