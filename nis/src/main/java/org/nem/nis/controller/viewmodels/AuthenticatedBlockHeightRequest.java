package org.nem.nis.controller.viewmodels;

import org.nem.core.model.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.peer.node.*;

/**
 * An authenticated request that has a block height parameter.
 *
 * TODO: This is glue that allows the automatic deserialization to work without hydrating multiple constructor parameters.
 * (The problem is that the base class doesn't have a constructor that accepts a single deserializer parameter)
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