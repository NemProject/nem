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
		final List<SynchronizationSample> samples = TimeSyncUtils.createTolerableSortedSamples(0, 1);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, new NodeAge(0))).thenReturn(samples);
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(filter);

		// Act:
		strategy.calculateTimeOffset(samples, new NodeAge(0));

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, new NodeAge(0));
	}

	// TODO-CR: J-B question should you add a test with a coupling between min and max
	// TODO     BR -> J I added it, though testing for linearity in coupling requires only 2 points.
	// TODO             So I am testing if the calculation is proportional to the mean time offset in the additional test.

	@Test
	public void calculateTimeOffsetWithMaximumCouplingReturnsCorrectValue() {
		// Act:
		final List<SynchronizationSample> samples = TimeSyncUtils.createTolerableSortedSamples(0, 100);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, new NodeAge(0))).thenReturn(samples);
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(filter);

		// Act:
		final long offset = strategy.calculateTimeOffset(samples, new NodeAge(0));

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, new NodeAge(0));
		Assert.assertThat(offset, IsEqual.equalTo((long)(100 * 101 / 2 * SynchronizationConstants.COUPLING_START / 100)));
	}

	@Test
	public void calculateTimeOffsetWitIntermediateCouplingReturnsCorrectValue() {
		// Act:
		final NodeAge age = new NodeAge(SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 5);
		final List<SynchronizationSample> samples = TimeSyncUtils.createRandomTolerableSortedSamplesAroundMean(100, 500);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, age)).thenReturn(samples);
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(filter);

		// Act:
		final long offset = strategy.calculateTimeOffset(samples, age);

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, age);
		Assert.assertThat(offset, IsEqual.equalTo((long)(500 * strategy.getCoupling(age))));
	}

	@Test
	public void calculateTimeOffsetWithMinimumCouplingReturnsCorrectValue() {
		// Act:
		final NodeAge age = new NodeAge(SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 10);
		final List<SynchronizationSample> samples = TimeSyncUtils.createTolerableSortedSamples(0, 100);
		final AggregateSynchronizationFilter filter = Mockito.mock(AggregateSynchronizationFilter.class);
		Mockito.when(filter.filter(samples, age)).thenReturn(samples);
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(filter);

		// Act:
		final long offset = strategy.calculateTimeOffset(samples, age);

		// Assert:
		Mockito.verify(filter, Mockito.times(1)).filter(samples, age);
		Assert.assertThat(offset, IsEqual.equalTo((long)(100 * 101 / 2 * SynchronizationConstants.COUPLING_MINIMUM / 100)));
	}

	@Test
	public void getCouplingReturnsCouplingStartForAgeSmallerThanOrEqualToStartDecayAfterRound() {
		// Arrange:
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(createAggregateFilter());

		// Assert:
		Assert.assertThat(strategy.getCoupling(new NodeAge(0)),
				IsEqual.equalTo(SynchronizationConstants.COUPLING_START));
		Assert.assertThat(strategy.getCoupling(new NodeAge(SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND)),
				IsEqual.equalTo(SynchronizationConstants.COUPLING_START));
	}

	@Test
	public void getCouplingDecaysToCouplingMinimumAfterStartDecayAfterRound() {
		// Arrange:
		final DefaultSynchronizationStrategy strategy = new DefaultSynchronizationStrategy(createAggregateFilter());

		// Assert:
		//TODO-CR: J-B i think checking the range [MIN, START] is too big or at least have one test that calculates a ~exact value
		//TODO-CR: also, you might want to move assertDoubleIsWithingRange since you are using it in multiple tests
		// TODO    BR -> J like this? If yes you can delete assertDoubleIsWithingRange.

		final double epsilon = 1e-10;
		// Assuming decay strength 0.3 for the following tests:
		// coupling = exp(-0.3) = 0.74081822068171786606687377931782
		Assert.assertEquals(0.74081822068171, strategy.getCoupling(new NodeAge(SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 1)), epsilon);
		// coupling = exp(-0.9) = 0.40656965974059911188345423964563
		Assert.assertEquals(0.40656965974059, strategy.getCoupling(new NodeAge(SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 3)), epsilon);
		// coupling = exp(-1.5) = 0.22313016014842982893328047076401
		Assert.assertEquals(0.22313016014842, strategy.getCoupling(new NodeAge(SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 5)), epsilon);
		// coupling = exp(-2.1) = 0.12245642825298191021864737607263
		Assert.assertEquals(0.12245642825298, strategy.getCoupling(new NodeAge(SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 7)), epsilon);
		// exp(-2.4) = 0.09071795328941250337517222007969 < COUPLING_MINIMUM
		Assert.assertThat(strategy.getCoupling(new NodeAge(SynchronizationConstants.START_COUPLING_DECAY_AFTER_ROUND + 8)),
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
