package org.nem.core.metadata;

import java.security.cert.X509Certificate;

import org.nem.core.serialization.*;

/**
 * Meta data information about the application.
 */
public class ApplicationMetaData implements SerializableEntity {
	final private String appName;
	final private String version;
	final private String certificateSigner;
	final private long startTime;

	/**
	 * Creates a new application meta data instance.
	 *
	 * @param appName The application name.
	 * @param version The application version.
	 * @param certificate The application certificate.
	 * @param startTime The time that the application was started.
	 */
	public ApplicationMetaData(
			final String appName,
			final String version,
			final X509Certificate certificate,
			final long startTime) {
		this.appName = appName;
		this.version = version;
		this.certificateSigner = null == certificate ? null : certificate.getIssuerX500Principal().getName();
		this.startTime = startTime;
	}

	/**
	 * Deserializes an application meta data instance.
	 *
	 * @param deserializer The deserializer
	 */
	public ApplicationMetaData(Deserializer deserializer) {
		this.appName = deserializer.readString("application");
		this.version = deserializer.readString("version");
		this.certificateSigner = deserializer.readString("signer");
		this.startTime = deserializer.readLong("startTime");
	}

	/**
	 * Gets the application name.
	 *
	 * @return The application name.
	 */
	public String getAppName() {
		return this.appName;
	}

	/**
	 * Gets the application version.
	 *
	 * @return The application version.
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Gets the signer of the application certificate.
	 *
	 * @return the signer of the application certificate.
	 */
	public String getCertificateSigner() {
		return this.certificateSigner;
	}

	/**
	 * Gets the start time of the application.
	 *
	 * @return The start time of the application.
	 */
	public long getStartTime() {
		return this.startTime;
	}

	@Override
	public void serialize(Serializer serializer) {
		//
		serializer.writeString("application", this.appName);
		serializer.writeString("version", this.version);
		serializer.writeString("signer", this.certificateSigner);
		serializer.writeLong("startTime", this.startTime);
	}

}
