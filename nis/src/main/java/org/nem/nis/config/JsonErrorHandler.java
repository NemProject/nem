package org.nem.nis.config;


import net.minidev.json.JSONObject;
import org.eclipse.jetty.server.handler.ErrorHandler;

import javax.servlet.http.*;
import java.io.IOException;
import java.io.Writer;
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
}
