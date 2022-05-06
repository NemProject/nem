package org.nem.nis.time.synchronization;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.nis.cache.*;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.state.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.lang.reflect.Field;
import java.util.*;

public class ImportanceAwareNodeSelectorTest extends NodeSelectorTest {

	@Override
	protected NodeSelector createSelector(final int maxNodes, final ColumnVector trustVector, final TrustContext context,
			final Random random) {
		final AccountImportance importance = Mockito.mock(AccountImportance.class);
		Mockito.when(importance.getImportance(Mockito.any())).thenReturn(0.125);
		Mockito.when(importance.getHeight()).thenReturn(new BlockHeight(14));

		final AccountState state = Mockito.mock(AccountState.class);
		Mockito.when(state.getImportanceInfo()).thenReturn(importance);

		final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		Mockito.when(accountStateCache.findLatestForwardedStateByAddress(Mockito.any())).thenReturn(state);

		final PoxFacade poxFacade = Mockito.mock(PoxFacade.class);
		Mockito.when(poxFacade.getLastRecalculationHeight()).thenReturn(new BlockHeight(14));
		return new ImportanceAwareNodeSelector(maxNodes, poxFacade, accountStateCache, trustVector, context.getNodes(), random);
	}

	// region selectNode

	@Test
	public void selectNodeReturnsNullWhenNoNodeHasRequiredImportance() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(1, 1, 1), new ColumnVector(0.0001, 0.0001, 0.0001),
				new ColumnVector(10, 10, 10), random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		MatcherAssert.assertThat(node, IsNull.nullValue());
	}

	@Test
	public void selectNodeReturnsNullWhenNoNodeHasImportanceCalculatedAtLastRecalculationHeight() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(1, 1, 1), new ColumnVector(0.2, 0.2, 0.2),
				new ColumnVector(11, 12, 13), random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		MatcherAssert.assertThat(node, IsNull.nullValue());
	}

	@Test
	public void selectNodeReturnsNonNullNodeWhenAtLeastOneNodeMeetsAllRequirements() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(0, 1, 0), new ColumnVector(0.0001, 0.2, 0.0001),
				new ColumnVector(11, 10, 12), random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		MatcherAssert.assertThat(node, IsEqual.equalTo(context.nodes[1]));
	}

	// endregion

	// region selectNodes

	@Test
	public void selectNodesReturnsEmptyListWhenNoNodeHasRequiredImportance() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(0, 0, 0, 0), new ColumnVector(0.0001, 0.0001, 0.0001, 0.0001),
				new ColumnVector(10, 10, 10, 10), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes.size(), IsEqual.equalTo(0));
	}

	@Test
	public void selectNodesReturnsEmptyListWhenNoNodeHasImportanceCalculatedAtLastRecalculationHeight() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(0, 0, 0, 0), new ColumnVector(0.2, 0.2, 0.2, 0.2),
				new ColumnVector(11, 12, 13, 14), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes.size(), IsEqual.equalTo(0));
	}

	@Test
	public void selectNodesOnlyReturnsUniqueNodesWhichMeetAllRequirements() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(10, new ColumnVector(1, 0, 1, 0), new ColumnVector(0.2, 0.0001, 0.2, 0.0001),
				new ColumnVector(10, 12, 10, 14), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesReturnsAllNodesIfMaxNodesIsGreaterThanAvailableNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40, 0.10, 0.40);
		final TestContext context = new TestContext(10, new ColumnVector(10, 20, 30, 40), new ColumnVector(0.2, 0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10, 10), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes)));

		// - assert that it took the shortcut
		Mockito.verify(random, Mockito.never()).nextDouble();
	}

	@Test
	public void selectNodesReturnsCorrectNodesIfReturnedRandomNumbersAreVeryHighAndNonCandidatesAreAtTheEndOfTheArray() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.9);
		final TestContext context = new TestContext(2, new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2),
				new ColumnVector(0.2, 0.2, 0.2, 0.00001, 0.00001), new ColumnVector(10, 10, 10, 10, 10), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		// the old algorithm would have returned no nodes because 0.2 + 0.2 + 0.2 < 0.9 and thus it would have skipped all good candidates.
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes[2], context.nodes[1])));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenCumulativeCandidateTrustIsLessThanOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40, 0.10, 0.40);
		// Node 0 and 3 do not qualify as candidate therefore only 0.5 trust is available.
		final TestContext context = new TestContext(10, new ColumnVector(0.25, 0.25, 0.25, 0.25),
				new ColumnVector(0.00001, 0.2, 0.2, 0.00001), new ColumnVector(10, 10, 10, 10), random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		MatcherAssert.assertThat(nodes, IsEqual.equalTo(Arrays.asList(context.nodes[1], context.nodes[2])));
	}

	// endregion

	// region use of forwarded state

	@Test
	public void selectNodesCallsFindLatestForwardedStateByAddressOnAccountStateCache() {
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.0);
		final TestContext context = new TestContext(1, new ColumnVector(0.25), new ColumnVector(0.2), new ColumnVector(10.0), random);

		// Act:
		context.selector.selectNodes();

		// Assert (one call due to context setup):
		Mockito.verify(context.accountStateCache, Mockito.times(1 + 1)).findLatestForwardedStateByAddress(Mockito.any());
		Mockito.verify(context.accountStateCache, Mockito.never()).findStateByAddress(Mockito.any());
	}

	// endregion

	private static class TestContext {
		private final DefaultPoxFacade poxFacade = new DefaultPoxFacade(Mockito.mock(ImportanceCalculator.class));
		private final AccountStateCache accountStateCache = Mockito.spy(new DefaultAccountStateCache().copy());
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final Node localNode = Mockito.mock(Node.class);
		private final Node[] nodes;
		private final NodeExperiences nodeExperiences;
		private final NodeSelector selector;

		public TestContext(final int maxNodes, final ColumnVector trustValues, final ColumnVector importanceValues,
				final ColumnVector heightValues, final Random random) {
			Mockito.when(this.context.getLocalNode()).thenReturn(this.localNode);

			this.nodes = new Node[trustValues.size()];
			for (int i = 0; i < this.nodes.length; ++i) {
				this.nodes[i] = new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("127.0.0.1"));
				final AccountState state = this.accountStateCache
						.findLatestForwardedStateByAddress(this.nodes[i].getIdentity().getAddress());
				state.getImportanceInfo().setImportance(new BlockHeight((long) heightValues.getAt(i)), importanceValues.getAt(i));
			}

			setFacadeInternalValues(this.poxFacade, this.nodes.length, new BlockHeight(10));

			Mockito.when(this.context.getNodes()).thenReturn(this.nodes);

			this.nodeExperiences = new NodeExperiences();
			Mockito.when(this.context.getNodeExperiences()).thenReturn(this.nodeExperiences);

			this.selector = new ImportanceAwareNodeSelector(maxNodes, this.poxFacade, this.accountStateCache, trustValues,
					this.context.getNodes(), random);
		}

		private static void setFacadeInternalValues(final DefaultPoxFacade facade, final int lastVectorSize, final BlockHeight height) {
			try {
				Field field = DefaultPoxFacade.class.getDeclaredField("lastVectorSize");
				field.setAccessible(true);
				field.set(facade, lastVectorSize);
				field = DefaultPoxFacade.class.getDeclaredField("lastRecalculationHeight");
				field.setAccessible(true);
				field.set(facade, height);
			} catch (IllegalAccessException | NoSuchFieldException e) {
				throw new RuntimeException("Exception in setFacadeInternalValues");
			}
		}
	}
}
