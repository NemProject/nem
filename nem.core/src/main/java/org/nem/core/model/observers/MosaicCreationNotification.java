package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.Mosaic;

public class MosaicCreationNotification extends Notification {
	private final Mosaic mosaic;

	public MosaicCreationNotification(final Mosaic mosaic) {
		super(NotificationType.MosaicCreation);
		this.mosaic = mosaic;
	}

	public Mosaic getMosaic() {
		return this.mosaic;
	}
}
