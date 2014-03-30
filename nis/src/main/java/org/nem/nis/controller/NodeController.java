package org.nem.nis.controller;

import org.nem.core.serialization.Deserializer;
import org.nem.nis.AccountAnalyzer;
import org.nem.peer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST node controller.
 */
@RestController
public class NodeController {

    @Autowired
    private AccountAnalyzer accountAnalyzer;

    /**
     * Gets information about the running node.
     *
     * @return Information about the running node.
     */
    @RequestMapping(value="/node/info", method = RequestMethod.GET)
    public String getInfo() {
    	final Node node = getNetwork().getLocalNode();
        return ControllerUtils.serialize(node);
    }

    /**
     * Gets a list of the active and inactive nodes currently known  by the running node.
     *
     * @return A list of the active and inactive nodes currently known  by the running node.
     */
    @RequestMapping(value="/node/peer-list", method = RequestMethod.GET)
    public String getPeerList() {
    	final NodeCollection nodes = getNetwork().getNodes();
        return ControllerUtils.serialize(nodes);
    }

    /**
     * Ping that means the pinging node is part of the NEM P2P network.
     *
     * @param body The request body (information about the requesting node).
     * @return OK json on success.
     */
    @RequestMapping(value="/node/ping", method = RequestMethod.POST)
    public String ping(@RequestBody String body) {
        final Deserializer deserializer = ControllerUtils.getDeserializer(body, this.accountAnalyzer);
        final Node node = new Node(deserializer);
        getNetwork().getNodes().update(node, NodeStatus.ACTIVE);
        return Utils.jsonOk();
    }

    private static PeerNetwork getNetwork() {
        return PeerNetworkHost.getDefaultHost().getNetwork();
    }
}
