package org.nem.peer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.nem.nis.NisMain;
import org.nem.util.NEMLogger;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class PeerInitializer extends Thread {
	private static final Logger logger = Logger.getLogger(PeerInitializer.class);

	// All GLOBAL Definitions are going here
	// No change during runtime
	//
	// gimer: moved it temporarily here, probably should be in NIS/NEM class
	public static final String VERSION = "0.1.0";
	public static final String APP_NAME = "NIS";
	public static final int NEM_PORT = 7890;

	@Override
	public void run() {
		
		NEMLogger.initializeLogger(APP_NAME);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
		}
		
		logger.debug("Configure own node.");
		Node localNode = null;

		String tmpStr = null;
		
		FileInputStream fin = null;
		try {
			fin = new FileInputStream("peers-config.json");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		logger.info("NIS settings: ");
		
		JSONObject config = (JSONObject) JSONValue.parse(fin);
		Integer myPort = (Integer)config.get("myPort");
		if (myPort == null) {
			myPort = 12345;
		}
		logger.info("  \"myPort\" = " + myPort);
		
		tmpStr = (String)config.get("myAddress");
		if (tmpStr != null) {
			tmpStr = tmpStr.trim();
		}
		if (tmpStr == null || tmpStr.length() == 0) {
			tmpStr = "localhost";
		}
		logger.info("  \"myAddress\" = \"" + tmpStr + "\"");
		localNode = new Node(tmpStr, myPort);
		
		tmpStr = (String)config.get("myPlatform");
		if (tmpStr == null) {
			tmpStr = "PC";
			
		} else {
			tmpStr = tmpStr.trim();
		}
		logger.info("  \"myPlatform\" = \"" + tmpStr + "\"");

		JSONArray knownPeers = (JSONArray)config.get("knownPeers");
		Set<String> wellKnownPeers;
		if (knownPeers != null) {
			Set<String> hosts = new HashSet<String>();
			for(Iterator<Object> i = knownPeers.iterator(); i.hasNext(); ) {
				String hostEntry = (String)i.next();
				hostEntry = hostEntry.trim();
				if (hostEntry.length() > 0) {
					hosts.add(hostEntry);
				}
			}
			wellKnownPeers = Collections.unmodifiableSet(hosts);
			
		} else {
			wellKnownPeers = Collections.emptySet();
			logger.warn("No wellKnownPeers defined, it is unlikely to work");
		}
		
		PeerNetwork network = new PeerNetwork("Default network", localNode, wellKnownPeers);
		network.boot();
	}
}
