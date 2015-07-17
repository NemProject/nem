package org.nem.core.model.observers;

import org.nem.core.model.mosaic.Mosaic;

/**
 * A notification that represents the creation of a new mosaic.
 */
public class MosaicCreationNotification extends Notification {
	private final Mosaic mosaic;

	/**
	 * Creates a new mosaic creation notification.
	 *
	 * @param mosaic The mosaic.
	 */
	public MosaicCreationNotification(final Mosaic mosaic) {
		super(NotificationType.MosaicCreation);
		this.mosaic = mosaic;
	}

	/**
	 * Gets the mosaic.
	 *
	 * @return The mosaic.
	 */
	public Mosaic getMosaic() {
		return this.mosaic;
	}
}
