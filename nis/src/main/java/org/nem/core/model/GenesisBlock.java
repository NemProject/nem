package org.nem.core.model;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.crypto.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;

/**
 * Represents the genesis block.
 */
public class GenesisBlock extends Block {

	/**
	 * The genesis account.
	 */
	public final static Account ACCOUNT;

	/**
	 * The amount of NEM in the genesis block.
	 */
	public final static Amount AMOUNT = Amount.fromNem(4000000000L);

//	// this will be removed later, only public key will be present in the code
//	// all signatures will be pre-generated and placed in-code
//	private final static PrivateKey CREATOR_PRIVATE_KEY = PrivateKey.fromHexString(
//			"aa761e0715669beb77f71de0ce3c29b792e8eb3130d21f697f59070665100c04");

	private final static PublicKey CREATOR_PUBLIC_KEY = new PublicKey(
			HexEncoder.getBytes("03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c")
	);
	//private final static Hash GENESIS_GENERATION_HASH = new Hash(Hashes.sha3("If ever I to the moment shall say: Beautiful moment, do not pass away! Then you may forge your chains to bind me,".getBytes()));
	private final static Hash GENESIS_GENERATION_HASH = new Hash(
			HexEncoder.getBytes("c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8")
	);
	private final static BlockHeight GENESIS_HEIGHT = BlockHeight.ONE;

	static {
		final KeyPair genesisKeyPair = new KeyPair(CREATOR_PUBLIC_KEY);
		ACCOUNT = new Account(genesisKeyPair);
	}

	/**
	 * Creates a genesis block.
	 *
	 * @param timestamp The block timestamp.
	 */
	public GenesisBlock(final TimeInstant timestamp) {
		/*
		super(ACCOUNT, Hash.ZERO, GENESIS_GENERATION_HASH, timestamp, GENESIS_HEIGHT);

		// TODO: as a placeholder distribute amounts equally
		final String[] recipientIds = NetworkInfo.getDefault().getGenesisRecipientAccountIds();
		final Amount shareAmount = new Amount(AMOUNT.getNumMicroNem() / recipientIds.length);
		final Amount lastAmount = AMOUNT.subtract(Amount.fromMicroNem((recipientIds.length-1)*shareAmount.getNumMicroNem()));
		int index = 1;
		for (final String id : recipientIds) {
			final Address address = Address.fromEncoded(id.toUpperCase());
			final Account account = new Account(address);
			final TransferTransaction transaction = new TransferTransaction(timestamp, ACCOUNT, account, (index == recipientIds.length) ? lastAmount : shareAmount, null);

			transaction.sign();
			this.addTransaction(transaction);

			index++;
		}

		this.sign();

		final JsonSerializer serializer = new JsonSerializer(true);
		this.serialize(serializer);

		System.out.println(serializer.getObject().toString());
		*/

		super(Block.BLOCK_TYPE, DeserializationOptions.VERIFIABLE, createDeserializer());
		this.setPrevious(GENESIS_GENERATION_HASH, Hash.ZERO);
	}

	private static Deserializer createDeserializer() {
		final Deserializer deserializer = new JsonDeserializer(createJsonObject(), new DeserializationContext((new AccountAnalyzer(null)).asAutoCache()));
		int type = deserializer.readInt("type");
		if (type != Block.BLOCK_TYPE) {
			throw new IllegalArgumentException("Unknown block type: " + type);
		}
		return deserializer;
	}

