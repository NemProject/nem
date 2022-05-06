package org.nem.nis.dao;

import org.nem.core.model.TransactionTypes;
import org.nem.nis.dbmodel.AbstractBlockTransfer;
import org.nem.nis.mappers.TransactionRegistry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class used by the block loader that contains maps of ids to (inner) multisig transactions.
 */
@SuppressWarnings("rawtypes")
public class MultisigTransferMap {
	private final Map<Integer, Entry> typeEntryMap;

	/**
	 * Creates a transfer id map.
	 */
	public MultisigTransferMap() {
		this.typeEntryMap = TransactionRegistry.stream().filter(e -> TransactionTypes.getMultisigEmbeddableTypes().contains(e.type))
				.collect(Collectors.toMap(e -> e.type, e -> new Entry(e.type)));
	}

	/**
	 * Gets the number of entries in the map.
	 *
	 * @return The number of entries in the map.
	 */
	public int size() {
		return this.typeEntryMap.size();
	}

	/**
	 * Gets the entry for the specified type.
	 *
	 * @param type The transfer type.
	 * @return The entry.
	 */
	public Entry getEntry(final int type) {
		return this.typeEntryMap.get(type);
	}

	/**
	 * Gets an entry for a specific transfer type.
	 */
	public static class Entry {
		private final HashMap<Long, AbstractBlockTransfer> idToTransferMap = new HashMap<>();
		private final int type;

		/**
		 * Creates a new entry.
		 *
		 * @param type The transaction type.
		 */
		private Entry(final int type) {
			this.type = type;
		}

		/**
		 * Gets the transaction type.
		 *
		 * @return The type.
		 */
		public int getType() {
			return this.type;
		}

		/**
		 * Gets the transfer with the specified id or null if it cannot be found.
		 *
		 * @param id The id.
		 * @return The transfer or null.
		 */
		public AbstractBlockTransfer getOrDefault(final Long id) {
			return null == id ? null : this.idToTransferMap.get(id);
		}

		/**
		 * Adds the transfer to the map.
		 *
		 * @param transfer The transfer.
		 */
		public void add(final AbstractBlockTransfer transfer) {
			this.idToTransferMap.put(transfer.getId(), transfer);
		}
	}
}
