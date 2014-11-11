package org.nem.core.connect;

import org.apache.http.*;
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

	private static final HttpDeserializerResponseStrategy DEFAULT_STRATEGY = new HttpJsonResponseStrategy(null);
	private static final int GOOD_TIMEOUT = 10000;

	private final TestRunner getTestRunner = new TestRunner("GET", HttpMethodClient::get);

	private final TestRunner postTestRunner = new TestRunner("POST", HttpMethodClientTest::sendPost);

	private static <T> HttpMethodClient.AsyncToken<T> sendPost(
			final HttpMethodClient<T> client,
			final URL url,
			final HttpResponseStrategy<T> responseStrategy) {
		return client.post(url, new HttpJsonPostRequest(new MockSerializableEntity()), responseStrategy);
	}

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
	public void getThrowsInactivePeerExceptionOnConnectionTimeout() {
		// Assert:
		this.getTestRunner.sendThrowsInactivePeerExceptionOnConnectionTimeout();
	}

	@Test(expected = BusyPeerException.class)
	public void getThrowsBusyPeerExceptionOnSocketTimeout() {
		// Assert:
		this.getTestRunner.sendThrowsInactivePeerExceptionOnSocketTimeout();
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

	@Test(expected = CancellationException.class, timeout = GOOD_TIMEOUT)
	public void getIsCancelledIfOperationTakesTooLong() {
		// Assert:
		this.getTestRunner.sendIsCancelledIfOperationTakesTooLong();
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
	public void postThrowsInactivePeerExceptionOnConnectionTimeout() {
		// Assert:
		this.postTestRunner.sendThrowsInactivePeerExceptionOnConnectionTimeout();
	}

	@Test(expected = BusyPeerException.class)
	public void postThrowsBusyPeerExceptionOnSocketTimeout() {
		// Assert:
		this.postTestRunner.sendThrowsInactivePeerExceptionOnSocketTimeout();
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

	@Test(expected = CancellationException.class, timeout = GOOD_TIMEOUT)
	public void postIsCancelledIfOperationTakesTooLong() {
		// Assert:
		this.postTestRunner.sendIsCancelledIfOperationTakesTooLong();
	}

	//endregion

	private static HttpMethodClient<Deserializer> createClient(final int timeout) {
		return new HttpMethodClient<>(timeout, timeout, 10000);
	}

	private static class TestRunner {

		private final String GOOD_URL = "http://echo.jsontest.com/key/value/one/two";
		private final String TIMEOUT_URL = "http://127.0.0.100/";
		private final String MALFORMED_URI = "http://www.example.com/customers/[12345]";
		private final String HOST_LESS_URI = "file:///~/calendar";

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

		public void sendReturnsJsonDeserializerOnSuccess() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			final Deserializer deserializer = this.strategy.send(client, this.stringToUrl(this.GOOD_URL), DEFAULT_STRATEGY).get();

			// Assert:
			Assert.assertThat(deserializer, IsNull.notNullValue());
			Assert.assertThat(deserializer.readString("one"), IsEqual.equalTo("two"));
			Assert.assertThat(deserializer.readString("key"), IsEqual.equalTo("value"));
		}

		public void sendDelegatesToStrategyOnSuccess() {
			// Arrange:
			final HttpDeserializerResponseStrategy strategy = Mockito.mock(HttpDeserializerResponseStrategy.class);
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, this.stringToUrl(this.GOOD_URL), strategy).get();

			// Assert:
			Mockito.verify(strategy, Mockito.times(1)).coerce(Mockito.any(HttpRequestBase.class), Mockito.any(HttpResponse.class));
		}

		public void sendSetsRequestHeadersCorrectly() {
			// Arrange:
			final MockHttpResponseStrategy<Object> strategy = new MockHttpResponseStrategy<>();
			final HttpMethodClient<Object> client = new HttpMethodClient<>(GOOD_TIMEOUT, GOOD_TIMEOUT, GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, this.stringToUrl(this.GOOD_URL), strategy).get();

			// Assert:
			Assert.assertThat(strategy.getRequestMethod(), IsEqual.equalTo(this.httpMethod));
			Assert.assertThat(strategy.getRequestContentType(), IsEqual.equalTo("application/json"));
			Assert.assertThat(strategy.getRequestAcceptHeader(), IsEqual.equalTo("content-type/supported"));
		}

		public void sendThrowsInactivePeerExceptionOnConnectionTimeout() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = new HttpMethodClient<>(1, GOOD_TIMEOUT, GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, this.stringToUrl("http://10.0.0.1:9999"), DEFAULT_STRATEGY).get();
		}

		public void sendThrowsInactivePeerExceptionOnSocketTimeout() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(1);

			// Act:
			this.strategy.send(client, this.stringToUrl(this.TIMEOUT_URL), DEFAULT_STRATEGY).get();
		}

		public void sendThrowsCancellationExceptionOnCancel() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(1);

			// Act:
			final HttpMethodClient.AsyncToken<Deserializer> token = this.strategy.send(
					client,
					this.stringToUrl(this.TIMEOUT_URL),
					DEFAULT_STRATEGY);
			token.abort();
			token.get();
		}

		public void sendThrowsFatalPeerExceptionOnUriError() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, this.stringToUrl(this.MALFORMED_URI), DEFAULT_STRATEGY).get();
		}

		public void sendThrowsFatalPeerExceptionOnClientProtocolError() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, this.stringToUrl(this.HOST_LESS_URI), DEFAULT_STRATEGY).get();
		}

		public void sendIsCancelledIfOperationTakesTooLong() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = new HttpMethodClient<>(
					100 * GOOD_TIMEOUT,
					100 * GOOD_TIMEOUT,
					GOOD_TIMEOUT / 20);

			// Act:
			this.strategy.send(client, this.stringToUrl("http://10.255.255.1"), DEFAULT_STRATEGY).get();
		}

		private URL stringToUrl(final String s) {
			return ExceptionUtils.propagate(() -> new URL(s));
		}
	}

	private static class MockHttpResponseStrategy<T> implements HttpResponseStrategy<T> {

		private String requestMethod;
		private String requestContentType;
		private String requestAcceptHeader;

		@Override
		public T coerce(final HttpRequestBase request, final HttpResponse response) {
			final HttpEntity entity = response.getEntity();
			this.requestMethod = request.getMethod();
			this.requestContentType = null == entity ? null : ContentType.get(entity).getMimeType();
			this.requestAcceptHeader = request.getFirstHeader("Accept").getValue();
			return null;
		}

		@Override
		public String getSupportedContentType() {
			return "content-type/supported";
		}

		public String getRequestMethod() {
			return this.requestMethod;
		}

		public String getRequestContentType() {
			return this.requestContentType;
		}

		public String getRequestAcceptHeader() {
			return this.requestAcceptHeader;
		}
	}
}
