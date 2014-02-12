package org.nem.nis.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashes {
	static public byte[] sha1(byte[] data) {
		byte[] digest = null;
		try {
			digest = MessageDigest.getInstance("SHA1").digest(data);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return digest;
	}
	
	static public byte[] sha256(byte[] data) {
		byte[] digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256").digest(data);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return digest;
	}
	
	static public byte[] sha512(byte[] data) {
		byte[] digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-512").digest(data);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return digest;
	}
}
