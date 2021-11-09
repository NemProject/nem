package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.NodeId;
import org.nem.core.test.ExceptionAssert;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.*;

public class NodeNeighborsTest {

	// region constructor

	@Test
	public void canCreateEmptyNodeNeighbors() {
		// Act:
		final NodeNeighbors neighbors = new NodeNeighbors();

		// Assert:
		MatcherAssert.assertThat(neighbors.toList(), IsEqual.equalTo(NisUtils.toNodeIdList()));
	}

	@Test
	public void canCreateNodeNeighborsFromAscendingIds() {
		// Act:
		final NodeNeighbors neighbors = new NodeNeighbors(NisUtils.toNodeIdArray(2, 3, 12));

		// Assert:
		MatcherAssert.assertThat(neighbors.toList(), IsEqual.equalTo(NisUtils.toNodeIdList(2, 3, 12)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotCreateNodeNeighborsFromNonAscendingIds() {
		// Assert:
		new NodeNeighbors(NisUtils.toNodeIdArray(2, 12, 3));
	}

	// endregion

	// region addNeighbor

	@Test
	public void canAddNeighborsInAscendingNodeIdOrder() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Act:
		neighbors.addNeighbor(new NodeId(15));
		neighbors.addNeighbor(new NodeId(20));

		// Assert:
		MatcherAssert.assertThat(neighbors.toList(), IsEqual.equalTo(NisUtils.toNodeIdList(2, 3, 6, 15, 20)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotAddNeighborsInNonAscendingNodeIdOrder() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(1, 2);

		// Act:
		neighbors.addNeighbor(new NodeId(6));
		neighbors.addNeighbor(new NodeId(3));
	}

	// endregion

	// region size

	@Test
	public void sizeReturnsTotalNumberOfNeighbors() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Assert:
		MatcherAssert.assertThat(neighbors.size(), IsEqual.equalTo(3));
	}

	// endregion

	// region removeAll

	@Test
	public void canRemoveAllNeighbors() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Act:
		neighbors.removeAll();

		// Assert:
		MatcherAssert.assertThat(neighbors.size(), IsEqual.equalTo(0));
	}

	@Test
	public void canAddNewNeighborsAfterRemovingAllNeighbors() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Act:
		neighbors.removeAll();
		neighbors.addNeighbor(new NodeId(0));
		neighbors.addNeighbor(new NodeId(7));

		// Assert:
		MatcherAssert.assertThat(neighbors.toList(), IsEqual.equalTo(NisUtils.toNodeIdList(0, 7)));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfNodeIdIsContained() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Assert:
		for (final int i : Arrays.asList(2, 3, 6)) {
			MatcherAssert.assertThat(neighbors.contains(new NodeId(i)), IsEqual.equalTo(true));
		}
	}

	@Test
	public void containsReturnsFalseIfNodeIdIsNotContained() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Assert:
		for (final int i : Arrays.asList(4, 5, 9, 15)) {
			MatcherAssert.assertThat(neighbors.contains(new NodeId(i)), IsEqual.equalTo(false));
		}
	}

	// endregion

	// region commonNeighborsSize

