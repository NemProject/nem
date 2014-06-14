package org.nem.nis.controller;

import javax.servlet.http.HttpServletRequest;

import org.nem.core.serialization.SerializableList;
import org.nem.deploy.CommonStarter;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.ExtendedNodeExperiencePair;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;
import org.nem.peer.trust.score.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST node controller.
 */
@RestController
public class NodeController {
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	NodeController(final NisPeerNetworkHost host) {
		this.host = host;
	}

	//region getInfo / getExtendedInfo

	/**
	 * Gets information about the running node.
	 *
	 * @return Information about the running node.
	 */
	@RequestMapping(value = "/node/info", method = RequestMethod.GET)
	@PublicApi
	public Node getInfo() {
		return this.host.getNetwork().getLocalNode();
	}

	/**
	 * Gets information about the running node.
	 *
	 * @param challenge The challenge.
	 * @return Information about the running node.
	 */
	@RequestMapping(value = "/node/info", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<Node> getInfo(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(localNode, localNode.getIdentity(), challenge);
	}

	/**
	 * Gets extended information about the running node.
	 *
	 * @return Extended information about the running node.
	 */
	@RequestMapping(value = "/node/extended-info", method = RequestMethod.GET)
	@ClientApi
	@PublicApi
	public NisNodeInfo getExtendedInfo() {
		return new NisNodeInfo(this.host.getNetwork().getLocalNode(), CommonStarter.META_DATA);
	}

	//endregion

	//region getPeerList / getActivePeerList

	/**
	 * Gets a list of the active and inactive nodes currently known by the
	 * running node.
	 *
	 * @return A list of the active and inactive nodes currently known by the
	 *         running node.
	 */
	@RequestMapping(value = "/node/peer-list/all", method = RequestMethod.GET)
	@PublicApi
	public NodeCollection getPeerList() {
		return this.host.getNetwork().getNodes();
	}

	/**
	 * Gets a list of the active nodes currently known by the running node.
	 *
	 * @return A list of the active nodes currently known by the running node.
	 */
	@RequestMapping(value = "/node/peer-list/active", method = RequestMethod.GET)
	@PublicApi
	public SerializableList<Node> getActivePeerList() {
		return new SerializableList<>(this.host.getNetwork().getNodes().getActiveNodes());
	}

	/**
	 * Gets a list of the active nodes currently known by the running node.
	 *
	 * @param challenge The challenge.
	 * @return A list of the active nodes currently known by the running node.
	 */
	@RequestMapping(value = "/node/peer-list/active", method = RequestMethod.POST)
	@P2PApi
	@PublicApi
	public AuthenticatedResponse<SerializableList<Node>> getActivePeerList(final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(this.getActivePeerList(), localNode.getIdentity(), challenge);
	}

	//endregion

	/**
	 * Gets the local node's experiences.
	 *
	 * @return Information about the experiences the local node has had with other nodes.
	 */
	@RequestMapping(value = "/node/experiences", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public SerializableList<ExtendedNodeExperiencePair> getExperiences() {
		final NodeExperiencesPair pair = this.host.getNetwork().getLocalNodeAndExperiences();

		final List<ExtendedNodeExperiencePair> nodeExperiencePairs = pair.getExperiences().stream()
				.map(this::extend)
				.collect(Collectors.toList());

		return new SerializableList<>(nodeExperiencePairs);
	}

	private ExtendedNodeExperiencePair extend(final NodeExperiencePair pair) {
		return new ExtendedNodeExperiencePair(
				pair.getNode(),
				pair.getExperience(),
				this.host.getSyncAttempts(pair.getNode()));
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
	 * address from the request.
	 *
	 * @param localEndpoint The local endpoint (what the local node knows about itself).
	 * @param request The http servlet request.
	 */
	@RequestMapping(value = "/node/cysm", method = RequestMethod.POST)
	@P2PApi
	public NodeEndpoint canYouSeeMe(
			@RequestBody final NodeEndpoint localEndpoint,
			final HttpServletRequest request) {
		// request.getRemotePort() is never the port on which the node is listening,
		// so let the client specify its desired port
		return new NodeEndpoint(
				request.getScheme(),
				request.getRemoteAddr(),
				localEndpoint.getBaseUrl().getPort());
	}
}
