package org.nem.nis.controller.requests;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.peer.node.*;

/**
 * An authenticated request that has a block height parameter. <br>
 * This is glue code that allows automatic deserialization to work without needing to hydrate multiple constructor parameters. This class is
 * required because the base class (AuthenticatedRequest) doesn't have a constructor that accepts a single Deserializer parameter.
 */
public class AuthenticatedBlockHeightRequest extends AuthenticatedRequest<BlockHeight> {

	/**
	 * Creates a new authenticated request.
	 *
	 * @param height The block height.
	 * @param challenge The node challenge.
	 */
	public AuthenticatedBlockHeightRequest(final BlockHeight height, final NodeChallenge challenge) {
		super(height, challenge);
	}

	/**
	 * Creates a new authenticated request.
	 *
	 * @param deserializer The deserializer
	 */
	public AuthenticatedBlockHeightRequest(final Deserializer deserializer) {
		super(deserializer, BlockHeight::new);
	}
}
