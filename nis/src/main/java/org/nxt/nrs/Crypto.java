package org.nxt.nrs;

import java.security.MessageDigest;
import java.util.Arrays;

public class Crypto {

	static public byte[] getPublicKey(byte[] privateKey) {
		try {
			byte[] publicKey = new byte[32];
			Curve25519.keygen(publicKey, null, privateKey);

			return publicKey;

		} catch (Exception e) {
			return null;
		}
	}

	static public byte[] sign(byte[] message, String secretPhrase) {
		try {
			byte[] P = new byte[32];
			byte[] s = new byte[32];
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			Curve25519.keygen(P, s, digest.digest(secretPhrase.getBytes("UTF-8")));

			byte[] m = digest.digest(message);

			digest.update(m);
			byte[] x = digest.digest(s);

			byte[] Y = new byte[32];
			Curve25519.keygen(Y, null, x);

			digest.update(m);
			byte[] h = digest.digest(Y);

			byte[] v = new byte[32];
			Curve25519.sign(v, h, x, s);

			byte[] signature = new byte[64];
			System.arraycopy(v, 0, signature, 0, 32);
			System.arraycopy(h, 0, signature, 32, 32);

			return signature;

		} catch (Exception e) {
			return null;
		}
	}

	static public boolean verify(byte[] signature, byte[] message, byte[] publicKey) {
		try {
			byte[] Y = new byte[32];
			byte[] v = new byte[32];
			System.arraycopy(signature, 0, v, 0, 32);
			byte[] h = new byte[32];
			System.arraycopy(signature, 32, h, 0, 32);
			Curve25519.verify(Y, v, h, publicKey);

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] m = digest.digest(message);
			digest.update(m);
			byte[] h2 = digest.digest(Y);

			return Arrays.equals(h, h2);

		} catch (Exception e) {
			return false;
		}
	}
}