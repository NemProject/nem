package org.nem.core.model;

/**
 * Represents information about a current network.
 */
public class NetworkInfo {

	private static NetworkInfo mainNetworkInfo = createMainNetworkInfo();
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
	public static NetworkInfo getMainNetworkInfo() {
		return mainNetworkInfo;
	}

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

	private static NetworkInfo createMainNetworkInfo() {
		final NetworkInfo info = new NetworkInfo();
		info.version = 0x68;
		info.addressStartChar = 'N';
		// TODO: change to real addresses before changing to main-net
		info.genesisAccountId = "Not-a-real-address-0";//"NBERUJIKSAPW54YISFOJZ2PLG3E7CACCNN2Z6SOW";
		info.genesisRecipientAccountIds = new String[] {
				"Not-a-real-address-1", // "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI",
				"Not-a-real-address-2", // "NCBWD3TSIMFRHV67PQUQPRL5SZ5CEE6MUL2ANOON",
				"Not-a-real-address-3", // "NBI5SUNZOYBHM3D6Q7BOHP6K327EIJ6EETIIRTS2",
				"Not-a-real-address-4", // "NAUULYJ4MSHXON3GDQVUN4WFRTAQNADYL5KYTTX7",
				"Not-a-real-address-5", // "NBT7M43C4X25VDNSL34IRQO5IRKO6WXSMSJ4PCFP",
				"Not-a-real-address-6", // "NAXNGGK5JEU7EXXFLV4L2NCGNJAWBGEOPEI4XHUN",
				"Not-a-real-address-7", // "NCVRRAC4GIGMY5BIHDQZO3K6HLAJIDKYZDF7RO5H",
				"Not-a-real-address-8", // "NBMSVDI52MR3KSO7RGIJEGGMGZAGSKV4A3ZNJJSM"
		};

		return info;
	}

	private static NetworkInfo createTestNetworkInfo() {
		final NetworkInfo info = new NetworkInfo();
		info.version = (byte)0x98;
		info.addressStartChar = 'T';
		info.genesisAccountId = "TBERUJIKSAPW54YISFOJZ2PLG3E7CACCNP3PP3P6";
		info.genesisRecipientAccountIds = new String[] {
				"TbloodZW6W4DUVL4NGAQXHZXFQJLNHPDXHULLHZW",
				"TAthiesMY6QO6XKPCBZFEVVVFVL2UT3ESDHAVGL7",
				"TDmakotEWZNTXYDSCYKAVGRHFSE6K33BSUATKQBT",
				"TDpatemA4HXS7D44AQNT6VH3AHKDSNVC3MYROLEZ",
				"TBgimreUQQ5ZQX6C3IGLBSVPMROHCMPEIHY4GV2L",
				"TDIUWEjaguaWGXI56V5MO7GJAQGHJXE2IZXEK6S5",
				"TCZloitrAOV4F5J2H2ACC4KXHHTKLQHN3G7HV4B4",
				"TDHDSTFY757SELOAE3FU7U7krystoP6FFB7XXSYH",
				"TD53NLTDK7EMSutopiAK4RSYQ523VBS3C62UMJC5"
		};

		return info;
	}
}
