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
	private final Function<Address, TEntity> createEntity;
	private final Function<Integer, TMetaData> createMetaData;
	private final BiFunction<TEntity, TMetaData, AbstractMetaDataPair<TEntity, TMetaData>> createPair;
	private final Function<Deserializer, AbstractMetaDataPair<TEntity, TMetaData>> deserializePair;
	private final Function<TEntity, Address> getAddress;
	private final Function<TMetaData, Integer> getId;

	protected AbstractMetaDataPairTest(
			final Function<Address, TEntity> createEntity,
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
	public void canCreateMosaicMetaDataPair() {
		// Arrange:
		final TEntity entity = this.createEntity.apply(Utils.generateRandomAddressWithPublicKey());
		final TMetaData metaData = this.createMetaData.apply(123);

		// Act:
		final AbstractMetaDataPair<TEntity, TMetaData> pair = this.createPair.apply(entity, metaData);

		// Assert:
		Assert.assertThat(pair.getEntity(), IsSame.sameInstance(entity));
		Assert.assertThat(pair.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripMosaicMetaDataPair() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final AbstractMetaDataPair<TEntity, TMetaData> pair = this.createRoundTrippedPair(address, 5678);

		// Assert:
		Assert.assertThat(this.getAddress.apply(pair.getEntity()), IsEqual.equalTo(address));
		Assert.assertThat(this.getId.apply(pair.getMetaData()), IsEqual.equalTo(5678));
	}

	private AbstractMetaDataPair<TEntity, TMetaData> createRoundTrippedPair(final Address address, final int id) {
		// Arrange:
		final TEntity entity = this.createEntity.apply(address);
		final TMetaData metaData = this.createMetaData.apply(id);
		final AbstractMetaDataPair<TEntity, TMetaData> pair = this.createPair.apply(entity, metaData);

		// Act:
		return this.deserializePair.apply(Utils.roundtripSerializableEntity(pair, new MockAccountLookup()));
	}
}