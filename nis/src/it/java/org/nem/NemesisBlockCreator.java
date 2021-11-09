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

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class NemesisBlockCreator {
	private static final PrivateKey NEMESIS_KEY = PrivateKey.fromHexString("c00bfd92ef0a5ca015037a878ad796db9372823daefb7f7b2aea88b79147b91f");
	private static final Hash GENERATION_HASH = Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4");

	private static final String USER_STAKES = "nemesisData/user-stakes.csv";
	private static final String DEV_STAKES = "nemesisData/dev-stakes.csv";
	private static final String MARKETING_STAKES = "nemesisData/marketing-stakes.csv";
	private static final String CONTRIBUTOR_STAKES = "nemesisData/contributor-stakes.csv";
	private static final String FUNDS_STAKES = "nemesisData/funds-stakes.csv";
	private static final String OUTPUT_FILE = "nemesis";
	private static final String OUTPUT_FOLDER = "nemesis-block/";
	private static final long AMOUNT_PER_STAKE = 2_250_000_000_000L;
	private static final long EXPECTED_CUMULATIVE_AMOUNT = 9_000_000_000_000_000L;
	private static final String COSIGNATORY_PUBLIC_KEYS = "nemesisData/cosignatories.csv";
	private static final String MULTISIG_ACCOUNTS = "nemesisData/multisig.csv";
	private static final int EXPECTED_NUM_MULTISIG_ACCOUNTS = 6;

	private static Account creator;
	private static long cumulativeAmount = 0L;
	private static int numMultisigAccounts = 0;

	@Before
	public void initNetwork() {
		NetworkInfos.setDefault(NetworkInfos.getMainNetworkInfo());
		creator = new Account(new KeyPair(NEMESIS_KEY));
	}

	@After
	public void resetNetwork() {
		NetworkInfos.setDefault(null);
	}

	@Test
	public void createNemesisBlock() {
		final Block block = new Block(creator, Hash.ZERO, GENERATION_HASH, TimeInstant.ZERO, BlockHeight.ONE);

		// stakes
		final HashMap<Address, Amount> nemesisAccountMap = new HashMap<>();
		this.readNemesisData(nemesisAccountMap, USER_STAKES);
		this.readNemesisData(nemesisAccountMap, DEV_STAKES);
		this.readNemesisData(nemesisAccountMap, MARKETING_STAKES);
		this.readNemesisData(nemesisAccountMap, CONTRIBUTOR_STAKES);
		this.readNemesisData(nemesisAccountMap, FUNDS_STAKES);

		// multisig accounts
		final HashMap<String, PublicKey> cosignatories = this.readCosignatories(COSIGNATORY_PUBLIC_KEYS);
		final HashMap<Account, List<Account>> multisigMap = this.readMultisigAccounts(MULTISIG_ACCOUNTS, cosignatories);

		// keep makoto happy
		this.fixFundStakes(nemesisAccountMap, multisigMap, FUNDS_STAKES);

		// remove one coin from SUST fund
		final Address sustainability = Address.fromEncoded("NDSUSTAAB2GWHBUFJXP7QQGYHBVEFWZESBUUWM4P");
		nemesisAccountMap.get(sustainability).subtract(Amount.fromNem(1));
		System.out.println("fund stakes fixed");

		nemesisAccountMap.keySet().stream()
				.forEach(address -> block.addTransaction(this.createTransferTransaction(address, nemesisAccountMap.get(address))));

		// not actually needed, one coin is left on nemesis acct
		//block.addTransaction(this.burnOneCoin());

		multisigMap.keySet().stream()
				.forEach(account -> block.addTransaction(this.createMultisigModificationTransaction(nemesisAccountMap, account, multisigMap.get(account))));

		final long xemGiven = block.getTransactions().stream()
				.filter(t -> t.getType() == TransactionTypes.TRANSFER)
				.map(t -> ((TransferTransaction)t).getAmount().getNumMicroNem())
				.reduce(0L, Long::sum);
		final long xemFees = block.getTotalFee().getNumMicroNem();

		if (xemGiven - xemFees != EXPECTED_CUMULATIVE_AMOUNT) {
			throw new RuntimeException(String.format(
					"invalid total number of XEM expected %d but got %d (%d - %d)",
					EXPECTED_CUMULATIVE_AMOUNT,
					xemGiven - xemFees, xemGiven, xemFees));
		}

		// check cumulative amount
		if (cumulativeAmount != EXPECTED_CUMULATIVE_AMOUNT) {
			throw new RuntimeException(String.format(
					"wrong cumulative amount: expected %d but got %d",
					EXPECTED_CUMULATIVE_AMOUNT,
					cumulativeAmount));
		}

		// check number of multisig accounts
		if (numMultisigAccounts != EXPECTED_NUM_MULTISIG_ACCOUNTS) {
			throw new RuntimeException(String.format(
					"wrong number of multisig accounts: expected %d but got %d",
					EXPECTED_NUM_MULTISIG_ACCOUNTS,
					numMultisigAccounts));
		}

		block.sign();
		this.saveNemesisBlock(block);
	}

	@SuppressWarnings("try")
	private void saveNemesisBlock(final Block block) {
		final BinarySerializer serializer = new BinarySerializer();
		block.serialize(serializer);
		final byte[] bytes = serializer.getBytes();
		ExceptionUtils.propagateVoid(() -> {
			final File dir = new File(OUTPUT_FOLDER);
			if (!dir.exists()) {
				if (!dir.mkdir()) {
					throw new RuntimeException(String.format("could not create output folder '%s'", OUTPUT_FOLDER));
				}
			}
		});
		ExceptionUtils.propagateVoid(() -> {
			// writes into user.dir
			try (final FileOutputStream fos = new FileOutputStream(OUTPUT_FOLDER + OUTPUT_FILE + ".bin")) {
				fos.write(bytes);
			}
		});

		final JsonSerializer jsonSerializer = new JsonSerializer();
		block.serialize(jsonSerializer);
		final JsonFormatter formatter = new JsonFormatter();
		ExceptionUtils.propagateVoid(() -> {
			// writes into user.dir
			final String jsonString = formatter.format(jsonSerializer.getObject());
			try (final PrintWriter out = new PrintWriter(OUTPUT_FOLDER + OUTPUT_FILE + ".json")) {
				out.println(jsonString);
				out.close();
			}
		});
	}

	private void readNemesisData(final HashMap<Address, Amount> map, final String file) {
		String line;

		try (final InputStream fin = NemesisBlockCreator.class.getClassLoader().getResourceAsStream(file)) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				final String[] accountData = line.split(",");
				final Address address = Address.fromEncoded(accountData[1]);
				if (!address.isValid()) {
					throw new RuntimeException(String.format("corrupt data in file %s", file));
				}

				final Amount oldAmount = map.getOrDefault(address, Amount.ZERO);
				final Amount amount = Amount.fromMicroNem((long)(Double.parseDouble(accountData[2]) * AMOUNT_PER_STAKE));
				map.put(address, amount.add(oldAmount));
				cumulativeAmount += amount.getNumMicroNem();
			}
		} catch (final IOException e) {
			throw new IllegalStateException("unable to parse nemesis data stream");
		}
	}

	private void fixFundStakes(final HashMap<Address, Amount> map, final HashMap<Account, List<Account>> multisigMap, final String file) {
		String line;

		try (final InputStream fin = NemesisBlockCreator.class.getClassLoader().getResourceAsStream(file)) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				final String[] accountData = line.split(",");
				final Address address = Address.fromEncoded(accountData[1]);
				if (!address.isValid()) {
					throw new RuntimeException(String.format("corrupt data in file %s", file));
				}

				final Amount oldAmount = map.getOrDefault(address, Amount.ZERO);

				final Optional<Account> multisigAccount = multisigMap.keySet().stream()
						.filter(a -> a.getAddress().compareTo(address) == 0)
						.findFirst();

				final MultisigAggregateModificationTransaction transaction = this.createMultisigModificationTransaction(
						map,
						multisigAccount.get(),
						multisigMap.get(multisigAccount.get()));
				final Amount amount = transaction.getFee();
				map.put(address, amount.add(oldAmount));
			}
		} catch (final IOException e) {
			throw new IllegalStateException("unable to parse nemesis data stream");
		}
	}

	private TransferTransaction createTransferTransaction(final Address address, final Amount amount) {
		final Account account = new Account(address);
		final Message message = new PlainMessage("Good luck!".getBytes());
		final TransferTransaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				creator,
				account,
				amount,
				new TransferTransactionAttachment(message));
		transaction.sign();
		return transaction;
	}

	private HashMap<String, PublicKey> readCosignatories(final String file) {
		final HashMap<String, PublicKey> cosignatoryAccountMap = new HashMap<>();
		String line;

		try (final InputStream fin = NemesisBlockCreator.class.getClassLoader().getResourceAsStream(file)) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				final String[] cosignatoryData = line.split(",");
				final String name = cosignatoryData[0];
				final PublicKey publicKey = PublicKey.fromHexString(cosignatoryData[1]);

				cosignatoryAccountMap.put(name, publicKey);
			}

			return cosignatoryAccountMap;
		} catch (final IOException e) {
			throw new IllegalStateException("unable to parse nemesis cosignatory stream");
		}
	}

	private HashMap<Account, List<Account>> readMultisigAccounts(final String file, final HashMap<String, PublicKey> cosignatories) {
		final HashMap<Account, List<Account>> multisigAccountMap = new HashMap<>();
		String line;

		try (final InputStream fin = NemesisBlockCreator.class.getClassLoader().getResourceAsStream(file)) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				final String[] multisigData = line.split(",");
				final PrivateKey multisigKey = PrivateKey.fromHexString(multisigData[1]);
				final Account multisig = new Account(new KeyPair(multisigKey));
				final List<Account> cosignatoriesForAccount = new ArrayList<>();
				for (int i = 2; i < multisigData.length; i++) {
					final PublicKey cosignatoryPublicKey = cosignatories.get(multisigData[i]);
					cosignatoriesForAccount.add(new Account(new KeyPair(cosignatoryPublicKey)));
				}

				if (3 > cosignatories.size()) {
					throw new RuntimeException(String.format("not enough cosignatories for multisig account %s", multisigData[0]));
				}

				multisigAccountMap.put(multisig, cosignatoriesForAccount);
				numMultisigAccounts += 1;
			}

			return multisigAccountMap;
		} catch (final IOException e) {
			throw new IllegalStateException("unable to parse nemesis multisig stream");
		}
	}

	private MultisigAggregateModificationTransaction createMultisigModificationTransaction(
			final HashMap<Address, Amount> nemesisAccountMap,
			final Account multisig,
			final List<Account> cosignatories) {
		final List<MultisigCosignatoryModification> modifications = cosignatories.stream()
				.map(c -> new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, c))
				.collect(Collectors.toList());
		final MultisigAggregateModificationTransaction transaction = new MultisigAggregateModificationTransaction(TimeInstant.ZERO, multisig, modifications);
		transaction.sign();

		System.out.println(String.format("%s : %d modifications, %d fee",
				multisig.getAddress().getEncoded(),
				transaction.getCosignatoryModifications().size(),
				transaction.getFee().getNumNem()));
		if (nemesisAccountMap.getOrDefault(multisig.getAddress(), Amount.ZERO).compareTo(transaction.getFee()) < 0) {
			throw new RuntimeException(String.format(
					"%s has not enough funds to create multisig: expected at least %d but got %d",
					multisig.getAddress().getEncoded(),
					transaction.getFee().getNumMicroNem(),
					nemesisAccountMap.getOrDefault(multisig.getAddress(), Amount.ZERO).getNumMicroNem()));
		}
		return transaction;
	}

	private class JsonFormatter {

		private String format(final JSONObject object) {
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

			private void visit(final JSONArray array, final int indent) {
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

			private void visit(final JSONObject obj, final int indent) {
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

			private void visit(final Object object, final int indent) {
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
