package org.nem.core.model;

import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class NisInfo implements SerializableEntity {
	private String version;
	private String appName;
	private String signerName;
	private long runningTime;

	public NisInfo(ApplicationMetaData metaData) {
		appName = metaData.getAppName();
		version = metaData.getVersion();
		signerName = metaData.getNemCertificateSigner();
		runningTime = System.currentTimeMillis() - metaData.getStartTime();
	}

	public NisInfo(Deserializer deserializer) {
		appName = deserializer.readString("appName");
		version = deserializer.readString("version");
		signerName = deserializer.readString("signerName");
		runningTime = deserializer.readLong("runningTime");
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public long getRunningTime() {
		return runningTime;
	}

	public void setRunningTime(long runningTime) {
		this.runningTime = runningTime;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	@Override
	public void serialize(Serializer serializer) {
		//
		serializer.writeString("appName", appName);
		serializer.writeString("version", version);
		serializer.writeLong("runningTime", runningTime);
		serializer.writeString("signerName", signerName);
	}

}
