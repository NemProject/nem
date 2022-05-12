package org.nem.deploy;

import org.eclipse.jetty.http.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.nem.core.connect.ErrorResponse;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.time.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.*;
import java.io.*;

/**
 * Custom error handler that returns JSON error responses in the same format as the ExceptionControllerAdvice.
 */
public class JsonErrorHandler extends ErrorHandler {
	private final TimeProvider timeProvider;

	/**
	 * Creates a new JSON error handler.
	 *
	 * @param timeProvider The time provider.
	 */
	@Autowired(required = true)
	public JsonErrorHandler(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		// note: handle needs to be overridden instead of something more specific like handleErrorPage
		// because we need to set the content type to application/json and this is the only way to do that.
		// the rest of the implementation comes from the reflected base class.

		final String method = request.getMethod();
		if (!HttpMethod.GET.is(method) && !HttpMethod.POST.is(method) && !HttpMethod.HEAD.is(method)) {
			baseRequest.setHandled(true);
			return;
		}

		baseRequest.setHandled(true);
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.asString());
		if (null != this.getCacheControl()) {
			response.setHeader(HttpHeader.CACHE_CONTROL.asString(), this.getCacheControl());
		}

		try (final ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(4096)) {
			final String reason = (response instanceof Response) ? ((Response) response).getReason() : null;
			this.handleErrorPage(request, writer, response.getStatus(), reason);
			writer.flush();
			response.setContentLength(writer.size());
			writer.writeTo(response.getOutputStream());
		}
	}

	@Override
	public void handleErrorPage(final HttpServletRequest request, final Writer writer, final int code, final String message)
			throws IOException {
		final ErrorResponse response = new ErrorResponse(this.timeProvider.getCurrentTime(), message, code);
		final String jsonString = JsonSerializer.serializeToJson(response).toJSONString();
		writer.write(jsonString + "\r\n");
	}
}
