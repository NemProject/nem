package org.nem.nis.boot;

import org.nem.core.node.Node;

import java.util.concurrent.CompletableFuture;

/**
 * Interface that exposes a function for booting a network.
 */
public interface NetworkHostBootstrapper {

	/**
	 * Boots the network.
	 *
	 * @param localNode The local node.
	 * @return Void future.
	 */
	CompletableFuture<Void> boot(Node localNode);
}
