package org.nem.peer.test;

import org.nem.peer.*;
import org.nem.peer.scheduling.ParallelSchedulerFactory;

/**
 * A mock node scheduler factory.
 */
public class MockNodeSchedulerFactory extends ParallelSchedulerFactory<Node> {

    /**
     * Creates a new mock node scheduler factory.
     */
    public MockNodeSchedulerFactory() {
        super(100);
    }
}
