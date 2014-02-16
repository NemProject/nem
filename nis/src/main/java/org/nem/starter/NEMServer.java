/**
 * 
 */
package org.nem.starter;

import java.lang.management.ManagementFactory;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.nem.NEM;

/**
 * Can be considered to have an alternative start for Jetty.
 * Sometimes it is easier to have control over the starting sequence of Jetty
 * 
 * @author Thies1965
 *
 */
public class NEMServer {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Server server = new Server(NEM.NEM_PORT);
		MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/nem");
        webapp.setWar("NEM-WebApp.war");
        server.setHandler(webapp);
        server.start();
        server.join();

	}

}