	@Test
	public void commonNeighborsSizeReturnsNumberOfNeighborsCommonToTwoNeighborhoods() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);
		final NodeNeighbors otherNeighbors = NisUtils.createNeighbors(3, 6, 11);

		// Assert:
		MatcherAssert.assertThat(neighbors.commonNeighborsSize(otherNeighbors), IsEqual.equalTo(2));
		MatcherAssert.assertThat(otherNeighbors.commonNeighborsSize(neighbors), IsEqual.equalTo(2));
	}

	// endregion

	// region union

	@Test
	public void unionCanBeCalledWithZeroNodeNeighbors() {
		// Act:
		final NodeNeighbors result = NodeNeighbors.union();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NisUtils.createNeighbors()));
	}

	@Test
	public void unionCanBeCalledWithOneNodeNeighbors() {
		// Arrange:
		final NodeNeighbors neighbors1 = NisUtils.createNeighbors(2, 3, 6);

		// Act:
		final NodeNeighbors result = NodeNeighbors.union(neighbors1);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NisUtils.createNeighbors(2, 3, 6)));
	}

	@Test
	public void unionCanBeCalledWithMultipleNodeNeighbors() {
		// Arrange:
		final NodeNeighbors neighbors1 = NisUtils.createNeighbors(2, 3, 6);
		final NodeNeighbors neighbors2 = NisUtils.createNeighbors(3, 6, 11);
		final NodeNeighbors neighbors3 = NisUtils.createNeighbors(1);
		final NodeNeighbors neighbors4 = NisUtils.createNeighbors();
		final NodeNeighbors neighbors5 = NisUtils.createNeighbors(29, 30);

		// Act:
		final NodeNeighbors result = NodeNeighbors.union(neighbors1, neighbors2, neighbors3, neighbors4, neighbors5);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NisUtils.createNeighbors(1, 2, 3, 6, 11, 29, 30)));
	}

	// endregion

	// region difference

	@Test
	public void differenceWithOtherNodeNeighborsReturnsCorrectResult() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);
		final NodeNeighbors otherNeighbors = NisUtils.createNeighbors(3, 6, 11);

		// Assert:
		MatcherAssert.assertThat(neighbors.difference(otherNeighbors), IsEqual.equalTo(NisUtils.createNeighbors(2)));
		MatcherAssert.assertThat(otherNeighbors.difference(neighbors), IsEqual.equalTo(NisUtils.createNeighbors(11)));
	}

	// endregion

	// region toList

	@Test
	public void toListReturnsListOfNodeIds() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Act:
		final List<NodeId> result = neighbors.toList();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(NisUtils.toNodeIdList(2, 3, 6)));
	}

	// endregion

	// region iterator

	@Test
	public void iteratorHasNextReturnsTrueIfMoreNodeIdsAreAvailable() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);
		final Iterator<NodeId> iterator = neighbors.iterator();

		// Assert:
		MatcherAssert.assertThat(iterator.hasNext(), IsEqual.equalTo(true));
		iterator.next();
		MatcherAssert.assertThat(iterator.hasNext(), IsEqual.equalTo(true));
		iterator.next();
		MatcherAssert.assertThat(iterator.hasNext(), IsEqual.equalTo(true));
	}

	@Test
	public void iteratorHasNextReturnsFalseIfNoMoreNodeIdsAreAvailable() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);
		final Iterator<NodeId> iterator = neighbors.iterator();

		// Act:
		iterator.next();
		iterator.next();
		iterator.next();

		// Assert:
		MatcherAssert.assertThat(iterator.hasNext(), IsEqual.equalTo(false));
		ExceptionAssert.assertThrows(v -> iterator.next(), IndexOutOfBoundsException.class);
	}

	@Test
	public void iteratorHasNextReturnsFalseForEmptyNodeNeighbors() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors();
		final Iterator<NodeId> iterator = neighbors.iterator();

		// Assert:
		MatcherAssert.assertThat(iterator.hasNext(), IsEqual.equalTo(false));
		ExceptionAssert.assertThrows(v -> iterator.next(), IndexOutOfBoundsException.class);
	}

	@Test
	public void iteratorIteratesThroughNodeNeighborsNodeIdsInAscendingOrder() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Assert:
		MatcherAssert.assertThat(StreamSupport.stream(neighbors.spliterator(), false).collect(Collectors.toList()),
				IsEqual.equalTo(NisUtils.toNodeIdList(2, 3, 6)));
	}

	// endregion

	// region hashCode/equals

	@SuppressWarnings("serial")
	private static final Map<String, NodeNeighbors> DESC_TO_NEIGHBORS_MAP = new HashMap<String, NodeNeighbors>() {
		{
			this.put("default", NisUtils.createNeighbors(2, 3, 6));
			this.put("diff-member-ids", NisUtils.createNeighbors(2, 4, 6));
			this.put("diff-less-member-ids", NisUtils.createNeighbors(3, 6));
			this.put("diff-more-member-ids", NisUtils.createNeighbors(2, 3, 7));
			this.put("diff-empty", NisUtils.createNeighbors());
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Assert:
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("default"), IsEqual.equalTo(neighbors));
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("diff-member-ids"), IsNot.not(IsEqual.equalTo(neighbors)));
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("diff-less-member-ids"), IsNot.not(IsEqual.equalTo(neighbors)));
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("diff-more-member-ids"), IsNot.not(IsEqual.equalTo(neighbors)));
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("diff-empty"), IsNot.not(IsEqual.equalTo(neighbors)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(neighbors)));
		MatcherAssert.assertThat(NisUtils.toNodeIdList(2, 3, 6), IsNot.not(IsEqual.equalTo((Object) neighbors)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);
		final int hashCode = neighbors.hashCode();

		// Assert:
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("diff-member-ids").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("diff-less-member-ids").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("diff-more-member-ids").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_NEIGHBORS_MAP.get("diff-empty").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsCorrectRepresentation() {
		// Arrange:
		final NodeNeighbors neighbors = NisUtils.createNeighbors(2, 3, 6);

		// Act and Assert:
		MatcherAssert.assertThat(neighbors.toString(), IsEqual.equalTo("{2,3,6}"));
	}

	// endregion
}
