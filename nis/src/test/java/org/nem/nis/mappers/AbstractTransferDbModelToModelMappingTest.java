package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public abstract class AbstractTransferDbModelToModelMappingTest<TDbModel extends AbstractTransfer, TModel extends Transaction> {

	/**
	 * Creates a db model.
	 *
	 * @return The db model.
	 */
	protected abstract TDbModel createDbModel();

	/**
	 * Creates a mapping that can map a db model to a model.
	 *
	 * @param mapper The mapper.
	 * @return The mapping.
	 */
	protected abstract IMapping<TDbModel, TModel> createMapping(final IMapper mapper);

	@Test
	public void abstractDbModelWithSignatureCanBeMappedToModel() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Signature signature = Utils.generateRandomSignature();
		final TDbModel dbModel = this.createDbModel();
		dbModel.setTimeStamp(334455);
		dbModel.setSender(new DbAccount(sender.getAddress()));
		dbModel.setSenderProof(signature.getBytes());
		dbModel.setFee(2310000000L);
		dbModel.setDeadline(800);
		dbModel.setVersion(0);

		final IMapper mapper = Mockito.mock(IMapper.class);
		addAccountMapping(mapper);

		// Act:
		final Transaction model = this.createMapping(mapper).map(dbModel);

		// Assert:
		MatcherAssert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(334455)));
		MatcherAssert.assertThat(model.getSigner(), IsEqual.equalTo(sender));
		MatcherAssert.assertThat(model.getSignature(), IsEqual.equalTo(signature));
		MatcherAssert.assertThat(model.getFee(), IsEqual.equalTo(Amount.fromMicroNem(2310000000L)));
		MatcherAssert.assertThat(model.getDeadline(), IsEqual.equalTo(new TimeInstant(800)));
	}

	@Test
	public void abstractDbModelWithoutSignatureCanBeMappedToModel() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final TDbModel dbModel = this.createDbModel();
		dbModel.setTimeStamp(334455);
		dbModel.setSender(new DbAccount(sender.getAddress()));
		dbModel.setSenderProof(null);
		dbModel.setFee(2310000000L);
		dbModel.setDeadline(800);
		dbModel.setVersion(0);

		final IMapper mapper = Mockito.mock(IMapper.class);
		addAccountMapping(mapper);

		// Act:
		final Transaction model = this.createMapping(mapper).map(dbModel);

		// Assert:
		MatcherAssert.assertThat(model.getTimeStamp(), IsEqual.equalTo(new TimeInstant(334455)));
		MatcherAssert.assertThat(model.getSigner(), IsEqual.equalTo(sender));
		MatcherAssert.assertThat(model.getSignature(), IsNull.nullValue());
		MatcherAssert.assertThat(model.getFee(), IsEqual.equalTo(Amount.fromMicroNem(2310000000L)));
		MatcherAssert.assertThat(model.getDeadline(), IsEqual.equalTo(new TimeInstant(800)));
	}

	private static void addAccountMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(Account.class))).thenAnswer(invocationOnMock -> {
			final DbAccount account = (DbAccount) (invocationOnMock.getArguments()[0]);
			return null == account || null == account.getPublicKey()
					? Utils.generateRandomAccount()
					: new Account(Address.fromPublicKey(account.getPublicKey()));
		});
	}
}
