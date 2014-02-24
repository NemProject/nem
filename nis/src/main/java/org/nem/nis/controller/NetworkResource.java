package org.nem.nis.controller;

import net.minidev.json.JSONObject;

import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thies1965: Extended class to suite the URI RMM2 level
 * Resource: "network"
 * This class covers the REST api for the Resource "network"
 * 
 * @author Thies196
 *
 */
@RestController
public class NetworkResource {
	
    @RequestMapping(value="/network", method = RequestMethod.GET)
    public String getDetails()
    {
    	PeerNetwork peerNetwork = PeerNetwork.getDefaultNetwork();
    	
    	JSONObject obj= peerNetwork.generatePeerList();
		
        return obj.toJSONString() + "\r\n";
    }
	
}
