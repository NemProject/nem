package org.nem.nis;

import java.io.UnsupportedEncodingException;

public class Converter {
	public static byte[] stringToBytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String bytesToString(byte[] encoded) {
		return new String(encoded);
	}
}
