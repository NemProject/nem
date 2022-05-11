package org.nem.core.serialization.primitive;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.serialization.*;

@RunWith(Enclosed.class)
public class JsonPrimitiveTruncationTest {

	public static class BytesTruncationTest extends AbstractBytesTruncationTest<JsonSerializer, JsonDeserializer> {

		public BytesTruncationTest() {
			super(new JsonSerializationPolicy());
		}
	}

	public static class StringTruncationTest extends AbstractStringTruncationTest<JsonSerializer, JsonDeserializer> {

		public StringTruncationTest() {
			super(new JsonSerializationPolicy());
		}
	}
}
