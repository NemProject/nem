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

	public static class BigIntegerSerializationTest extends AbstractBigIntegerSerializationTest<BinarySerializer, BinaryDeserializer> {

		public BigIntegerSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class BytesSerializationTest extends AbstractBytesSerializationTest<BinarySerializer, BinaryDeserializer> {

		public BytesSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class StringSerializationTest extends AbstractStringSerializationTest<BinarySerializer, BinaryDeserializer> {

		public StringSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class TruncatedBytesSerializationTest extends AbstractTruncatedBytesSerializationTest<BinarySerializer, BinaryDeserializer> {

		public TruncatedBytesSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class TruncatedStringSerializationTest extends AbstractTruncatedStringSerializationTest<BinarySerializer, BinaryDeserializer> {

		public TruncatedStringSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class ObjectSerializationTest extends AbstractObjectSerializationTest<BinarySerializer, BinaryDeserializer> {

		public ObjectSerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class ObjectArraySerializationTest extends AbstractObjectArraySerializationTest<BinarySerializer, BinaryDeserializer> {

		public ObjectArraySerializationTest() {
			super(new BinarySerializationPolicy());
		}
	}
}
