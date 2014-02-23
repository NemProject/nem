package org.nem.nis.controller;

import net.minidev.json.JSONObject;

import org.nem.peer.Node;
import org.nem.peer.PeerInitializer;
import org.nem.peer.PeerNetwork;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thies1965: Extended class to suite the UIR RMM2 level
 * Resource: peer
 * This class covers the REST api for the Resource peer
 * 
 * @author gimre, Thies196
 *
 */
@RestController
public class PeerResource {

    @RequestMapping(value="/peer", method = RequestMethod.GET)
    public String getInfo()
    {
    	PeerNetwork peerNetwork = PeerNetwork.getDefaultNetwork();
    	Node node = peerNetwork.getLocalNode();
    	
    	JSONObject obj= node.generateNodeInfo();
		
        return obj.toJSONString() + "\r\n";
    }
	
}
