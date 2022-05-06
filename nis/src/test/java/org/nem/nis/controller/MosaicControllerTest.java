package org.nem.nis.controller;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.MosaicIdSupplyPair;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.controller.requests.MosaicIdBuilder;
import org.nem.nis.test.MosaicTestContext;

import java.util.*;
import java.util.stream.*;

public class MosaicControllerTest {

	// getMosaicSupply

	@Test
	public void getMosaicSupplyReturnsExpectedPair() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicIdSupplyPair pair = context.controller.getMosaicSupply(context.getMosaicIdBuilder("id3:name3"));

		// Assert:
		MatcherAssert.assertThat(pair.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(3)));
		MatcherAssert.assertThat(pair.getSupply(), IsEqual.equalTo(Supply.fromValue(300)));
		context.assertNamespaceCacheGetDelegation("id3", true);
	}

	@Test
	public void cannotGetMosaicSupplyForUnknownMosaicId() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		ExceptionAssert.assertThrows(v -> context.controller.getMosaicSupply(context.getMosaicIdBuilder("foo:bar")),
				MissingResourceException.class);
		context.assertNamespaceCacheGetDelegation("foo", false);
	}

	// endregion

	// getMosaicSupplyBatch

	@Test
	public void getMosaicSupplyBatchReturnsExpectedPairs() {
		// Assert:
		assertGetMosaicSupplyBatchReturnsExpectedPairs(new int[]{
				1, 5, 6, 9
		});
	}

	@Test
	public void getMosaicSupplyBatchReturnsExpectedPairsAndCollapsesDuplicateIds() {
		// Assert:
		assertGetMosaicSupplyBatchReturnsExpectedPairs(new int[]{
				1, 5, 5, 9, 6, 9
		});
	}

	private static void assertGetMosaicSupplyBatchReturnsExpectedPairs(final int[] requestIds) {
		// Arrange:
		final TestContext context = new TestContext();
		final SerializableList<MosaicId> list = new SerializableList<>(getMosaicIds(requestIds));
		final JsonDeserializer deserializer = new JsonDeserializer(JsonSerializer.serializeToJson(list), null);

		// Act:
		final Collection<MosaicIdSupplyPair> pairs = context.controller.getMosaicSupplyBatch(deserializer).asCollection();
		final Collection<MosaicIdSupplyPair> expectedPairs = Arrays.stream(requestIds)
				.mapToObj(i -> new MosaicIdSupplyPair(Utils.createMosaicId(i), Supply.fromValue(100 * i))).collect(Collectors.toSet());

		// Assert:
		MatcherAssert.assertThat(pairs.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(pairs, IsEquivalent.equivalentTo(expectedPairs));
		Arrays.stream(requestIds).forEach(id -> context.assertNamespaceCacheGetDelegation(String.format("id%d", id), true));
		context.assertNamespaceCacheNumGetDelegations(4);
	}

	@Test
	public void cannotGetMosaicSupplyBatchIfListContainsAtLeastOneUnknownMosaicId() {
		// Arrange:
		final TestContext context = new TestContext();
		final int[] requestIds = new int[]{
				1, 123, 6, 9
		};
		final SerializableList<MosaicId> list = new SerializableList<>(getMosaicIds(requestIds));
		final JsonDeserializer deserializer = new JsonDeserializer(JsonSerializer.serializeToJson(list), null);

		// Act:
		ExceptionAssert.assertThrows(v -> context.controller.getMosaicSupplyBatch(deserializer), MissingResourceException.class);

		// Assert:
		context.assertNamespaceCacheNumGetDelegations(1);
	}

	// endregion

	private static class TestContext extends MosaicTestContext {
		private final MosaicController controller;

		public TestContext() {
			this.controller = new MosaicController(this.namespaceCache);
			final List<MosaicId> mosaicIds = IntStream.range(0, 10).mapToObj(i -> this.createMosaicId(i, 100L * i))
					.collect(Collectors.toList());
			this.prepareMosaics(mosaicIds);
		}

		public MosaicIdBuilder getMosaicIdBuilder(final String mosaicId) {
			final MosaicIdBuilder builder = new MosaicIdBuilder();
			builder.setMosaicId(mosaicId);
			return builder;
		}

		public void assertNamespaceCacheGetDelegation(final String id, final boolean isInitialized) {
			// if the id is initialized, an extra call was made by prepareMosaics in the constructor
			Mockito.verify(this.namespaceCache, Mockito.times(isInitialized ? 2 : 1)).get(new NamespaceId(id));
		}

		public void assertNamespaceCacheNumGetDelegations(final int count) {
			// 10 get calls were made by prepareMosaics in the constructor
			Mockito.verify(this.namespaceCache, Mockito.times(10 + count)).get(Mockito.any());
		}
	}

	private static Collection<MosaicId> getMosaicIds(final int... ids) {
		return Arrays.stream(ids).mapToObj(Utils::createMosaicId).collect(Collectors.toList());
	}
}
