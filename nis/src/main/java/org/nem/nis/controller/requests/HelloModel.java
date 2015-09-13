package org.nem.nis.controller.requests;

import org.nem.core.serialization.Deserializer;

public class HelloModel {
	private String name;

	public HelloModel(final String name) {
		this.name = name;
	}

	public HelloModel(final Deserializer deserializer) {
		this.name = deserializer.readString("name");
	}

	public String getName() {
		return name;
	}
}
