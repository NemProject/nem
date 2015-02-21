package org.nem.nis.poi.graph;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.BlockFileLoader;
import org.nem.core.math.SparseBitmap;

/**
 * A repository class for loading the transactions from the Bitcoin blockchain.
 */
public class BtcDatabaseRepository implements DatabaseRepository {
	private static final Logger LOGGER = Logger.getLogger(BtcDatabaseRepository.class.getName());

	private final String databasePath;
	private static final Map<String, Collection<GraphClusteringTransaction>> transactionCache = new HashMap<>();

	/**
	 * Creates a new BTC repository.
	 */
	public BtcDatabaseRepository() {
		this(System.getProperty("user.home") + "/nem/btc_db");
	}

	/**
	 * Creates a new BTC repository.
	 *
	 * @param databasePath The database path.
	 */
	public BtcDatabaseRepository(final String databasePath) {
		this.databasePath = databasePath;
	}

	/**
	 * Loads all transactions from blocks with heights between startHeight and stopHeight, inclusive.
	 *
	 * @param startHeight The start height.
	 * @param stopHeight The stop height.
	 * @return The transactions.
	 */
	public Collection<GraphClusteringTransaction> loadTransactionData(final long startHeight, final long stopHeight) {
		LOGGER.info(String.format("loading transactions in blocks [%d, %d]...", startHeight, stopHeight));

		final String transCacheKey = startHeight + "_" + stopHeight;
		if (this.transactionCache.containsKey(transCacheKey)) {
			return this.transactionCache.get(transCacheKey);
		}

		final List<GraphClusteringTransaction> transactionData = new ArrayList<>();

		final NetworkParameters networkParams = new MainNetParams();
		final List<File> blockChainFiles = new ArrayList<>();
		FileUtils.listFiles(new File(this.databasePath), new String[]{ "dat" }, false)
				.stream()
				.forEach(
						i -> {
							blockChainFiles.add(i);
						});

		final BlockFileLoader blockFileLoader = new BlockFileLoader(networkParams, blockChainFiles);
		final List<BTCTransaction> transactions = new ArrayList<>();
		final HashMap<String, Integer> uniqueAddresses = new HashMap<>();
		final List<SparseBitmap> inputsToMerge = new ArrayList<>();

		// Iterate over the blocks and build the transaction graph
		int blockHeight = 1; // Start at block height of 1, so it matches NEM's convention
		for (final Block block : blockFileLoader) {
			if (blockHeight > stopHeight) {
				break;
			}
			for (final Transaction trans : block.getTransactions()) {
				if (trans.isCoinBase()) {
					continue; // We don't care about mined coins as they are not related to our transaction graph analysis
				}

				final int currBlockHeight = blockHeight;
				try { // We do this because the BTC blockchain has a lot of junk that throws exceptions
					// 1. Handle transaction inputs
					final Integer nextAddress = uniqueAddresses.size() + 1;
					final SparseBitmap inputs = SparseBitmap.createFromUnsortedData(
							trans.getInputs()
									.stream()
									.map(input ->
									{
										final String currAddress = input.getFromAddress().toString();
										Integer curr = uniqueAddresses.get(currAddress);
										if (null == curr) {
											curr = nextAddress;
											uniqueAddresses.put(currAddress, curr);
										}

										return curr;
									}).mapToInt(input -> input)
							.toArray());

					// 2. Handle transaction outputs
					trans.getOutputs()
							.stream()
							.forEach(o ->
							{
								String outputAddress = null;
								final Script script = o.getScriptPubKey();
								if (script.isSentToAddress() || script.isPayToScriptHash()) {
									outputAddress = script.getToAddress(networkParams).toString();
								} else if (script.isSentToRawPubKey()) {
									outputAddress = Utils.HEX.encode(script.getPubKey());
								}

								if (null != outputAddress) {
									final long value = o.getValue().getValue();
									if (value > 10000 && (value % 10000 != 0 || uniqueAddresses.containsKey(outputAddress))) {
										Integer output = uniqueAddresses.get(outputAddress);
										if (null == output) {
											output = uniqueAddresses.size() + 1;
											uniqueAddresses.put(outputAddress, output);
										}
										transactions.add(new BTCTransaction(currBlockHeight, inputs, output, value));

									} else { // If there is an uneven amount sent, it is going to be the change transaction in most cases
										if (!uniqueAddresses.containsKey(outputAddress)) {
											uniqueAddresses.put(outputAddress, nextAddress);
										}
									}
								}
							});
				} catch (final ScriptException err) {
					// Nothing to see here, move along.
				}
			}
			++blockHeight;
		}

		// merge accts in transactions
		System.out.println(transactions.size());
		mergeAccts(transactions);
		transactions
				.stream()
				.forEach(btcTrans -> transactionData.add(new GraphClusteringTransaction(
									btcTrans.blockHeight,
									btcTrans.inputAddress,
									btcTrans.outputAddress,
									btcTrans.value)));

		this.transactionCache.put(transCacheKey, transactionData);

		return transactionData;
	}

	private void mergeAccts(final List<BTCTransaction> transactions) {
		final Map<Integer, Integer> remapping = new HashMap<>();
		transactions
				.stream()
				.map(trans -> trans.inputs)
				.filter(inputs -> 1 < inputs.cardinality())
				.forEach(inputs -> {
					final Integer lowestBit = inputs.toList().get(0);
					inputs.forEach(val -> remapping.put(val, lowestBit));
				});
		final SparseBitmap remapKeys = SparseBitmap.createFromUnsortedData(remapping.keySet().stream().mapToInt(k -> k).toArray());

		// Now go through and remap the addresses
		transactions
				.parallelStream()
				.forEach(trans -> {
					// handle inputs
					final SparseBitmap remap = remapKeys.and(trans.inputs);
					if (0 < remap.cardinality()) {
						trans.inputAddress = remapping.get(trans.inputs.toList().get(0));
					} else {
						trans.inputAddress = trans.inputs.toList().get(0);
					}

					//handle output
					if (remapKeys.get(trans.output)) {
						trans.outputAddress = remapping.get(trans.output);
					} else {
						trans.outputAddress = trans.output;
					}
				});
	}

	private class BTCTransaction {
		final int blockHeight;
		final SparseBitmap inputs;
		Integer output; //TODO: Making this final complicated things a bit...
		final long value;

		long inputAddress;
		long outputAddress;

		BTCTransaction(final int blockHeight, final SparseBitmap inputs, final Integer output, final long value) {
			this.blockHeight = blockHeight;
			this.inputs = inputs;
			this.output = output;
			this.value = value;
		}
	}
}
