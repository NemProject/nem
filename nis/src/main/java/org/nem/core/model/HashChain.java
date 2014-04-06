package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.BlockChain;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for storing list of hashes. Wraps List of ByteArray objects.
 */
public class HashChain implements SerializableEntity {
	List<ByteArray> hashChainList;

	/**
	 * Creates new empty HashChain with specified capacity.
	 *
	 * @param initialCapacity the initial capacity of HashChain
	 */
	public HashChain(int initialCapacity) {
		hashChainList = new ArrayList<>(initialCapacity);
	}

	/**
	 * Creates new HashChain and initializes it with passed list of hashes.
	 *
	 * @param rawByteArrayList list of hashes
	 */
	public HashChain(List<byte[]> rawByteArrayList) {
		this(rawByteArrayList.size());
		for (byte[] elem : rawByteArrayList) {
			this.add(elem);
		}
	}

	/**
	 * Deserializes new HashChain.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public HashChain(Deserializer deserializer) {
		hashChainList = deserializer.readObjectArray("data", ByteArrayFactory.deserializer);
	}

	@Override
	public void serialize(Serializer serializer) {
		serializer.writeObjectArray("data", hashChainList);
	}

	/**
	 * add new hash to this HashChain
	 *
	 * @param bytes hash
	 */
	public void add(byte[] bytes) {
		hashChainList.add(new ByteArray(bytes));
	}

	/**
	 * Returns number of hashes in this chain.
	 *
	 * @return
	 */
	public int size() {
		return hashChainList.size();
	}

	private ByteArray get(int i) {
		return this.hashChainList.get(i);
	}

	/**
	 * Compares with another HashChain (might be of different length)
	 * and finds first different element.
	 *
	 * @param other HashChain to compare to.
	 *
	 * @return index of different element.
	 */
	public int findFirstDifferen(HashChain other) {
		int limit = Math.min(this.size(), other.size());
		int i;
		for (i = 0; i < limit; ++i) {
			if (this.get(i) == null) {
				System.out.println("null");
			}
			if (other.get(i) == null) {
				System.out.println("null");
			}
			if (! this.get(i).equals(other.get(i))) {
				break;
			}
		}
		return i;
	}
}
