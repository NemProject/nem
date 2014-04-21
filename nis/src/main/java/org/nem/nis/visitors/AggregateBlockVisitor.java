package org.nem.nis.visitors;

import org.nem.core.model.Block;

import java.util.List;

/**
 * Aggregate block visitor.
 */
public class AggregateBlockVisitor implements BlockVisitor {

	private List<BlockVisitor> visitors;

	/**
	 * Creates a new aggregate block visitor.
	 *
	 * @param visitors The aggregated visitors.
	 */
	public AggregateBlockVisitor(final List<BlockVisitor> visitors) {
		this.visitors = visitors;
	}

	@Override
	public void visit(Block parentBlock, final Block block) {
		for (final BlockVisitor visitor : visitors) {
			visitor.visit(parentBlock, block);
		}
	}
}
