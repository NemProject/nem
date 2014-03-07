package org.nem.peer.v2;

import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Information about a node in the NEM network.
 */
public class NodeInfo implements SerializableEntity {

    private final static int DEFAULT_VERSION = 2;
    private final static String DEFAULT_PLATFORM = "PC";

    private final NodeEndpoint endpoint;
    private final String platform;
    private final Integer version;
    private final String application;

    /**
     * Creates a new node info.
     *
     * @param endpoint The endpoint.
     * @param platform The platform.
     * @param application The application.
     */
    public NodeInfo(final NodeEndpoint endpoint, final String platform, final String application) {
        this.endpoint = endpoint;
        this.platform = null == platform ? DEFAULT_PLATFORM : platform;
        this.application = application;
        this.version = DEFAULT_VERSION;
        this.ensureValidity();
    }

    /**
     * Deserializes a node info.
     *
     * @param deserializer The deserializer.
     */
    public NodeInfo(final Deserializer deserializer) {
        this.endpoint = deserializer.readObject("endpoint", NodeEndpoint.DESERIALIZER);

        this.platform = deserializer.readString("platform");

        final Integer version = deserializer.readInt("version");
        this.version = null == version ? DEFAULT_VERSION : version;

        this.application = deserializer.readString("application");
        this.ensureValidity();
    }

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeObject("endpoint", this.endpoint);
        serializer.writeString("platform", this.platform);
        serializer.writeInt("version", this.version);
        serializer.writeString("application", this.application);
    }

    //region Getters and Setters

    /**
     * Gets the endpoint.
     *
     * @return The endpoint.
     */
    public NodeEndpoint getEndpoint() { return this.endpoint; }

    /**
     * Gets the platform.
     *
     * @return The platform.
     */
    public String getPlatform() { return this.platform; }

    /**
     * Gets the version.
     *
     * @return The version.
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * Gets the application.
     *
     * @return The application.
     */
    public String getApplication() {
        return this.application;
    }

    //endregion

    private void ensureValidity() {
        if (null == this.endpoint)
            throw new InvalidParameterException("endpoint must be non-null");
    }

    @Override
    public int hashCode() {
        return this.endpoint.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NodeInfo))
            return false;

        // TODO: review this change (making node equality the same as endpoint equality)
        NodeInfo rhs = (NodeInfo)obj;
        return this.endpoint.equals(rhs.endpoint);
    }
}
