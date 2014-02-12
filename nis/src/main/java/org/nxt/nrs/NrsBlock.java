package org.nxt.nrs;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.minidev.json.JSONObject;

public class NrsBlock implements Serializable {
	static final long serialVersionUID = 0;

	public static NrsBlock getBlock(JSONObject blockData) {

		int version = ((Integer)blockData.get("version")).intValue();
		int timestamp = ((Integer)blockData.get("timestamp")).intValue();
		long previousBlock = (new BigInteger((String)blockData.get("previousBlock"))).longValue();
		int numberOfTransactions = ((Integer)blockData.get("numberOfTransactions")).intValue();
		int totalAmount = ((Integer)blockData.get("totalAmount")).intValue();
		int totalFee = ((Integer)blockData.get("totalFee")).intValue();
		int payloadLength = ((Integer)blockData.get("payloadLength")).intValue();
		byte[] payloadHash = Utils.hs2b((String)blockData.get("payloadHash"));
		byte[] generatorPublicKey = Utils.hs2b((String)blockData.get("generatorPublicKey"));
		byte[] generationSignature = Utils.hs2b((String)blockData.get("generationSignature"));
		byte[] blockSignature = Utils.hs2b((String)blockData.get("blockSignature"));
		byte previousBlockHash[] = version != 1 ? Utils.hs2b((String)blockData.get("previousBlockHash")) : null;
		if(numberOfTransactions > 255 || payloadLength > 32640)
			return null;
		return new NrsBlock(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
	}

	NrsBlock(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature) {
		this(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, null);
	}
	
	NrsBlock(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte previousBlockHash[]) {
		if(numberOfTransactions > 255 || numberOfTransactions < 0) {
			throw new IllegalArgumentException((new StringBuilder()).append("attempted to create a block with ").append(numberOfTransactions).append(" transactions").toString());
		}
		if(payloadLength > 32640 || payloadLength < 0) {
			throw new IllegalArgumentException((new StringBuilder()).append("attempted to create a block with payloadLength ").append(payloadLength).toString());
		}
		
		this.version = version;
		this.timestamp = timestamp;
		this.previousBlock = previousBlock;
		this.numberOfTransactions = numberOfTransactions;
		this.totalAmount = totalAmount;
		this.totalFee = totalFee;
		this.payloadLength = payloadLength;
		this.payloadHash = payloadHash;
		this.generatorPublicKey = generatorPublicKey;
		this.generationSignature = generationSignature;
		this.blockSignature = blockSignature;

		this.previousBlockHash = previousBlockHash;
	}
	
	byte[] getBytes()
    {
        ByteBuffer buffer = ByteBuffer.allocate(224);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.putInt(timestamp);
        buffer.putLong(previousBlock);
        buffer.putInt(numberOfTransactions);
        buffer.putInt(totalAmount);
        buffer.putInt(totalFee);
        buffer.putInt(getPayloadLength());
        buffer.put(payloadHash);
        buffer.put(generatorPublicKey);
        buffer.put(generationSignature);
        if(version > 1)
            buffer.put(previousBlockHash);
        buffer.put(blockSignature);
        return buffer.array();
    }

	
	public long getId() {
		byte[] h;
		try {
			h = MessageDigest.getInstance("SHA-256").digest(getBytes());
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return 0;
		}
		BigInteger bigInteger = new BigInteger(1, new byte[] {h[7], h[6], h[5], h[4], h[3], h[2], h[1], h[0]});
		return bigInteger.longValue();

	}

	public int getVersion() {
		return version;
	}
	public int getTimestamp() {
		return timestamp;
	}
	public long getPreviousBlock() {
		return previousBlock;
	}
	public int getTotalAmount() {
		return totalAmount;
	}
	public int getTotalFee() {
		return totalFee;
	}

	public int getPayloadLength() {
		return payloadLength;
	}

	public byte[] getGenerationSignature() {
		return generationSignature;
	}
	public byte[] getBlockSignature() {
		return blockSignature;
	}
	public byte[] getGeneratorPublicKey() {
		return generatorPublicKey;
	}

	public int getNumberOfTransactions() {
		return numberOfTransactions;
	}

	private int version;
	private int timestamp;
	private long previousBlock;
	private int totalAmount;
	private int totalFee;
	private int payloadLength;
	byte[] payloadHash;
	private byte[] generatorPublicKey;
	private byte[] generationSignature;
	private byte[] blockSignature;
	private byte[] previousBlockHash;
	private int numberOfTransactions;

	int index;
	long[] transactions;
	long baseTarget;
	int height;
	long nextBlock;
	BigInteger cumulativeDifficulty;
	long prevBlockPtr;
}
