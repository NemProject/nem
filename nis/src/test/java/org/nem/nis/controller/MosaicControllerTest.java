package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.controller.viewmodels.MosaicIdSupplyPair;
import org.nem.nis.test.MosaicTestContext;

import java.util.*;
import java.util.stream.*;

public class MosaicControllerTest {

	// getMosaicSupply

	@Test
	public void getMosaicSupplyDelegatesToNamespaceCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.controller.getMosaicSupply(context.getMosaicIdBuilder("id3 * name3"));

		// Assert (one call to get during prepareMosaics):
		Mockito.verify(context.namespaceCache, Mockito.times(1 + 1)).get(new NamespaceId("id3"));
	}

	@Test
	public void getMosaicSupplyReturnsExpectedPair() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicIdSupplyPair pair = context.controller.getMosaicSupply(context.getMosaicIdBuilder("id3 * name3"));

		// Assert:
		Assert.assertThat(pair.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(3)));
		Assert.assertThat(pair.getSupply(), IsEqual.equalTo(Supply.fromValue(300)));
	}

	@Test
	public void cannotGetMosaicSupplyForUnknownMosaicId() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		ExceptionAssert.assertThrows(v -> context.controller.getMosaicSupply(context.getMosaicIdBuilder("foo * bar")), MissingResourceException.class);
	}

	// endregion

	// getMosaicSupplyBatch

	@Test
	public void getMosaicSupplyBatchDelegatesToNamespaceCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final SerializableList<MosaicId> list = new SerializableList<>(context.mosaicDefinitions.keySet());
		final JsonDeserializer deserializer = new JsonDeserializer(JsonSerializer.serializeToJson(list), null);

		// Act:
		context.controller.getMosaicSupplyBatch(deserializer);

		// Assert (10 calls to get during prepareMosaics):
		context.mosaicDefinitions.keySet().forEach(m -> Mockito.verify(context.namespaceCache, Mockito.times(1 + 1)).get(m.getNamespaceId()));
	}

	@Test
	public void getMosaicSupplyBatchReturnsExpectedPairs() {
		// Arrange:
		final TestContext context = new TestContext();
		final SerializableList<MosaicId> list = new SerializableList<>(context.mosaicDefinitions.keySet());
		final JsonDeserializer deserializer = new JsonDeserializer(JsonSerializer.serializeToJson(list), null);

		// Act:
		final Collection<MosaicIdSupplyPair> pairs = context.controller.getMosaicSupplyBatch(deserializer).asCollection();
		final Collection<MosaicIdSupplyPair> expectedPairs = IntStream.range(0, 10)
				.mapToObj(i -> new MosaicIdSupplyPair(Utils.createMosaicId(i), Supply.fromValue(100 * i)))
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(pairs, IsEquivalent.equivalentTo(expectedPairs));
	}

	@Test
	public void cannotGetMosaicSupplyBatchIfListContainsAtLeastOneUnknownMosaicId() {
		// Arrange:
		final TestContext context = new TestContext();
		final SerializableList<MosaicId> list = new SerializableList<>(context.mosaicDefinitions.keySet());
		list.add(Utils.createMosaicId(123));
		final JsonDeserializer deserializer = new JsonDeserializer(JsonSerializer.serializeToJson(list), null);

		// Act:
		ExceptionAssert.assertThrows(v -> context.controller.getMosaicSupplyBatch(deserializer), MissingResourceException.class);
	}

	// endregion

	private static class TestContext extends MosaicTestContext {
		private final MosaicController controller;

		public TestContext() {
			this.controller = new MosaicController(this.namespaceCache);
			final List<MosaicId> mosaicIds = IntStream.range(0, 10)
					.mapToObj(i -> this.createMosaicId(i, 100L * i))
					.collect(Collectors.toList());
			this.prepareMosaics(mosaicIds);
		}
	}
}
