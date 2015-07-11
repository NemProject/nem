package org.nem.nis.dao;

import org.hamcrest.core.IsEqual;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.*;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class DaoUtilsTest {
	private final static int NUM_ACCOUNTS = 10;
	private static final List<Account> ACCOUNTS = IntStream.range(0, NUM_ACCOUNTS).mapToObj(i -> Utils.generateRandomAccount()).collect(Collectors.toList());

	@Autowired
	AccountDao accountDao;

	@Autowired
	SessionFactory sessionFactory;

	private Session session;

	@Before
	public void before() {
		this.session = this.sessionFactory.openSession();
		this.createAccounts(NUM_ACCOUNTS);
	}

	@After
	public void after() {
		DbUtils.dbCleanup(this.session);
		this.session.close();
	}

	@Test
	public void getAccountIdReturnsExpectedId() {
		LongStream.range(0, NUM_ACCOUNTS).forEach(i -> Assert.assertThat(DaoUtils.getAccountId(this.session, ACCOUNTS.get((int)i)), IsEqual.equalTo(i + 1)));
	}

	@Test
	public void getAccountIdsReturnsExpectedIds() {
		final List<Long> expectedIds = Arrays.asList(2L, 4L, 6L, 8L);
		final Collection<Address> accounts = expectedIds.stream().map(id -> ACCOUNTS.get(id.intValue() - 1).getAddress()).collect(Collectors.toList());
		Assert.assertThat(DaoUtils.getAccountIds(this.session, accounts), IsEquivalent.equivalentTo(expectedIds));
	}

	private void createAccounts(final int count) {
		for (int i = 0; i < count; i++) {
			final Address address = ACCOUNTS.get(i).getAddress();
			final String statement = String.format("Insert into accounts (printableKey, publicKey) values('%s', '%s')",
					address.toString(),
					address.getPublicKey().toString());
			this.session.createSQLQuery(statement).executeUpdate();
		}
	}
}
