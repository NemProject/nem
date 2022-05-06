package org.nem.nis.dbmodel;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DbProvisionNamespaceTransactionTest {

	// region setNamespace

	@Test
	public void canSetNamespaceOnce() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		final DbNamespace namespace = new DbNamespace();

		// Act:
		transaction.setNamespace(namespace);

		// Assert:
		MatcherAssert.assertThat(transaction.getNamespace(), IsEqual.equalTo(namespace));
	}

	@Test
	public void cannotResetNamespace() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		transaction.setNamespace(new DbNamespace());

		// Act:
		ExceptionAssert.assertThrows(v -> transaction.setNamespace(new DbNamespace()), IllegalStateException.class);
	}

	// endregion

	// region setSender

	@Test
	public void setSenderSetsOwnerInNamespace() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		transaction.setNamespace(new DbNamespace());
		final DbAccount sender = new DbAccount();

		// Act:
		transaction.setSender(sender);

		// Assert:
		MatcherAssert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		MatcherAssert.assertThat(transaction.getNamespace().getOwner(), IsEqual.equalTo(sender));
	}

	@Test
	public void setSenderCannotBeCalledBeforeSetNamespace() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		final DbAccount sender = new DbAccount();

		// Act:
		ExceptionAssert.assertThrows(v -> transaction.setSender(sender), IllegalStateException.class);
	}

	// endregion

	// region setBlock

	@Test
	public void setBlockSetsHeightInNamespace() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		transaction.setNamespace(new DbNamespace());
		final DbBlock block = new DbBlock();
		block.setHeight(20L);

		// Act:
		transaction.setBlock(block);

		// Assert:
		MatcherAssert.assertThat(transaction.getBlock(), IsEqual.equalTo(block));
		MatcherAssert.assertThat(transaction.getNamespace().getHeight(), IsEqual.equalTo(20L));
	}

	@Test
	public void setBlockCannotBeCalledBeforeSetNamespace() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		final DbBlock block = new DbBlock();
		block.setHeight(20L);

		// Act:
		ExceptionAssert.assertThrows(v -> transaction.setBlock(block), IllegalStateException.class);
	}

	@Test
	public void setBlockDoesNotUpdateNamespaceHeightIfBlockHeightIsNull() {
		// Arrange:
		final DbNamespace dbNamespace = new DbNamespace();
		dbNamespace.setHeight(20L);
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		transaction.setNamespace(dbNamespace);
		final DbBlock block = new DbBlock();

		// Act:
		transaction.setBlock(block);

		// Assert:
		MatcherAssert.assertThat(block.getHeight(), IsNull.nullValue());
		MatcherAssert.assertThat(transaction.getBlock(), IsEqual.equalTo(block));
		MatcherAssert.assertThat(transaction.getNamespace().getHeight(), IsEqual.equalTo(20L));
	}

	// endregion
}
