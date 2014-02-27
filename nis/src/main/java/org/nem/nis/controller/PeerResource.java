package org.nem.nis.controller;

import org.json.JSONObject;

import org.nem.core.serialization.JsonSerializer;
import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thies1965: Extended class to suite the URI RMM2 level
 * Resource: peer
 * This class covers the REST api for the Resource peer
 * 
 * @author gimre, Thies196
 *
 */
@RestController
public class PeerResource {

    @RequestMapping(value="/node/info", method = RequestMethod.GET)
    public String getInfo()
    {
    	PeerNetwork peerNetwork = PeerNetwork.getDefaultNetwork();
    	Node node = peerNetwork.getLocalNode();
    	
    	//TODO: Serializer should depend on the requester GUI vs NEM Server
    	JsonSerializer serializer = new JsonSerializer();
    	node.serialize(serializer);

        return serializer.getObject().toString() + "\r\n";
    }
    
    @RequestMapping(value="/node/peerlist", method = RequestMethod.GET)
    public String getPeerList()
    {
    	PeerNetwork peerNetwork = PeerNetwork.getDefaultNetwork();
    	
    	//TODO: Switch to J's serializer
    	JSONObject obj= peerNetwork.generatePeerList();
		
        return obj.toString() + "\r\n";
    }
}
