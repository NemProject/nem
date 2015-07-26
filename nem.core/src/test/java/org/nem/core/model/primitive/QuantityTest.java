package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.function.BiFunction;

public class QuantityTest extends AbstractQuantityTest<Quantity> {

	@Override
	protected Quantity getZeroConstant() {
		return Quantity.ZERO;
	}

	@Override
	protected Quantity fromValue(final long raw) {
		return Quantity.fromValue(raw);
	}

	@Override
	protected Quantity construct(final long raw) {
		return new Quantity(raw);
	}

	@Override
	protected Quantity readFrom(final Deserializer deserializer, final String label) {
		return Quantity.readFrom(deserializer, label);
	}

	@Override
	protected void writeTo(final Serializer serializer, final String label, final Quantity quantity) {
		Quantity.writeTo(serializer, label, quantity);
	}
}
