package org.nem.nis.dao;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.Account;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.DbAccount;
import org.nem.nis.test.DbTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.*;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AccountDaoTest extends AbstractTransactionalJUnit4SpringContextTests {
	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private AccountDao accountDao;

	private Session session;

	@Before
	public void before() {
		this.session = this.sessionFactory.openSession();
	}

	@After
	public void after() {
		DbTestUtils.dbCleanup(this.session);
		this.session.close();
	}

	@Test
	public void cannotRetrieveUnknownAccount() {
		// Arrange
		final Account account = Utils.generateRandomAccount();
		final DbAccount dbAccount = new DbAccount(account.getAddress());

		// Act:
		final DbAccount entity = this.accountDao.getAccountByPrintableAddress(dbAccount.getPrintableKey());

		// Assert:
		MatcherAssert.assertThat(entity, IsNull.nullValue());
	}

	@Test
	public void canRetrieveSavedAccount() {
		// Arrange
		final Account account = Utils.generateRandomAccount();
		final DbAccount dbAccount = new DbAccount(account.getAddress());
		this.session.saveOrUpdate(dbAccount);

		// Act:
		final DbAccount entity = this.accountDao.getAccountByPrintableAddress(dbAccount.getPrintableKey());

		// Assert:
		MatcherAssert.assertThat(entity.getId(), IsNull.notNullValue());
		MatcherAssert.assertThat(entity.getId(), IsEqual.equalTo(dbAccount.getId()));
		MatcherAssert.assertThat(entity.getPrintableKey(), IsEqual.equalTo(account.getAddress().getEncoded()));
		MatcherAssert.assertThat(entity.getPublicKey(), IsEqual.equalTo(account.getAddress().getPublicKey()));
	}
}
