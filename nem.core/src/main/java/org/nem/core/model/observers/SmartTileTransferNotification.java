package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.SmartTile;

/**
 * A notification that one account has transferred a quantity of a smart tile to another account.
 */
public class SmartTileTransferNotification extends Notification {
	private final Account sender;
	private final Account recipient;
	private final SmartTile smartTile; // TODO 20150715 J-B: shouldn't this be mosaic id? 20150716 BR -> J: right now no.

	/**
	 * Creates a new smart tile transfer notification.
	 *
	 * @param sender The sender.
	 * @param recipient The recipient.
	 * @param smartTile The smart tile.
	 */
	public SmartTileTransferNotification(
			final Account sender,
			final Account recipient,
			final SmartTile smartTile) {
		super(NotificationType.SmartTileTransfer);
		this.sender = sender;
		this.recipient = recipient;
		this.smartTile = smartTile;
	}

	/**
	 * Gets the sender.
	 *
	 * @return The sender.
	 */
	public Account getSender() {
		return this.sender;
	}

	/**
	 * Gets the recipient.
	 *
	 * @return The recipient.
	 */
	public Account getRecipient() {
		return this.recipient;
	}

	/**
	 * Gets the smart tile.
	 *
	 * @return The smart tile.
	 */
	public SmartTile getSmartTile() {
		return this.smartTile;
	}
}
