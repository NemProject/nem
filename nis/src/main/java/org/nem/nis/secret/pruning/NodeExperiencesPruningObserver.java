package org.nem.nis.secret.pruning;

import org.nem.nis.secret.BlockNotificationContext;
import org.nem.peer.trust.score.NodeExperiences;

/**
 * Pruning observer that automatically prunes outdated node experiences once every 360 blocks.
 */
public class NodeExperiencesPruningObserver extends AbstractPruningObserver {
	private final NodeExperiences nodeExperiences;

	/**
	 * Creates a new observer.
	 *
	 * @param nodeExperiences The node experiences.
	 */
	public NodeExperiencesPruningObserver(final NodeExperiences nodeExperiences) {
		this.nodeExperiences = nodeExperiences;
	}
	@Override
	protected void prune(BlockNotificationContext context) {
		this.nodeExperiences.prune(context.getTimeStamp());
	}
}
