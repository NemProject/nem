package org.nem.nis.dbmodel;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DbProvisionNamespaceTransactionTest {

	//region setSender

	@Test
	public void setSenderSetsOwnerInNamespace() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		transaction.setNamespace(new DbNamespace());
		final DbAccount sender = new DbAccount();

		// Act:
		transaction.setSender(sender);

		// Assert:
		Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transaction.getNamespace().getOwner(), IsEqual.equalTo(sender));
	}

	@Test
	public void setSenderCannotBeCalledBeforeSetNamespace() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		final DbAccount sender = new DbAccount();

		// Act:
		ExceptionAssert.assertThrows(v -> transaction.setSender(sender), IllegalStateException.class);
	}

	//endregion

	//region setBlock

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
		Assert.assertThat(transaction.getBlock(), IsEqual.equalTo(block));
		Assert.assertThat(transaction.getNamespace().getHeight(), IsEqual.equalTo(20L));
	}

	@Test
	public void setBlockSucceedsIfNoNamespaceIsSet() {
		// Arrange:
		final DbProvisionNamespaceTransaction transaction = new DbProvisionNamespaceTransaction();
		final DbBlock block = new DbBlock();
		block.setHeight(20L);

		// Act:
		transaction.setBlock(block);

		// Assert:
		Assert.assertThat(transaction.getBlock(), IsEqual.equalTo(block));
		Assert.assertThat(transaction.getNamespace(), IsNull.nullValue());
	}

	//endregion
}