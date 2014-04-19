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
}