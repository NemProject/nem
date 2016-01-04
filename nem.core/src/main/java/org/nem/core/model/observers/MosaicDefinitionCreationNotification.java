package org.nem.core.model.observers;

import org.nem.core.model.mosaic.MosaicDefinition;

/**
 * A notification that represents the creation of a new mosaic definition.
 */
public class MosaicDefinitionCreationNotification extends Notification {
	private final MosaicDefinition mosaicDefinition;

	/**
	 * Creates a new mosaic definition creation notification.
	 *
	 * @param mosaicDefinition The mosaic definition.
	 */
	public MosaicDefinitionCreationNotification(final MosaicDefinition mosaicDefinition) {
		super(NotificationType.MosaicDefinitionCreation);
		this.mosaicDefinition = mosaicDefinition;
	}

	/**
	 * Gets the mosaic definition.
	 *
	 * @return The mosaic definition.
	 */
	public MosaicDefinition getMosaicDefinition() {
		return this.mosaicDefinition;
	}
}
