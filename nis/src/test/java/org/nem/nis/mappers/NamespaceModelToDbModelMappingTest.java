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

public class NamespaceModelToDbModelMappingTest {

	@Test
	public void canMapNamespaceToDbNamespace() {
		// Arrange:
		final TestContext context = new TestContext();
		final Namespace namespace = new Namespace(new NamespaceId("foo.bar"), context.owner, new BlockHeight(123));

		// Act:
		final DbNamespace dbNamespace = context.mapping.map(namespace);

		// Assert:
		MatcherAssert.assertThat(dbNamespace.getOwner(), IsEqual.equalTo(context.dbOwner));
		MatcherAssert.assertThat(dbNamespace.getFullName(), IsEqual.equalTo("foo.bar"));
		MatcherAssert.assertThat(dbNamespace.getHeight(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbNamespace.getLevel(), IsEqual.equalTo(1));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbOwner = Mockito.mock(DbAccount.class);
		private final Account owner = Utils.generateRandomAccount();
		private final NamespaceModelToDbModelMapping mapping = new NamespaceModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.owner, DbAccount.class)).thenReturn(this.dbOwner);
		}
	}
}
