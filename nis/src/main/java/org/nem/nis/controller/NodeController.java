package org.nem.nis.controller;

import org.nem.core.serialization.Deserializer;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.*;
import org.nem.peer.*;
import org.nem.peer.trust.NodeExperiencesPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST node controller.
 */
@RestController
public class NodeController {

	private NisPeerNetworkHost host;

	@Autowired(required = true)
	NodeController(final NisPeerNetworkHost host) {
		this.host = host;
	}

	/**
	 * Gets information about the running node.
	 *
	 * @return Information about the running node.
	 */
	@RequestMapping(value = "/node/info", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public Node getInfo() {
		return this.host.getNetwork().getLocalNode();
	}

	/**
	 * Gets a list of the active and inactive nodes currently known  by the running node.
	 *
	 * @return A list of the active and inactive nodes currently known  by the running node.
	 */
	@RequestMapping(value = "/node/peer-list", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public NodeCollection getPeerList() {
		return this.host.getNetwork().getNodes();
	}

	/**
	 * Ping that means the pinging node is part of the NEM P2P network.
	 *
	 * @param deserializer The request body (information about the requesting node).
	 *
	 * @return OK json on success.
	 */
	@RequestMapping(value = "/node/ping", method = RequestMethod.POST)
	@P2PApi
	public String ping(final Deserializer deserializer) {
		final NodeExperiencesPair pair = new NodeExperiencesPair(deserializer);

		final PeerNetwork network = this.host.getNetwork();
		network.getNodes().update(pair.getNode(), NodeStatus.ACTIVE);
		network.setRemoteNodeExperiences(pair);
		return Utils.jsonOk();
	}
}
