package org.nem.nis.dao.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.TransactionTypes;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public abstract class AbstractTransferRawToDbModelMappingTest<TDbModel extends AbstractTransfer> {
	private static final int RAW_SIZE = 10 + TransactionTypes.getBlockEmbeddableTypes().size();

	/**
	 * Creates a mapping that can map raw data to a db model.
	 *
	 * @param mapper The mapper.
	 * @return The mapping.
	 */
	protected abstract IMapping<Object[], TDbModel> createMapping(final IMapper mapper);

	@Test
	public void rawCanBeMappedToDbModelWithNullSenderProof() {
		this.assertRawCanBeMappedToDbModel(null);
	}

	@Test
	public void rawCanBeMappedToDbModelWithNotNullSenderProof() {
		this.assertRawCanBeMappedToDbModel(Utils.generateRandomBytes(64));
	}

	private void assertRawCanBeMappedToDbModel(final byte[] senderProof) {
		final byte[] rawHash = Utils.generateRandomBytes(32);
		final Object[] raw = new Object[RAW_SIZE];
		raw[1] = BigInteger.valueOf(123L);          // id
		raw[2] = rawHash;                           // raw hash
		raw[3] = 1;                                 // version
		raw[4] = BigInteger.valueOf(234L);          // fee
		raw[5] = 345;                               // timestamp
		raw[6] = 456;                               // deadline
		raw[7] = BigInteger.valueOf(567L);          // sender id
		raw[8] = senderProof;                       // sender proof

		final DbAccount dbAccount = new DbAccount(567);
		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(567L, DbAccount.class)).thenReturn(dbAccount);

		// Act:
		final AbstractTransfer dbModel = this.createMapping(mapper).map(raw);

		// Assert:
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(new Hash(rawHash)));
		Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(234L));
		Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(345));
		Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(456));
		Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(dbAccount));
		Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(senderProof));
	}
}
