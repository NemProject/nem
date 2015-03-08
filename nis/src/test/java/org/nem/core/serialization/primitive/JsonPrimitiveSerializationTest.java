package org.nem.core.serialization.primitive;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.serialization.*;

@RunWith(Enclosed.class)
public class JsonPrimitiveSerializationTest {

	public static class IntegerSerializationTest extends AbstractIntegerSerializationTest<JsonSerializer, JsonDeserializer> {

		public IntegerSerializationTest() {
			super(new JsonSerializationPolicy());
		}
	}

	public static class LongSerializationTest extends AbstractLongSerializationTest<JsonSerializer, JsonDeserializer> {

		public LongSerializationTest() {
			super(new JsonSerializationPolicy());
		}
	}

	public static class DoubleSerializationTest extends AbstractDoubleSerializationTest<JsonSerializer, JsonDeserializer> {

		public DoubleSerializationTest() {
			super(new JsonSerializationPolicy());
		}
	}

	public static class BigIntegerSerializationTest extends AbstractBigIntegerSerializationTest<JsonSerializer, JsonDeserializer> {

		public BigIntegerSerializationTest() {
			super(new JsonSerializationPolicy());
		}
	}
}
