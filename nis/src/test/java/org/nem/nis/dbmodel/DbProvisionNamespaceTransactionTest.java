package org.nem.nis.dbmodel;

import org.hamcrest.core.*;
import org.junit.*;

public class DbProvisionNamespaceTransactionTest {

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
}