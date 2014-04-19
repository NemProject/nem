package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;

import java.util.*;

public class AccountAnalyzerTest {

	//region addAccountToCache

	@Test
	public void accountWithoutPublicKeyCanBeAddedToCache() {
		// Arrange:
		final AccountAnalyzer analyzer = new AccountAnalyzer();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account account = analyzer.addAccountToCache(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(account, IsNot.not(IsEqual.equalTo(null)));
	}

	@Test
	public void accountWithPublicKeyCanBeAddedToCache() {
		// Arrange:
		final AccountAnalyzer analyzer = new AccountAnalyzer();
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final Account account = analyzer.addAccountToCache(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(1));
		Assert.assertThat(account, IsNot.not(IsEqual.equalTo(null)));
	}

	@Test
	public void cachedAccountWithPublicKeyIsUnchangedWhenQueryingByPublicKey() {
		// Arrange:
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();
		final Address address = Address.fromPublicKey(Utils.generateRandomPublicKey());
		final String realAddress = address.getEncoded();
		final String fakeAddress = realAddress.substring(0, realAddress.length() - 1);

		// Act:
		analyzer.findByAddress(Address.fromEncoded(fakeAddress));
	}

	@Test
	public void findByAddressReturnsCachedAddressIfAvailable() {
		// Arrange:
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();
		final Address address = Utils.generateRandomAddress();

		// Act:
		final Account foundAccount = analyzer.findByAddress(address);

		// Assert:
		Assert.assertThat(analyzer.size(), IsEqual.equalTo(0));
		Assert.assertThat(foundAccount.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(foundAccount.getAddress().getPublicKey(), IsEqual.equalTo(null));
	}

	@Test
	public void findByAddressUpdatesAccountPublicKeyIfQueryingAccountHasPublicKeyButCachedAccountDoesNot() {
		// Arrange:
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();
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
		final AccountAnalyzer analyzer = new AccountAnalyzer();

		final Account account1 = analyzer.addAccountToCache(address1);
		final Account account2 = analyzer.addAccountToCache(address2);
		final Account account3 = analyzer.addAccountToCache(address3);

		// Act:
		final AccountAnalyzer copyAnalyzer = analyzer.copy();

		final Account copyAccount1 = copyAnalyzer.findByAddress(address1);
		final Account copyAccount2 = copyAnalyzer.findByAddress(address2);
		final Account copyAccount3 = copyAnalyzer.findByAddress(address3);

		// Assert:
		Assert.assertThat(copyAnalyzer.size(), IsEqual.equalTo(3));
		Assert.assertThat(copyAccount1, IsNot.not(IsSame.sameInstance(account1)));
		Assert.assertThat(copyAccount2, IsNot.not(IsSame.sameInstance(account2)));
		Assert.assertThat(copyAccount3, IsNot.not(IsSame.sameInstance(account3)));
	}

	@Test
	 public void copyReturnsSameAccountGivenPublicKeyOrAddress() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final AccountAnalyzer analyzer = new AccountAnalyzer();

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
		final AccountAnalyzer analyzer = new AccountAnalyzer();

		final Account account1 = analyzer.addAccountToCache(address1);
		final Account account2 = analyzer.addAccountToCache(address2);
		final Account account3 = analyzer.addAccountToCache(address3);

		// Act:
		final AccountAnalyzer copyAnalyzer = new AccountAnalyzer();
		analyzer.shallowCopyTo(copyAnalyzer);

		final Account copyAccount1 = copyAnalyzer.findByAddress(address1);
		final Account copyAccount2 = copyAnalyzer.findByAddress(address2);
		final Account copyAccount3 = copyAnalyzer.findByAddress(address3);

		// Assert:
		Assert.assertThat(copyAnalyzer.size(), IsEqual.equalTo(3));
		Assert.assertThat(copyAccount1, IsSame.sameInstance(account1));
		Assert.assertThat(copyAccount2, IsSame.sameInstance(account2));
		Assert.assertThat(copyAccount3, IsSame.sameInstance(account3));
	}

	@Test
	public void shallowCopyToRemovesAnyPreviouslyExistingEntries() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final AccountAnalyzer analyzer = new AccountAnalyzer();

		final Account account1 = analyzer.addAccountToCache(address1);

		final AccountAnalyzer copyAnalyzer = new AccountAnalyzer();
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
}