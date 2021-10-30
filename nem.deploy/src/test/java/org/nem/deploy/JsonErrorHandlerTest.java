package org.nem.deploy;

import net.minidev.json.*;
import org.eclipse.jetty.server.*;
import org.hamcrest.core.*;
import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.ErrorResponse;
import org.nem.core.serialization.*;
import org.nem.core.time.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class JsonErrorHandlerTest {
	private static final TimeInstant CURRENT_TIME = new TimeInstant(84);

	@Test
	public void nothingIsWrittenIfHttpMethodIsUnsupported() throws Exception {
		// Arrange:
		final TestContext context = new TestContext("DELETE");

		// Act:
		context.handle();

		// Assert:
		Mockito.verify(context.getBaseRequest()).setHandled(true);
		Mockito.verify(context.getResponse(), Mockito.never()).getOutputStream();
	}

	@Test
	public void responseHeadersAreWrittenCorrectlyWhenCacheControlIsSpecified() throws Exception {
		// Arrange:
		final TestContext context = new TestContext("GET");
		context.getErrorHandler().setCacheControl("foo");

		// Act:
		context.handle();

		// Assert:
		Mockito.verify(context.getBaseRequest()).setHandled(true);
		Mockito.verify(context.getResponse()).setContentType("application/json");
		Mockito.verify(context.getResponse()).setHeader("Cache-Control", "foo");
	}

	@Test
	public void responseHeadersAreWrittenCorrectlyWhenCacheControlIsNotSpecified() throws Exception {
		// Arrange:
		final TestContext context = new TestContext("GET");
		context.getErrorHandler().setCacheControl(null);

		// Act:
		context.handle();

		// Assert:
		Mockito.verify(context.getBaseRequest()).setHandled(true);
		Mockito.verify(context.getResponse()).setContentType("application/json");
		Mockito.verify(context.getResponse(), Mockito.never()).setHeader(Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void responseContentLengthIsWrittenCorrectly() throws Exception {
		// Arrange:
		final TestContext context = new TestContext("GET");

		// Act:
		context.handle();

		// Assert:
		Mockito.verify(context.getResponse()).setContentLength(context.getOutputStreamContent().length());
	}

	@Test
	public void jsonBodyEndsWithTerminatingNewLine() throws Exception {
		// Arrange:
		final TestContext context = new TestContext("GET");

		// Act:
		context.handle();

		// Assert:
		MatcherAssert.assertThat(context.getOutputStreamContent().endsWith("\r\n"), IsEqual.equalTo(true));
	}

	@Test
	public void jsonBodyIsCorrectWhenReasonIsProvided() throws Exception {
		// Arrange:
		final Response mockResponse = Mockito.mock(Response.class);
		Mockito.when(mockResponse.getReason()).thenReturn("badness");

		final TestContext context = new TestContext("GET", mockResponse);
		Mockito.when(context.getResponse().getStatus()).thenReturn(123);

		// Act:
		context.handle();
		final ErrorResponse response = context.getErrorResponse();

		// Assert:
		MatcherAssert.assertThat(response.getTimeStamp(), IsEqual.equalTo(CURRENT_TIME));
		MatcherAssert.assertThat(response.getStatus(), IsEqual.equalTo(123));
		MatcherAssert.assertThat(response.getMessage(), IsEqual.equalTo("badness"));
	}

	@Test
	public void jsonBodyIsCorrectWhenReasonIsNotProvided() throws Exception {
		// Arrange:
		final TestContext context = new TestContext("GET");
		Mockito.when(context.getResponse().getStatus()).thenReturn(123);

		// Act:
		context.handle();
		final ErrorResponse response = context.getErrorResponse();

		// Assert:
		MatcherAssert.assertThat(response.getTimeStamp(), IsEqual.equalTo(CURRENT_TIME));
		MatcherAssert.assertThat(response.getStatus(), IsEqual.equalTo(123));
		MatcherAssert.assertThat(response.getMessage(), IsNull.nullValue());
	}

	// region MockServletOutputStream

	private static class MockServletOutputStream extends ServletOutputStream {

		private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

		@Override
		public void write(final int i) throws IOException {
			this.stream.write(i);
		}

		@Override
		public boolean isReady() {
			return false;
		}

		@Override
		public void setWriteListener(final WriteListener writeListener) {
		}

		public String getContent() {
			return this.stream.toString();
		}
	}

	// endregion

	// region TestContext

	private static class TestContext {
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final JsonErrorHandler handler = new JsonErrorHandler(this.timeProvider);
		private final Request mockBaseRequest = Mockito.mock(Request.class);
		private final HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		private final MockServletOutputStream outputStream = new MockServletOutputStream();
		private final HttpServletResponse mockResponse;

		public TestContext(final String httpMethod) throws IOException {
			this(httpMethod, Mockito.mock(HttpServletResponse.class));
		}

		public TestContext(final String httpMethod, final HttpServletResponse response) throws IOException {
			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(CURRENT_TIME);

			this.mockResponse = response;
			Mockito.when(this.getRequest().getMethod()).thenReturn(httpMethod);
			Mockito.when(this.getResponse().getOutputStream()).thenReturn(this.outputStream);
		}

		public void handle() throws IOException {
			this.handler.handle("target", this.mockBaseRequest, this.mockRequest, this.mockResponse);
		}

		public JsonErrorHandler getErrorHandler() {
			return this.handler;
		}

		public Request getBaseRequest() {
			return this.mockBaseRequest;
		}

		public HttpServletRequest getRequest() {
			return this.mockRequest;
		}

		public HttpServletResponse getResponse() {
			return this.mockResponse;
		}

		public String getOutputStreamContent() {
			return this.outputStream.getContent();
		}

		public ErrorResponse getErrorResponse() {
			final String jsonString = this.outputStream.getContent();
			final Deserializer deserializer = new JsonDeserializer((JSONObject) JSONValue.parse(jsonString), null);
			return new ErrorResponse(deserializer);
		}
	}

	// endregion
}
