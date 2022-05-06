package org.nem.core.serialization.primitive;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.serialization.*;

@RunWith(Enclosed.class)
public class BinaryPrimitiveTruncationTest {

	public static class BytesTruncationTest extends AbstractBytesTruncationTest<BinarySerializer, BinaryDeserializer> {

		public BytesTruncationTest() {
			super(new BinarySerializationPolicy());
		}
	}

	public static class StringTruncationTest extends AbstractStringTruncationTest<BinarySerializer, BinaryDeserializer> {

		public StringTruncationTest() {
			super(new BinarySerializationPolicy());
		}
	}
}
