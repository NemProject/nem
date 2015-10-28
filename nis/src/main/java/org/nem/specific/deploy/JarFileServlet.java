package org.nem.specific.deploy;

import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JarFileServlet extends DefaultServlet {
	private static final Logger LOGGER = Logger.getLogger(JarFileServlet.class.getName());

	/**
	 * get Resource to serve. Map a path to a resource. The default implementation calls HttpContext.getResource but derived servlets may provide their own
	 * mapping.
	 *
	 * @param pathInContext The path to find a resource for.
	 * @return The resource to serve.
	 */
	@Override
	public Resource getResource(final String pathInContext) {
		// very basic redirector:
		//    if path starts with /lightweight/ and does not contain . -> /static/lightweight/index.html
		//    else if starts with /lightweight -> prepend /static, /static/lightweight/foo.bar.baz
		//    else reat it normally
		final String prefixedPath = pathInContext.startsWith("/lightweight") ?
				(pathInContext.contains(".") ? ("/static" + pathInContext) : "/static/lightweight/index.html") :
				pathInContext;
		final String contextStr = "/static/";
		if (contextStr.length() > prefixedPath.length()) {
			LOGGER.severe(String.format("Resource not found: <%s>, mapping to welcome page.", prefixedPath));
			return null;
		}

		if (! prefixedPath.startsWith(contextStr)) {
			return null;
		}

		final ClassLoader classLoader = this.getClass().getClassLoader();
		final URL url = classLoader.getResource(prefixedPath.substring(1));
		if (url == null) {
			LOGGER.severe(String.format("Resource not found: <%s>, mapping to welcome page.", prefixedPath));
			return null;
		}

		final Resource r = Resource.newResource(url);
		if (!r.exists()) {
			LOGGER.log(Level.SEVERE, "Resource does not exist", url);
		}

		return r;
	}

	/* ------------------------------------------------------------ */
	/*
	 * Set the no cache pragma and proxy settings. Never cache anything.
	 *
	 * @see org.eclipse.jetty.servlet.DefaultServlet#sendData(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, boolean,
	 * org.eclipse.jetty.util.resource.Resource, org.eclipse.jetty.http.HttpContent, java.util.Enumeration)
	 */
	@Override
	protected void sendData(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final boolean include,
			final Resource resource,
			final HttpContent content,
			final Enumeration<String> reqRanges) throws IOException {

		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setDateHeader("Expires", 0); // Proxies.
		super.sendData(request, response, include, resource, content, reqRanges);
	}
}