package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.NisCacheFactory;
import org.nem.nis.validators.ValidationState;

public class NisCacheUtilsTest {

	//region createValidationState

	@Test
	public void validationStateDelegatesToCacheForCanDebitXem() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final ReadOnlyNisCache readOnlyNisCache = NisCacheFactory.createReal();

		final NisCache nisCache = readOnlyNisCache.copy();
		nisCache.getAccountCache().addAccountToCache(account.getAddress());
		nisCache.getAccountStateCache().findStateByAddress(account.getAddress()).getAccountInfo().incrementBalance(Amount.fromNem(444));
		nisCache.commit();

		// Act:
		final ValidationState validationState = NisCacheUtils.createValidationState(nisCache);

		// Assert:
		Assert.assertThat(validationState.canDebit(account, Amount.fromNem(443)), IsEqual.equalTo(true));
		Assert.assertThat(validationState.canDebit(account, Amount.fromNem(444)), IsEqual.equalTo(true));
		Assert.assertThat(validationState.canDebit(account, Amount.fromNem(445)), IsEqual.equalTo(false));
	}

	@Test
	public void validationStateDelegatesToCacheForCanDebitMosaic() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final ReadOnlyNisCache readOnlyNisCache = NisCacheFactory.createReal();

		final NisCache nisCache = readOnlyNisCache.copy();
		nisCache.getAccountCache().addAccountToCache(account.getAddress());

		final NamespaceId namespaceId = new NamespaceId("foo");
		final Account namespaceOwner = Utils.generateRandomAccount();
		nisCache.getNamespaceCache().add(new Namespace(namespaceId, namespaceOwner, BlockHeight.MAX));

		final MosaicId mosaicId = new MosaicId(namespaceId, "tokens");
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(namespaceOwner, mosaicId, Utils.createMosaicProperties());
		final MosaicEntry entry = nisCache.getNamespaceCache().get(namespaceId).getMosaics().add(mosaicDefinition);
		entry.getBalances().incrementBalance(account.getAddress(), new Quantity(444));
		nisCache.commit();

		// Act:
		final ValidationState validationState = NisCacheUtils.createValidationState(nisCache);

		// Assert:
		Assert.assertThat(validationState.canDebit(account, new Mosaic(mosaicId, new Quantity(443))), IsEqual.equalTo(true));
		Assert.assertThat(validationState.canDebit(account, new Mosaic(mosaicId, new Quantity(444))), IsEqual.equalTo(true));
		Assert.assertThat(validationState.canDebit(account, new Mosaic(mosaicId, new Quantity(445))), IsEqual.equalTo(false));
	}

	//endregion
}