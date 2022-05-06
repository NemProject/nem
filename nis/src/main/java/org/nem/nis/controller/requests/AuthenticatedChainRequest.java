package org.nem.nis.controller.requests;

import org.nem.core.serialization.Deserializer;
import org.nem.peer.node.*;
import org.nem.peer.requests.ChainRequest;

/**
 * An authenticated request that has a chain request parameter. <br>
 * This is glue code that allows automatic deserialization to work without needing to hydrate multiple constructor parameters. This class is
 * required because the base class (AuthenticatedRequest) doesn't have a constructor that accepts a single Deserializer parameter.
 */
public class AuthenticatedChainRequest extends AuthenticatedRequest<ChainRequest> {
	/**
	 * Creates a new authenticated request.
	 *
	 * @param request The chain request.
	 * @param challenge The node challenge.
	 */
	public AuthenticatedChainRequest(final ChainRequest request, final NodeChallenge challenge) {
		super(request, challenge);
	}

	/**
	 * Creates a new authenticated request.
	 *
	 * @param deserializer The deserializer
	 */
	public AuthenticatedChainRequest(final Deserializer deserializer) {
		super(deserializer, ChainRequest::new);
	}
}
