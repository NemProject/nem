package org.nem.peer.net;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.test.MockSerializableEntity;
import org.nem.peer.*;

import java.net.URL;

public class HttpMethodClientTest {

	private final String GOOD_URL = "http://echo.jsontest.com/key/value/one/two";
	private final String MALFORMED_URI = "http://www.example.com/customers/[12345]";
	private final int GOOD_TIMEOUT = 5;

	//region get

	@Test
	public void getReturnsJsonDeserializerOnSuccess() throws Exception {
		// Arrange:
		final HttpMethodClient client = new HttpMethodClient(null, GOOD_TIMEOUT);

		// Act:
		JsonDeserializer deserializer = client.get(new URL(GOOD_URL));

		// Assert:
		Assert.assertThat(deserializer, IsNot.not(IsEqual.equalTo(null)));
		Assert.assertThat(deserializer.readString("one"), IsEqual.equalTo("two"));
		Assert.assertThat(deserializer.readString("key"), IsEqual.equalTo("value"));
	}

	@Test(expected = InactivePeerException.class)
	public void getThrowsInactivePeerExceptionOnTimeout() throws Exception {
		// Arrange:
		final HttpMethodClient client = new HttpMethodClient(null, 0);

		// Act:
		client.get(new URL(GOOD_URL));
	}

	@Test(expected = FatalPeerException.class)
	public void getThrowsFatalPeerExceptionOnOtherError() throws Exception {
		// Arrange:
		final HttpMethodClient client = new HttpMethodClient(null, GOOD_TIMEOUT);

		// Act:
		client.get(new URL(MALFORMED_URI));
	}

	//endregion

	//region post

	@Test
	public void postReturnsJsonDeserializerOnSuccess() throws Exception {
		// Arrange:
		final HttpMethodClient client = new HttpMethodClient(null, GOOD_TIMEOUT);

		// Act:
		JsonDeserializer deserializer = client.post(new URL(GOOD_URL), new MockSerializableEntity());

		// Assert:
		Assert.assertThat(deserializer, IsNot.not(IsEqual.equalTo(null)));
		Assert.assertThat(deserializer.readString("one"), IsEqual.equalTo("two"));
		Assert.assertThat(deserializer.readString("key"), IsEqual.equalTo("value"));
	}

	@Test(expected = InactivePeerException.class)
	public void postThrowsInactivePeerExceptionOnTimeout() throws Exception {
		// Arrange:
		final HttpMethodClient client = new HttpMethodClient(null, 0);

		// Act:
		client.post(new URL(GOOD_URL), new MockSerializableEntity());
	}

	@Test(expected = FatalPeerException.class)
	public void postThrowsFatalPeerExceptionOnOtherError() throws Exception {
		// Arrange:
		final HttpMethodClient client = new HttpMethodClient(null, GOOD_TIMEOUT);

		// Act:
		client.post(new URL(MALFORMED_URI), new MockSerializableEntity());
	}

	//endregion
}
