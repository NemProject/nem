package org.nem.core.connect;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.*;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.function.*;
import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.utils.ExceptionUtils;

@RunWith(Enclosed.class)
public class HttpMethodClientTest {
	private static final HttpDeserializerResponseStrategy DEFAULT_STRATEGY = new HttpJsonResponseStrategy(null);
	private static final int GOOD_TIMEOUT = 15000;

	private static <T> HttpMethodClient.AsyncToken<T> sendPost(final HttpMethodClient<T> client, final URL url,
			final HttpResponseStrategy<T> responseStrategy) {
		return client.post(url, new HttpJsonPostRequest(new MockSerializableEntity()), responseStrategy);
	}

	public static class GetMethodTest extends TestRunner {
		public GetMethodTest() {
			super("GET", HttpMethodClient::get);
		}
	}

	public static class PostMethodTest extends TestRunner {
		public PostMethodTest() {
			super("POST", HttpMethodClientTest::sendPost);
		}
	}

	private static HttpMethodClient<Deserializer> createClient(final int timeout) {
		return new HttpMethodClient<>(timeout, timeout, 10000);
	}

	private static class TestRunner {
		private static final int LOCAL_TEST_SERVER_PORT = 8890;

		private static final String TEST_JSON_ENDPOINT_URI = "/test.json";
		private static final String GOOD_URL = String.format("http://localhost:%d%s", LOCAL_TEST_SERVER_PORT, TEST_JSON_ENDPOINT_URI);

		private static final String TIMEOUT_ENDPOINT_URI = "/timeout";
		private static final String TIMEOUT_URL = String.format("http://localhost:%d%s", LOCAL_TEST_SERVER_PORT, TIMEOUT_ENDPOINT_URI);

		private static final String MALFORMED_URI = "http://www.example.com/customers/[12345]";
		private static final String HOST_LESS_URI = "file:///~/calendar";

		public interface SendAsyncStrategy {
			<T> HttpMethodClient.AsyncToken<T> send(final HttpMethodClient<T> client, final URL url,
					final HttpResponseStrategy<T> responseStrategy);
		}

		private final String httpMethod;
		private final SendAsyncStrategy strategy;

		public TestRunner(final String httpMethod, final SendAsyncStrategy strategy) {
			this.httpMethod = httpMethod;
			this.strategy = strategy;
		}

		@Test
		public void cannotCallSendAfterClose() {
			this.runTestJsonWithMockService((mockService, requestUrl) -> {
				// Arrange:
				final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

				// Act:
				client.close();

				// Assert: this should fail because the underlying client is closed
				ExceptionAssert.assertThrows(v -> this.strategy.send(client, this.stringToUrl(requestUrl), DEFAULT_STRATEGY).get(),
						FatalPeerException.class);
			});
		}

		@Test
		public void sendReturnsJsonDeserializerOnSuccess() {
			this.runTestJsonWithMockService((mockService, requestUrl) -> {
				// Arrange:
				final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

				// Act:
				final Deserializer deserializer = this.strategy.send(client, this.stringToUrl(requestUrl), DEFAULT_STRATEGY).get();

				// Assert:
				MatcherAssert.assertThat(deserializer, IsNull.notNullValue());
				MatcherAssert.assertThat(deserializer.readString("test"), IsEqual.equalTo("org.nem.core.connect.HttpMethodClientTest"));
				MatcherAssert.assertThat(deserializer.readString("one"), IsEqual.equalTo("two"));
			});

		}

		@Test
		public void sendDelegatesToStrategyOnSuccess() {
			this.runTestJsonWithMockService((mockService, requestUrl) -> {
				// Arrange:
				final HttpDeserializerResponseStrategy strategy = Mockito.mock(HttpDeserializerResponseStrategy.class);
				final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

				// Act:
				this.strategy.send(client, this.stringToUrl(requestUrl), strategy).get();

				// Assert:
				Mockito.verify(strategy, Mockito.times(1)).coerce(Mockito.any(HttpRequestBase.class), Mockito.any(HttpResponse.class));
			});
		}

		@Test
		public void sendSetsRequestHeadersCorrectly() {
			this.runTestJsonWithMockService((mockService, requestUrl) -> {
				// Arrange:
				final MockHttpResponseStrategy<Object> strategy = new MockHttpResponseStrategy<>();
				final HttpMethodClient<Object> client = new HttpMethodClient<>(GOOD_TIMEOUT, GOOD_TIMEOUT, GOOD_TIMEOUT);

				// Act:
				this.strategy.send(client, this.stringToUrl(requestUrl), strategy).get();

				// Assert:
				MatcherAssert.assertThat(strategy.getRequestMethod(), IsEqual.equalTo(this.httpMethod));
				MatcherAssert.assertThat(strategy.getRequestContentType(), IsEqual.equalTo("application/json"));
				MatcherAssert.assertThat(strategy.getRequestAcceptHeader(), IsEqual.equalTo("content-type/supported"));
			});
		}

