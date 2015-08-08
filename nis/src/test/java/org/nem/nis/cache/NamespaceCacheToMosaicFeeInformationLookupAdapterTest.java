package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;

public class NamespaceCacheToMosaicFeeInformationLookupAdapterTest {

	@Test
	public void findByIdReturnsNullIfNamespaceIdIsNotInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicFeeInformation information = context.lookup.findById(Utils.createMosaicId("bar", "coins"));

		// Assert:
		Assert.assertThat(information, IsNull.nullValue());
	}

	@Test
	public void findByIdReturnsNullIfMosaicIdIsNotInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicFeeInformation information = context.lookup.findById(Utils.createMosaicId("foo", "tokens"));

		// Assert:
		Assert.assertThat(information, IsNull.nullValue());
	}

	@Test
	public void findByIdReturnsFeeInformationIfMosaicIdIsInCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicFeeInformation information = context.lookup.findById(Utils.createMosaicId("foo", "coins"));

		// Assert:
		Assert.assertThat(information, IsNull.notNullValue());
		Assert.assertThat(information.getSupply(), IsEqual.equalTo(new Supply(1133)));
		Assert.assertThat(information.getDivisibility(), IsEqual.equalTo(4));
		Assert.assertThat(information.getTransferFeeInfo(), IsEqual.equalTo(context.feeInfo));
	}

	private static class TestContext {
		private final NamespaceCache cache = new DefaultNamespaceCache();
		private final MosaicFeeInformationLookup lookup = new NamespaceCacheToMosaicFeeInformationLookupAdapter(this.cache);
		private final MosaicLevy feeInfo = Utils.createMosaicLevy();

		public TestContext() {
			final NamespaceId namespaceId = new NamespaceId("foo");
			final Account namespaceOwner = Utils.generateRandomAccount();
			this.cache.add(new Namespace(namespaceId, namespaceOwner, BlockHeight.ONE));

			final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(
					namespaceOwner,
					Utils.createMosaicId("foo", "coins"),
					Utils.createMosaicProperties(1111L, 4, null, null),
					this.feeInfo);
			this.cache.get(namespaceId).getMosaics().add(mosaicDefinition).increaseSupply(new Supply(22));
		}
	}
}