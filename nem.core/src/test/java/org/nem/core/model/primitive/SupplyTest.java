package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

public class SupplyTest extends AbstractQuantityTest<Supply> {

	@Override
	protected Supply getZeroConstant() {
		return Supply.ZERO;
	}

	@Override
	protected Supply fromValue(final long raw) {
		return Supply.fromValue(raw);
	}

	@Override
	protected Supply construct(final long raw) {
		return new Supply(raw);
	}

	@Override
	protected Supply readFrom(final Deserializer deserializer, final String label) {
		return Supply.readFrom(deserializer, label);
	}

	@Override
	protected void writeTo(final Serializer serializer, final String label, final Supply quantity) {
		Supply.writeTo(serializer, label, quantity);
	}
}
