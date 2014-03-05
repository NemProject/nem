package org.nem.peer.v2;

import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Information about a node in the NEM network.
 */
public class NodeInfo implements SerializableEntity {

    private final static int DEFAULT_VERSION = 2;
    private final static String DEFAULT_PLATFORM = "PC";

    private final NodeAddress address;
    private final String platform;
    private final Integer version;
    private final String application;

    /**
     * Creates a new node info.
     *
     * @param address The address.
     * @param platform The platform.
     * @param application The application.
     */
    public NodeInfo(final NodeAddress address, final String platform, final String application) {
        this.address = address;
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
        this.address = deserializer.readObject("address", new ObjectDeserializer<NodeAddress>() {
            @Override
            public NodeAddress deserialize(final Deserializer deserializer) {
                return new NodeAddress(deserializer);
            }
        });

        this.platform = deserializer.readString("platform");

        final Integer version = deserializer.readInt("version");
        this.version = null == version ? DEFAULT_VERSION : version;

        this.application = deserializer.readString("application");
        this.ensureValidity();
    }

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeObject("address", this.address);
        serializer.writeString("platform", this.platform);
        serializer.writeInt("version", this.version);
        serializer.writeString("application", this.application);
    }

    //region Getters and Setters

    /**
     * Gets the address.
     *
     * @return The address.
     */
    public NodeAddress getAddress() { return this.address; }

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
        if (null == this.address)
            throw new InvalidParameterException("address must be non-null");
    }

    @Override
    public int hashCode() {
        return this.address.hashCode() ^
            this.platform.hashCode() ^
            this.version.hashCode() ^
            this.application.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NodeInfo))
            return false;

        NodeInfo rhs = (NodeInfo)obj;
        return this.address.equals(rhs.address) &&
            this.platform.equals(rhs.platform) &&
            this.version.equals(rhs.version) &&
            this.application.equals(rhs.application);
    }
}
