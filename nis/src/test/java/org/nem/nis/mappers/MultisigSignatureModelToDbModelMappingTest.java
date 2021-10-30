package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbMultisigSignatureTransaction;

public class MultisigSignatureModelToDbModelMappingTest
		extends
			AbstractTransferModelToDbModelMappingTest<MultisigSignatureTransaction, DbMultisigSignatureTransaction> {

	@Test
	public void multisigTransactionIsMappedToDbModelAsNull() {
		// Arrange:
		final TestContext context = new TestContext();
		final MultisigSignatureTransaction signature = context.createModel();

		// Act:
		final DbMultisigSignatureTransaction dbModel = context.mapping.map(signature);

		// Assert:
		context.assertDbModel(dbModel, signature);
	}

	@Override
	protected MultisigSignatureTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return new MultisigSignatureTransaction(timeStamp, sender, Utils.generateRandomAccount(), Utils.generateRandomHash());
	}

	@Override
	protected MultisigSignatureModelToDbModelMapping createMapping(final IMapper mapper) {
		return new MultisigSignatureModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Hash otherTransactionHash = Utils.generateRandomHash();
		private final MultisigSignatureModelToDbModelMapping mapping = new MultisigSignatureModelToDbModelMapping(this.mapper);

		public MultisigSignatureTransaction createModel() {
			return new MultisigSignatureTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), Utils.generateRandomAccount(),
					this.otherTransactionHash);
		}

		public void assertDbModel(final DbMultisigSignatureTransaction dbModel, final MultisigSignatureTransaction model) {
			MatcherAssert.assertThat(dbModel.getMultisigTransaction(), IsNull.nullValue());
			MatcherAssert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(HashUtils.calculateHash(model)));
		}
	}
}
