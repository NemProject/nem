package org.nem.nis.controller;

import java.util.*;
import java.util.stream.Collectors;
import net.minidev.json.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.cache.DefaultExpiredMosaicCache;
import org.nem.nis.controller.requests.*;
import org.nem.nis.controller.viewmodels.ExpiredMosaicViewModel;
import org.nem.nis.state.*;
import org.nem.specific.deploy.NisConfiguration;

public class ExpiredMosaicControllerTest {
	@Test
	public void expiredMosaicsFailsWhenTrackExpiredMosaicsIsDisabled() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.nisConfiguration.isFeatureSupported(NodeFeature.TRACK_EXPIRED_MOSAICS)).thenReturn(false);

		final BlockHeightBuilder builder = new BlockHeightBuilder();
		builder.setHeight("123");

		// Act + Assert:
		ExceptionAssert.assertThrows(v -> context.controller.expiredMosaics(builder), UnsupportedOperationException.class);
	}

	@Test
	public void expiredMosaicsSucceedsWhenNoExpirationsAtBlock() {
		// Arrange:
		final TestContext context = new TestContext();

		final BlockHeightBuilder builder = new BlockHeightBuilder();
		builder.setHeight("130");

		// Act:
		final SerializableList<ExpiredMosaicViewModel> expiredMosaics = context.controller.expiredMosaics(builder);

		// Assert:
		MatcherAssert.assertThat(expiredMosaics.size(), IsEqual.equalTo(0));
	}

	@Test
	public void expiredMosaicsSucceedsWhenExpirationsAtBlock() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final TestContext context = new TestContext(address1, address2);

		final BlockHeightBuilder builder = new BlockHeightBuilder();
		builder.setHeight("123");

		// Act:
		final SerializableList<ExpiredMosaicViewModel> expiredMosaics = context.controller.expiredMosaics(builder);

		// Assert:
		MatcherAssert.assertThat(expiredMosaics.size(), IsEqual.equalTo(2));

		// - there are no accessors on the view model, so serialize and inspect the json
		final JSONObject jsonObject1 = JsonSerializer.serializeToJson(expiredMosaics.get(0));
		final JSONObject jsonObject2 = JsonSerializer.serializeToJson(expiredMosaics.get(1));

		final MosaicId mosaicId1 = deserializeMosaicId(jsonObject1);
		final MosaicId mosaicId2 = deserializeMosaicId(jsonObject2);

		MatcherAssert.assertThat(Arrays.asList(mosaicId1, mosaicId2),
				IsEquivalent.equivalentTo(Arrays.asList(Utils.createMosaicId(222), Utils.createMosaicId(333))));

		// - ensure balanceMap1 => mosaicId 222 and balanceMap2 => mosaicId 333
		final Map<Address, Quantity> balanceMap1 = this
				.deserializeBalanceMap(mosaicId1.equals(Utils.createMosaicId(222)) ? jsonObject1 : jsonObject2);
		final Map<Address, Quantity> balanceMap2 = this
				.deserializeBalanceMap(mosaicId1.equals(Utils.createMosaicId(222)) ? jsonObject2 : jsonObject1);

		MatcherAssert.assertThat(balanceMap1.keySet(), IsEquivalent.equivalentTo(Arrays.asList(address1, address2)));
		MatcherAssert.assertThat(balanceMap1.get(address1), IsEqual.equalTo(new Quantity(20000)));
		MatcherAssert.assertThat(balanceMap1.get(address2), IsEqual.equalTo(new Quantity(9999)));

		MatcherAssert.assertThat(balanceMap2.keySet(), IsEquivalent.equivalentTo(Collections.singletonList(address1)));
		MatcherAssert.assertThat(balanceMap2.get(address1), IsEqual.equalTo(new Quantity(30000)));

		// - check expired mosaic types (1 - expired, 2 - restored)
		MatcherAssert.assertThat((Integer) jsonObject1.get("expiredMosaicType"),
				IsEqual.equalTo(mosaicId1.equals(Utils.createMosaicId(222)) ? 1 : 2));
		MatcherAssert.assertThat((Integer) jsonObject2.get("expiredMosaicType"),
				IsEqual.equalTo(mosaicId1.equals(Utils.createMosaicId(222)) ? 2 : 1));
	}

	private MosaicId deserializeMosaicId(final JSONObject jsonObject) {
		return new MosaicId(new NamespaceId((String) ((JSONObject) jsonObject.get("mosaicId")).get("namespaceId")),
				(String) ((JSONObject) jsonObject.get("mosaicId")).get("name"));
	}

	private Map<Address, Quantity> deserializeBalanceMap(final JSONObject jsonObject) {
		JSONArray jsonBalances = (JSONArray) jsonObject.get("balances");
		return jsonBalances.stream().map(obj -> (JSONObject) obj)
				.collect(Collectors.toMap(jsonBalance -> Address.fromEncoded((String) jsonBalance.get("address")),
						jsonBalance -> new Quantity((Long) jsonBalance.get("quantity"))));
	}

	private static class TestContext {
		private final NisConfiguration nisConfiguration = Mockito.mock(NisConfiguration.class);
		private final DefaultExpiredMosaicCache expiredMosaicCache;
		private final ExpiredMosaicController controller;

		public TestContext() {
			this(Utils.generateRandomAddress(), Utils.generateRandomAddress());
		}

		public TestContext(final Address address1, final Address address2) {
			this.expiredMosaicCache = this.createAndSeedCache(address1, address2);
			this.controller = new ExpiredMosaicController(this.nisConfiguration, this.expiredMosaicCache);

			Mockito.when(this.nisConfiguration.isFeatureSupported(NodeFeature.TRACK_EXPIRED_MOSAICS)).thenReturn(true);
		}

		private DefaultExpiredMosaicCache createAndSeedCache(final Address address1, final Address address2) {
			final DefaultExpiredMosaicCache cache = new DefaultExpiredMosaicCache();
			final DefaultExpiredMosaicCache copy = cache.copy();

			final MosaicBalances balances1 = this.createMosaicBalancesWithSingleBalance(address1, 10000);
			copy.addExpiration(new BlockHeight(122), Utils.createMosaicId(111), balances1, ExpiredMosaicType.Expired);

			final MosaicBalances balances2 = this.createMosaicBalancesWithSingleBalance(address1, 20000);
			balances2.incrementBalance(address2, new Quantity(9999));
			copy.addExpiration(new BlockHeight(123), Utils.createMosaicId(222), balances2, ExpiredMosaicType.Expired);

			final MosaicBalances balances3 = this.createMosaicBalancesWithSingleBalance(address1, 30000);
			copy.addExpiration(new BlockHeight(123), Utils.createMosaicId(333), balances3, ExpiredMosaicType.Restored);

			final MosaicBalances balances4 = this.createMosaicBalancesWithSingleBalance(address1, 40000);
			copy.addExpiration(new BlockHeight(124), Utils.createMosaicId(444), balances4, ExpiredMosaicType.Expired);

			copy.commit();
			return cache;
		}

		private MosaicBalances createMosaicBalancesWithSingleBalance(final Address address, final long balance) {
			final MosaicBalances balances = new MosaicBalances();
			balances.incrementBalance(address, new Quantity(balance));
			return balances;
		}
	}
}
