package org.nem.nis.poi.graph;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.NodeId;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class NodeNeighborsTest {

	// region constructor

	@Test
	public void canCreateNodeNeighborsFromAscendingIds() {
		// Assert:
		NisUtils.createNeighbors(2, 3, 12);
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotCreateNodeNeighborsFromAscendingIds() {
		// Assert:
		NisUtils.createNeighbors(2, 12, 3);
	}

	// endregion

	// region addNeighbor

	@Test
	public void canAddNeighborsInAscendingNodeIdOrder() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);

		// Act:
		nodeNeighbors.addNeighbor(new NodeId(15));
		nodeNeighbors.addNeighbor(new NodeId(20));

		// Assert:
		Assert.assertThat(nodeNeighbors.contains(new NodeId(15)), IsEqual.equalTo(true));
		Assert.assertThat(nodeNeighbors.contains(new NodeId(20)), IsEqual.equalTo(true));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotAddNeighborsInNonAscendingNodeIdOrder() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(1, 2);

		// Act:
		nodeNeighbors.addNeighbor(new NodeId(6));
		nodeNeighbors.addNeighbor(new NodeId(3));
	}

	// endregion

	@Test
	public void sizeReturnsTheCorrectSize() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);

		// Assert:
		Assert.assertThat(nodeNeighbors.size(), IsEqual.equalTo(3));
	}

	@Test
	public void canRemoveAllNeighbors() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);

		// Act:
		nodeNeighbors.removeAll();

		// Assert:
		Assert.assertThat(nodeNeighbors.size(), IsEqual.equalTo(0));
	}

	@Test
	public void containsReturnsTrueIfNodeIdIsContained() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);

		// Assert:
		assertNodeNeighborsContain(nodeNeighbors, 2, 3, 6);
	}

	@Test
	public void containsReturnsFalseIfNodeIdIsNotContained() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);

		// Assert:
		assertNodeNeighborsDoNotContain(nodeNeighbors, 4, 5, 9, 15);
	}

	@Test
	public void commonNeighborsSizeReturnsCorrectSize() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);
		NodeNeighbors otherNodeNeighbors = NisUtils.createNeighbors(3, 6, 11);

		// Assert:
		Assert.assertThat(nodeNeighbors.commonNeighborsSize(otherNodeNeighbors), IsEqual.equalTo(2));
	}

	@Test
	public void unionWithOtherNodeNeighborsDelegatesToUnionWithNodeNeighborsArray() {
		// Arrange:
		NodeNeighbors nodeNeighbors = Mockito.mock(NodeNeighbors.class);
		NodeNeighbors otherNodeNeighbors = Mockito.mock(NodeNeighbors.class);
		Mockito.when(nodeNeighbors.union(new NodeNeighbors[] { otherNodeNeighbors })).thenReturn(NisUtils.createNeighbors());
		Mockito.when(nodeNeighbors.union(otherNodeNeighbors)).thenCallRealMethod();

		// Act:
		nodeNeighbors.union(otherNodeNeighbors);

		// Assert:
		Mockito.verify(nodeNeighbors, Mockito.times(1)).union(new NodeNeighbors[] { otherNodeNeighbors });
	}

	@Test
	public void unionWithNodeNeighborsArrayReturnsCorrectResult() {
		// Arrange:
		NodeNeighbors nodeNeighbors1 = NisUtils.createNeighbors(2, 3, 6);
		NodeNeighbors nodeNeighbors2 = NisUtils.createNeighbors(3, 6, 11);
		NodeNeighbors nodeNeighbors3 = NisUtils.createNeighbors(1);
		NodeNeighbors nodeNeighbors4 = NisUtils.createNeighbors();
		NodeNeighbors nodeNeighbors5 = NisUtils.createNeighbors(29, 30);

		// Act:
		NodeNeighbors result = nodeNeighbors1.union(new NodeNeighbors[] { nodeNeighbors2, nodeNeighbors3, nodeNeighbors4, nodeNeighbors5 });

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(NisUtils.createNeighbors(1, 2, 3, 6, 11, 29, 30)));
	}

	@Test
	public void differenceWithOtherNodeNeighborsReturnsCorrectResult() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);
		NodeNeighbors otherNodeNeighbors = NisUtils.createNeighbors(3, 6, 11);

		// Act:
		NodeNeighbors result = nodeNeighbors.difference(otherNodeNeighbors);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(NisUtils.createNeighbors(2)));
	}

	@Test
	public void toListReturnsListOfNodeIds() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);

		// Act:
		List<NodeId> result = nodeNeighbors.toList();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(Arrays.asList(new NodeId(2), new NodeId(3), new NodeId(6))));
	}

	// region iterator

	@Test
	public void iteratorHasNextReturnsTrueIfMoreNodeIdsAreAvailable() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);
		Iterator<NodeId> iterator = nodeNeighbors.iterator();

		// Assert:
		Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(true));
		iterator.next();
		Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(true));
		iterator.next();
		Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(true));
	}

	@Test
	public void iteratorHasNextReturnsFalseIfNoMoreNodeIdsAreAvailable() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);
		Iterator<NodeId> iterator = nodeNeighbors.iterator();

		// Act:
		iterator.next();
		iterator.next();
		iterator.next();

		// Assert:
		Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(false));
	}

	@Test
	public void iteratorHasNextReturnsFalseForEmptyNodeNeighbors() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors();
		Iterator<NodeId> iterator = nodeNeighbors.iterator();

		// Assert:
		Assert.assertThat(iterator.hasNext(), IsEqual.equalTo(false));
	}

	@Test
	public void iteratorIteratesThroughNodeNeighborsNodeIdsInAscendingOrder() {
		// Arrange:
		NodeNeighbors nodeNeighbors = NisUtils.createNeighbors(2, 3, 6);
		Iterator<NodeId> iterator = nodeNeighbors.iterator();

		// Assert:
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new NodeId(2)));
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new NodeId(3)));
		Assert.assertThat(iterator.next(), IsEqual.equalTo(new NodeId(6)));
	}

	// endregion

	// region hashCode/equals

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		NodeNeighbors nodeNeighbors1 = NisUtils.createNeighbors(2, 3, 6);
		NodeNeighbors nodeNeighbors2 = NisUtils.createNeighbors(2, 3, 6);
		NodeNeighbors nodeNeighbors3 = NisUtils.createNeighbors(1, 3, 6);
		NodeNeighbors nodeNeighbors4 = NisUtils.createNeighbors();

		// Assert:
		Assert.assertThat(nodeNeighbors1.hashCode(), IsEqual.equalTo(nodeNeighbors2.hashCode()));
		Assert.assertThat(nodeNeighbors1.hashCode(), IsNot.not(IsEqual.equalTo(nodeNeighbors3.hashCode())));
		Assert.assertThat(nodeNeighbors1.hashCode(), IsNot.not(IsEqual.equalTo(nodeNeighbors4.hashCode())));
		Assert.assertThat(nodeNeighbors3.hashCode(), IsNot.not(IsEqual.equalTo(nodeNeighbors4.hashCode())));
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		NodeNeighbors nodeNeighbors1 = NisUtils.createNeighbors(2, 3, 6);
		NodeNeighbors nodeNeighbors2 = NisUtils.createNeighbors(2, 3, 6);
		NodeNeighbors nodeNeighbors3 = NisUtils.createNeighbors(1, 3, 6);
		NodeNeighbors nodeNeighbors4 = NisUtils.createNeighbors();

		// Assert:
		Assert.assertThat(nodeNeighbors1, IsEqual.equalTo(nodeNeighbors2));
		Assert.assertThat(nodeNeighbors1, IsNot.not(IsEqual.equalTo(nodeNeighbors3)));
		Assert.assertThat(nodeNeighbors1, IsNot.not(IsEqual.equalTo(nodeNeighbors4)));
		Assert.assertThat(nodeNeighbors3, IsNot.not(IsEqual.equalTo(nodeNeighbors4)));
	}

	// endregion

	private void assertNodeNeighborsContain(final NodeNeighbors nodeNeighbors, final int... ids) {
		for (int id : ids) {
			Assert.assertThat(nodeNeighbors.contains(new NodeId(id)), IsEqual.equalTo(true));
		}
	}

	private void assertNodeNeighborsDoNotContain(final NodeNeighbors nodeNeighbors, final int... ids) {
		for (int id : ids) {
			Assert.assertThat(nodeNeighbors.contains(new NodeId(id)), IsEqual.equalTo(false));
		}
	}
}