	private static JSONObject createJsonObject() {
		final String jsonGenesisBlock = "{\"signature\":\"XK+tZaVlEXVkyuW+paNZ7hx9oTGL0jeyUvDTzPfEqReF9SAVyzIj+C5UqkR\\/YHOwxDDe3nfPI0IkX6aBQ4G9dw==\",\"totalFee\":0,\"prevBlockHash\":{\"data\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\"},\"type\":1,\"transactions\":[{\"amount\":444444444444444,\"signature\":\"L2gGr5ywclORvTqc+HySdjyYtBASboWdfHF6y3tfyp\\/F0fQJuIjX6GdlDX6PfdRpo15EYGJs8tq9px8MxjrrFg==\",\"fee\":0,\"recipient\":\"TBLOODZW6W4DUVL4NGAQXHZXFQJLNHPDXHULLHZW\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"},{\"amount\":444444444444444,\"signature\":\"Dt8kPHNiclsvxJNsd9hUmcG+ahoeJ4CtJUlIQwmbQTWaZp4wQ+uiv6YUF9UBxxpGCdRRMKiDhhDV+6Blcd2Yfg==\",\"fee\":0,\"recipient\":\"TATHIESMY6QO6XKPCBZFEVVVFVL2UT3ESDHAVGL7\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"},{\"amount\":444444444444444,\"signature\":\"ECJ7e+qAf7DS8YVicYGXQe1Knr1pW38P1L3Cg\\/MCaErcxQgVP8ByGKBnLOjALE+\\/g97Bg\\/ye2pszSW0hxXmuGQ==\",\"fee\":0,\"recipient\":\"TDMAKOTEWZNTXYDSCYKAVGRHFSE6K33BSUATKQBT\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"},{\"amount\":444444444444444,\"signature\":\"HWIRQami9bYmzEYVCx0sPUoXRzr6MKujAFwSN1y+E\\/LSp0T1xBDrhGJWVZDbqjiZ5qOtmvhDKutzeLW19VSUeQ==\",\"fee\":0,\"recipient\":\"TDPATEMA4HXS7D44AQNT6VH3AHKDSNVC3MYROLEZ\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"},{\"amount\":444444444444444,\"signature\":\"tARglTqsRWAFCFpxTJf7C9NLlHoutgoXgYABFQZbggcJJ3X6NDsXcjhucMS57iKPcY45NuDYZ\\/Cu\\/3OWQGNLeg==\",\"fee\":0,\"recipient\":\"TBGIMREUQQ5ZQX6C3IGLBSVPMROHCMPEIHY4GV2L\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"},{\"amount\":444444444444444,\"signature\":\"tqqlek5cItp88wp4Aeo\\/AUdhM+z\\/q87E4HKfO5HncCYTRR8XE\\/dQ9DCsjqz79KwzChSE\\/YEGELsI\\/YVZBTJeMQ==\",\"fee\":0,\"recipient\":\"TDIUWEJAGUAWGXI56V5MO7GJAQGHJXE2IZXEK6S5\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"},{\"amount\":444444444444444,\"signature\":\"c0Qjhbfkx7TDyLPe76tc\\/apHrOEG9xIBtwbmXc\\/oCQxWIUdkKAniWAOmUEakx7Ev8XNPMn569hQheozLplgQdg==\",\"fee\":0,\"recipient\":\"TCZLOITRAOV4F5J2H2ACC4KXHHTKLQHN3G7HV4B4\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"},{\"amount\":444444444444444,\"signature\":\"RrRaM6r0p3DfWxdUBNkEqltxirdaUdB4GqeIVctut2UzFXOVec8YRBa1VUD9djEMd8OiSFOEuyZjiU9lyWmuCQ==\",\"fee\":0,\"recipient\":\"TDHDSTFY757SELOAE3FU7U7KRYSTOP6FFB7XXSYH\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"},{\"amount\":444444444444448,\"signature\":\"I3mhPsOjK6FFEpl+vHQpdLksn3XRA8Aj+T6mPOZWFSckmgvDqEmqiCWHfmFNcOFyYEQMudmmQPznSwpkyOa0PQ==\",\"fee\":0,\"recipient\":\"TD53NLTDK7EMSUTOPIAK4RSYQ523VBS3C62UMJC5\",\"type\":257,\"deadline\":0,\"message\":{},\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\"}],\"version\":1,\"timestamp\":0,\"signer\":\"A9ZxwAKbqBeBvgVwLfYtBdcRG+IiNlfFuIN5TLeE48A8\",\"height\":1,\"_propertyOrderArray\":[\"type\",\"version\",\"timestamp\",\"signer\",\"signature\",\"prevBlockHash\",\"height\",\"totalFee\",\"transactions\"]}\n";
		return (JSONObject)JSONValue.parse(jsonGenesisBlock);
	}
}
