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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NetworkSpammer {
	private static final Logger LOGGER = Logger.getLogger(NetworkSpammer.class.getName());

	static {
		NetworkInfos.setDefault(NetworkInfos.fromFriendlyName("mijinnet"));
	}

	private static final int MAX_AMOUNT = 25_000;
	private static final List<String> HEX_STRINGS = Arrays.asList(
			"5051363f9c72f068b32d121a28ea34747d4892416dcd6488bbbd3f2bc31ed685",
			"7206c8e0d997701ca9b41ee2449f1dda00f8c16dd1f83b3354f4de22f8abb2b5",
			"07e38011514bcce7cbc0d80aa1e29e1666183eb144bcc175f89a818e80454536",
			"d0f77aca106aa070523dd9ef0ef9fdf91d594c557e54a4894ba94ab26f804a18",
			"4d6bcee45a4416c5c63de19bfabe5301aa59fe84b7eb9aed6c703b1c68c971f9");
	private static final List<NodeEndpoint> ENDPOINTS = Arrays.asList(
			new NodeEndpoint("http", "45.32.11.215", 7895),
			new NodeEndpoint("http", "108.61.162.159", 7895),
			new NodeEndpoint("http", "104.238.150.159", 7895),
			new NodeEndpoint("http", "45.32.9.197", 7895)
	);
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
		final TimeProvider timeProvider = new SystemTimeProvider();
		final SecureRandom random = new SecureRandom();
		final int transactionsPerSecond = 25;
		final List<CompletableFuture<Deserializer>> futures = new ArrayList<>();
		final List<Transaction> transactions = new ArrayList<>();
		IntStream.range(1, MAX_AMOUNT + 1).forEach(i -> transactions.add(createTransaction(
				timeProvider.getCurrentTime(),
				random.nextInt(5),
				random.nextInt(5),
				i)));
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
		final long start = System.currentTimeMillis();
		scheduler.scheduleAtFixedRate(() -> {
			try {
				for (int i = 0; i < transactionsPerSecond; i++) {
					if (transactions.isEmpty()) {
						continue;
					}

					final Transaction transaction = transactions.remove(0);
					final byte[] data = BinarySerializer.serializeToBytes(transaction.asNonVerifiable());
					final RequestAnnounce request = new RequestAnnounce(data, transaction.getSignature().getBytes());
					CompletableFuture<Deserializer> future = CONNECTOR.postAsync(
							ENDPOINTS.get(random.nextInt(4)),
							NisApiId.NIS_REST_TRANSACTION_ANNOUNCE,
							new HttpJsonPostRequest(request));
					futures.add(future);
					future.thenAccept(d -> {
						final NemAnnounceResult result = new NemAnnounceResult(d);
						if (result.isError()) {
							transactions.add(transaction);
						}
					})
					.exceptionally(e -> {
						System.out.println(e.getMessage());
						transactions.add(transaction);
						return null;
					});
				}

				final Iterator<CompletableFuture<Deserializer>> iter = futures.iterator();
				while (iter.hasNext()) {
					final CompletableFuture<Deserializer> future = iter.next();
					if (future.isDone()) {
						iter.remove();
					}
				}
			} catch (Exception e) {
				LOGGER.info(String.format("Exception: %s", e.getMessage()));
			}
		}, 1, 1000, TimeUnit.MILLISECONDS);

		while (!transactions.isEmpty() || !futures.isEmpty()) {
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
		transaction.setDeadline(timeInstant.addHours(23));
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
