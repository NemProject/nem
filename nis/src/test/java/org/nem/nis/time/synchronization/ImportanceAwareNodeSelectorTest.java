package org.nem.nis.time.synchronization;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.nis.poi.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.*;

// TODO 20140909 since these seem to be based on the BasicNodeSelectorTest consider refactoring the common tests into an abstract NodeSelectorTest base class

public class ImportanceAwareNodeSelectorTest {

	//region recalculations

	@Test
	public void constructorRecalculatesTrustValues() {
		// Act:
		final TestContext context = new TestContext(
				new ColumnVector(1, 1, 1, 1, 1),
				new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10, 10, 10));

		// Assert:
		Mockito.verify(context.trustProvider, Mockito.times(1)).computeTrust(context.context);
	}

	@Test
	public void selectNodeDoesNotRecalculateTrustValues() {
		// Arrange:
		final TestContext context = new TestContext(
				new ColumnVector(1, 1, 1, 1, 1),
				new ColumnVector(0.2, 0.2, 0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10, 10, 10));

		// Act:
		for (int i = 0; i < 10; ++i) {
			context.selector.selectNode();
		}

		// Assert:
		Mockito.verify(context.trustProvider, Mockito.times(1)).computeTrust(context.context);
	}

	//endregion

	//region selectNode

	@Test
	public void selectNodeReturnsNullWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(0, 0, 0),
				new ColumnVector(0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10),
				random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		Assert.assertThat(node, IsNull.nullValue());
	}

	@Test
	public void selectNodeReturnsNullWhenNoNodeHasRequiredImportance() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(1, 1, 1),
				new ColumnVector(0.0001, 0.0001, 0.0001),
				new ColumnVector(10, 10, 10),
				random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		Assert.assertThat(node, IsNull.nullValue());
	}

	@Test
	public void selectNodeReturnsNullWhenNoNodeHasImportanceCalculatedAtLastPoiRecalculationHeight() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(1, 1, 1),
				new ColumnVector(0.2, 0.2, 0.2),
				new ColumnVector(11, 12, 13),
				random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		Assert.assertThat(node, IsNull.nullValue());
	}

	@Test
	public void selectNodeReturnsNonNullNodeWhenAtLeastOneNodeMeetsAllRequirements() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(0, 1, 0),
				new ColumnVector(0.0001, 0.2, 0.0001),
				new ColumnVector(11, 10, 12),
				random);

		// Act:
		final Node node = context.selector.selectNode();

		// Assert:
		Assert.assertThat(node, IsEqual.equalTo(context.nodes[1]));
	}

	//endregion

	//region selectNodes

	@Test
	public void selectNodesReturnsEmptyListWhenAllNodesHaveZeroTrustValues() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(0, 0, 0, 0),
				new ColumnVector(0.2, 0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10, 10),
				random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(0));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenTrustValuesSumToOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(
				2,
				new ColumnVector(0.1, 0.2, 0.3, 0.4),
				new ColumnVector(0.2, 0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10, 10),
				random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesSelectsNodesCorrectlyWhenTrustValuesDoNotSumToOne() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(
				2,
				new ColumnVector(20, 40, 60, 80),
				new ColumnVector(0.2, 0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10, 10),
				random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesReturnsEmptyListWhenNoNodeHasRequiredImportance() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(0, 0, 0, 0),
				new ColumnVector(0.0001, 0.0001, 0.0001, 0.0001),
				new ColumnVector(10, 10, 10, 10),
				random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(0));
	}

	@Test
	public void selectNodesReturnsEmptyListWhenNoNodeHasImportanceCalculatedAtLastPoiRecalculationHeight() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(0, 0, 0, 0),
				new ColumnVector(0.2, 0.2, 0.2, 0.2),
				new ColumnVector(11, 12, 13, 14),
				random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		Assert.assertThat(nodes.size(), IsEqual.equalTo(0));
	}

	@Test
	public void selectNodesOnlyReturnsUniqueNodesWhichMeetAllRequirements() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.50);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(1, 0, 1, 0),
				new ColumnVector(0.2, 0.0001, 0.2, 0.0001),
				new ColumnVector(10, 12, 10, 14),
				random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesDoesNotReturnMoreThanMaxNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40);
		final TestContext context = new TestContext(
				2,
				new ColumnVector(10, 20, 30, 40),
				new ColumnVector(0.2, 0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10, 10),
				random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2])));
	}

	@Test
	public void selectNodesReturnsAllNodesIfMaxNodesIsGreaterThanAvailableNodes() {
		// Arrange:
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.10, 0.40, 0.10, 0.40);
		final TestContext context = new TestContext(
				10,
				new ColumnVector(10, 20, 30, 40),
				new ColumnVector(0.2, 0.2, 0.2, 0.2),
				new ColumnVector(10, 10, 10, 10),
				random);

		// Act:
		final List<Node> nodes = context.selector.selectNodes();

		// Assert:
		Assert.assertThat(
				nodes,
				IsEqual.equalTo(Arrays.asList(context.nodes[0], context.nodes[2], context.nodes[1], context.nodes[3])));
	}

	//endregion

	private static class TestContext {
		private final PoiFacade poiFacade = new PoiFacade(Mockito.mock(PoiImportanceGenerator.class));
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final TrustProvider trustProvider = Mockito.mock(TrustProvider.class);
		private final Node localNode = Mockito.mock(Node.class);
		private final Node[] nodes;
		private final NodeExperiences nodeExperiences;
		private final NodeSelector selector;

		public TestContext(final ColumnVector trustValues, final ColumnVector importanceValues, final ColumnVector heightValues) {
			this(10, trustValues, importanceValues, heightValues, new SecureRandom());
		}

		public TestContext(
				final int maxNodes,
				final ColumnVector trustValues,
				final ColumnVector importanceValues,
				final ColumnVector heightValues,
				final Random random) {
			Mockito.when(this.context.getLocalNode()).thenReturn(this.localNode);

			this.nodes = new Node[trustValues.size()];
			for (int i = 0; i < this.nodes.length; ++i) {
				this.nodes[i] = new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("127.0.0.1"));
				PoiAccountState state = this.poiFacade.findStateByAddress(this.nodes[i].getIdentity().getAddress());
				state.getImportanceInfo().setImportance(new BlockHeight((long)heightValues.getAt(i)), importanceValues.getAt(i));
			}
			setFacadeInternalValues(this.poiFacade, this.nodes.length, new BlockHeight(10));

			Mockito.when(this.context.getNodes()).thenReturn(this.nodes);

			this.nodeExperiences = new NodeExperiences();
			Mockito.when(this.context.getNodeExperiences()).thenReturn(this.nodeExperiences);

			Mockito.when(this.trustProvider.computeTrust(this.context)).thenReturn(trustValues);
			this.selector = new ImportanceAwareNodeSelector(maxNodes, this.poiFacade, this.trustProvider, this.context, random);
		}

		private static void setFacadeInternalValues(final PoiFacade facade, final int lastPoiVectorSize, final BlockHeight height) {
			try {
				Field field = PoiFacade.class.getDeclaredField("lastPoiVectorSize");
				field.setAccessible(true);
				field.set(facade, lastPoiVectorSize);
				field = PoiFacade.class.getDeclaredField("lastPoiRecalculationHeight");
				field.setAccessible(true);
				field.set(facade, height);
			} catch(IllegalAccessException | NoSuchFieldException e) {
				throw new RuntimeException("Exception in setFacadeInternalValues");
			}
		}
	}
}
