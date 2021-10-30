package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
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
		raw[1] = BigInteger.valueOf(123L); // id
		raw[2] = rawHash; // raw hash
		raw[3] = 1; // version
		raw[4] = BigInteger.valueOf(234L); // fee
		raw[5] = 345; // timestamp
		raw[6] = 456; // deadline
		raw[7] = BigInteger.valueOf(567L); // sender id
		raw[8] = senderProof; // sender proof

		final DbAccount dbAccount = new DbAccount(567);
		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(567L, DbAccount.class)).thenReturn(dbAccount);

		// Act:
		final AbstractTransfer dbModel = this.createMapping(mapper).map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(new Hash(rawHash)));
		MatcherAssert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(dbModel.getFee(), IsEqual.equalTo(234L));
		MatcherAssert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(345));
		MatcherAssert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(456));
		MatcherAssert.assertThat(dbModel.getSender(), IsEqual.equalTo(dbAccount));
		MatcherAssert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(senderProof));
	}
}
