package org.nem.nis.controller;

import javax.servlet.http.HttpServletRequest;

import org.nem.deploy.CommonStarter;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.*;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;
import org.nem.peer.trust.score.NodeExperiencesPair;
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
	 * Gets extended information about the running node.
	 *
	 * @return Extended information about the running node.
	 */
	@RequestMapping(value = "/node/extended-info", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public NisNodeInfo getExtendedInfo() {
		return new NisNodeInfo(this.host.getNetwork().getLocalNode(), CommonStarter.META_DATA);
	}

	/**
	 * Gets a list of the active and inactive nodes currently known by the
	 * running node.
	 *
	 * @return A list of the active and inactive nodes currently known by the
	 *         running node.
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
	 * @param nodeExperiencesPair
	 *            Information about the experiences the pinging node has had
	 *            with other nodes.
	 */
	@RequestMapping(value = "/node/ping", method = RequestMethod.POST)
	@P2PApi
	public void ping(@RequestBody final NodeExperiencesPair nodeExperiencesPair) {
		final PeerNetwork network = this.host.getNetwork();
		network.getNodes().update(nodeExperiencesPair.getNode(), NodeStatus.ACTIVE);
		network.setRemoteNodeExperiences(nodeExperiencesPair);
	}

	/**
	 * Just return the Node info the requester "Can You See Me" using the IP
	 * address from the request. If the port is not the default NIS port where
	 * the requesting node is listening it has to be passed as parameter
	 * "myNisPort"
	 */
	@RequestMapping(value = "/node/cysm", method = RequestMethod.GET)
	@P2PApi
	public NodeEndpoint canYouSeeMe(final HttpServletRequest request) {
		String portStr = request.getParameter("myNisPort");
		int portNr = 7890;
		if (portStr != null) {
			try {
				portNr = Integer.parseInt(portStr);
			} catch (NumberFormatException nfe) {
				// we stick with the default for NIS
			}
		}
		return new NodeEndpoint(request.getScheme(), request.getRemoteAddr(),
		// remote port is never the
		// port on which node is listening
		// Maybe it has to be passed by the request get
				portNr);
	}
}
