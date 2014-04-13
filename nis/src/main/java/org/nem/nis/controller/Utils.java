package org.nem.nis.controller;

import net.minidev.json.JSONObject;

public class Utils {
	public static String jsonOk() {
		JSONObject obj = new JSONObject();
		obj.put("ok", 42);
		return obj.toJSONString() + "\r\n";
	}
}
