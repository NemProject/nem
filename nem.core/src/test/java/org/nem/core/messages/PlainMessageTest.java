package org.nem.core.messages;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.MessageTypes;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class PlainMessageTest {

	@Test
	public void ctorCanCreateMessage() {
		// Act:
		final byte[] input = new byte[]{
				12, 46, 7, 43, 22, 15
		};
		final PlainMessage message = new PlainMessage(input);

		// Assert:
		MatcherAssert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.PLAIN));
		MatcherAssert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
		MatcherAssert.assertThat(message.getEncodedPayload(), IsEqual.equalTo(input));
	}

	@Test
	public void messageCanBeRoundTripped() {
		// Arrange:
		final byte[] input = new byte[]{
				12, 46, 7, 43, 22, 15
		};
		final PlainMessage originalMessage = new PlainMessage(input);

		// Act:
		final PlainMessage message = createRoundTrippedMessage(originalMessage);

		// Assert:
		MatcherAssert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.PLAIN));
		MatcherAssert.assertThat(message.canDecode(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(input));
		MatcherAssert.assertThat(message.getEncodedPayload(), IsEqual.equalTo(input));
	}

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final PlainMessage message = new PlainMessage(new byte[]{
				12, 77, 56
		});

		// Assert:
		MatcherAssert.assertThat(new PlainMessage(new byte[]{
				12, 77, 56
		}), IsEqual.equalTo(message));
		MatcherAssert.assertThat(new PlainMessage(new byte[]{
				12, 77
		}), IsNot.not(IsEqual.equalTo(message)));
		MatcherAssert.assertThat(new byte[]{
				12, 77, 56
		}, IsNot.not((Object) IsEqual.equalTo(message)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(message)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final PlainMessage message = new PlainMessage(new byte[]{
				12, 77, 56
		});
		final int hashCode = message.hashCode();

		// Assert:
		MatcherAssert.assertThat(new PlainMessage(new byte[]{
				12, 77, 56
		}).hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(new PlainMessage(new byte[]{
				12, 77
		}).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	private static PlainMessage createRoundTrippedMessage(final PlainMessage originalMessage) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, null);
		deserializer.readInt("type");
		return new PlainMessage(deserializer);
	}
}
