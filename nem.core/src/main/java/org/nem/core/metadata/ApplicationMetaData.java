package org.nem.core.metadata;

import org.nem.core.serialization.*;
import org.nem.core.time.*;

import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Meta data information about the application.
 */
public class ApplicationMetaData implements SerializableEntity {
	private final String appName;
	private final String version;
	private final String certificateSigner;
	private final TimeProvider timeProvider;
	private final TimeInstant startTime;
	private final TimeInstant currentTime;

	/**
	 * Creates a new application meta data instance.
	 *
	 * @param appName The application name.
	 * @param version The application version.
	 * @param certificate The application certificate.
	 * @param timeProvider The time provider.
	 */
	public ApplicationMetaData(
			final String appName,
			final String version,
			final X509Certificate certificate,
			final TimeProvider timeProvider) {
		this.appName = appName;
		this.version = version;
		this.certificateSigner = null == certificate ? null : certificate.getIssuerX500Principal().getName();
		this.timeProvider = timeProvider;
		this.startTime = this.timeProvider.getCurrentTime();
		this.currentTime = TimeInstant.ZERO;
	}

	/**
	 * Deserializes an application meta data instance.
	 *
	 * @param deserializer The deserializer
	 */
	public ApplicationMetaData(final Deserializer deserializer) {
		this.appName = deserializer.readString("application");
		this.version = deserializer.readString("version");
		this.certificateSigner = deserializer.readOptionalString("signer");
		this.startTime = TimeInstant.readFrom(deserializer, "startTime");
		this.currentTime = TimeInstant.readFrom(deserializer, "currentTime");
		this.timeProvider = null;
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
	public TimeInstant getStartTime() {
		return this.startTime;
	}

	/**
	 * Gets the current time of the application.
	 *
	 * @return The current time of the application.
	 */
	public TimeInstant getCurrentTime() {
		return null == this.timeProvider ? this.currentTime : this.timeProvider.getCurrentTime();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("application", this.appName);
		serializer.writeString("version", this.version);
		serializer.writeString("signer", this.certificateSigner);
		TimeInstant.writeTo(serializer, "startTime", this.startTime);
		TimeInstant.writeTo(serializer, "currentTime", this.getCurrentTime());
	}

	@Override
	public int hashCode() {
		return this.appName.hashCode() ^
				this.version.hashCode() ^
				(this.certificateSigner == null ? 0 : this.certificateSigner.hashCode());
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ApplicationMetaData)) {
			return false;
		}

		final ApplicationMetaData rhs = (ApplicationMetaData)obj;
		return this.appName.equals(rhs.appName) &&
				this.version.equals(rhs.version) &&
				Objects.equals(this.certificateSigner, rhs.certificateSigner);
	}
}
