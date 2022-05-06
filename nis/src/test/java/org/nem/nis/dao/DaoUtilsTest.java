package org.nem.nis.dao;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.test.DbTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.*;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class DaoUtilsTest {
	private static final int NUM_ACCOUNTS = 10;
	private static final List<Account> ACCOUNTS = IntStream.range(0, NUM_ACCOUNTS).mapToObj(i -> Utils.generateRandomAccount())
			.collect(Collectors.toList());

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
		DbTestUtils.dbCleanup(this.session);
		this.session.close();
	}

	@Test
	public void getAccountIdReturnsExpectedId() {
		LongStream.range(0, NUM_ACCOUNTS).forEach(i -> {
			// Act:
			final long accountId = DaoUtils.getAccountId(this.session, ACCOUNTS.get((int) i).getAddress());

			// Assert:
			MatcherAssert.assertThat(accountId, IsEqual.equalTo(i + 1));
		});
	}

	@Test
	public void getAccountIdsReturnsExpectedIds() {
		// Arrange:
		final Collection<Address> accounts = Arrays.asList(1, 3, 5, 7).stream().map(id -> ACCOUNTS.get(id).getAddress())
				.collect(Collectors.toList());

		// Act:
		final Collection<Long> accountIds = DaoUtils.getAccountIds(this.session, accounts);

		// Assert:
		final List<Long> expectedIds = Arrays.asList(2L, 4L, 6L, 8L);
		MatcherAssert.assertThat(accountIds, IsEquivalent.equivalentTo(expectedIds));
	}

	private void createAccounts(final int count) {
		for (int i = 0; i < count; i++) {
			final Address address = ACCOUNTS.get(i).getAddress();
			final String statement = String.format("Insert into accounts (printableKey, publicKey) values('%s', '%s')", address.toString(),
					address.getPublicKey().toString());
			this.session.createSQLQuery(statement).executeUpdate();
		}
	}
}
