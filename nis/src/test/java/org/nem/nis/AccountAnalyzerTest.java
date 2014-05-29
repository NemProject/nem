package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.poi.PoiImportanceGenerator;

import java.util.*;
import java.util.stream.*;

public class AccountAnalyzerTest {

	//region addAccountToCache

	@Test
	public void accountWithoutPublicKeyCanBeAddedToCache() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account account = analyzer.addAccountToCache(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(account, IsNull.notNullValue());
	}

	@Test
	public void accountWithPublicKeyCanBeAddedToCache() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account account = analyzer.addAccountToCache(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(account, IsNull.notNullValue());
	}

	@Test
	public void cachedAccountWithPublicKeyIsUnchangedWhenQueryingByPublicKey() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account cachedAccount1 = analyzer.addAccountToCache(address);
		final Account cachedAccount2 = analyzer.addAccountToCache(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(cachedAccount2, IsSame.sameInstance(cachedAccount1));
	}

	@Test
	public void cachedAccountWithoutPublicKeyIsUnchangedWhenQueryingByEncodedAddress() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account cachedAccount1 = analyzer.addAccountToCache(address);
		final Account cachedAccount2 = analyzer.addAccountToCache(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(cachedAccount2, IsSame.sameInstance(cachedAccount1));
	}

	@Test
	public void cachedAccountWithoutPublicKeyIsUpdatedWhenQueryingWithPublicKey() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Address addressWithoutPublicKey = Address.fromEncoded(address.getEncoded());

		// Act:
		final Account cachedAccount1 = analyzer.addAccountToCache(addressWithoutPublicKey);
		final Account cachedAccount2 = analyzer.addAccountToCache(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(cachedAccount2.getAddress(), IsEqual.equalTo(cachedAccount1.getAddress()));
		Assert.assertThat(cachedAccount2, IsNot.not(IsSame.sameInstance(cachedAccount1)));
	}

	@Test
	public void balanceIsPreservedWhenPublicKeyIsAddedToAccountWithNonZeroBalanceWithoutPublicKey() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Address addressWithoutPublicKey = Address.fromEncoded(address.getEncoded());

		// Act:
		final Account cachedAccount1 = analyzer.addAccountToCache(addressWithoutPublicKey);
		cachedAccount1.incrementBalance(new Amount(9527L));
		final Account cachedAccount2 = analyzer.addAccountToCache(address);

