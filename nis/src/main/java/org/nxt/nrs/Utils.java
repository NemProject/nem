package org.nxt.nrs;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
	
	// public key 2 id
	public static long pk2Id(byte[] publicKey) {
		byte[] h;
		try {
			h = MessageDigest.getInstance("SHA-256").digest(publicKey);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return 0;
		}
		BigInteger bigInteger = new BigInteger(1, new byte[] {h[7], h[6], h[5], h[4], h[3], h[2], h[1], h[0]});
		return bigInteger.longValue();
	}
	
	// hex string 2 bytes
	public static byte[] hs2b(String string) {

		byte[] bytes = new byte[string.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)Integer.parseInt(string.substring(i * 2, i * 2 + 2), 16);

		}
		return bytes;
	}

}
