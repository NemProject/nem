package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.NodeUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BroadcastableEntityListTest {

	// region ctor

	@Test
	public void canCreateBroadcastablePair() {
		// Act:
		final SerializableList<BlockHeight> entities = new SerializableList<>(Collections.singletonList(new BlockHeight(123)));
		final BroadcastableEntityList list = new BroadcastableEntityList(NisPeerId.REST_BLOCK_AT, entities);

		// Assert:
		Assert.assertThat(list.getApiId(), IsEqual.equalTo(NisPeerId.REST_BLOCK_AT));
		Assert.assertThat(list.getEntities().size(), IsEqual.equalTo(1));
		Assert.assertThat(list.getEntities().get(0), IsEqual.equalTo(new BlockHeight(123)));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final BroadcastableEntityList list =  new BroadcastableEntityList(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(123)));

		// Assert:
		for (final Map.Entry<String, BroadcastableEntityList> entry : createBroadcastableEntityListForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					!entry.getKey().equals("default") ? IsNot.not(IsEqual.equalTo(list)) : IsEqual.equalTo(list));
		}

		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(list)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(list)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode =  new BroadcastableEntityList(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(123))).hashCode();

		// Assert:
		for (final Map.Entry<String, BroadcastableEntityList> entry : createBroadcastableEntityListForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					!entry.getKey().equals("default") ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static Map<String, BroadcastableEntityList> createBroadcastableEntityListForEqualityTests() {
		return new HashMap<String, BroadcastableEntityList>() {
			{
				this.put("default", new BroadcastableEntityList(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(123))));
				this.put("diff-api-id", new BroadcastableEntityList(NisPeerId.REST_CHAIN_SCORE, createList(new BlockHeight(123))));
				this.put("diff-entity-type", new BroadcastableEntityList(NisPeerId.REST_BLOCK_AT, createList(NodeUtils.createNodeWithName("Alice"))));
				this.put("diff-entity-values", new BroadcastableEntityList(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(234))));
			}
		};
	}

	private static SerializableList<SerializableEntity> createList(final SerializableEntity... entities) {
		final SerializableList<SerializableEntity> list = new SerializableList<>(entities.length);
		Arrays.stream(entities).forEach(list::add);
		return list;
	}

	// endregion
}
