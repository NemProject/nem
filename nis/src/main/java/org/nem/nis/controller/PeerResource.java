package org.nem.nis.controller;

import org.nem.core.serialization.JsonSerializer;
import org.nem.peer.*;
import org.springframework.web.bind.annotation.*;

/**
 * REST peer controller.
 */
@RestController
public class PeerResource {

    @RequestMapping(value="/node/info", method = RequestMethod.GET)
    public String getInfo()
    {
    	final PeerNetwork network = PeerNetworkHost.getDefaultHost().getNetwork();
    	final Node node = network.getLocalNode();
        return JsonSerializer.serializeToJson(node).toString() + "\r\n";
    }
    
    @RequestMapping(value="/node/peer-list", method = RequestMethod.GET)
    public String getPeerList()
    {
        final PeerNetwork network = PeerNetworkHost.getDefaultHost().getNetwork();
    	final NodeCollection nodes = network.getNodes();
        return JsonSerializer.serializeToJson(nodes).toString() + "\r\n";
    }
}
