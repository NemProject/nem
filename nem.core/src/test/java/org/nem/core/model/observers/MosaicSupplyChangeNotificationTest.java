package org.nem.core.model.observers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Supply;
import org.nem.core.test.Utils;

public class MosaicSupplyChangeNotificationTest {

	@Test
	public void canCreateNotification() {
		// Act:
		final Account supplier = Utils.generateRandomAccount();
		final MosaicId mosaicId = new MosaicId(new NamespaceId("foo"), "bar");
		final Supply delta = Supply.fromValue(123);
		final MosaicSupplyChangeNotification notification = new MosaicSupplyChangeNotification(supplier, mosaicId, delta,
				MosaicSupplyType.Create);

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.MosaicSupplyChange));
		MatcherAssert.assertThat(notification.getSupplier(), IsSame.sameInstance(supplier));
		MatcherAssert.assertThat(notification.getMosaicId(), IsSame.sameInstance(mosaicId));
		MatcherAssert.assertThat(notification.getDelta(), IsSame.sameInstance(delta));
		MatcherAssert.assertThat(notification.getSupplyType(), IsEqual.equalTo(MosaicSupplyType.Create));
	}
}
