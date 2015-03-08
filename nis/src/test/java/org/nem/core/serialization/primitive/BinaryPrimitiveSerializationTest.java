package org.nem.core.serialization.primitive;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.serialization.*;

@RunWith(Enclosed.class)
public class BinaryPrimitiveSerializationTest {

	public static class IntegerSerializationTest extends AbstractIntegerSerializationTest<BinarySerializer, BinaryDeserializer> {

		public IntegerSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class LongSerializationTest extends AbstractLongSerializationTest<BinarySerializer, BinaryDeserializer> {

		public LongSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class DoubleSerializationTest extends AbstractDoubleSerializationTest<BinarySerializer, BinaryDeserializer> {

		public DoubleSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}
}
