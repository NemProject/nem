package org.nem.nis.controller.requests;

import org.nem.core.serialization.*;
import org.nem.peer.node.*;

/**
 * An authenticated request that has an unconfirmed transactions request parameter.
 * <br/>
 * This is glue code that allows automatic deserialization to work
 * without needing to hydrate multiple constructor parameters.
 * This class is required because the base class (AuthenticatedRequest) doesn't
 * have a constructor that accepts a single Deserializer parameter.
 */
public class AuthenticatedUnconfirmedTransactionsRequest extends AuthenticatedRequest<UnconfirmedTransactionsRequest> {
	private static int availableBytes = 0;

	/**
	 * Creates a new authenticated request.
	 *
	 * @param request The unconfirmed transactions request.
	 * @param challenge The node challenge.
	 */
	public AuthenticatedUnconfirmedTransactionsRequest(final UnconfirmedTransactionsRequest request, final NodeChallenge challenge) {
		super(request, challenge);
	}

	/**
	 * Creates a new authenticated request.
	 *
	 * @param challenge The node challenge.
	 */
	public AuthenticatedUnconfirmedTransactionsRequest(final NodeChallenge challenge) {
		super(new UnconfirmedTransactionsRequest(), challenge);
	}

	/**
	 * Creates a new authenticated request.
	 *
	 * @param deserializer The deserializer
	 */
	public AuthenticatedUnconfirmedTransactionsRequest(final Deserializer deserializer) {
		// TODO Remove this ugly fix in the next release!
		super((availableBytes = ((BinaryDeserializer)deserializer).availableBytes()) > 68
						? deserializer.readObject("entity", UnconfirmedTransactionsRequest::new)
						: new UnconfirmedTransactionsRequest(),
				availableBytes > 68
						? deserializer.readObject("challenge", NodeChallenge::new)
						: new NodeChallenge(deserializer));
	}
}
