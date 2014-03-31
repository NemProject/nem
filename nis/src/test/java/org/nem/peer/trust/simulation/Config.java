package org.nem.peer.trust.simulation;

import org.nem.peer.*;

import java.io.*;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Network simulator configuration.
 */
public class Config {

    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());

    /**
     * Number of node attributes in the node configuration file.
     */
    private final static int NODE_ATTRIBUTE_COUNT = 7;

    private final List<Entry> entries;
    private final NodeCollection nodes;
    private final Node localNode;


    public Config(final List<Entry> entries) {
        this.entries = entries;

        int numPreTrustedNodes = 0;
        for (final Entry entry : this.entries)
            numPreTrustedNodes += entry.isPreTrusted() ? 1 : 0;

        this.nodes = new NodeCollection();
        for (final Entry entry : this.entries)
            this.nodes.update(entry.getNode(), NodeStatus.ACTIVE);

        final Entry localNodeEntry = new Entry("127.0.0.1", false, false, 1.0, 1.0, false, false);
        this.entries.add(localNodeEntry);
        this.localNode = localNodeEntry.getNode();

        LOGGER.info(String.format("Found %d nodes (%d pre-trusted)", this.entries.size(), numPreTrustedNodes));
    }

    public List<Entry> getEntries() { return this.entries; }

    public NodeCollection getNodes() { return this.nodes; }

    public Node getLocalNode() { return this.localNode; }

    public Set<Node> getPreTrustedNodes() {
        final Set<Node> preTrustedNodes = new HashSet<>();
        for (final Entry entry : this.entries) {
            if (entry.isPreTrusted())
                preTrustedNodes.add(entry.getNode());
        }

        return preTrustedNodes;
    }

    /**
     * Loads configuration from a file.
     *
     * The file contains one node description per line.
     * Each description contains multiple node attributes that are semicolon delimited.
     * address:                     the ip of the node
     * evil:                        determines if the node is evil.
     * pre-trusted:                  determines if the node belongs to the set of pre-trusted nodes.
     * honest data probability:     probability that the node will send valid data.
     * honest feedback probability: probability that the node will give valid feedback about another node.
     * leech:                       determines if the node tries to attack the network by excessive requesting data.
     * collusive:                   determines if the node is colluding with other evil nodes.
     *
     * @param fileName The configuration file name.
     */
    public static Config loadFromFile(final String fileName) throws IOException {
        LOGGER.info(String.format("Loading nodes from configuration file <%s>", fileName));

        final List<Entry> entries = new ArrayList<>();

        final File file = new File(fileName);
        try (final FileReader fileReader = new FileReader(file)) {
            try (final BufferedReader reader = new BufferedReader(fileReader)) {

                while (reader.ready())
                    entries.add(new Entry(reader.readLine()));
            }
        }

        return new Config(entries);
    }

    public static class Entry {

        final boolean isPreTrusted;
        final NodeBehavior behavior;
        final Node node;

        public Entry(
            final String address,
            final boolean isEvil,
            final boolean isPreTrusted,
            final double honestDataProbability,
            final double honestFeedbackProbability,
            final boolean isLeech,
            final boolean isCollusive) {

            this.isPreTrusted = isPreTrusted;

            this.behavior = new NodeBehavior(
                isEvil,
                honestDataProbability,
                honestFeedbackProbability,
                isLeech,
                isCollusive);

            NodeEndpoint endpoint = new NodeEndpoint("http", address, 8000);
            this.node = new Node(endpoint, "PC", "NEM SIMULATOR");
        }

        public Entry(final String line) {
            final String[] nodeAttributes = line.split(";");
            if (nodeAttributes.length != NODE_ATTRIBUTE_COUNT)
                throw new InvalidParameterException(String.format("Malformed data in configuration file [%s]", line));

            this.isPreTrusted = nodeAttributes[2].equals("1");

            boolean isEvil = nodeAttributes[1].equals("1");
            double honestDataProbability = Double.parseDouble(nodeAttributes[3]);
            double honestFeedbackProbability = Double.parseDouble(nodeAttributes[4]);
            boolean isLeech = nodeAttributes[5].equals("1");
            boolean isCollusive = nodeAttributes[6].equals("1");
            this.behavior = new NodeBehavior(
                isEvil,
                honestDataProbability,
                honestFeedbackProbability,
                isLeech,
                isCollusive);

            NodeEndpoint endpoint = new NodeEndpoint("http", nodeAttributes[0], 8000);
            this.node = new Node(endpoint, "PC", "NEM SIMULATOR");
        }

        public boolean isPreTrusted() { return this.isPreTrusted; }

        public NodeBehavior getBehavior() { return this.behavior; }

        public Node getNode() { return this.node; }
    }
}