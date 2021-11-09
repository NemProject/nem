package org.nem.nis;

import org.junit.Test;
import org.nem.core.async.SleepFuture;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.node.*;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.core.time.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.cache.DefaultAccountCache;
import org.nem.nis.connect.HttpConnectorPool;
import org.nem.peer.SecureSerializableEntity;
import org.nem.peer.connect.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.*;

public class NetworkSpammer {
	private static final Logger LOGGER = Logger.getLogger(NetworkSpammer.class.getName());
	private static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();
	private static final PrivateKey PRIVATE_KEY = PrivateKey.fromHexString("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
	private static final NodeIdentity IDENTITY = new NodeIdentity(new KeyPair(PRIVATE_KEY), "Alice");

	static {
		NetworkInfos.setDefault(NetworkInfos.fromFriendlyName("mijinnet"));
	}

	private static final int MAX_AMOUNT = 100_000;
	private static final List<String> HEX_STRINGS = Arrays.asList(
			"5051363f9c72f068b32d121a28ea34747d4892416dcd6488bbbd3f2bc31ed685",
			"7206c8e0d997701ca9b41ee2449f1dda00f8c16dd1f83b3354f4de22f8abb2b5",
			"07e38011514bcce7cbc0d80aa1e29e1666183eb144bcc175f89a818e80454536",
			"4d6bcee45a4416c5c63de19bfabe5301aa59fe84b7eb9aed6c703b1c68c971f9",
			"d0f77aca106aa070523dd9ef0ef9fdf91d594c557e54a4894ba94ab26f804a18");

	private static final List<Node> NODES_50 = Arrays.asList(
			new Node(IDENTITY, new NodeEndpoint("http", "209.126.124.70", 7895)),
			new Node(IDENTITY, new NodeEndpoint("http", "108.61.247.91", 7895)),
			new Node(IDENTITY, new NodeEndpoint("http", "45.63.12.236", 7895)),
			new Node(IDENTITY, new NodeEndpoint("http", "5.9.81.198", 7895))
	);
	private static final List<PrivateKey> PRIVATE_KEYS = HEX_STRINGS.stream()
			.map(PrivateKey::fromHexString)
			.collect(Collectors.toList());
	private static final List<Address> ADDRESSES = PRIVATE_KEYS.stream()
			.map(p -> new KeyPair(p).getPublicKey())
			.map(Address::fromPublicKey)
			.collect(Collectors.toList());
	private static final PeerConnector PEER_CONNECTOR = createPeerConnector();

	@Test
	public void continuousSpamming() {
		// spam 10M transactions into the network
		for (int i = 0; i < 1000; i++) {
			this.spamNetwork();
		}
	}

	@Test
	public void spamNetwork() {
		this.spamNetwork(NODES_50);
	}

	@SuppressWarnings("rawtypes")
	private void spamNetwork(final List<Node> nodes) {
		final SecureRandom random = new SecureRandom();
		final int transactionsPerSecond = 100;
		final List<CompletableFuture<?>> futures = new ArrayList<>();
		final List<Transaction> transactions = new ArrayList<>();
		final TimeInstant curTime = TIME_PROVIDER.getCurrentTime();
		IntStream.range(1, MAX_AMOUNT + 1).forEach(i -> transactions.add(createTransaction(
				curTime.addSeconds((i - 1) / transactionsPerSecond),
				random.nextInt(5),
				random.nextInt(5),
				i)));
		final int numNodes = nodes.size();
		final int transactionsPerNode = transactionsPerSecond / numNodes;
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
		final long start = System.currentTimeMillis();
		scheduler.scheduleAtFixedRate(() -> {
			try {
				for (int i = 0; i < numNodes; i++) {
					final SerializableList<SecureSerializableEntity> entities = new SerializableList<>(transactionsPerNode);
					final Collection<Transaction> pendingTransactions = new ArrayList<>();
					for (int j = 0; j < transactionsPerNode; j++) {
						if (transactions.isEmpty()) {
							continue;
						}
						final Transaction transaction = transactions.remove(0);
						pendingTransactions.add(transaction);
						final SecureSerializableEntity<Transaction> secureEntity = new SecureSerializableEntity<>(
								transaction,
								IDENTITY);
						entities.add(secureEntity);
					}

					if (0 != entities.size()) {
						CompletableFuture<?> future = this.send(nodes.get(i % numNodes), entities);
						this.send(nodes.get((i + 1) % numNodes), entities);
						futures.add(future);
						final int nodeNumber = i;
						future.exceptionally(e -> {
							System.out.println(String.format("Node %d: %s", nodeNumber, e.getMessage()));
							transactions.addAll(pendingTransactions);
							return null;
						});
					}
				}

				final Iterator<CompletableFuture<?>> iter = futures.iterator();
				while (iter.hasNext()) {
					final CompletableFuture<?> future = iter.next();
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

	private CompletableFuture<?> send(final Node node, final SerializableEntity entity) {
		return PEER_CONNECTOR.announce(
				node,
				NisPeerId.REST_PUSH_TRANSACTIONS,
				entity);
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

	private static PeerConnector createPeerConnector() {
		final HttpConnectorPool pool = new HttpConnectorPool(CommunicationMode.BINARY, new AuditCollection(50, TIME_PROVIDER));
		return pool.getPeerConnector(new DefaultAccountCache().copy());
	}
}
