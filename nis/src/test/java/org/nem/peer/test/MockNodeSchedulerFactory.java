package org.nem.peer.test;

import org.nem.peer.*;
import org.nem.peer.scheduling.*;

/**
 * A mock node scheduler factory.
 */
public class MockNodeSchedulerFactory extends ParallelSchedulerFactory<Node> {

	final Scheduler<Node> scheduler;
	final int triggerIndex;
	int numCreateSchedulerCalls;

	/**
	 * Creates a new mock node scheduler factory.
	 */
	public MockNodeSchedulerFactory() {
		this(null, -1);
	}

	/**
	 * Creates a new mock node scheduler factory.
	 *
	 * @param scheduler    The scheduler to return.
	 * @param triggerIndex The call number at which the custom scheduler should be returned.
	 */
	public MockNodeSchedulerFactory(final Scheduler<Node> scheduler, final int triggerIndex) {
		super(100);
		this.scheduler = scheduler;
		this.triggerIndex = triggerIndex;
	}

	@Override
	public Scheduler<Node> createScheduler(final Action<Node> action) {
		return this.triggerIndex == this.numCreateSchedulerCalls++
				? this.scheduler
				: super.createScheduler(action);
	}
}
