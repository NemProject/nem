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
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.*;
import org.nem.nis.test.NisUtils;
import org.nem.peer.PeerNetwork;
import org.nem.peer.test.PeerUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class DebugControllerTest {

	// TODO: refactor the creation of Block and dbBlock

	@Test
	public void blockDebugInfoDelegatesToBlockDaoAndBlockScorer() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = new BlockHeight(10);
		final TimeInstant timeStamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(
				new AccountCache(),
				new PoiFacade(NisUtils.createImportanceCalculator()));
		final Account signer1 = addRandomAccountWithBalance(accountAnalyzer);
		final Account signer2 = addRandomAccountWithBalance(accountAnalyzer);

		final Block blockDaoBlock = new Block(
				signer1,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timeStamp.addSeconds(60),
				height);
		blockDaoBlock.setDifficulty(difficulty);

		final Block blockDaoParent = new Block(
				signer2,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timeStamp,
				height.prev());
		blockDaoParent.setDifficulty(difficulty);

		final BlockScorer scorer = new BlockScorer(accountAnalyzer.getPoiFacade());
		scorer.forceImportanceCalculation();
		final BigInteger hit = scorer.calculateHit(blockDaoBlock);
		final BigInteger target = scorer.calculateTarget(blockDaoParent, blockDaoBlock);

		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block();
		dbBlock.setPrevBlockHash(blockDaoBlock.getPreviousBlockHash().getRaw());
		dbBlock.setGenerationHash(blockDaoBlock.getGenerationHash().getRaw());
		dbBlock.setForger(new org.nem.nis.dbmodel.Account("", blockDaoBlock.getSigner().getKeyPair().getPublicKey()));
		dbBlock.setDifficulty(blockDaoBlock.getDifficulty().getRaw());
		dbBlock.setHeight(blockDaoBlock.getHeight().getRaw());
		dbBlock.setTimeStamp(blockDaoBlock.getTimeStamp().getRawTime());
		dbBlock.setForgerProof(new byte[64]);
		dbBlock.setBlockTransfers(new ArrayList<>());
		dbBlock.setBlockImportanceTransfers(new ArrayList<>());

		final org.nem.nis.dbmodel.Block dbParent = NisUtils.createDbBlockWithTimeStamp(383);
		dbParent.setPrevBlockHash(blockDaoParent.getPreviousBlockHash().getRaw());
		dbParent.setGenerationHash(blockDaoParent.getGenerationHash().getRaw());
		dbParent.setForger(new org.nem.nis.dbmodel.Account("", blockDaoBlock.getSigner().getKeyPair().getPublicKey()));
		dbParent.setDifficulty(blockDaoParent.getDifficulty().getRaw());
		dbParent.setHeight(blockDaoParent.getHeight().getRaw());
		dbParent.setTimeStamp(blockDaoParent.getTimeStamp().getRawTime());
		dbParent.setForgerProof(new byte[64]);
		dbParent.setBlockTransfers(new ArrayList<>());
		Mockito.when(context.blockDao.findByHeight(new BlockHeight(10))).thenReturn(dbBlock);
		Mockito.when(context.blockDao.findByHeight(new BlockHeight(9))).thenReturn(dbParent);

		Mockito.when(context.blockChain.copyAccountAnalyzer()).thenReturn(accountAnalyzer);

		// Act:
		final BlockDebugInfo blockDebugInfo = context.controller.blockDebugInfo("10");

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getForagerAddress(), IsEqual.equalTo(signer1.getAddress()));
		Assert.assertThat(blockDebugInfo.getTimeStamp(), IsEqual.equalTo(timeStamp.addSeconds(60)));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));
		Assert.assertThat(blockDebugInfo.getTarget(), IsEqual.equalTo(target));
		Assert.assertThat(blockDebugInfo.getInterBlockTime(), IsEqual.equalTo(60));

		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(new BlockHeight(10));
		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(new BlockHeight(9));
		Mockito.verify(context.blockDao, Mockito.times(2)).findByHeight(Mockito.any());
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
				IsEquivalent.equivalentTo(new String[] { "foo", "bar" }));
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

	private static Account addRandomAccountWithBalance(final AccountAnalyzer accountAnalyzer) {
		final Account accountWithPrivateKey = Utils.generateRandomAccount();
		final Account account = accountAnalyzer.getAccountCache().addAccountToCache(accountWithPrivateKey.getAddress());
		account.incrementBalance(Amount.fromNem(10000));

		final PoiAccountState accountState = accountAnalyzer.getPoiFacade().findStateByAddress(account.getAddress());
		accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, Amount.fromNem(10000));
		accountState.setHeight(BlockHeight.ONE);
		return accountWithPrivateKey;
	}

	private static class TestContext {
		private final BlockChain blockChain = Mockito.mock(BlockChain.class);
		private final BlockDao blockDao = Mockito.mock(BlockDao.class);
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final DebugController controller;

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(PeerUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new DebugController(this.host, this.blockChain, this.blockDao);
		}
	}
}