		// Assert:
		Assert.assertThat(cachedAccount2.getBalance(), IsEqual.equalTo(new Amount(9527L)));
	}

	//endregion

	//region findByAddress

	@Test(expected = MissingResourceException.class)
	public void findByAddressFailsIfAddressIsInvalid() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Address.fromPublicKey(Utils.generateRandomPublicKey());
		final String realAddress = address.getEncoded();
		final String fakeAddress = realAddress.substring(0, realAddress.length() - 1);

		// Act:
		analyzer.findByAddress(Address.fromEncoded(fakeAddress));
	}

	@Test
	public void findByAddressReturnsCachedAddressIfAvailable() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account cachedAccount1 = analyzer.addAccountToCache(address);
		final Account foundAddress = analyzer.findByAddress(address);

		// Assert:
		Assert.assertThat(foundAddress, IsSame.sameInstance(cachedAccount1));
	}

	@Test
	public void findByAddressReturnsNonCachedAddressIfPublicKeyIsNotFound() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account foundAccount = analyzer.findByAddress(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(0));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(foundAccount.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
	}

	@Test
	public void findByAddressReturnsNonCachedAddressIfEncodedAddressIsNotFound() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account foundAccount = analyzer.findByAddress(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(0));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(foundAccount.getAddress().getPublicKey(), IsNull.nullValue());
	}

	@Test
	public void findByAddressUpdatesAccountPublicKeyIfQueryingAccountHasPublicKeyButCachedAccountDoesNot() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final Address addressWithoutPublicKey = Address.fromEncoded(address.getEncoded());

		// Act:
		final Account cachedAccount1 = analyzer.addAccountToCache(addressWithoutPublicKey);
		final Account foundAccount = analyzer.findByAddress(address);

		// Assert:
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(cachedAccount1.getAddress()));
		Assert.assertThat(foundAccount, IsNot.not(IsSame.sameInstance(cachedAccount1)));
	}

	//endregion

	//region asAutoCache

	@Test
	public void asAutoCacheFindByAddressReturnsCachedAddressIfPublicKeyIsNotFound() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account foundAccount = analyzer.asAutoCache().findByAddress(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void asAutoCacheFindByAddressReturnsCachedAddressIfEncodedAddressIsNotFound() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account foundAccount = analyzer.asAutoCache().findByAddress(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
	}

	//endregion

	//region copy

	@Test
	public void copyCreatesUnlinkedAnalyzerCopy() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final AccountAnalyzer analyzer = new AccountAnalyzer(importanceGenerator);
		analyzer.recalculateImportances(new BlockHeight(1234));

		final ColumnVector finalImportanceVector = new ColumnVector(3, 6, 1);
		final ArgumentCaptor<Collection<Account>> argument = createAccountCollectionArgumentCaptor();
		Mockito.when(importanceGenerator.getAccountImportances(Mockito.eq(new BlockHeight(1234)), argument.capture()))
				.thenReturn(finalImportanceVector);

		final Account account1 = analyzer.addAccountToCache(address1);
		final Account account2 = analyzer.addAccountToCache(address2);
		final Account account3 = analyzer.addAccountToCache(address3);

		// Act:
		final AccountAnalyzer copyAnalyzer = analyzer.copy();

		final Account copyAccount1 = copyAnalyzer.findByAddress(address1);
		final Account copyAccount2 = copyAnalyzer.findByAddress(address2);
		final Account copyAccount3 = copyAnalyzer.findByAddress(address3);

		copyAnalyzer.recalculateImportances(new BlockHeight(1234));

		// Assert:
		Assert.assertThat(copyAnalyzer.size(), IsEqual.equalTo(3));
		Assert.assertThat(copyAccount1, IsNot.not(IsSame.sameInstance(account1)));
		Assert.assertThat(copyAccount2, IsNot.not(IsSame.sameInstance(account2)));
		Assert.assertThat(copyAccount3, IsNot.not(IsSame.sameInstance(account3)));
		Mockito.verify(importanceGenerator, Mockito.times(1)).getAccountImportances(Mockito.any(), Mockito.any());
	}

	@Test
	 public void copyReturnsSameAccountGivenPublicKeyOrAddress() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final AccountAnalyzer analyzer = createAccountAnalyzer();

		analyzer.addAccountToCache(address1);

		// Act:
		final AccountAnalyzer copyAnalyzer = analyzer.copy();

		final Account copyAccountFromEncoded = copyAnalyzer.findByAddress(Address.fromEncoded(address1.getEncoded()));
		final Account copyAccountFromPublicKey = copyAnalyzer.findByAddress(address1);

		// Assert:
		Assert.assertThat(copyAnalyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(copyAccountFromEncoded, IsSame.sameInstance(copyAccountFromPublicKey));
	}

	//endregion

	//region replace

	@Test
	public void shallowCopyToCreatesLinkedAnalyzerCopy() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();

		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final AccountAnalyzer analyzer = new AccountAnalyzer(importanceGenerator);
		analyzer.recalculateImportances(new BlockHeight(1234));

		final ColumnVector finalImportanceVector = new ColumnVector(3, 6, 1);
		final ArgumentCaptor<Collection<Account>> argument = createAccountCollectionArgumentCaptor();
		Mockito.when(importanceGenerator.getAccountImportances(Mockito.eq(new BlockHeight(1234)), argument.capture()))
				.thenReturn(finalImportanceVector);

		final Account account1 = analyzer.addAccountToCache(address1);
		final Account account2 = analyzer.addAccountToCache(address2);
		final Account account3 = analyzer.addAccountToCache(address3);

		// Act:
		final AccountAnalyzer copyAnalyzer = createAccountAnalyzer();
		analyzer.shallowCopyTo(copyAnalyzer);

		final Account copyAccount1 = copyAnalyzer.findByAddress(address1);
		final Account copyAccount2 = copyAnalyzer.findByAddress(address2);
		final Account copyAccount3 = copyAnalyzer.findByAddress(address3);

		copyAnalyzer.recalculateImportances(new BlockHeight(1234));

		// Assert:
		Assert.assertThat(copyAnalyzer.size(), IsEqual.equalTo(3));
		Assert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		Assert.assertThat(copyAccount2, IsSame.sameInstance(account2));
		Assert.assertThat(copyAccount3, IsSame.sameInstance(account3));
		Mockito.verify(importanceGenerator, Mockito.times(1)).getAccountImportances(Mockito.any(), Mockito.any());
	}

	@Test
	public void shallowCopyToRemovesAnyPreviouslyExistingEntries() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final AccountAnalyzer analyzer = createAccountAnalyzer();

		final Account account1 = analyzer.addAccountToCache(address1);

		final AccountAnalyzer copyAnalyzer = createAccountAnalyzer();
		final Account account2 = copyAnalyzer.addAccountToCache(address2);

		// Act:
		analyzer.shallowCopyTo(copyAnalyzer);

		final Account copyAccount1 = copyAnalyzer.findByAddress(address1);
		final Account copyAccount2 = copyAnalyzer.findByAddress(address2);

		// Assert:
		Assert.assertThat(copyAnalyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		Assert.assertThat(copyAccount2, IsNot.not(IsSame.sameInstance(account2)));
	}

	//endregion

	//region iterator

	@Test
	public void iteratorReturnsAllAccounts() {
		// Arrange:
		final AccountAnalyzer analyzer = createAccountAnalyzer();

		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < 3; ++i)
			accounts.add(analyzer.addAccountToCache(Utils.generateRandomAddress()));

		// Act:
		final List<Account> iteratedAccounts = StreamSupport.stream(analyzer.spliterator(), false)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(iteratedAccounts.size(), IsEqual.equalTo(3));
		Assert.assertThat(iteratedAccounts, IsEquivalent.equivalentTo(accounts));
	}

	//endregion

	//region recalculateImportances

	@Test
	public void recalculateImportancesDelegatesToImportanceGenerator() {
		// Arrange:
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final ColumnVector finalImportanceVector = new ColumnVector(3, 6, 1);
		final ArgumentCaptor<Collection<Account>> argument = createAccountCollectionArgumentCaptor();
		Mockito.when(importanceGenerator.getAccountImportances(Mockito.eq(new BlockHeight(7)), argument.capture()))
				.thenReturn(finalImportanceVector);

		final AccountAnalyzer analyzer = new AccountAnalyzer(importanceGenerator);
		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < finalImportanceVector.size(); ++i)
			accounts.add(analyzer.addAccountToCache(Utils.generateRandomAddress()));

		// Act:
		analyzer.recalculateImportances(new BlockHeight(7));

		// Assert: the generator was called once and passed a collection with three accounts
		Mockito.verify(importanceGenerator, Mockito.times(1)).getAccountImportances(Mockito.any(), Mockito.any());
		Assert.assertThat(argument.getValue().size(), IsEqual.equalTo(3));

		Assert.assertThat(
				importancesAsList(accounts, 7),
				IsEquivalent.equivalentTo(columnVectorAsList(finalImportanceVector)));
	}

	@Test
	public void recalculateImportancesDoesNotRecalculateImportancesForLastBlockHeight() {
		// Arrange:
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final AccountAnalyzer analyzer = new AccountAnalyzer(importanceGenerator);

		// Act:
		analyzer.recalculateImportances(new BlockHeight(7));
		analyzer.recalculateImportances(new BlockHeight(7));

		// Assert: the generator was only called once
		Mockito.verify(importanceGenerator, Mockito.times(1)).getAccountImportances(Mockito.any(), Mockito.any());
	}

	@Test
	public void recalculateImportancesRecalculatesImportancesForNewBlockHeight() {
		// Arrange:
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		Mockito.when(importanceGenerator.getAccountImportances(Mockito.eq(new BlockHeight(7)), Mockito.any()))
				.thenReturn(new ColumnVector(11, 13, 17));
		final ColumnVector finalImportanceVector = new ColumnVector(7, 10, 5);

		final ArgumentCaptor<Collection<Account>> argument = createAccountCollectionArgumentCaptor();
		Mockito.when(importanceGenerator.getAccountImportances(Mockito.eq(new BlockHeight(8)), argument.capture()))
				.thenReturn(finalImportanceVector);

		final AccountAnalyzer analyzer = new AccountAnalyzer(importanceGenerator);
		final List<Account> accounts = new ArrayList<>();
		for (int i = 0; i < finalImportanceVector.size(); ++i)
			accounts.add(analyzer.addAccountToCache(Utils.generateRandomAddress()));

		// Act:
		analyzer.recalculateImportances(new BlockHeight(7));
		analyzer.recalculateImportances(new BlockHeight(8));

		// Assert: the generator was called twice and passed a collection with three accounts
		Mockito.verify(importanceGenerator, Mockito.times(2)).getAccountImportances(Mockito.any(), Mockito.any());
		Assert.assertThat(argument.getValue().size(), IsEqual.equalTo(3));

		Assert.assertThat(
				importancesAsList(accounts, 8),
				IsEquivalent.equivalentTo(columnVectorAsList(finalImportanceVector)));
	}

	private List<Double> importancesAsList(final List<Account> accounts, final long blockHeight) {
		return accounts.stream()
				.map(Account::getImportanceInfo)
				.map(a -> a.getImportance(new BlockHeight(blockHeight))).collect(Collectors.toList());
	}

	private List<Double> columnVectorAsList(final ColumnVector vector) {
		final List<Double> list = new ArrayList<>();
		for (int i = 0; i < vector.size(); ++i)
			list.add(vector.getAt(i));

		return list;
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<Collection<Account>> createAccountCollectionArgumentCaptor() {
		return ArgumentCaptor.forClass((Class)Collection.class);
	}

	//endregion

	private static AccountAnalyzer createAccountAnalyzer() {
		return new AccountAnalyzer(Mockito.mock(PoiImportanceGenerator.class));
	}
}