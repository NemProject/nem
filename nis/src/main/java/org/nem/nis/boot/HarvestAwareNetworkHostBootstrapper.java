package org.nem.nis.boot;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.node.Node;
import org.nem.nis.harvesting.*;
import org.nem.specific.deploy.NisConfiguration;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * A network host bootstrapper that supports auto harvesting.
 */
public class HarvestAwareNetworkHostBootstrapper implements NetworkHostBootstrapper {
	private static final Logger LOGGER = Logger.getLogger(HarvestAwareNetworkHostBootstrapper.class.getName());
	private final NisPeerNetworkHost networkHost;
	private final UnlockedAccounts unlockedAccounts;
	private final NisConfiguration configuration;

	/**
	 * Creates a new bootstrapper.
	 *
	 * @param networkHost The network host.
	 * @param unlockedAccounts The unlocked accounts.
	 * @param configuration The configuration.
	 */
	public HarvestAwareNetworkHostBootstrapper(final NisPeerNetworkHost networkHost, final UnlockedAccounts unlockedAccounts,
			final NisConfiguration configuration) {
		this.networkHost = networkHost;
		this.unlockedAccounts = unlockedAccounts;
		this.configuration = configuration;
	}

	@Override
	public CompletableFuture<Void> boot(final Node localNode) {
		return this.networkHost.boot(localNode).thenAccept(v -> this.autoHarvest(localNode.getIdentity().getKeyPair().getPrivateKey()));
	}

	private void autoHarvest(final PrivateKey bootKey) {
		if (!this.networkHost.isNetworkBooted()) {
			LOGGER.info("bypassing auto harvesting because network is not booted");
			return;
		}

		if (!this.configuration.shouldAutoHarvestOnBoot()) {
			LOGGER.info("bypassing auto harvesting because feature is turned off in configuration");
			return;
		}

		this.addHarvester(bootKey);
		Arrays.stream(this.configuration.getAdditionalHarvesterPrivateKeys()).forEach(this::addHarvester);
	}

	private void addHarvester(final PrivateKey privateKey) {
		final Account account = new Account(new KeyPair(privateKey));
		final UnlockResult result = this.unlockedAccounts.addUnlockedAccount(account);

		LOGGER.info(String.format("auto harvesting with '%s' -> '%s'", account, result));
		if (UnlockResult.SUCCESS != result) {
			LOGGER.severe(String.format("Could not start harvesting with account %s, reason: %s", account.toString(), result.toString()));
		}
	}
}
