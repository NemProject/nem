package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class NamespaceDbModelToModelMappingTest {

	@Test
	public void canMapDbNamespaceToNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbNamespace dbNamespace = new DbNamespace();
		dbNamespace.setOwner(context.dbOwner);
		dbNamespace.setFullName("foo.bar");
		dbNamespace.setHeight(123L);

		// Act:
		final Namespace namespace = context.mapping.map(dbNamespace);

		// Assert:
		MatcherAssert.assertThat(namespace.getOwner(), IsEqual.equalTo(context.owner));
		MatcherAssert.assertThat(namespace.getId(), IsEqual.equalTo(new NamespaceId("foo.bar")));
		MatcherAssert.assertThat(namespace.getHeight(), IsEqual.equalTo(new BlockHeight(123)));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbOwner = Mockito.mock(DbAccount.class);
		private final Account owner = Utils.generateRandomAccount();
		private final NamespaceDbModelToModelMapping mapping = new NamespaceDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbOwner, Account.class)).thenReturn(this.owner);
		}
	}
}
