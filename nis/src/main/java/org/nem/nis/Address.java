package org.nem.nis;

import org.apache.commons.codec.binary.Base32;
import org.nem.nis.crypto.Hashes;

public class Address {
	private byte[] base32Address;
	
	public static byte MAIN_NET = 0x69;
	public static byte TEST_NET = (byte)0x99;
	public static byte VERSION = 0x18;
	
	
	// network -> main = 0x11, testnet 0x66
	// version == 0x01
	
	// this temporarily uses sha1 instead of ripemd
	// and sha256 instead of sha3
	//
	public Address(byte network, byte version, byte[] publicKey) {
		byte hash[] = Hashes.sha1(Hashes.sha256(publicKey));
		byte versioned[] = new byte[hash.length + 2];
		versioned[0] = network;
		versioned[1] = version;
		System.arraycopy(hash, 0, versioned, 2, hash.length);
		
		byte check[] = Hashes.sha256(Hashes.sha256(versioned));
		byte fullHash[] = new byte[versioned.length + 8];
		
		System.arraycopy(versioned, 0, fullHash, 0, versioned.length);
		System.arraycopy(check, 0, fullHash, versioned.length, 8);
		Base32 b = new Base32();
		this.base32Address = b.encode(fullHash);
		
//		System.out.println(String.format(" ==== ==== ====    hexencoded: %040x", new BigInteger(1, fullHash)));
//		
//		System.out.print(" ==== ==== ==== base32Address: ");
//		System.out.println(Converter.bytesToString(base32Address));
    }
	
	public byte[] getBase32Address() {
		return base32Address;
	}
	public void setBase32Address(byte[] base32Address) {
		this.base32Address = base32Address;
	}
}
