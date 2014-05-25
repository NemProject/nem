package org.nem.core.metadata;

import java.security.cert.X509Certificate;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class ApplicationMetaData implements SerializableEntity {
	final private String appName;
	final private String version;
	final private String nemCertificateSigner;
	final private long startTime;

	public ApplicationMetaData(String appName, String version, X509Certificate nemCertificate, long startTime) {
		//
		this.appName = appName;
		this.version = version;
		if (nemCertificate != null) {
			this.nemCertificateSigner = nemCertificate.getIssuerX500Principal().getName();
		} else {
			this.nemCertificateSigner = null;
		}
		this.startTime = startTime;
	}

	public String getAppName() {
		return appName;
	}

	public String getVersion() {
		return version;
	}

	public String getNemCertificateSigner() {
		return nemCertificateSigner;
	}

	public long getStartTime() {
		return startTime;
	}

	public ApplicationMetaData(Deserializer deserializer) {
		//
		this.appName = deserializer.readString("application");
		this.version = deserializer.readString("version");
		this.nemCertificateSigner = deserializer.readString("nemSigner");
		this.startTime = deserializer.readLong("startTime");
	}

	@Override
	public void serialize(Serializer serializer) {
		//
		serializer.writeString("application", appName);
		serializer.writeString("version", version);
		serializer.writeString("nemSigner", nemCertificateSigner);
		serializer.writeLong("startTime", startTime);
	}

}
