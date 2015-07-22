package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.function.*;

public abstract class AbstractMetaDataPairTest<
		TEntity extends SerializableEntity,
		TMetaData extends SerializableEntity> {
	private final Function<Account, TEntity> createEntity;
	private final Function<Integer, TMetaData> createMetaData;
	private final BiFunction<TEntity, TMetaData, AbstractMetaDataPair<TEntity, TMetaData>> createPair;
	private final Function<Deserializer, AbstractMetaDataPair<TEntity, TMetaData>> deserializePair;
	private final Function<TEntity, Address> getAddress;
	private final Function<TMetaData, Integer> getId;

	protected AbstractMetaDataPairTest(
			final Function<Account, TEntity> createEntity,
			final Function<Integer, TMetaData> createMetaData,
			final BiFunction<TEntity, TMetaData, AbstractMetaDataPair<TEntity, TMetaData>> createPair,
			final Function<Deserializer, AbstractMetaDataPair<TEntity, TMetaData>> deserializePair,
			final Function<TEntity, Address> getAddress,
			final Function<TMetaData, Integer> getId) {
		this.createEntity = createEntity;
		this.createMetaData = createMetaData;
		this.createPair = createPair;
		this.deserializePair = deserializePair;
		this.getAddress = getAddress;
		this.getId = getId;
	}

	@Test
	public void canCreateMetaDataPair() {
		// Arrange:
		final TEntity entity = this.createEntity.apply(Utils.generateRandomAccount());
		final TMetaData metaData = this.createMetaData.apply(123);

		// Act:
		final AbstractMetaDataPair<TEntity, TMetaData> pair = this.createPair.apply(entity, metaData);

		// Assert:
		Assert.assertThat(pair.getEntity(), IsSame.sameInstance(entity));
		Assert.assertThat(pair.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripMetaDataPair() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		final AbstractMetaDataPair<TEntity, TMetaData> pair = this.createRoundTrippedPair(account, 5678);

		// Assert:
		Assert.assertThat(this.getAddress.apply(pair.getEntity()), IsEqual.equalTo(account.getAddress()));
		Assert.assertThat(this.getId.apply(pair.getMetaData()), IsEqual.equalTo(5678));
	}

	private AbstractMetaDataPair<TEntity, TMetaData> createRoundTrippedPair(final Account account, final int id) {
		// Arrange:
		final TEntity entity = this.createEntity.apply(account);
		final TMetaData metaData = this.createMetaData.apply(id);
		final AbstractMetaDataPair<TEntity, TMetaData> pair = this.createPair.apply(entity, metaData);

		// Act:
		return this.deserializePair.apply(Utils.roundtripSerializableEntity(pair, new MockAccountLookup()));
	}
}