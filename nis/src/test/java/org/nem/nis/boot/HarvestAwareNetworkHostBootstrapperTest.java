package org.nem.nis.boot;

import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.node.*;
import org.nem.core.test.*;
import org.nem.nis.harvesting.*;
import org.nem.specific.deploy.NisConfiguration;

import java.util.concurrent.CompletableFuture;

public class HarvestAwareNetworkHostBootstrapperTest {

	// region delegation

	@Test
	public void bootDelegatesToNetworkHostBoot() {
		// Arrange:
		final TestContext context = new TestContext();
		context.mockBoot(false);

		// Act:
		context.bootstrapper.boot(NodeUtils.createNodeWithName("a")).join();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.times(1)).boot(Mockito.any());
	}

	// endregion

	// region no unlocked accounts

	@Test
	public void noAccountsAreUnlockedIfBootFails() {
		// Arrange:
		final TestContext context = new TestContext();
		context.mockBoot(false);
		context.setAutoHarvestOnBoot(true);

		// Act:
		context.bootstrapper.boot(NodeUtils.createNodeWithName("a")).join();

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.never()).addUnlockedAccount(Mockito.any());
	}

	@Test
	public void noAccountsAreUnlockedIfAutoHarvestOnBootIsDisabled() {
		// Arrange:
		final TestContext context = new TestContext();
		context.mockBoot(true);
		context.setAutoHarvestOnBoot(false);

		// Act:
		context.bootstrapper.boot(NodeUtils.createNodeWithName("a")).join();

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.never()).addUnlockedAccount(Mockito.any());
	}

	// endregion

	// region unlocked accounts

	@Test
	public void bootAccountIsUnlockedIfBootSucceedsAndAutoHarvestOnBootIsEnabled() {
		// Arrange:
		final TestContext context = new TestContext();
		context.mockBoot(true);
		context.setAutoHarvestOnBoot(true);

		// Act:
		final Node bootNode = createBootNode();
		context.bootstrapper.boot(bootNode).join();

		// Assert:
		Mockito.verify(context.unlockedAccounts, Mockito.only()).addUnlockedAccount(bootNodeToAccount(bootNode));
	}

	@Test
	public void bootAccountAndAdditionalAccountsAreUnlockedIfBootSucceedsAndAutoHarvestOnBootIsEnabled() {
		// Arrange:
		final TestContext context = new TestContext();
		context.mockBoot(true);
		context.setAutoHarvestOnBoot(true);
		final PrivateKey additionalKey1 = new KeyPair().getPrivateKey();
		final PrivateKey additionalKey2 = new KeyPair().getPrivateKey();
		Mockito.when(context.configuration.getAdditionalHarvesterPrivateKeys()).thenReturn(new PrivateKey[]{
				additionalKey1, additionalKey2
		});

		// Act:
		final Node bootNode = createBootNode();
		context.bootstrapper.boot(bootNode).join();

		// Assert:
		final ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
		Mockito.verify(context.unlockedAccounts, Mockito.times(3)).addUnlockedAccount(accountCaptor.capture());
		MatcherAssert.assertThat(accountCaptor.getAllValues(), IsEquivalent.equivalentTo(bootNodeToAccount(bootNode),
				privateKeyToAccount(additionalKey1), privateKeyToAccount(additionalKey2)));
	}

	@Test
	public void bootSucceedsIfAutoHarvestAccountCannotBeUnlocked() {
		// Arrange:
		final TestContext context = new TestContext();
		context.mockBoot(true);
		context.setAutoHarvestOnBoot(true);
		final PrivateKey additionalKey1 = new KeyPair().getPrivateKey();
		final PrivateKey additionalKey2 = new KeyPair().getPrivateKey();
		Mockito.when(context.configuration.getAdditionalHarvesterPrivateKeys()).thenReturn(new PrivateKey[]{
				additionalKey1, additionalKey2
		});
		Mockito.when(context.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.SUCCESS,
				UnlockResult.FAILURE_HARVESTING_INELIGIBLE, UnlockResult.SUCCESS);

		// Act:
		final Node bootNode = createBootNode();
		context.bootstrapper.boot(bootNode).join();

		// Assert:
		final ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
		Mockito.verify(context.unlockedAccounts, Mockito.times(3)).addUnlockedAccount(accountCaptor.capture());
		MatcherAssert.assertThat(accountCaptor.getAllValues(), IsEquivalent.equivalentTo(bootNodeToAccount(bootNode),
				privateKeyToAccount(additionalKey1), privateKeyToAccount(additionalKey2)));
	}

	// endregion

	private static Account bootNodeToAccount(final Node node) {
		return privateKeyToAccount(node.getIdentity().getKeyPair().getPrivateKey());
	}

	private static Account privateKeyToAccount(final PrivateKey privateKey) {
		final Address address = Address.fromPublicKey(new KeyPair(privateKey).getPublicKey());
		return new Account(address);
	}

	private static Node createBootNode() {
		return new Node(new NodeIdentity(new KeyPair()), new NodeEndpoint("http", "localhost", 80));
	}

	private static class TestContext {
		private final NisPeerNetworkHost networkHost = Mockito.mock(NisPeerNetworkHost.class);
		private final NisConfiguration configuration = Mockito.mock(NisConfiguration.class);
		private final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		private final HarvestAwareNetworkHostBootstrapper bootstrapper = new HarvestAwareNetworkHostBootstrapper(this.networkHost,
				this.unlockedAccounts, this.configuration);

		public TestContext() {
			Mockito.when(this.configuration.getAdditionalHarvesterPrivateKeys()).thenReturn(new PrivateKey[]{});
			Mockito.when(this.unlockedAccounts.addUnlockedAccount(Mockito.any())).thenReturn(UnlockResult.SUCCESS);
		}

		public void mockBoot(final boolean bootResult) {
			Mockito.when(this.networkHost.boot(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
			Mockito.when(this.networkHost.isNetworkBooted()).thenReturn(bootResult);
		}

		public void setAutoHarvestOnBoot(final boolean autoHarvest) {
			Mockito.when(this.configuration.shouldAutoHarvestOnBoot()).thenReturn(autoHarvest);
		}
	}
}
