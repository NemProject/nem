package org.nem.nis.sync;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.DebitPredicate;

public class DefaultMosaicDebitPredicateTest {

	@Test
	public void getDebitPredicateEvaluatesAmountAgainstBalancesInAccountState() {
		// Arrange:
		final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		final Account account = addAccountWithMosaicBalance(namespaceCache, Utils.createMosaicId(5), Supply.fromValue(123));

		// Act:
		final DebitPredicate<Mosaic> debitPredicate = new DefaultMosaicDebitPredicate(namespaceCache);

		// Assert (mosaic has divisibility of 3):
		Assert.assertThat(debitPredicate.canDebit(account, createMosaic(5, 122999)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account, createMosaic(5, 123000)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account, createMosaic(5, 123001)), IsEqual.equalTo(false));
	}

	private static Account addAccountWithMosaicBalance(final NamespaceCache namespaceCache, final MosaicId mosaicId, final Supply supply) {
		final Account account = Utils.generateRandomAccount();
		final Namespace namespace = new Namespace(mosaicId.getNamespaceId(), account, BlockHeight.ONE);
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(
				account,
				mosaicId,
				Utils.createMosaicProperties());
		namespaceCache.add(namespace);
		final NamespaceEntry namespaceEntry = namespaceCache.get(namespace.getId());
		final MosaicEntry mosaicEntry = namespaceEntry.getMosaics().add(mosaicDefinition);
		mosaicEntry.increaseSupply(supply);
		return account;
	}

	private static Mosaic createMosaic(final int id, final long quantity) {
		return new Mosaic(Utils.createMosaicId(id), Quantity.fromValue(quantity));
	}
}
