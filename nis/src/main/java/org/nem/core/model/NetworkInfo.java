package org.nem.core.model;

/**
 * Represents information about a current network.
 */
public class NetworkInfo {

//	private static NetworkInfo mainNetworkInfo = createMainNetworkInfo();
	private static NetworkInfo testNetworkInfo = createTestNetworkInfo();

	private byte version;
	private char addressStartChar;
	private String genesisAccountId;
	private String[] genesisRecipientAccountIds;

	/**
	 * Gets the network version.
	 *
	 * @return The network version.
	 */
	public byte getVersion() {
		return this.version;
	}

	/**
	 * Gets the character with which all network addresses should start.
	 *
	 * @return The character with which all network addresses should start.
	 */
	public char getAddressStartChar() {
		return this.addressStartChar;
	}

	/**
	 * Gets the network genesis account.
	 *
	 * @return The network genesis account.
	 */
	public String getGenesisAccountId() {
		return this.genesisAccountId;
	}

	/**
	 * Gets the network genesis recipient account ids.
	 *
	 * @return The network genesis recipient account ids.
	 */
	public String[] getGenesisRecipientAccountIds() {
		return this.genesisRecipientAccountIds;
	}

	/**
	 * Gets information about the MAIN network.
	 *
	 * @return Information about the MAIN network.
	 */
//	public static NetworkInfo getMainNetworkInfo() {
//		return mainNetworkInfo;
//	}

	/**
	 * Gets information about the TEST network.
	 *
	 * @return Information about the TEST network.
	 */
	public static NetworkInfo getTestNetworkInfo() {
		return testNetworkInfo;
	}

	/**
	 * Gets information about the DEFAULT network.
	 *
	 * @return Information about the DEFAULT network.
	 */
	public static NetworkInfo getDefault() {
		return getTestNetworkInfo();
	}

//	private static NetworkInfo createMainNetworkInfo() {
//		final NetworkInfo info = new NetworkInfo();
//		info.version = 0x68;
//		info.addressStartChar = 'N';
//		info.genesisAccountId = "NBERUJIKSAPW54YISFOJZ2PLG3E7CACCNN2Z6SOW";
//		info.genesisRecipientAccountIds = new String[] {
//				"NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI",
//				"NCBWD3TSIMFRHV67PQUQPRL5SZ5CEE6MUL2ANOON",
//				"NBI5SUNZOYBHM3D6Q7BOHP6K327EIJ6EETIIRTS2",
//				"NAUULYJ4MSHXON3GDQVUN4WFRTAQNADYL5KYTTX7",
//				"NBT7M43C4X25VDNSL34IRQO5IRKO6WXSMSJ4PCFP",
//				"NAXNGGK5JEU7EXXFLV4L2NCGNJAWBGEOPEI4XHUN",
//				"NCVRRAC4GIGMY5BIHDQZO3K6HLAJIDKYZDF7RO5H",
//				"NBMSVDI52MR3KSO7RGIJEGGMGZAGSKV4A3ZNJJSM"
//		};
//
//		return info;
//	}

	private static NetworkInfo createTestNetworkInfo() {
		final NetworkInfo info = new NetworkInfo();
		info.version = (byte)0x98;
		info.addressStartChar = 'T';
		info.genesisAccountId = "TBERUJIKSAPW54YISFOJZ2PLG3E7CACCNP3PP3P6";
		info.genesisRecipientAccountIds = new String[] {
				"TBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6VYUBA363",
				"TCBWD3TSIMFRHV67PQUQPRL5SZ5CEE6MUKYZCGN6",
				"TBI5SUNZOYBHM3D6Q7BOHP6K327EIJ6EESOPNFUP",
				"TAUULYJ4MSHXON3GDQVUN4WFRTAQNADYL7FQPZDR",
				"TBT7M43C4X25VDNSL34IRQO5IRKO6WXSMTNVYQWZ",
				"TAXNGGK5JEU7EXXFLV4L2NCGNJAWBGEOPHA36MIM",
				"TCVRRAC4GIGMY5BIHDQZO3K6HLAJIDKYZDYQZMAX",
				"TBMSVDI52MR3KSO7RGIJEGGMGZAGSKV4AYXF56OJ"
		};

		return info;
	}
}
