/**
 * 
 */
package org.nem;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.nem.util.NEMLogger;
import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;

/**
 * Only initial coding state, just for testing purposes. Might be enhanced
 * later.
 * 
 * It provides an initial boot of the server for NEM, configuration, and starts
 * connecting with peers. Just for fun, currently to the NXT network
 * 
 * @author Thies1965
 * 
 */
public class NEM extends HttpServlet {

	private static final long serialVersionUID = 1L;
	// All GLOBAL Definitions are going here
	// No change during runtime
	public static final String VERSION = "0.0.1";
	public static final String APP_NAME = "NEM Server";
	public static final int NEM_PORT = 7487;
	public static String APP_CONTEXT;

	private Set<String> wellKnownPeers;
	private int maxNumberOfConnectedPublicPeers;
	private int connectTimeout;
	private int readTimeout;
	private Set<String> allowedUserHosts;

	public void init(ServletConfig paramServletConfig) throws ServletException {
		// Initialize Logging
		NEMLogger.initializeLogger(APP_NAME);

		NEMLogger.LOG.log(Level.INFO, APP_NAME + " " + VERSION + " starting...");
		NEMLogger.LOG.log(Level.FINE, "DEBUG logging enabled");

		NEMLogger.LOG.log(Level.FINE, "Configure own node.");
		Node localNode = null;

		String tmpStr = null;
		try {
			// How is this servlet configured?
			// Get the path
			APP_CONTEXT = paramServletConfig.getServletContext().getContextPath();

			// Read configuration parameter "myPort"
			int myPort = NEM_PORT;
			tmpStr = paramServletConfig.getInitParameter("myPort");
			try {
				myPort = Integer.parseInt(tmpStr);
				NEMLogger.LOG.log(Level.INFO, "\"myPort\" = \"" + tmpStr + "\"");
			} catch (NumberFormatException localNumberFormatException1) {
				myPort = NEM_PORT;

				NEMLogger.LOG.log(Level.INFO, "Invalid value for myPort " + tmpStr + ", using default " + myPort);
			}

			// Read configuration parameter "myAddress"
			tmpStr = paramServletConfig.getInitParameter("myAddress");
			NEMLogger.LOG.log(Level.INFO, "\"myAddress\" = \"" + tmpStr + "\"");
			if (tmpStr != null) {
				tmpStr = tmpStr.trim();
			}
			localNode = new Node(tmpStr, myPort);

			// Read configuration parameter "myPlatform"
			tmpStr = paramServletConfig.getInitParameter("myPlatform");
			if (tmpStr == null) {
				tmpStr = "PC";
			} else {
				tmpStr = tmpStr.trim();
			}

			localNode.setMyPlatform(tmpStr);
			NEMLogger.LOG.log(Level.INFO, "\"myPlatform\" = \"" + tmpStr + "\"");

			// For the moment we place our node at the application context, then
			// it is aavailable
			// for each request
			paramServletConfig.getServletContext().setAttribute("OurLocalNode", localNode);
			NEMLogger.LOG.log(Level.FINE, "Local Node configured.");

			// Read configuration parameter "wellKnownPeers"
			tmpStr = paramServletConfig.getInitParameter("wellKnownPeers");
			NEMLogger.LOG.log(Level.INFO, "\"wellKnownPeers\" = \"" + tmpStr + "\"");
			if (tmpStr != null) {
				Set<String> hosts = new HashSet<String>();
				for (String hostEntry : tmpStr.split(";")) {
					hostEntry = hostEntry.trim();
					if (hostEntry.length() > 0) {
						hosts.add(hostEntry);
					}
				}
				wellKnownPeers = Collections.unmodifiableSet(hosts);
			} else {
				wellKnownPeers = Collections.emptySet();
				NEMLogger.LOG.log(Level.WARNING, "No wellKnownPeers defined, it is unlikely to work");
			}

			// Read configuration parameter "maxNumberOfConnectedPublicPeers"
			tmpStr = paramServletConfig.getInitParameter("maxNumberOfConnectedPublicPeers");
			NEMLogger.LOG.log(Level.INFO, "\"maxNumberOfConnectedPublicPeers\" = \"" + tmpStr + "\"");
			try {
				maxNumberOfConnectedPublicPeers = Integer.parseInt(tmpStr);
			} catch (NumberFormatException localNumberFormatException2) {
				maxNumberOfConnectedPublicPeers = 10;
				NEMLogger.LOG.log(Level.INFO, "Invalid value for maxNumberOfConnectedPublicPeers " + tmpStr + ", using default "
						+ maxNumberOfConnectedPublicPeers);
			}

			// Read configuration parameter "connectTimeout"
			tmpStr = paramServletConfig.getInitParameter("connectTimeout");
			NEMLogger.LOG.log(Level.INFO, "\"connectTimeout\" = \"" + tmpStr + "\"");
			try {
				connectTimeout = Integer.parseInt(tmpStr);
			} catch (NumberFormatException localNumberFormatException3) {
				connectTimeout = 1000;
				NEMLogger.LOG.log(Level.INFO, "Invalid value for connectTimeout " + tmpStr + ", using default " + connectTimeout);
			}

			// Read configuration parameter "readTimeout"
			tmpStr = paramServletConfig.getInitParameter("readTimeout");
			NEMLogger.LOG.log(Level.INFO, "\"readTimeout\" = \"" + tmpStr + "\"");
			try {
				readTimeout = Integer.parseInt(tmpStr);
			} catch (NumberFormatException localNumberFormatException4) {
				readTimeout = 1000;
				NEMLogger.LOG.log(Level.INFO, "Invalid value for readTimeout " + tmpStr + ", using default " + readTimeout);
			}

			// Read configuration parameter "allowedUserHosts"
			tmpStr = paramServletConfig.getInitParameter("allowedUserHosts");
			NEMLogger.LOG.log(Level.INFO, "\"allowedUserHosts\" = \"" + tmpStr + "\"");
			if (tmpStr != null) {
				if (!tmpStr.trim().equals("*")) {
					Set<String> hosts = new HashSet<String>();
					for (String addr : tmpStr.split(";")) {
						addr = addr.trim();
						if (addr.length() > 0) {
							hosts.add(addr);
						}
					}
					allowedUserHosts = Collections.unmodifiableSet(hosts);
				}
			}

			// Start the network
			PeerNetwork network = new PeerNetwork("Default network", localNode, wellKnownPeers);
			network.boot();

			NEMLogger.LOG.log(Level.INFO, APP_NAME + " " + VERSION + " started.");
		} catch (Exception localException) {
			NEMLogger.LOG.log(Level.SEVERE, "Error initializing servlet", localException);
			System.exit(1);
		}
	}
}
