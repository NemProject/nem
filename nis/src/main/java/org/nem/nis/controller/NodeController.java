package org.nem.nis.controller;

import org.nem.peer.*;
import org.springframework.web.bind.annotation.*;

/**
 * REST node controller.
 */
@RestController
public class NodeController {

    @RequestMapping(value="/node/info", method = RequestMethod.GET)
    public String getInfo() {
    	final Node node = getNetwork().getLocalNode();
        return ControllerUtils.serialize(node);
    }
    
    @RequestMapping(value="/node/peer-list", method = RequestMethod.GET)
    public String getPeerList() {
    	final NodeCollection nodes = getNetwork().getNodes();
        return ControllerUtils.serialize(nodes);
    }

    private static PeerNetwork getNetwork() {
        return PeerNetworkHost.getDefaultHost().getNetwork();
    }
}
