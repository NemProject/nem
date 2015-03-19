package org.nem;

import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.BinarySerializer;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ExceptionUtils;

import java.io.*;
import java.util.HashMap;

public class NemesisBlockCreator {
	private static final PrivateKey NEMESIS_KEY = PrivateKey.fromHexString("c00bfd92ef0a5ca015037a878ad796db9372823daefb7f7b2aea88b79147b91f");
	private static final Account CREATOR = new Account(new KeyPair(NEMESIS_KEY));
	private static final Hash GENERATION_HASH = Hash.fromHexString("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
	private static final String NEMESIS_FILE = "nemesis-data.csv";
	private static final long AMOUNT_PER_STAKE = 2_000_000_000_000L;

	@Before
	public void initNetwork() {
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());
	}

	@After
	public void resetNetwork() {
		NetworkInfos.setDefault(null);
	}

	@Test
	public void createNemesisBlock() {
		final HashMap<Address, Amount> nemesisAccountMap = readNemesisData();
		final Block block = new Block(CREATOR, Hash.ZERO, GENERATION_HASH, TimeInstant.ZERO, BlockHeight.ONE);
		nemesisAccountMap.keySet().stream()
				.forEach(address -> this.addTransaction(block, address, nemesisAccountMap.get(address)));
		block.sign();
		this.saveNemesisBlock(block);
	}

	private void saveNemesisBlock(final Block block) {
		final BinarySerializer serializer = new BinarySerializer();
		block.serialize(serializer);
		final byte[] bytes = serializer.getBytes();
		ExceptionUtils.propagateVoid(() -> {
			// writes into user.dir
			try (final FileOutputStream fos = new FileOutputStream("nemesis-fake.bin")) {
				fos.write(bytes);
			}
		});
	}

	private HashMap<Address, Amount> readNemesisData() {
		final HashMap<Address, Amount> nemesisAccountMap = new HashMap<>();
		String line;

		try (final InputStream fin = NemesisBlockCreator.class.getClassLoader().getResourceAsStream(NEMESIS_FILE)) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
			while ((line = reader.readLine()) != null) {
				final String[] accountData = line.split(",");
				final Address address = Address.fromEncoded(accountData[1]);
				if (!address.isValid()) {
					throw new RuntimeException(String.format("corrupt data in file %s", NEMESIS_FILE));
				}

				final Amount amount = Amount.fromMicroNem((long)(Double.parseDouble(accountData[2]) * AMOUNT_PER_STAKE));
				nemesisAccountMap.put(address, amount);
			}

			return nemesisAccountMap;
		} catch (final IOException e) {
			throw new IllegalStateException("unable to parse nemesis data stream");
		}
	}

	private void addTransaction(final Block block, final Address address, final Amount amount) {
		final Account account = new Account(address);
		final TransferTransaction transaction = new TransferTransaction(TimeInstant.ZERO, CREATOR, account, amount, null);
		transaction.sign();
		block.addTransaction(transaction);
	}
}
