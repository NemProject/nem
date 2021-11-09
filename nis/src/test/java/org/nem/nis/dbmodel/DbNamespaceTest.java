package org.nem.nis.dbmodel;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class DbNamespaceTest {

	@Test
	public void ctorCanWrapExistingDbNamespace() {
		// Arrange:
		final DbNamespace dbOriginal = new DbNamespace();
		dbOriginal.setId(5L);
		dbOriginal.setFullName("a.b.c");
		dbOriginal.setLevel(22);
		dbOriginal.setHeight(123L);
		dbOriginal.setOwner(new DbAccount());

		// Act:
		final DbAccount dbAccount = new DbAccount();
		final DbNamespace dbNamespace = new DbNamespace(dbOriginal, dbAccount, 543L);

		// Assert:
		MatcherAssert.assertThat(dbNamespace.getId(), IsEqual.equalTo(5L));
		MatcherAssert.assertThat(dbNamespace.getFullName(), IsEqual.equalTo("a.b.c"));
		MatcherAssert.assertThat(dbNamespace.getLevel(), IsEqual.equalTo(22));
		MatcherAssert.assertThat(dbNamespace.getHeight(), IsEqual.equalTo(543L));
		MatcherAssert.assertThat(dbNamespace.getOwner(), IsEqual.equalTo(dbAccount));
	}
}
