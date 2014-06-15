package org.nem.nis.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AccountDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	AccountDao accountDao;

	public static String NEMESIS_ADDRESS = "TBERUJIKSAPW54YISFOJZ2PLG3E7CACCNP3PP3P6";

	@Test
	public void canSaveAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		org.nem.nis.dbmodel.Account entity = new org.nem.nis.dbmodel.Account(account.getAddress().getEncoded(), account.getAddress().getPublicKey());

		// Act:
		accountDao.save(entity);

		// Assert:
		Assert.assertThat(entity.getId(), not(nullValue()));
	}

	@Test
	public void canRetrieveSavedAccount() {
		// Arrange
		final Account account = Utils.generateRandomAccount();
		org.nem.nis.dbmodel.Account dbAccount = new org.nem.nis.dbmodel.Account(account.getAddress().getEncoded(), account.getAddress().getPublicKey());

		// Act:
		accountDao.save(dbAccount);
		org.nem.nis.dbmodel.Account entity = accountDao.getAccountByPrintableAddress(dbAccount.getPrintableKey());

		// Assert:
		Assert.assertThat(entity.getId(), notNullValue());
		Assert.assertThat(entity.getId(), equalTo(dbAccount.getId()));
		Assert.assertThat(entity.getPrintableKey(), equalTo(account.getAddress().getEncoded()));
		Assert.assertThat(entity.getPublicKey(), equalTo(account.getKeyPair().getPublicKey()));
	}
}
