package org.nem.peer.test;

import org.junit.Assert;
import org.nem.core.test.IsEquivalent;
import org.nem.peer.Node;
import org.nem.peer.NodeCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Static class containing asserts that are used to validate NodeCollection objects.
 */
public class NodeCollectionAssert {

    /**
     * Asserts that nodes have matching active and inactive hosts.
     *
     * @param nodes The nodes.
     * @param expectedActiveHosts The expected active hosts.
     * @param expectedInactiveHosts The expected inactive hosts.
     */
    public static void areHostsEquivalent(
        final NodeCollection nodes,
        final String[] expectedActiveHosts,
        final String[] expectedInactiveHosts) {
        // Assert:
        Assert.assertThat(getHosts(nodes.getActiveNodes()), IsEquivalent.equivalentTo(expectedActiveHosts));
        Assert.assertThat(getHosts(nodes.getInactiveNodes()), IsEquivalent.equivalentTo(expectedInactiveHosts));
    }

    private static List<String> getHosts(final Collection<Node> nodes) {
        final List<String> hosts = new ArrayList<>();
        for (final Node node : nodes)
            hosts.add(node.getEndpoint().getBaseUrl().getHost());
        return hosts;
    }

    /**
     * Asserts that nodes have matching active and inactive ports.
     *
     * @param nodes The nodes.
     * @param expectedActiveHosts The expected active ports.
     * @param expectedInactiveHosts The expected inactive ports.
     */
    public static void arePortsEquivalent(
        final NodeCollection nodes,
        final Integer[] expectedActiveHosts,
        final Integer[] expectedInactiveHosts) {
        // Assert:
        Assert.assertThat(getPorts(nodes.getActiveNodes()), IsEquivalent.equivalentTo(expectedActiveHosts));
        Assert.assertThat(getPorts(nodes.getInactiveNodes()), IsEquivalent.equivalentTo(expectedInactiveHosts));
    }

    private static List<Integer> getPorts(final Collection<Node> nodes) {
        final List<Integer> posts = new ArrayList<>();
        for (final Node node : nodes)
            posts.add(node.getEndpoint().getBaseUrl().getPort());
        return posts;
    }

    /**
     * Asserts that nodes have matching active and inactive platforms.
     *
     * @param nodes The nodes.
     * @param expectedActivePlatforms The expected active platforms.
     * @param expectedInactivePlatforms The expected inactive platforms.
     */
    public static void arePlatformsEquivalent(
        final NodeCollection nodes,
        final String[] expectedActivePlatforms,
        final String[] expectedInactivePlatforms) {
        // Assert:
        Assert.assertThat(getPlatforms(nodes.getActiveNodes()), IsEquivalent.equivalentTo(expectedActivePlatforms));
        Assert.assertThat(getPlatforms(nodes.getInactiveNodes()), IsEquivalent.equivalentTo(expectedInactivePlatforms));
    }

    private static List<String> getPlatforms(final Collection<Node> nodes) {
        final List<String> platforms = new ArrayList<>();
        for (final Node node : nodes)
            platforms.add(node.getPlatform());
        return platforms;
    }
}
