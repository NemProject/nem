package org.nem;

import net.minidev.json.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ExceptionUtils;
import wiremock.org.json.JSONException;

import java.io.*;
import java.util.*;

@Ignore
public class NemesisBlockCreator {
	private static final PrivateKey NEMESIS_KEY = PrivateKey.fromHexString("c00bfd92ef0a5ca015037a878ad796db9372823daefb7f7b2aea88b79147b91f");
	private static final Account CREATOR = new Account(new KeyPair(NEMESIS_KEY));
	private static final Hash GENERATION_HASH = Hash.fromHexString("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
	private static final String USER_STAKES = "stakes/user-stakes.csv";
	private static final String DEV_STAKES = "stakes/dev-stakes.csv";
	private static final String OUTPUT_FILE = "nemesis-fake"; // TODO 20150320 can you output to a subdirectory that we can put gitignore?
	private static final long AMOUNT_PER_STAKE = 2_249_999_999_750L;
	private static final long EXPECTED_CUMULATIVE_AMOUNT = 8_999_999_999_000_000L;
	private static long CUMULATIVE_AMOUNT = 0L;

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
		final HashMap<Address, Amount> nemesisAccountMap = this.readNemesisData(USER_STAKES);
		//nemesisAccountMap.putAll(readNemesisData(DEV_STAKES));

		// check cumulative amount
		if (CUMULATIVE_AMOUNT != EXPECTED_CUMULATIVE_AMOUNT) {
			throw new RuntimeException(String.format("wrong cumulative amount: expected %d but got %d", EXPECTED_CUMULATIVE_AMOUNT, CUMULATIVE_AMOUNT));
		}

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
			try (final FileOutputStream fos = new FileOutputStream(OUTPUT_FILE + ".bin")) {
				fos.write(bytes);
			}
		});

		final JsonSerializer jsonSerializer = new JsonSerializer();
		block.serialize(jsonSerializer);
		final JsonFormatter formatter = new JsonFormatter();
		ExceptionUtils.propagateVoid(() -> {
			// writes into user.dir
			final String jsonString = formatter.format(jsonSerializer.getObject());
			try (final PrintWriter out = new PrintWriter(OUTPUT_FILE + ".json")) {
				out.println(jsonString);
			}
		});
	}

	private HashMap<Address, Amount> readNemesisData(final String file) {
		final HashMap<Address, Amount> nemesisAccountMap = new HashMap<>();
		String line;

		try (final InputStream fin = NemesisBlockCreator.class.getClassLoader().getResourceAsStream(file)) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
			while ((line = reader.readLine()) != null) {
				final String[] accountData = line.split(",");
				final Address address = Address.fromEncoded(accountData[1]);
				if (!address.isValid()) {
					throw new RuntimeException(String.format("corrupt data in file %s", file));
				}

				final Amount amount = Amount.fromMicroNem((long)(Double.parseDouble(accountData[2]) * AMOUNT_PER_STAKE));
				nemesisAccountMap.put(address, amount);
				CUMULATIVE_AMOUNT += amount.getNumMicroNem();
			}

			return nemesisAccountMap;
		} catch (final IOException e) {
			throw new IllegalStateException("unable to parse nemesis data stream");
		}
	}

	private void addTransaction(final Block block, final Address address, final Amount amount) {
		final Account account = new Account(address);
		final Message message = new PlainMessage("Good luck!".getBytes());
		final TransferTransaction transaction = new TransferTransaction(TimeInstant.ZERO, CREATOR, account, amount, message);
		transaction.sign();
		block.addTransaction(transaction);
	}

	private class JsonFormatter {

		private String format(final JSONObject object) throws JSONException {
			final JsonVisitor visitor = new JsonVisitor(2, ' ');
			visitor.visit(object, 0);
			return visitor.toString();
		}

		private class JsonVisitor {

			private final StringBuilder builder = new StringBuilder();
			private final int indentationSize;
			private final char indentationChar;

			private JsonVisitor(final int indentationSize, final char indentationChar) {
				this.indentationSize = indentationSize;
				this.indentationChar = indentationChar;
			}

			private void visit(final JSONArray array, final int indent) throws JSONException {
				final int length = array.size();
				if (length == 0) {
					this.write("[]", indent);
				} else {
					this.write("[", 0);
					for (int i = 0; i < length; i++) {
						this.visit(array.get(i), indent + 1);
						this.write(i < length - 1 ? ", " : "", 0);
					}

					this.write("]", 0);
				}
			}

			private void visit(final JSONObject obj, final int indent) throws JSONException {
				final int length = obj.size();
				if (length == 0) {
					this.write("{}", 0);
				} else {
					this.write("{", 0, true);
					final Iterator<String> keys = obj.keySet().iterator();
					while (keys.hasNext()) {
						final String key = keys.next();
						this.write("\"" + key + "\" : ", indent + 1);
						this.visit(obj.get(key), indent + 1);
						this.write(keys.hasNext() ? "," : "", 0, true);
					}

					this.write("}", indent);
				}
			}

			private void visit(final Object object, final int indent) throws JSONException {
				if (object instanceof JSONArray) {
					this.visit((JSONArray)object, indent);
				} else if (object instanceof JSONObject) {
					this.visit((JSONObject)object, indent);
				} else {
					if (object instanceof String) {
						this.write("\"" + object + "\"", 0);
					} else {
						this.write(String.valueOf(object), 0);
					}
				}
			}

			private void write(final String data, final int indent) {
				this.write(data, indent, false);
			}

			private void write(final String data, final int indent, final boolean lineSeparator) {
				for (int i = 0; i < (indent * this.indentationSize); i++) {
					this.builder.append(this.indentationChar);
				}

				this.builder.append(data);
				if (lineSeparator) {
					this.builder.append(System.getProperty("line.separator"));
				}
			}

			@Override
			public String toString() {
				return this.builder.toString();
			}
		}
	}
}
