package org.nem.nis;

import org.junit.Test;
import org.nem.core.async.SleepFuture;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.connect.HttpJsonPostRequest;
import org.nem.core.connect.HttpMethodClient;
import org.nem.core.connect.client.DefaultAsyncNemConnector;
import org.nem.core.connect.client.NisApiId;
import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.model.*;
import org.nem.core.model.ncc.NemAnnounceResult;
import org.nem.core.model.ncc.RequestAnnounce;
import org.nem.core.model.primitive.Amount;
import org.nem.core.node.NodeEndpoint;
import org.nem.core.serialization.BinarySerializer;
import org.nem.core.serialization.Deserializer;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeInstant;
import org.nem.core.time.TimeProvider;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NetworkSpammer {
	private static final Logger LOGGER = Logger.getLogger(NetworkSpammer.class.getName());

	static {
		NetworkInfos.setDefault(NetworkInfos.fromFriendlyName("mijinnet"));
	}

	private static final int MAX_AMOUNT = 1_000_000;
	private static final List<String> HEX_STRINGS = Arrays.asList(
			"47f3efa89a513aa99b38066ec53152680ead37f2e91fa07aa46a471ede0bb139",
			"130369743394c9cad191e0a5ed100fde315b4e6ec6171a27f28015dca259c523",
			"9cd96a3332f8fde3ebf72d42d9a869f57a4d5011e87562d210a3901109377e51",
			"8055265b2427236cf36d8a36b56636e1dd73a91f20438d3e44d42d4a724cb69d",
			"d402db94353e710f0518ed6bcc3d7b1f1bffdae8e96ff93a1936921095b7c0c8");
	private static final List<PrivateKey> PRIVATE_KEYS = HEX_STRINGS.stream()
			.map(PrivateKey::fromHexString)
			.collect(Collectors.toList());
	private static final List<Address> ADDRESSES = PRIVATE_KEYS.stream()
			.map(p -> new KeyPair(p).getPublicKey())
			.map(Address::fromPublicKey)
			.collect(Collectors.toList());
	private static final HttpMethodClient<ErrorResponseDeserializerUnion> CLIENT = createHttpMethodClient();
	private static final DefaultAsyncNemConnector<NisApiId> CONNECTOR = createConnector();

	@Test
	public void spamNetwork() {
		final NodeEndpoint endpoint = new NodeEndpoint("http", "127.0.0.1", 7895);
		final TimeProvider timeProvider = new SystemTimeProvider();
		final SecureRandom random = new SecureRandom();
		final AtomicLong microNem = new AtomicLong(1);
		final int transactionsPerSecond = 25;
		final List<Transaction> transactions = new ArrayList<>();
		IntStream.range(1, MAX_AMOUNT + 1).forEach(i -> transactions.add(createTransaction(
				timeProvider.getCurrentTime(),
				random.nextInt(5),
				random.nextInt(5),
				i)));
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
		final long start = System.currentTimeMillis();
		scheduler.scheduleAtFixedRate(() -> {
			if (microNem.get() <= MAX_AMOUNT) {
				final Transaction transaction = transactions.get(0);
				final byte[] data = BinarySerializer.serializeToBytes(transaction.asNonVerifiable());
				final RequestAnnounce request = new RequestAnnounce(data, transaction.getSignature().getBytes());
				CompletableFuture<Deserializer> future = CONNECTOR.postAsync(
						endpoint,
						NisApiId.NIS_REST_TRANSACTION_ANNOUNCE,
						new HttpJsonPostRequest(request));
				future.thenAccept(d -> {
						final NemAnnounceResult result = new NemAnnounceResult(d);
						if (!result.isError()) {
							transactions.remove(transaction);
						}
					})
					.exceptionally(e -> {
						System.out.println(e.getMessage());
						return null;
					});
			}
		}, 1, 1000 / transactionsPerSecond, TimeUnit.MILLISECONDS);

		while (!transactions.isEmpty()) {
			SleepFuture.create(1000).join();
			final long stop = System.currentTimeMillis();
			LOGGER.info(String.format("%.2f transactions/second", (1000.0 * (MAX_AMOUNT - transactions.size())) / (stop - start)));
		}
	}

	private static Transaction createTransaction(
			final TimeInstant timeInstant,
			final int senderIndex,
			final int recipientIndex,
			final long amount) {
		final Account sender = new Account(new KeyPair(PRIVATE_KEYS.get(senderIndex)));
		final Account recipient = new Account(ADDRESSES.get(recipientIndex));
		final TransferTransaction transaction = new TransferTransaction(
				1,
				timeInstant,
				sender,
				recipient,
				Amount.fromMicroNem(amount),
				null);
		transaction.setFee(Amount.fromNem(10));
		transaction.setDeadline(timeInstant.addHours(1));
		transaction.sign();
		return transaction;
	}

	private static HttpMethodClient<ErrorResponseDeserializerUnion> createHttpMethodClient() {
		final int connectionTimeout = 2000;
		final int socketTimeout = 10000;
		final int requestTimeout = 30000;
		return new HttpMethodClient<>(connectionTimeout, socketTimeout, requestTimeout);
	}

	private static DefaultAsyncNemConnector<NisApiId> createConnector() {
		final DefaultAsyncNemConnector<NisApiId> connector = new DefaultAsyncNemConnector<>(
				CLIENT,
				r -> { throw new RuntimeException(); });
		connector.setAccountLookup(Account::new);
		return connector;
	}
}
