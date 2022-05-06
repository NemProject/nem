package org.nem.nis.sync;

import org.hamcrest.MatcherAssert;
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
	public void canDebitEvaluatesQuantityAgainstBalancesInMosaicEntry() {
		// Arrange:
		final NamespaceCache namespaceCache = createNamespaceCache();
		final Account account = addAccountWithMosaicBalance(namespaceCache, Utils.createMosaicId(5), Quantity.fromValue(123));

		// Act:
		final DebitPredicate<Mosaic> debitPredicate = new DefaultMosaicDebitPredicate(namespaceCache);

		// Assert:
		MatcherAssert.assertThat(debitPredicate.canDebit(account, createMosaic(5, 122)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(debitPredicate.canDebit(account, createMosaic(5, 123)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(debitPredicate.canDebit(account, createMosaic(5, 124)), IsEqual.equalTo(false));
	}

	@Test
	public void canDebitReturnsCorrectResultWhenMosaicAccountBalanceIsZero() {
		// Arrange:
		final NamespaceCache namespaceCache = createNamespaceCache();
		final Account account = addAccountWithMosaicBalance(namespaceCache, Utils.createMosaicId(5), Quantity.fromValue(123));

		// Act:
		final DebitPredicate<Mosaic> debitPredicate = new DefaultMosaicDebitPredicate(namespaceCache);

		// Assert:
		MatcherAssert.assertThat(debitPredicate.canDebit(account, createMosaic(4, 0)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(debitPredicate.canDebit(account, createMosaic(4, 1)), IsEqual.equalTo(false));
	}

	private static Account addAccountWithMosaicBalance(final NamespaceCache namespaceCache, final MosaicId mosaicId,
			final Quantity balance) {
		final Account namespaceOwner = Utils.generateRandomAccount();
		final Namespace namespace = new Namespace(mosaicId.getNamespaceId(), namespaceOwner, BlockHeight.ONE);
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(namespaceOwner, mosaicId, Utils.createMosaicProperties());
		namespaceCache.add(namespace);
		final NamespaceEntry namespaceEntry = namespaceCache.get(namespace.getId());
		final MosaicEntry mosaicEntry = namespaceEntry.getMosaics().add(mosaicDefinition);
		mosaicEntry.increaseSupply(new Supply(1000));

		final Account otherAccount = Utils.generateRandomAccount();
		mosaicEntry.getBalances().incrementBalance(otherAccount.getAddress(), balance);
		return otherAccount;
	}

	private static Mosaic createMosaic(final int id, final long quantity) {
		return new Mosaic(Utils.createMosaicId(id), Quantity.fromValue(quantity));
	}

	private static NamespaceCache createNamespaceCache() {
		return new DefaultNamespaceCache().copy();
	}
}
