package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.cache.NisCache;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.ImportanceCalculator;
import org.nem.nis.state.AccountState;
import org.nem.peer.PeerNetwork;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class DebugControllerTest {

	@Test
	public void blockDebugInfoIsNotSupported() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.controller.blockDebugInfo("10"),
				UnsupportedOperationException.class);
	}

	@Ignore
	@Test
	public void blockDebugInfoDelegatesToBlockDaoAndBlockScorer() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = new BlockHeight(10);
		final TimeInstant timeStamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);

		// Arrange: simulate block loading by (1) copying all of the information in the NisCache
		class TestState {
			Account signer1;
			BigInteger hit;
			BigInteger target;
		}

		final TestState state = new TestState();
		Mockito.when(context.blockAnalyzer.analyze(Mockito.any(), Mockito.eq(10L))).then(invocationOnMock -> {
			final NisCache nisCache = ((NisCache)invocationOnMock.getArguments()[0]).copy();
			state.signer1 = addRandomAccountWithBalance(nisCache);
			final Account signer2 = addRandomAccountWithBalance(nisCache);

			final Block blockDaoBlock = createBlock(state.signer1, timeStamp.addSeconds(60), height, difficulty);
			final Block blockDaoParent = createBlock(signer2, timeStamp, height.prev(), difficulty);

			final BlockScorer scorer = new BlockScorer(nisCache.getAccountStateCache());
			state.hit = scorer.calculateHit(blockDaoBlock);
			state.target = scorer.calculateTarget(blockDaoParent, blockDaoBlock);

			final org.nem.nis.dbmodel.Block dbBlock = createDbBlock(blockDaoBlock);
			final org.nem.nis.dbmodel.Block dbParent = createDbBlock(blockDaoParent);
			Mockito.when(context.blockDao.findByHeight(new BlockHeight(10))).thenReturn(dbBlock);
			Mockito.when(context.blockDao.findByHeight(new BlockHeight(9))).thenReturn(dbParent);
			return true;
		});

		// Act:
		final BlockDebugInfo blockDebugInfo = context.controller.blockDebugInfo("10");

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getForagerAddress(), IsEqual.equalTo(state.signer1.getAddress()));
		Assert.assertThat(blockDebugInfo.getTimeStamp(), IsEqual.equalTo(timeStamp.addSeconds(60)));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(state.hit));
		Assert.assertThat(blockDebugInfo.getTarget(), IsEqual.equalTo(state.target));
		Assert.assertThat(blockDebugInfo.getInterBlockTime(), IsEqual.equalTo(60));

		Mockito.verify(context.blockAnalyzer, Mockito.only()).analyze(Mockito.any(), Mockito.eq(10L));
		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(new BlockHeight(10));
		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(new BlockHeight(9));
		Mockito.verify(context.blockDao, Mockito.times(2)).findByHeight(Mockito.any());
	}

	private static Block createBlock(
			final Account signer,
			final TimeInstant timeStamp,
			final BlockHeight height,
			final BlockDifficulty difficulty) {
		final Block block = new Block(
				signer,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timeStamp,
				height);
		block.setDifficulty(difficulty);
		return block;
	}

	private static org.nem.nis.dbmodel.Block createDbBlock(final Block block) {
		final Address signerAddress = block.getSigner().getAddress();
		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block();
		dbBlock.setPrevBlockHash(block.getPreviousBlockHash().getRaw());
		dbBlock.setGenerationHash(block.getGenerationHash().getRaw());
		dbBlock.setForger(new org.nem.nis.dbmodel.Account(signerAddress.getEncoded(), signerAddress.getPublicKey()));
		dbBlock.setDifficulty(block.getDifficulty().getRaw());
		dbBlock.setHeight(block.getHeight().getRaw());
		dbBlock.setTimeStamp(block.getTimeStamp().getRawTime());
		dbBlock.setForgerProof(new byte[64]);
		dbBlock.setBlockTransfers(new ArrayList<>());
		dbBlock.setBlockImportanceTransfers(new ArrayList<>());
		return dbBlock;
	}

	@Test
	public void timersInfoDelegatesToHost() {
		// Arrange:
		final List<NemAsyncTimerVisitor> originalVisitors = Arrays.asList(
				new NemAsyncTimerVisitor("foo", null),
				new NemAsyncTimerVisitor("bar", null));
		final TestContext context = new TestContext();
		Mockito.when(context.host.getVisitors()).thenReturn(originalVisitors);

		// Act:
		final SerializableList<NemAsyncTimerVisitor> visitors = context.controller.timersInfo();

		// Assert:
		Mockito.verify(context.host, Mockito.times(1)).getVisitors();
		Assert.assertThat(
				visitors.asCollection().stream().map(v -> v.getTimerName()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo("foo", "bar"));
	}

	@Test
	public void incomingConnectionsInfoDelegatesToHost() {
		// Arrange:
		final TestContext context = new TestContext();
		final AuditCollection hostAuditCollection = Mockito.mock(AuditCollection.class);
		Mockito.when(context.host.getIncomingAudits()).thenReturn(hostAuditCollection);

		// Act:
		final AuditCollection auditCollection = context.controller.incomingConnectionsInfo();

		// Assert:
		Assert.assertThat(auditCollection, IsSame.sameInstance(hostAuditCollection));
		Mockito.verify(context.host, Mockito.times(1)).getIncomingAudits();
	}

	@Test
	public void outgoingConnectionsInfoDelegatesToHost() {
		// Arrange:
		final TestContext context = new TestContext();
		final AuditCollection hostAuditCollection = Mockito.mock(AuditCollection.class);
		Mockito.when(context.host.getOutgoingAudits()).thenReturn(hostAuditCollection);

		// Act:
		final AuditCollection auditCollection = context.controller.outgoingConnectionsInfo();

		// Assert:
		Assert.assertThat(auditCollection, IsSame.sameInstance(hostAuditCollection));
		Mockito.verify(context.host, Mockito.times(1)).getOutgoingAudits();
	}

	private static Account addRandomAccountWithBalance(final NisCache nisCache) {
		final Account accountWithPrivateKey = Utils.generateRandomAccount();
		final Account account = nisCache.getAccountCache().addAccountToCache(accountWithPrivateKey.getAddress());

		final Amount balance = Amount.fromNem(10000);
		final AccountState accountState = nisCache.getAccountStateCache().findStateByAddress(account.getAddress());
		accountState.getAccountInfo().incrementBalance(balance);
		accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
		accountState.setHeight(BlockHeight.ONE);
		return accountWithPrivateKey;
	}

	private static class TestContext {
		private final BlockDao blockDao = Mockito.mock(BlockDao.class);
		private final BlockAnalyzer blockAnalyzer = Mockito.mock(BlockAnalyzer.class);
		private final ImportanceCalculator importanceCalculator = Mockito.mock(ImportanceCalculator.class);
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final DebugController controller;

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new DebugController(this.host, this.blockDao, this.blockAnalyzer, this.importanceCalculator);
		}
	}
}