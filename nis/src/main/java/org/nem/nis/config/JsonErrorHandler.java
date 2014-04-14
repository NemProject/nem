package org.nem.nis.config;


import net.minidev.json.JSONObject;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.log.Log;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonErrorHandler extends ErrorHandler {

	private static final Logger LOGGER = Logger.getLogger(JsonErrorHandler.class.getName());

	@Override
	public void handleErrorPage(HttpServletRequest request, Writer writer, int code, String message) throws IOException {

		// TODO: refactor this
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("status", code);
		jsonObject.put("error", message);
		writer.write(jsonObject.toJSONString() + "\r\n");

	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
//		response.setContentType("application/json");
//		super.handle(target, baseRequest, request, response);

		String method = request.getMethod();
		if (!HttpMethod.GET.is(method) && !HttpMethod.POST.is(method) && !HttpMethod.HEAD.is(method))
		{
			baseRequest.setHandled(true);
			return;
		}

		baseRequest.setHandled(true);
		response.setContentType(MimeTypes.Type.APPLICATION_JSON.asString());
//		response.setContentType("application/json");
		if (getCacheControl()!=null)
			response.setHeader(HttpHeader.CACHE_CONTROL.asString(), getCacheControl());
		ByteArrayISO8859Writer writer= new ByteArrayISO8859Writer(4096);
		String reason=(response instanceof Response)?((Response)response).getReason():null;
		handleErrorPage(request, writer, response.getStatus(), reason);
		writer.flush();
		response.setContentLength(writer.size());
		writer.writeTo(response.getOutputStream());
		writer.destroy();
	}
//
//	@Override
//	public void writeErrorPageHead(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
//		LOGGER.log(Level.INFO, "in writeErrorPageHead");
//	}
//
//	@Override
//	public void writeErrorPageBody(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException {
//		LOGGER.log(Level.INFO, "in writeErrorPageBody");
//	}
}
