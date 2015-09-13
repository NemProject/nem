package org.nem.nis.controller.viewmodels;

import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

public class GreetingViewModel implements SerializableEntity {
	private String content;

	public GreetingViewModel(final String content) {
		this.content = content;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("content", this.content);
	}
}
