package org.nem.nis.boot;

import java.util.concurrent.CompletableFuture;
import org.nem.core.node.Node;

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
