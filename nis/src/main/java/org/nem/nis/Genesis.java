package org.nem.nis;

import org.nem.core.crypto.Hashes;
import org.nem.core.utils.HexEncoder;
import org.nem.core.utils.StringEncoder;

import java.math.BigInteger;

public class Genesis {
	final static long BLOCK_ID = 0x1234567890abcdefL;

	//final static String CREATOR_PASS = "Remember, remember, the fifth of November, Gunpowder Treason and Plot";
	// final static BigInteger CREATOR_PRIVATE_KEY = new BigInteger( Hashes.sha3(StringEncoder.getBytes(CREATOR_PASS)) );

	// this will be removed later, only public key will be present in the code
	// all signatures will be pre-generated and placed in-code
	final static BigInteger CREATOR_PRIVATE_KEY = new BigInteger(HexEncoder.getBytesSilent("aa761e0715669beb77f71de0ce3c29b792e8eb3130d21f697f59070665100c04"));

//	Hashes.sha3(StringEncoder.getBytes("super-duper-special")),
//	Hashes.sha3(StringEncoder.getBytes("Jaguar0625")),
//	Hashes.sha3(StringEncoder.getBytes("BloodyRookie")),
//	Hashes.sha3(StringEncoder.getBytes("Thies1965")),
//	Hashes.sha3(StringEncoder.getBytes("borzalom")),
//	Hashes.sha3(StringEncoder.getBytes("gimre")),
//	Hashes.sha3(StringEncoder.getBytes("Makoto")),
//	Hashes.sha3(StringEncoder.getBytes("UtopianFuture")),
//	Hashes.sha3(StringEncoder.getBytes("minusbalancer"))
	final static String[] RECIPIENT_IDS = {
			"NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI",
			"NCBWD3TSIMFRHV67PQUQPRL5SZ5CEE6MUL2ANOON",
			"NBI5SUNZOYBHM3D6Q7BOHP6K327EIJ6EETIIRTS2",
			"NAUULYJ4MSHXON3GDQVUN4WFRTAQNADYL5KYTTX7",
			"NBT7M43C4X25VDNSL34IRQO5IRKO6WXSMSJ4PCFP",
			"NAXNGGK5JEU7EXXFLV4L2NCGNJAWBGEOPEI4XHUN",
			"NCVRRAC4GIGMY5BIHDQZO3K6HLAJIDKYZDF7RO5H",
			"NBMSVDI52MR3KSO7RGIJEGGMGZAGSKV4A3ZNJJSM",
			"NBZUVLKB7THC5QH5IJUJVEF66QJZUCQLMVTIFXUC"
	};
	public static final String CREATOR_ACCOUNT_ID = "NBERUJIKSAPW54YISFOJZ2PLG3E7CACCNN2Z6SOW";
}
