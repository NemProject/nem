package org.nem.nis.controller;

import org.nem.nis.NisPeerNetworkHost;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST node controller.
 */
@RestController
public class NodeController {

    @Autowired
    private NisPeerNetworkHost host;

    @RequestMapping(value="/node/info", method = RequestMethod.GET)
    public String getInfo() {
    	final Node node = this.host.getNetwork().getLocalNode();
        return ControllerUtils.serialize(node);
    }
    
    @RequestMapping(value="/node/peer-list", method = RequestMethod.GET)
    public String getPeerList() {
    	final NodeCollection nodes = this.host.getNetwork().getNodes();
        return ControllerUtils.serialize(nodes);
    }
}
