package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class NamespaceCacheLookupAdaptersTest {

	// region asMosaicFeeInformationLookup

	@Test
	public void asMosaicFeeInformationLookupFindByIdReturnsNullIfNamespaceIdIsNotInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicFeeInformation information = context.adapters.asMosaicFeeInformationLookup()
				.findById(Utils.createMosaicId("bar", "coins"));

		// Assert:
		MatcherAssert.assertThat(information, IsNull.nullValue());
	}

	@Test
	public void asMosaicFeeInformationLookupFindByIdReturnsNullIfMosaicIdIsNotInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicFeeInformation information = context.adapters.asMosaicFeeInformationLookup()
				.findById(Utils.createMosaicId("foo", "tokens"));

		// Assert:
		MatcherAssert.assertThat(information, IsNull.nullValue());
	}

	@Test
	public void asMosaicFeeInformationLookupFindByIdReturnsFeeInformationIfMosaicIdIsInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicFeeInformation information = context.adapters.asMosaicFeeInformationLookup()
				.findById(Utils.createMosaicId("foo", "coins"));

		// Assert:
		MatcherAssert.assertThat(information, IsNull.notNullValue());
		MatcherAssert.assertThat(information.getSupply(), IsEqual.equalTo(new Supply(1133)));
		MatcherAssert.assertThat(information.getDivisibility(), IsEqual.equalTo(4));
	}

	// endregion

	// region asMosaicLevyLookup

	@Test
	public void asMosaicLevyLookupFindByIdReturnsNullIfNamespaceIdIsNotInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicLevy levy = context.adapters.asMosaicLevyLookup().findById(Utils.createMosaicId("bar", "coins"));

		// Assert:
		MatcherAssert.assertThat(levy, IsNull.nullValue());
	}

	@Test
	public void asMosaicLevyLookupFindByIdReturnsNullIfMosaicIdIsNotInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicLevy levy = context.adapters.asMosaicLevyLookup().findById(Utils.createMosaicId("foo", "tokens"));

		// Assert:
		MatcherAssert.assertThat(levy, IsNull.nullValue());
	}

	@Test
	public void asMosaicLevyLookupFindByIdReturnsLevyInformationIfMosaicIdIsInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicLevy levy = context.adapters.asMosaicLevyLookup().findById(Utils.createMosaicId("foo", "coins"));

		// Assert:
		MatcherAssert.assertThat(levy, IsNull.notNullValue());
		MatcherAssert.assertThat(levy, IsEqual.equalTo(context.levy));
	}

	// endregion

	private static class TestContext {
		private final NamespaceCache cache = new DefaultNamespaceCache().copy();
		private final NamespaceCacheLookupAdapters adapters = new NamespaceCacheLookupAdapters(this.cache);
		private final MosaicLevy levy = Utils.createMosaicLevy();

		public TestContext() {
			final NamespaceId namespaceId = new NamespaceId("foo");
			final Account namespaceOwner = Utils.generateRandomAccount();
			this.cache.add(new Namespace(namespaceId, namespaceOwner, BlockHeight.ONE));

			final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(namespaceOwner, Utils.createMosaicId("foo", "coins"),
					Utils.createMosaicProperties(1111L, 4, null, null), this.levy);
			this.cache.get(namespaceId).getMosaics().add(mosaicDefinition).increaseSupply(new Supply(22));
		}
	}
}
