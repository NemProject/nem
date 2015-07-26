package org.nem.core.model.observers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.Utils;

public class SmartTileSupplyChangeNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account supplier = Utils.generateRandomAccount();
		final MosaicId mosaicId = new MosaicId(new NamespaceId("foo"), "bar");
		final Quantity delta = Quantity.fromValue(123);
		final SmartTileSupplyChangeNotification notification = new SmartTileSupplyChangeNotification(
				supplier,
				mosaicId,
				delta,
				SmartTileSupplyType.CreateSmartTiles);

		// Assert:
		Assert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.SmartTileSupplyChange));
		Assert.assertThat(notification.getSupplier(), IsSame.sameInstance(supplier));
		Assert.assertThat(notification.getMosaicId(), IsSame.sameInstance(mosaicId));
		Assert.assertThat(notification.getDelta(), IsSame.sameInstance(delta));
		Assert.assertThat(notification.getSupplyType(), IsEqual.equalTo(SmartTileSupplyType.CreateSmartTiles));
	}
}
