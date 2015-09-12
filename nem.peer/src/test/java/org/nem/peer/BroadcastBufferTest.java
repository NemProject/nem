package org.nem.peer;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.BroadcastableEntityList;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.NisPeerId;
import org.nem.core.node.NodeEndpoint;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.IsEquivalent;

import java.util.*;

public class BroadcastBufferTest {

	// region ctor

	@Test
	public void bufferIsInitiallyEmpty() {
		// Act:
		final BroadcastBuffer buffer = new BroadcastBuffer();

		// Assert:
		Assert.assertThat(buffer.size(), IsEqual.equalTo(0));
		Assert.assertThat(buffer.deepSize(), IsEqual.equalTo(0));
	}

	// endregion

	// region size / deepSize

	@Test
	public void deepSizeReturnsExpectedValue() {
		// Arrange:
		final BroadcastBuffer buffer = new BroadcastBuffer();

		// Act:
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(234));
		buffer.add(NisPeerId.REST_CHAIN_HASHES_FROM, new BlockHeight(345));

		// Assert:
		Assert.assertThat(buffer.size(), IsEqual.equalTo(2));
		Assert.assertThat(buffer.deepSize(), IsEqual.equalTo(3 + 1));
	}

	// endregion

	// region add

	@Test
	public void canAddEntityToEmptyBuffer() {
		// Arrange:
		final Map<NisPeerId, Collection<SerializableEntity>> map = new HashMap<>();
		final BroadcastBuffer buffer = new BroadcastBuffer(map);

		// Act:
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));

		// Assert:
		Assert.assertThat(buffer.size(), IsEqual.equalTo(1));
		Assert.assertThat(buffer.deepSize(), IsEqual.equalTo(1));
		Assert.assertThat(map.get(NisPeerId.REST_BLOCK_AT), IsEqual.equalTo(Collections.singletonList(new BlockHeight(123))));
	}

	@Test
	public void canAddMultipleEntitiesWithSameApiIds() {
		// Arrange:
		final Map<NisPeerId, Collection<SerializableEntity>> map = new HashMap<>();
		final BroadcastBuffer buffer = new BroadcastBuffer(map);

		// Act:
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(234));

		// Assert:
		Assert.assertThat(buffer.size(), IsEqual.equalTo(1));
		Assert.assertThat(buffer.deepSize(), IsEqual.equalTo(3));
		Assert.assertThat(
				map.get(NisPeerId.REST_BLOCK_AT),
				IsEquivalent.equivalentTo(Arrays.asList(new BlockHeight(123), new BlockHeight(123), new BlockHeight(234))));
	}

	@Test
	public void canAddMultipleEntitiesWithDifferentApiIds() {
		// Arrange:
		final Map<NisPeerId, Collection<SerializableEntity>> map = new HashMap<>();
		final BroadcastBuffer buffer = new BroadcastBuffer(map);

		// Act:
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(234));
		buffer.add(NisPeerId.REST_NODE_CAN_YOU_SEE_ME, new NodeEndpoint("http", "127.0.0.1", 1234));
		buffer.add(NisPeerId.REST_CHAIN_HASHES_FROM, new BlockHeight(345));

		// Assert:
		Assert.assertThat(buffer.size(), IsEqual.equalTo(3));
		Assert.assertThat(buffer.deepSize(), IsEqual.equalTo(4));
		Assert.assertThat(
				map.get(NisPeerId.REST_BLOCK_AT),
				IsEquivalent.equivalentTo(Arrays.asList(new BlockHeight(123), new BlockHeight(234))));
		Assert.assertThat(
				map.get(NisPeerId.REST_NODE_CAN_YOU_SEE_ME),
				IsEqual.equalTo(Collections.singletonList(new NodeEndpoint("http", "127.0.0.1", 1234))));
		Assert.assertThat(
				map.get(NisPeerId.REST_CHAIN_HASHES_FROM),
				IsEqual.equalTo(Collections.singletonList(new BlockHeight(345))));
	}

	// endregion

	// region getAllPairsAndClearMap

	@Test
	public void getAllPairsAndClearMapReturnsAllPairsAndClearsMap() {
		// Arrange:
		final BroadcastBuffer buffer = new BroadcastBuffer();
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(123));
		buffer.add(NisPeerId.REST_BLOCK_AT, new BlockHeight(234));
		buffer.add(NisPeerId.REST_CHAIN_HASHES_FROM, new BlockHeight(345));
		final Collection<BroadcastableEntityList> expectedPairs = Arrays.asList(
				new BroadcastableEntityList(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(123), new BlockHeight(234))),
				new BroadcastableEntityList(NisPeerId.REST_CHAIN_HASHES_FROM, createList(new BlockHeight(345))));

		// Act:
		final Collection<BroadcastableEntityList> pairs = buffer.getAllPairsAndClearMap();

		// Assert:
		Assert.assertThat(buffer.size(), IsEqual.equalTo(0));
		Assert.assertThat(buffer.deepSize(), IsEqual.equalTo(0));
		Assert.assertThat(pairs.size(), IsEqual.equalTo(2));
		Assert.assertThat(pairs, IsEquivalent.equivalentTo(expectedPairs));
	}

	// endregion

	private static SerializableList<SerializableEntity> createList(final SerializableEntity... entities) {
		final SerializableList<SerializableEntity> list = new SerializableList<>(entities.length);
		Arrays.stream(entities).forEach(list::add);
		return list;
	}
}
