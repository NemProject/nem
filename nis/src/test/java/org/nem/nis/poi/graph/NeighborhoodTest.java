package org.nem.nis.poi.graph;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.SparseMatrix;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.AccountLink;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class NeighborhoodTest {

	//region getCommunity

	@Test
	public void getCommunityCreatesCommunityAroundSpecifiedNode() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(0, 1, 3, 7, 9));

		final SimilarityStrategy strategy = Mockito.mock(SimilarityStrategy.class);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(0))).thenReturn(0.4);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(1))).thenReturn(0.701);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(3))).thenReturn(0.7);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(7))).thenReturn(0.8);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(9))).thenReturn(0.699);

		final Neighborhood neighborhood = new Neighborhood(repository, strategy);

		// Act:
		final Community community = neighborhood.getCommunity(new NodeId(2));

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(2)));
		Assert.assertThat(community.getSimilarNeighbors().toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(1, 3, 7, 9)));
		Assert.assertThat(community.getDissimilarNeighbors().toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0)));
	}

	@Test
	public void getCommunityCachesCommunityResult() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors());
		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final Community community1 = neighborhood.getCommunity(new NodeId(2));
		final Community community2 = neighborhood.getCommunity(new NodeId(2));

		// Assert:
		Assert.assertThat(community2, IsSame.sameInstance(community1));
	}
	//endregion

	//region getNeighboringCommunities / getNeighboringCommunities
	@Test
	public void getNeighboringCommunitiesReturnsAllNeighboringCommunities() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(0, 1, 3, 7, 9));
		Mockito.when(repository.getNeighbors(new NodeId(0))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(repository.getNeighbors(new NodeId(1))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(repository.getNeighbors(new NodeId(7))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(repository.getNeighbors(new NodeId(9))).thenReturn(NisUtils.createNeighbors(2));
		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final Collection<Community> neighboringCommunities = neighborhood.getNeighboringCommunities(new NodeId(2));

		// Assert:
		Assert.assertThat(
				neighboringCommunities.stream().map(Community::getPivotId).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 1, 3, 7, 9)));
	}

	@Test
	public void getTwoHopAwayCommunitiesReturnsAllTwoHopAwayCommunities() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(1))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(3, 5, 7));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(3, 5, 7, 11));
		Mockito.when(repository.getNeighbors(new NodeId(7))).thenReturn(NisUtils.createNeighbors(2, 9, 11));
		Mockito.when(repository.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(1, 10, 12));
		Mockito.when(repository.getNeighbors(new NodeId(10))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(11))).thenReturn(NisUtils.createNeighbors(3, 7));
		Mockito.when(repository.getNeighbors(new NodeId(12))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(9))).thenReturn(NisUtils.createNeighbors(7));
		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final Collection<Community> neighboringCommunities = neighborhood.getTwoHopAwayCommunities(new NodeId(2));

		// Assert: 3, 7, 5 are not included
		Assert.assertThat(
				neighboringCommunities.stream().map(Community::getPivotId).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(11, 9, 10, 12, 1)));
	}

	@Test
	public void getTwoHopAwayExcludesPivot() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(1))).thenReturn(NisUtils.createNeighbors(1));
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(3, 5, 7));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(11));
		Mockito.when(repository.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(1, 10, 12));
		Mockito.when(repository.getNeighbors(new NodeId(7))).thenReturn(NisUtils.createNeighbors(2, 9, 11));
		Mockito.when(repository.getNeighbors(new NodeId(9))).thenReturn(NisUtils.createNeighbors(7));
		Mockito.when(repository.getNeighbors(new NodeId(10))).thenReturn(NisUtils.createNeighbors(10));
		Mockito.when(repository.getNeighbors(new NodeId(11))).thenReturn(NisUtils.createNeighbors(3, 7));
		Mockito.when(repository.getNeighbors(new NodeId(12))).thenReturn(NisUtils.createNeighbors(12));

		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final Collection<Community> neighboringCommunities = neighborhood.getTwoHopAwayCommunities(new NodeId(2));

		// Assert: 2 is not included
		Assert.assertThat(
				neighboringCommunities.stream().map(Community::getPivotId).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(11, 9, 10, 12, 1)));
	}

	@Test
	public void getTwoHopAwayExcludesDirectNeighbors() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(3, 5, 7));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(3, 5, 7, 11));
		Mockito.when(repository.getNeighbors(new NodeId(7))).thenReturn(NisUtils.createNeighbors(2, 9, 11));
		Mockito.when(repository.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(1, 10, 12));
		Mockito.when(repository.getNeighbors(new NodeId(11))).thenReturn(NisUtils.createNeighbors(3, 7));
		Mockito.when(repository.getNeighbors(new NodeId(1))).thenReturn(NisUtils.createNeighbors(5));
		Mockito.when(repository.getNeighbors(new NodeId(10))).thenReturn(NisUtils.createNeighbors(5));
		Mockito.when(repository.getNeighbors(new NodeId(12))).thenReturn(NisUtils.createNeighbors(5));
		Mockito.when(repository.getNeighbors(new NodeId(9))).thenReturn(NisUtils.createNeighbors(7));
		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final Collection<Community> neighboringCommunities = neighborhood.getTwoHopAwayCommunities(new NodeId(2));

		// Assert: 3, 7, 5 are not included
		Assert.assertThat(
				neighboringCommunities.stream().map(Community::getPivotId).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(11, 9, 10, 12, 1)));
	}

	//endregion

	//region size

	@Test
	public void sizeReturnsNeighborhoodRepositoryLogicalSize() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getLogicalSize()).thenReturn(18);
		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final int size = neighborhood.size();

		// Assert:
		Assert.assertThat(size, IsEqual.equalTo(18));
		Mockito.verify(repository, Mockito.times(1)).getLogicalSize();
	}

	//endregion

	//region integration tests

	@Test
	public void neighborhoodReturnsCommunitiesCorrectly() {
		// Act:
		final Neighborhood neighborhood = createNeighborhood();
		final Community[] testCommunities = createTestCommunities();

		// Assert:
		for (int i = 0; i < testCommunities.length; ++i) {
			Assert.assertThat(neighborhood.getCommunity(new NodeId(i)), IsEqual.equalTo(testCommunities[i]));
		}
	}

	@Test
	public void neighborhoodReturnsTwoHopAwayCommunitiesCorrectly() {
		// Act:
		final Neighborhood neighborhood = createNeighborhood();
		final Community[][] testCommunities = createTwoHopTestCommunities();

		// Assert:
		for (int i = 0; i < testCommunities.length; ++i) {
			Assert.assertThat(neighborhood.getTwoHopAwayCommunities(new NodeId(i)), IsEquivalent.equivalentTo(testCommunities[i]));
		}
	}

	//endregion

	//region test infrastructure

	private static Community[] createTestCommunities() {
		final Community[] testCommunities = new Community[14];

		testCommunities[0] = new Community(new NodeId(0), NisUtils.createNeighbors(0), NisUtils.createNeighbors(1));
		testCommunities[1] = new Community(new NodeId(1), NisUtils.createNeighbors(1, 2, 4), NisUtils.createNeighbors(0, 5));
		testCommunities[2] = new Community(new NodeId(2), NisUtils.createNeighbors(1, 2, 3, 4), NisUtils.createNeighbors());
		testCommunities[3] = new Community(new NodeId(3), NisUtils.createNeighbors(2, 3, 4, 6), NisUtils.createNeighbors(7));
		testCommunities[4] = new Community(new NodeId(4), NisUtils.createNeighbors(1, 2, 3, 4, 5, 6), NisUtils.createNeighbors());
		testCommunities[5] = new Community(new NodeId(5), NisUtils.createNeighbors(4, 5, 6), NisUtils.createNeighbors(1, 7));
		testCommunities[6] = new Community(new NodeId(6), NisUtils.createNeighbors(3, 4, 5, 6, 7), NisUtils.createNeighbors());
		testCommunities[7] = new Community(new NodeId(7), NisUtils.createNeighbors(6, 7, 8), NisUtils.createNeighbors(3, 5, 9, 11));
		testCommunities[8] = new Community(new NodeId(8), NisUtils.createNeighbors(7, 8, 9, 10, 11), NisUtils.createNeighbors());
		testCommunities[9] = new Community(new NodeId(9), NisUtils.createNeighbors(8, 9, 10, 13), NisUtils.createNeighbors(7));
		testCommunities[10] = new Community(new NodeId(10), NisUtils.createNeighbors(8, 9, 10, 11, 12, 13), NisUtils.createNeighbors());
		testCommunities[11] = new Community(new NodeId(11), NisUtils.createNeighbors(8, 10, 11, 12), NisUtils.createNeighbors(7));
		testCommunities[12] = new Community(new NodeId(12), NisUtils.createNeighbors(10, 11, 12, 13), NisUtils.createNeighbors());
		testCommunities[13] = new Community(new NodeId(13), NisUtils.createNeighbors(9, 10, 12, 13), NisUtils.createNeighbors());

		return testCommunities;
	}

	private static Community[][] createTwoHopTestCommunities() {
		final Community[] oneHopCommunities = createTestCommunities();
		final Community[][] twoHopCommunities = new Community[14][];

		twoHopCommunities[0] = new Community[] { oneHopCommunities[2], oneHopCommunities[4], oneHopCommunities[5] };
		twoHopCommunities[1] = new Community[] { oneHopCommunities[3], oneHopCommunities[6], oneHopCommunities[7] };
		twoHopCommunities[2] = new Community[] { oneHopCommunities[0], oneHopCommunities[5], oneHopCommunities[6], oneHopCommunities[7] };
		twoHopCommunities[3] = new Community[] { oneHopCommunities[1], oneHopCommunities[5], oneHopCommunities[8], oneHopCommunities[9], oneHopCommunities[11] };
		twoHopCommunities[4] = new Community[] { oneHopCommunities[0], oneHopCommunities[7] };
		twoHopCommunities[5] = new Community[] { oneHopCommunities[0], oneHopCommunities[2], oneHopCommunities[3], oneHopCommunities[8], oneHopCommunities[9], oneHopCommunities[11] };
		twoHopCommunities[6] = new Community[] { oneHopCommunities[1], oneHopCommunities[2], oneHopCommunities[8], oneHopCommunities[9], oneHopCommunities[11] };
		twoHopCommunities[7] = new Community[] { oneHopCommunities[1], oneHopCommunities[2], oneHopCommunities[4], oneHopCommunities[10], oneHopCommunities[12], oneHopCommunities[13] };
		twoHopCommunities[8] = new Community[] { oneHopCommunities[3], oneHopCommunities[5], oneHopCommunities[6], oneHopCommunities[12], oneHopCommunities[13] };
		twoHopCommunities[9] = new Community[] { oneHopCommunities[3], oneHopCommunities[5], oneHopCommunities[6], oneHopCommunities[11], oneHopCommunities[12] };
		twoHopCommunities[10] = new Community[] { oneHopCommunities[7] };
		twoHopCommunities[11] = new Community[] { oneHopCommunities[3], oneHopCommunities[5], oneHopCommunities[6], oneHopCommunities[9], oneHopCommunities[13] };
		twoHopCommunities[12] = new Community[] { oneHopCommunities[7], oneHopCommunities[8], oneHopCommunities[9] };
		twoHopCommunities[13] = new Community[] { oneHopCommunities[7], oneHopCommunities[8], oneHopCommunities[11] };

		return twoHopCommunities;
	}

	private static Neighborhood createNeighborhood() {
		// Arrange:
		final NodeNeighborMap repository = new NodeNeighborMap(createTestOutlinkMatrix());
		final SimilarityStrategy similarityStrategy = new DefaultSimilarityStrategy(repository);
		return new Neighborhood(repository, similarityStrategy);
	}

	private static SparseMatrix createTestOutlinkMatrix() {
		// Arrange: create 4 accounts
		final long multiplier = 1000 * Amount.MICRONEMS_IN_NEM;
		final List<TestAccountInfo> accountInfos = Arrays.asList(
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null),
				new TestAccountInfo(multiplier, multiplier, null));

		final BlockHeight height = new BlockHeight(21);
		final List<PoiAccountState> accountStates = createTestPoiAccounts(accountInfos, height);

		// set up account links
		addAccountLink(height, accountStates.get(0), accountStates.get(1), 8);
		addAccountLink(height, accountStates.get(1), accountStates.get(2), 4);
		addAccountLink(height, accountStates.get(1), accountStates.get(4), 2);
		addAccountLink(height, accountStates.get(1), accountStates.get(5), 6);
		addAccountLink(height, accountStates.get(2), accountStates.get(3), 3);
		addAccountLink(height, accountStates.get(2), accountStates.get(4), 5);
		addAccountLink(height, accountStates.get(3), accountStates.get(4), 5);
		addAccountLink(height, accountStates.get(3), accountStates.get(6), 2);
		addAccountLink(height, accountStates.get(3), accountStates.get(7), 2);
		addAccountLink(height, accountStates.get(4), accountStates.get(5), 2);
		addAccountLink(height, accountStates.get(4), accountStates.get(6), 2);
		addAccountLink(height, accountStates.get(5), accountStates.get(6), 2);
		addAccountLink(height, accountStates.get(5), accountStates.get(7), 2);
		addAccountLink(height, accountStates.get(6), accountStates.get(7), 2);
		addAccountLink(height, accountStates.get(7), accountStates.get(8), 2);
		addAccountLink(height, accountStates.get(7), accountStates.get(9), 2);
		addAccountLink(height, accountStates.get(7), accountStates.get(11), 2);
		addAccountLink(height, accountStates.get(8), accountStates.get(9), 2);
		addAccountLink(height, accountStates.get(8), accountStates.get(10), 2);
		addAccountLink(height, accountStates.get(8), accountStates.get(11), 2);
		addAccountLink(height, accountStates.get(9), accountStates.get(10), 2);
		addAccountLink(height, accountStates.get(9), accountStates.get(13), 2);
		addAccountLink(height, accountStates.get(10), accountStates.get(11), 2);
		addAccountLink(height, accountStates.get(10), accountStates.get(11), 2);
		addAccountLink(height, accountStates.get(10), accountStates.get(12), 2);
		addAccountLink(height, accountStates.get(10), accountStates.get(13), 2);
		addAccountLink(height, accountStates.get(11), accountStates.get(12), 2);
		addAccountLink(height, accountStates.get(12), accountStates.get(13), 2);

		final PoiContext context = new PoiContext(accountStates, height);
		final SparseMatrix outlinkMatrix = context.getOutlinkMatrix();

		// Now we need to go make the outlink matrix undirected
		for (int i = 0; i < outlinkMatrix.getColumnCount(); i++) {
			for (int j = 0; j < outlinkMatrix.getRowCount(); j++) {
				outlinkMatrix.incrementAt(j, i, outlinkMatrix.getAt(i, j));
			}
		}
		outlinkMatrix.removeNegatives();

		//		final PoiGraphParameters params = PoiGraphParameters.getDefaultParams();
		//		params.set("layout", Integer.toString(PoiGraphViewer.KAMADA_KAWAI_LAYOUT));
		//		final PoiGraphViewer viewer = new PoiGraphViewer(outlinkMatrix, params);
		//		try {
		//			viewer.saveGraph();
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}

		// Act:
		return outlinkMatrix;
	}

	private static void addAccountLink(
			final BlockHeight height,
			final PoiAccountState sender,
			final PoiAccountState recipient,
			final int amount) {

		final AccountLink link = new AccountLink(height, Amount.fromNem(amount), recipient.getAddress());
		sender.getImportanceInfo().addOutlink(link);
	}

	private static List<PoiAccountState> createTestPoiAccounts(
			final List<TestAccountInfo> accountInfos,
			final BlockHeight height) {

		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (final TestAccountInfo info : accountInfos) {
			final PoiAccountState account = createAccountWithBalance(info.balance);

			for (final int amount : info.amounts) {
				final AccountLink link = new AccountLink(height, Amount.fromNem(amount), Utils.generateRandomAddress());
				account.getImportanceInfo().addOutlink(link);
			}

			accountStates.add(account);
		}

		return accountStates;
	}

	private static PoiAccountState createAccountWithBalance(final long numNEM) {
		return createAccountWithBalance(1, numNEM);
	}

	private static PoiAccountState createAccountWithBalance(final long blockHeight, final long numNEM) {
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		state.getWeightedBalances().addFullyVested(new BlockHeight(blockHeight), Amount.fromNem(numNEM));
		return state;
	}

	private static class TestAccountInfo {

		public final long vestedBalance;
		public final long balance;
		public final int[] amounts;

		public TestAccountInfo(final long vestedBalance, final long balance, final int[] amounts) {
			this.vestedBalance = vestedBalance;
			this.balance = balance;
			this.amounts = null == amounts ? new int[] { } : amounts;
		}
	}

	//endregion
}
