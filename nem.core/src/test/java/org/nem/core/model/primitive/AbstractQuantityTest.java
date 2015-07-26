package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.function.BiFunction;

/**
 * Base class for AbstractTDerived tests.
 *
 * @param <TDerived> The derived type.
 */
public abstract class AbstractQuantityTest<TDerived extends AbstractQuantity<TDerived>> {

	protected abstract TDerived getZeroConstant();

	protected abstract TDerived fromValue(long raw);

	protected abstract TDerived construct(long raw);

	protected abstract TDerived readFrom(final Deserializer deserializer, final String label);

	protected abstract void writeTo(final Serializer serializer, final String label, final TDerived quantity);

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(this.getZeroConstant(), IsEqual.equalTo(this.construct(0)));
	}

	//endregion

	//region fromValue

	@Test
	public void canCreateQuantityFromValue() {
		// Act:
		final TDerived quantity = this.fromValue(11);

		// Assert:
		Assert.assertThat(quantity.getRaw(), IsEqual.equalTo(11L));
	}

	//endregion

	//region constructor

	@Test
	public void cannotBeCreatedAroundNegativeQuantity() {
		// Act:
		ExceptionAssert.assertThrows(v -> this.construct(-1), NegativeQuantityException.class);
		ExceptionAssert.assertThrows(v -> this.fromValue(-1), NegativeQuantityException.class);
	}

	@Test
	public void canBeCreatedAroundZeroQuantity() {
		// Act:
		final TDerived quantity = this.construct(0);

		// Assert:
		Assert.assertThat(quantity.getRaw(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveQuantity() {
		// Act:
		final TDerived quantity = this.construct(1);

		// Assert:
		Assert.assertThat(quantity.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region add / subtract

	@Test
	public void twoQuantitiesCanBeAdded() {
		// Arrange:
		final TDerived quantity1 = this.construct(65);
		final TDerived quantity2 = this.construct(111);

		// Act:
		final TDerived result1 = quantity1.add(quantity2);
		final TDerived result2 = quantity2.add(quantity1);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(this.construct(176)));
		Assert.assertThat(result2, IsEqual.equalTo(this.construct(176)));
	}

	@Test
	public void smallerQuantityCanBeSubtractedFromLargerQuantity() {
		// Arrange:
		final TDerived quantity1 = this.construct(65);
		final TDerived quantity2 = this.construct(111);

		// Act:
		final TDerived result = quantity2.subtract(quantity1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(this.construct(46)));
	}

	@Test
	public void largerQuantityCannotBeSubtractedFromSmallerQuantity() {
		// Arrange:
		final TDerived quantity1 = this.construct(65);
		final TDerived quantity2 = this.construct(111);

		// Act:
		ExceptionAssert.assertThrows(v -> quantity1.subtract(quantity2), NegativeQuantityException.class);
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteQuantity() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final TDerived quantity = this.construct(0x7712411223456L);

		// Act:
		this.writeTo(serializer, "quantity", quantity);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("quantity"), IsEqual.equalTo(0x7712411223456L));
	}

	@Test
	public void canRoundTripQuantity() {
		// Assert:
		this.assertCanRoundTripQuantity(this::readFrom);
	}

	private void assertCanRoundTripQuantity(final BiFunction<Deserializer, String, TDerived> readFrom) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final TDerived original = this.construct(0x7712411223456L);

		// Act:
		this.writeTo(serializer, "quantity", original);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final TDerived quantity = readFrom.apply(deserializer, "quantity");

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(original));
	}

	//endregion
}