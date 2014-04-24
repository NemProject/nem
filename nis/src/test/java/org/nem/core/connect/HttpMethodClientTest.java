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
import org.nem.core.utils.ExceptionUtils;

import java.net.URL;
import java.util.concurrent.CancellationException;

public class HttpMethodClientTest {

	private static final HttpDeserializerResponseStrategy DEFAULT_STRATEGY = new HttpDeserializerResponseStrategy(null);

	private final TestRunner getTestRunner = new TestRunner("GET", new TestRunner.SendAsyncStrategy() {
		@Override
		public <T> HttpMethodClient.AsyncToken<T> send(HttpMethodClient<T> client, URL url, HttpResponseStrategy<T> responseStrategy) {
			return client.get(url, responseStrategy);
		}
	});

	private final TestRunner postTestRunner = new TestRunner("POST", new TestRunner.SendAsyncStrategy() {
		@Override
		public <T> HttpMethodClient.AsyncToken<T> send(HttpMethodClient<T> client, URL url, HttpResponseStrategy<T> responseStrategy) {
			return client.post(url, new MockSerializableEntity(), responseStrategy);
		}
	});

	//region get

	@Test
	public void getReturnsJsonDeserializerOnSuccess() {
		// Assert:
		this.getTestRunner.sendReturnsJsonDeserializerOnSuccess();
	}

	@Test
	public void getDelegatesToStrategyOnSuccess() {
		// Assert:
		this.getTestRunner.sendDelegatesToStrategyOnSuccess();
	}

	@Test
	public void getSetsRequestHeadersCorrectly() {
		// Assert:
		this.getTestRunner.sendSetsRequestHeadersCorrectly();
	}

	@Test(expected = InactivePeerException.class)
	public void getThrowsInactivePeerExceptionOnTimeout() {
		// Assert:
		this.getTestRunner.sendThrowsInactivePeerExceptionOnTimeout();
	}

	@Test(expected = CancellationException.class)
	public void getThrowsCancellationExceptionOnCancel() {
		// Assert:
		this.getTestRunner.sendThrowsCancellationExceptionOnCancel();
	}

	@Test(expected = FatalPeerException.class)
	public void getThrowsFatalPeerExceptionOnUriError() {
		// Assert:
		this.getTestRunner.sendThrowsFatalPeerExceptionOnUriError();
	}

	@Test(expected = FatalPeerException.class)
	public void getThrowsFatalPeerExceptionOnClientProtocolError() {
		// Assert:
		this.getTestRunner.sendThrowsFatalPeerExceptionOnClientProtocolError();
	}

	//endregion

	//region post

	@Test
	public void postReturnsJsonDeserializerOnSuccess() {
		// Assert:
		this.postTestRunner.sendReturnsJsonDeserializerOnSuccess();
	}

	@Test
	public void postDelegatesToStrategyOnSuccess() {
		// Assert:
		this.postTestRunner.sendDelegatesToStrategyOnSuccess();
	}

	@Test
	public void postSetsRequestHeadersCorrectly() {
		// Assert:
		this.postTestRunner.sendSetsRequestHeadersCorrectly();
	}

	@Test(expected = InactivePeerException.class)
	public void postThrowsInactivePeerExceptionOnTimeout() {
		// Assert:
		this.postTestRunner.sendThrowsInactivePeerExceptionOnTimeout();
	}

	@Test(expected = CancellationException.class)
	public void postThrowsCancellationExceptionOnCancel() {
		// Assert:
		this.postTestRunner.sendThrowsCancellationExceptionOnCancel();
	}

	@Test(expected = FatalPeerException.class)
	public void postThrowsFatalPeerExceptionOnUriError() {
		// Assert:
		this.postTestRunner.sendThrowsFatalPeerExceptionOnUriError();
	}

	@Test(expected = FatalPeerException.class)
	public void postThrowsFatalPeerExceptionOnClientProtocolError() {
		// Assert:
		this.postTestRunner.sendThrowsFatalPeerExceptionOnClientProtocolError();
	}

	//endregion

	private static HttpMethodClient<Deserializer> createClient(int timeout) {
		return new HttpMethodClient<>(timeout);
	}

	private static class TestRunner {

		private final String GOOD_URL = "http://echo.jsontest.com/key/value/one/two";
		private final String TIMEOUT_URL = "http://127.0.0.100/";
		private final String MALFORMED_URI = "http://www.example.com/customers/[12345]";
		private final String HOST_LESS_URI = "file:///~/calendar";
		private final int GOOD_TIMEOUT = 5;

		public interface SendAsyncStrategy {

			<T> HttpMethodClient.AsyncToken<T> send(
					final HttpMethodClient<T> client,
					final URL url,
					final HttpResponseStrategy<T> responseStrategy);
		}

		private final String httpMethod;
		private final SendAsyncStrategy strategy;

		public TestRunner(final String httpMethod, final SendAsyncStrategy strategy) {
			this.httpMethod = httpMethod;
			this.strategy = strategy;
		}

		@Test
		public void sendReturnsJsonDeserializerOnSuccess() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			final Deserializer deserializer = this.strategy.send(client, stringToUrl(GOOD_URL), DEFAULT_STRATEGY).get();

			// Assert:
			Assert.assertThat(deserializer, IsNot.not(IsEqual.equalTo(null)));
			Assert.assertThat(deserializer.readString("one"), IsEqual.equalTo("two"));
			Assert.assertThat(deserializer.readString("key"), IsEqual.equalTo("value"));
		}

		@Test
		public void sendDelegatesToStrategyOnSuccess() {
			// Arrange:
			final HttpDeserializerResponseStrategy strategy = Mockito.mock(HttpDeserializerResponseStrategy.class);
			final HttpMethodClient<Deserializer> client = new HttpMethodClient<>(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, stringToUrl(GOOD_URL), strategy).get();

			// Assert:
			Mockito.verify(strategy, Mockito.times(1)).coerce(Mockito.any(HttpRequestBase.class), Mockito.any(HttpResponse.class));
		}

		@Test
		public void sendSetsRequestHeadersCorrectly() {
			// Arrange:
			final MockHttpResponseStrategy<Object> strategy = new MockHttpResponseStrategy<>();
			final HttpMethodClient<Object> client = new HttpMethodClient<>(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, stringToUrl(GOOD_URL), strategy).get();

			// Assert:
			Assert.assertThat(strategy.getRequestMethod(), IsEqual.equalTo(this.httpMethod));
			Assert.assertThat(strategy.getRequestContentType(), IsEqual.equalTo("application/json"));
		}

		@Test(expected = InactivePeerException.class)
		public void sendThrowsInactivePeerExceptionOnTimeout() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(1);

			// Act:
			this.strategy.send(client, stringToUrl(TIMEOUT_URL), DEFAULT_STRATEGY).get();
		}

		@Test(expected = CancellationException.class)
		public void sendThrowsCancellationExceptionOnCancel() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(1);

			// Act:
			final HttpMethodClient.AsyncToken<Deserializer> token = this.strategy.send(
					client,
					stringToUrl(TIMEOUT_URL),
					DEFAULT_STRATEGY);
			token.abort();
			token.get();
		}

		@Test(expected = FatalPeerException.class)
		public void sendThrowsFatalPeerExceptionOnUriError() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, stringToUrl(MALFORMED_URI), DEFAULT_STRATEGY).get();
		}

		public void sendThrowsFatalPeerExceptionOnClientProtocolError() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, stringToUrl(HOST_LESS_URI), DEFAULT_STRATEGY).get();
		}

		private URL stringToUrl(final String s) {
			return ExceptionUtils.propagate(() -> new URL(s));
		}
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
