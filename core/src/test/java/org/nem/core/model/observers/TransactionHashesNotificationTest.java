package org.nem.core.model.observers;

import java.util.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;

public class TransactionHashesNotificationTest {
	@Test
	public void canCreateNotification() {
		// Act:
		final List<HashMetaDataPair> pairs = new ArrayList<>();
		for (int i = 0; i < 5; ++i) {
			pairs.add(new HashMetaDataPair(Utils.generateRandomHash(),
					new HashMetaData(new BlockHeight(12), Utils.generateRandomTimeStamp())));
		}

		final TransactionHashesNotification notification = new TransactionHashesNotification(pairs);

		// Assert:
		MatcherAssert.assertThat(notification.getType(), IsEqual.equalTo(NotificationType.TransactionHashes));
		MatcherAssert.assertThat(notification.getPairs().size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(notification.getPairs(), IsEqual.equalTo(pairs));
	}
}