		@Test(expected = InactivePeerException.class)
		public void sendThrowsInactivePeerExceptionOnConnectionTimeout() {
			this.runTestWithTimeoutService((mockService, requestUrl) -> {
				// Arrange:
				// - stop the service so that it's no longer running and will reject connections
				mockService.stop();
				final HttpMethodClient<Deserializer> client = new HttpMethodClient<>(500, GOOD_TIMEOUT, GOOD_TIMEOUT);

				// Act:
				this.strategy.send(client, this.stringToUrl(requestUrl), DEFAULT_STRATEGY).get();
			});
		}

		@Test(expected = BusyPeerException.class)
		public void sendThrowsBusyPeerExceptionOnSocketTimeout() {
			this.runTestWithTimeoutService((mockService, requestUrl) -> {
				// Arrange:
				// - set a delay in request processing to simulate a socket timeout
				mockService.addRequestProcessingDelay(10000);
				final HttpMethodClient<Deserializer> client = new HttpMethodClient<>(GOOD_TIMEOUT, 500, GOOD_TIMEOUT);

				// Act:
				this.strategy.send(client, this.stringToUrl(requestUrl), DEFAULT_STRATEGY).get();
			});
		}

		private void runTestJsonWithMockService(final BiConsumer<WireMockServer, String> action) {
			this.runTestWithMockService(action, new MappingBuilder[]{
					createJsonTestEndpointStub(WireMock::get), createJsonTestEndpointStub(WireMock::post)
			}, GOOD_URL);
		}

		private void runTestWithTimeoutService(final BiConsumer<WireMockServer, String> action) {
			this.runTestWithMockService(action, new MappingBuilder[]{
					createTimeoutStub(WireMock::get), createTimeoutStub(WireMock::post)
			}, TIMEOUT_URL);
		}

		private void runTestWithMockService(final BiConsumer<WireMockServer, String> action, MappingBuilder[] stubbedEndpoints,
				String requestUrl) {
			WireMockServer mockService = null;
			try {
				mockService = new WireMockServer(LOCAL_TEST_SERVER_PORT);
				mockService.start();
				for (MappingBuilder endpoint : stubbedEndpoints) {
					mockService.stubFor(endpoint);
				}
				action.accept(mockService, requestUrl);
			} finally {
				if (null != mockService) {
					mockService.stop();
				}
			}
		}

		private static MappingBuilder createJsonTestEndpointStub(final Function<UrlMatchingStrategy, MappingBuilder> createBuilder) {
			return createEndpointStub(createBuilder, TEST_JSON_ENDPOINT_URI,
					"{ \"test\": \"org.nem.core.connect" + ".HttpMethodClientTest\", \"one\": \"two\" }", 200);
		}

		private static MappingBuilder createTimeoutStub(final Function<UrlMatchingStrategy, MappingBuilder> createBuilder) {
			return createEndpointStub(createBuilder, TIMEOUT_ENDPOINT_URI, null, 200);
		}

		private static MappingBuilder createEndpointStub(final Function<UrlMatchingStrategy, MappingBuilder> createBuilder, String uri,
				String body, int status) {
			ResponseDefinitionBuilder responseDefinitionBuilder = WireMock.aResponse().withHeader("content-type", "application/json")
					.withStatus(status);
			if (body != null) {
				responseDefinitionBuilder = responseDefinitionBuilder.withBody(body);
			}
			return createBuilder.apply(WireMock.urlEqualTo(uri)).willReturn(responseDefinitionBuilder);
		}

		@Test(expected = CancellationException.class)
		public void sendThrowsCancellationExceptionOnCancel() {
			this.runTestJsonWithMockService((mockService, requestUrl) -> {
				// Arrange:
				final HttpMethodClient<Deserializer> client = createClient(1);

				// Act:
				final HttpMethodClient.AsyncToken<Deserializer> token = this.strategy.send(client, this.stringToUrl(requestUrl),
						DEFAULT_STRATEGY);
				token.abort();
				token.get();
			});
		}

		@Test(expected = FatalPeerException.class)
		public void sendThrowsFatalPeerExceptionOnUriError() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, this.stringToUrl(MALFORMED_URI), DEFAULT_STRATEGY).get();
		}

		@Test(expected = FatalPeerException.class)
		public void sendThrowsFatalPeerExceptionOnClientProtocolError() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = createClient(GOOD_TIMEOUT);

			// Act:
			this.strategy.send(client, this.stringToUrl(HOST_LESS_URI), DEFAULT_STRATEGY).get();
		}

		@Test(expected = CancellationException.class)
		public void sendIsCancelledIfOperationTakesTooLong() {
			// Arrange:
			final HttpMethodClient<Deserializer> client = new HttpMethodClient<>(100 * GOOD_TIMEOUT, 100 * GOOD_TIMEOUT, GOOD_TIMEOUT / 20);

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
