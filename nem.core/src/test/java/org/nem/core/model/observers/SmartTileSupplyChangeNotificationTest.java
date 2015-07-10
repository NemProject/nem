package org.nem.core.model.observers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.SmartTileSupplyType;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;

public class SmartTileSupplyChangeNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final MosaicId mosaicId = new MosaicId(new NamespaceId("foo"), "bar");
		final SmartTileSupplyChangeNotification notification = new SmartTileSupplyChangeNotification(
				mosaicId,
				SmartTileSupplyType.CreateSmartTiles,
				Quantity.fromValue(123));

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.SmartTileSupplyChange));
		Assert.assertThat(notification.getMosaicId(), IsSame.sameInstance(mosaicId));
		Assert.assertThat(notification.getSupplyType(), IsEqual.equalTo(SmartTileSupplyType.CreateSmartTiles));
		Assert.assertThat(notification.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
	}
}
