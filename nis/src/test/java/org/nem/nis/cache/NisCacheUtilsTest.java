package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.state.MosaicEntry;
import org.nem.nis.test.NisCacheFactory;
import org.nem.nis.validators.ValidationState;

import java.util.function.Function;

public class NisCacheUtilsTest {

	// region createValidationState

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
		MatcherAssert.assertThat(validationState.canDebit(account, Amount.fromNem(443)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(validationState.canDebit(account, Amount.fromNem(444)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(validationState.canDebit(account, Amount.fromNem(445)), IsEqual.equalTo(false));
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
		MatcherAssert.assertThat(validationState.canDebit(account, new Mosaic(mosaicId, new Quantity(443))), IsEqual.equalTo(true));
		MatcherAssert.assertThat(validationState.canDebit(account, new Mosaic(mosaicId, new Quantity(444))), IsEqual.equalTo(true));
		MatcherAssert.assertThat(validationState.canDebit(account, new Mosaic(mosaicId, new Quantity(445))), IsEqual.equalTo(false));
	}

	@Test
	public void validationStateDelegatesToCacheForTransactionExecutionState() {
		// Assert:
		AssertTransactionExecutionStateDelegatesToCache(
				nisCache -> NisCacheUtils.createValidationState(nisCache).transactionExecutionState());
	}

	// endregion

	// region createTransactionExecutionState

	@Test
	public void transactionExecutionStateDelegatesToCache() {
		// Assert:
		AssertTransactionExecutionStateDelegatesToCache(NisCacheUtils::createTransactionExecutionState);
	}

	private static void AssertTransactionExecutionStateDelegatesToCache(
			final Function<ReadOnlyNisCache, TransactionExecutionState> stateFactory) {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final ReadOnlyNisCache readOnlyNisCache = NisCacheFactory.createReal();

		final NisCache nisCache = readOnlyNisCache.copy();
		nisCache.getAccountCache().addAccountToCache(account.getAddress());

		final NamespaceId namespaceId = new NamespaceId("foo");
		final Account namespaceOwner = Utils.generateRandomAccount();
		nisCache.getNamespaceCache().add(new Namespace(namespaceId, namespaceOwner, BlockHeight.MAX));

		final MosaicId mosaicId = new MosaicId(namespaceId, "tokens");
		final MosaicLevy mosaicLevy = Utils.createMosaicLevy();
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(namespaceOwner, mosaicId, new MosaicDescriptor("awesome mosaic"),
				Utils.createMosaicProperties(), mosaicLevy);
		final MosaicEntry entry = nisCache.getNamespaceCache().get(namespaceId).getMosaics().add(mosaicDefinition);
		entry.getBalances().incrementBalance(account.getAddress(), new Quantity(444));
		nisCache.commit();

		// Act:
		final TransactionExecutionState state = NisCacheUtils.createTransactionExecutionState(nisCache);

		// Assert: the calculator only returns a levy for the mosaic registered above
		final Mosaic mosaicWithLevy = new Mosaic(mosaicId, Quantity.fromValue(123));
		final Mosaic mosaicWithoutLevy = new Mosaic(new MosaicId(namespaceId, "coupons"), Quantity.fromValue(123));
		final MosaicTransferFeeCalculator calculator = state.getMosaicTransferFeeCalculator();

		MatcherAssert.assertThat(calculator.calculateAbsoluteLevy(mosaicWithLevy), IsEqual.equalTo(mosaicLevy));
		MatcherAssert.assertThat(calculator.calculateAbsoluteLevy(mosaicWithoutLevy), IsEqual.equalTo(null));
	}

	// endregion
}
