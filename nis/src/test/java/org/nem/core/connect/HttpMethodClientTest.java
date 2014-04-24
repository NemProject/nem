package org.nem.core.connect;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.MockSerializableEntity;

import java.net.URL;
import java.util.concurrent.CancellationException;

public class HttpMethodClientTest {

	private final String GOOD_URL = "http://echo.jsontest.com/key/value/one/two";
	private final String TIMEOUT_URL = "http://127.0.0.100/";
	private final String MALFORMED_URI = "http://www.example.com/customers/[12345]";
	private final int GOOD_TIMEOUT = 5;


	private static final HttpDeserializerResponseStrategy DEFAULT_STRATEGY = new HttpDeserializerResponseStrategy(null);

	//region get

	@Test
	public void getReturnsJsonDeserializerOnSuccess() throws Exception {
		// Arrange:
		final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

		// Act:
		final Deserializer deserializer = client.get(new URL(GOOD_URL), DEFAULT_STRATEGY).get();

		// Assert:
		Assert.assertThat(deserializer, IsNot.not(IsEqual.equalTo(null)));
		Assert.assertThat(deserializer.readString("one"), IsEqual.equalTo("two"));
		Assert.assertThat(deserializer.readString("key"), IsEqual.equalTo("value"));
	}

	@Test
	public void getDelegatesToStrategyOnSuccess() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = Mockito.mock(HttpDeserializerResponseStrategy.class);
		final HttpMethodClient<Deserializer> client = new HttpMethodClient<>(GOOD_TIMEOUT);

		// Act:
		client.get(new URL(GOOD_URL), strategy).get();

		// Assert:
		Mockito.verify(strategy, Mockito.times(1)).coerce(Mockito.any(HttpRequestBase.class), Mockito.any(HttpResponse.class));
	}

	@Test
	public void getSetsRequestHeadersCorrectly() throws Exception {
		// Arrange:
		final MockHttpResponseStrategy<Object> strategy = new MockHttpResponseStrategy<>();
		final HttpMethodClient<Object> client = new HttpMethodClient<>(GOOD_TIMEOUT);

		// Act:
		client.get(new URL(GOOD_URL), strategy).get();

		// Assert:
		Assert.assertThat(strategy.getRequestMethod(), IsEqual.equalTo("GET"));
		Assert.assertThat(strategy.getRequestContentType(), IsEqual.equalTo("application/json"));
	}

	@Test(expected = InactivePeerException.class)
	public void getThrowsInactivePeerExceptionOnTimeout() throws Exception {
		// Arrange:
		final HttpMethodClient<Deserializer> client = createClient(1);

		// Act:
		client.get(new URL(TIMEOUT_URL), DEFAULT_STRATEGY).get();
	}

	@Test(expected = CancellationException.class)
	public void getThrowsCancellationExceptionOnCancel() throws Exception {
		// Arrange:
		final HttpMethodClient<Deserializer> client = createClient(1);

		// Act:
		final HttpMethodClient.AsyncToken<Deserializer> token = client.get(new URL(TIMEOUT_URL), DEFAULT_STRATEGY);
		token.abort();
		token.get();
	}

	@Test(expected = FatalPeerException.class)
	public void getThrowsFatalPeerExceptionOnOtherError() throws Exception {
		// Arrange:
		final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

		// Act:
		client.get(new URL(MALFORMED_URI), DEFAULT_STRATEGY).get();
	}

	//endregion

	//region post

	@Test
	public void postReturnsJsonDeserializerOnSuccess() throws Exception {
		// Arrange:
		final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

		// Act:
		final Deserializer deserializer = client.post(new URL(GOOD_URL), new MockSerializableEntity(), DEFAULT_STRATEGY).get();

		// Assert:
		Assert.assertThat(deserializer, IsNot.not(IsEqual.equalTo(null)));
		Assert.assertThat(deserializer.readString("one"), IsEqual.equalTo("two"));
		Assert.assertThat(deserializer.readString("key"), IsEqual.equalTo("value"));
	}

	@Test
	public void postDelegatesToStrategyOnSuccess() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = Mockito.mock(HttpDeserializerResponseStrategy.class);
		final HttpMethodClient<Deserializer> client = new HttpMethodClient<>(GOOD_TIMEOUT);

		// Act:
		client.post(new URL(GOOD_URL), new MockSerializableEntity(), strategy).get();

		// Assert:
		Mockito.verify(strategy, Mockito.times(1)).coerce(Mockito.any(HttpRequestBase.class), Mockito.any(HttpResponse.class));
	}

	@Test
	public void postSetsRequestHeadersCorrectly() throws Exception {
		// Arrange:
		final MockHttpResponseStrategy<Object> strategy = new MockHttpResponseStrategy<>();
		final HttpMethodClient<Object> client = new HttpMethodClient<>(GOOD_TIMEOUT);

		// Act:
		client.post(new URL(GOOD_URL), new MockSerializableEntity(), strategy).get();

		// Assert:
		Assert.assertThat(strategy.getRequestMethod(), IsEqual.equalTo("POST"));
		Assert.assertThat(strategy.getRequestContentType(), IsEqual.equalTo("application/json"));
	}

	@Test(expected = InactivePeerException.class)
	public void postThrowsInactivePeerExceptionOnTimeout() throws Exception {
		// Arrange:
		final HttpMethodClient<Deserializer> client = createClient(1);

		// Act:
		client.post(new URL(TIMEOUT_URL), new MockSerializableEntity(), DEFAULT_STRATEGY).get();
	}

	@Test(expected = CancellationException.class)
	public void postThrowsCancellationExceptionOnCancel() throws Exception {
		// Arrange:
		final HttpMethodClient<Deserializer> client = createClient(1);

		// Act:
		final HttpMethodClient.AsyncToken<Deserializer> token = client.post(
				new URL(TIMEOUT_URL),
				new MockSerializableEntity(),
				DEFAULT_STRATEGY);
		token.abort();
		token.get();
	}

	@Test(expected = FatalPeerException.class)
	public void postThrowsFatalPeerExceptionOnOtherError() throws Exception {
		// Arrange:
		final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

		// Act:
		client.post(new URL(MALFORMED_URI), new MockSerializableEntity(), DEFAULT_STRATEGY).get();
	}

	//endregion

	private static HttpMethodClient<Deserializer> createClient(int timeout) {
		return new HttpMethodClient<>(timeout);
	}

	private static class MockHttpResponseStrategy<T> implements HttpResponseStrategy<T> {

		private String requestMethod;
		private String requestContentType;

		@Override
		public T coerce(final HttpRequestBase request, final HttpResponse response) {
			final HttpEntity entity = response.getEntity();
			this.requestMethod = request.getMethod();
			this.requestContentType = null == entity ? null : ContentType.get(entity).getMimeType();
			return null;
		}

		public String getRequestMethod() { return this.requestMethod; }
		public String getRequestContentType() { return this.requestContentType; }
	}
}
