package org.nem.peer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.NisPeerId;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class NisPeerIdAndEntityListPairTest {

	// region ctor

	@Test
	public void canCreatePair() {
		// Act:
		final SerializableList<BlockHeight> entities = new SerializableList<>(Collections.singletonList(new BlockHeight(123)));
		final NisPeerIdAndEntityListPair pair = new NisPeerIdAndEntityListPair(NisPeerId.REST_BLOCK_AT, entities);

		// Assert:
		MatcherAssert.assertThat(pair.getApiId(), IsEqual.equalTo(NisPeerId.REST_BLOCK_AT));
		MatcherAssert.assertThat(pair.getEntities().size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(pair.getEntities().get(0), IsEqual.equalTo(new BlockHeight(123)));
	}

	@Test
	public void cannotCreatePairWithInvalidParameters() {
		// Act:
		final SerializableList<BlockHeight> entities = new SerializableList<>(Collections.singletonList(new BlockHeight(123)));
		ExceptionAssert.assertThrows(v -> new NisPeerIdAndEntityListPair(null, entities), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new NisPeerIdAndEntityListPair(NisPeerId.REST_BLOCK_AT, null), IllegalArgumentException.class);
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NisPeerIdAndEntityListPair pair = new NisPeerIdAndEntityListPair(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(123)));

		// Assert:
		for (final Map.Entry<String, NisPeerIdAndEntityListPair> entry : createBroadcastableEntityListForEqualityTests().entrySet()) {
			MatcherAssert.assertThat(entry.getValue(),
					!entry.getKey().equals("default") ? IsNot.not(IsEqual.equalTo(pair)) : IsEqual.equalTo(pair));
		}

		MatcherAssert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(pair)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(pair)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new NisPeerIdAndEntityListPair(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(123))).hashCode();

		// Assert:
		for (final Map.Entry<String, NisPeerIdAndEntityListPair> entry : createBroadcastableEntityListForEqualityTests().entrySet()) {
			MatcherAssert.assertThat(entry.getValue().hashCode(),
					!entry.getKey().equals("default") ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	@SuppressWarnings("serial")
	private static Map<String, NisPeerIdAndEntityListPair> createBroadcastableEntityListForEqualityTests() {
		return new HashMap<String, NisPeerIdAndEntityListPair>() {
			{
				this.put("default", new NisPeerIdAndEntityListPair(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(123))));
				this.put("diff-api-id", new NisPeerIdAndEntityListPair(NisPeerId.REST_CHAIN_SCORE, createList(new BlockHeight(123))));
				this.put("diff-entity-type",
						new NisPeerIdAndEntityListPair(NisPeerId.REST_BLOCK_AT, createList(NodeUtils.createNodeWithName("Alice"))));
				this.put("diff-entity-values", new NisPeerIdAndEntityListPair(NisPeerId.REST_BLOCK_AT, createList(new BlockHeight(234))));
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
