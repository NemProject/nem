package org.nem.nis.time.synchronization;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.NodeAge;
import org.nem.nis.test.TimeSyncUtils;
import org.nem.nis.time.synchronization.filter.*;

import java.util.*;

public class DefaultSynchronizationStrategyTest {

	@Test
	public void defaultSynchronizationStrategyCanBeConstructed() {
		// Act:
		new DefaultSynchronizationStrategy(createAggregateFilter());
	}

	@Test(expected = SynchronizationException.class)
	public void defaultSynchronizationStrategyCtorThrowsIfFilterIsNull() {
		// Act:
		new DefaultSynchronizationStrategy(null);
	}

	@Test(expected = SynchronizationException.class)
	public void calculateTimeOffsetThrowsIfNoSamplesAreAvailable() {
		// Arrange:
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(createAggregateFilter());

		// Act:
		strategy.calculateTimeOffset(new ArrayList<>(), new NodeAge(0));
	}

	@Test
	public void calculateTimeOffsetDelegatesToFilter() {
		// Act:
		final List<SynchronizationSample> samples = TimeSyncUtils.createTolerableSamples(0, 1, true);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(Mockito.any(), Mockito.any())).thenReturn(samples);
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(filter);

		// Act:
		strategy.calculateTimeOffset(samples, new NodeAge(0));

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(Mockito.any(), Mockito.any());
	}

	@Test
	public void calculateTimeOffsetWithMaximumCouplingReturnsCorrectValue() {
		// Act:
		final List<SynchronizationSample> samples = TimeSyncUtils.createTolerableSamples(0, 100, true);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(Mockito.any(), Mockito.any())).thenReturn(samples);
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(filter);

		// Act:
		final long offset = strategy.calculateTimeOffset(samples, new NodeAge(0));

		// Assert:
		Assert.assertThat(offset, IsEqual.equalTo((long)(100 * 101 / 2 * SynchronizationConstants.COUPLING_START / 100)));
	}


	@Test
	public void calculateTimeOffsetWithMinimumCouplingReturnsCorrectValue() {
		// Act:
		final List<SynchronizationSample> samples = TimeSyncUtils.createTolerableSamples(0, 100, true);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(Mockito.any(), Mockito.any())).thenReturn(samples);
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(filter);

		// Act:
		final long offset = strategy.calculateTimeOffset(samples, new NodeAge(SynchronizationConstants.START_DECAY_AFTER_ROUND + 10));

		// Assert:
		Assert.assertThat(offset, IsEqual.equalTo((long)(100 * 101 / 2 * SynchronizationConstants.COUPLING_MINIMUM / 100)));
	}

	@Test
	public void getCouplingReturnsCouplingStartForAgeSmallerThanOrEqualToStartDecayAfterRound() {
		// Arrange:
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(createAggregateFilter());

		// Assert:
		Assert.assertThat(strategy.getCoupling(new NodeAge(0)),
				IsEqual.equalTo(SynchronizationConstants.COUPLING_START));
		Assert.assertThat(strategy.getCoupling(new NodeAge(SynchronizationConstants.START_DECAY_AFTER_ROUND)),
				IsEqual.equalTo(SynchronizationConstants.COUPLING_START));
	}

	@Test
	public void getCouplingDecaysToCouplingMinimumAfterStartDecayAfterRound() {
		// Arrange:
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(createAggregateFilter());

		// Assert:
		assertDoubleIsWithingRange(
				strategy.getCoupling(new NodeAge(SynchronizationConstants.START_DECAY_AFTER_ROUND + 1)),
				SynchronizationConstants.COUPLING_MINIMUM,
				SynchronizationConstants.COUPLING_START);
		assertDoubleIsWithingRange(
				strategy.getCoupling(new NodeAge(SynchronizationConstants.START_DECAY_AFTER_ROUND + 5)),
				SynchronizationConstants.COUPLING_MINIMUM,
				SynchronizationConstants.COUPLING_START);
		Assert.assertThat(strategy.getCoupling(new NodeAge(SynchronizationConstants.START_DECAY_AFTER_ROUND + 10)),
				IsEqual.equalTo(SynchronizationConstants.COUPLING_MINIMUM));
	}

	private void assertDoubleIsWithingRange(final double value, final double min, final double max) {
		Assert.assertThat(value > min, IsEqual.equalTo(true));
		Assert.assertThat(value < max, IsEqual.equalTo(true));
	}

	private SynchronizationFilter createAggregateFilter() {
		return new AggregateSynchronizationFilter(Arrays.asList(new ClampingFilter(), new AlphaTrimmedMeanFilter()));
	}
}
