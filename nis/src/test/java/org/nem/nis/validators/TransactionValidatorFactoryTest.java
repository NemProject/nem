package org.nem.nis.validators;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.poi.PoiOptions;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransactionValidatorFactoryTest {

	//region create validator

	@Test
	public void createSingleReturnsValidValidator() {
		// Assert:
		assertNonNullValidator(factory -> factory.createSingle(Mockito.mock(ReadOnlyAccountStateCache.class)));
	}

	@Test
	public void createBatchReturnsValidValidator() {
		// Assert:
		assertNonNullValidator(factory -> factory.createBatch(Mockito.mock(DefaultHashCache.class)));
	}

	private static void assertNonNullValidator(final Function<TransactionValidatorFactory, Object> createValidator) {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();

		// Act:
		final Object validator = createValidator.apply(factory);

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	//endregion

	//region create validator builder

	@Test
	public void createSingleBuilderReturnsValidBuilder() {
		// Assert:
		assertNonNullBuilder(factory -> factory.createSingleBuilder(Mockito.mock(ReadOnlyAccountStateCache.class)));
	}

	@Test
	public void createIncompleteSingleBuilderReturnsValidBuilder() {
		// Assert:
		assertNonNullBuilder(factory -> factory.createIncompleteSingleBuilder(Mockito.mock(ReadOnlyAccountStateCache.class)));
	}

	private static void assertNonNullBuilder(final Function<TransactionValidatorFactory, AggregateSingleTransactionValidatorBuilder> createBuilder) {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();

		// Act:
		final Object validator = createBuilder.apply(factory);

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	//endregion

	//region visitors

	@Test
	public void createSingleAddsDesiredSingleValidators() {
		// Arrange:
		final List<Class<?>> expectedClasses = Arrays.asList(
				UniversalTransactionValidator.class,
				MultisigNonOperationalValidator.class,
				NonFutureEntityValidator.class,
				TransferTransactionValidator.class,
				ImportanceTransferTransactionValidator.class,
				MultisigTransactionSignerValidator.class,
				MultisigAggregateModificationTransactionValidator.class,
				MaxCosignatoryValidator.class,
				MultisigSignaturesPresentValidator.class);

		// Act:
		final Collection<Class<?>> classes = getVisitedSingleSubValidators(true);

		// Assert:
		Assert.assertThat(classes, IsEquivalent.equivalentTo(expectedClasses));
	}

	@Test
	public void createIncompleteSingleAddsDesiredSingleValidators() {
		// Arrange:
		final List<Class<?>> expectedClasses = Arrays.asList(
				UniversalTransactionValidator.class,
				MultisigNonOperationalValidator.class,
				NonFutureEntityValidator.class,
				TransferTransactionValidator.class,
				ImportanceTransferTransactionValidator.class,
				MultisigTransactionSignerValidator.class,
				MaxCosignatoryValidator.class,
				MultisigAggregateModificationTransactionValidator.class);

		// Act:
		final Collection<Class<?>> classes = getVisitedSingleSubValidators(false);

		// Assert:
		Assert.assertThat(classes, IsEquivalent.equivalentTo(expectedClasses));
	}

	private static Collection<Class<?>> getVisitedSingleSubValidators(final boolean includeAllValidators) {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();

		// Act:
		final List<SingleTransactionValidator> validators = new ArrayList<>();
		factory.visitSingleSubValidators(validators::add, Mockito.mock(ReadOnlyAccountStateCache.class), includeAllValidators);
		return validators.stream().map(Object::getClass).collect(Collectors.toList());
	}

	@Test
	public void createBatchAddsDesiredBatchValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final List<Class<?>> expectedClasses = Arrays.asList(
				BatchUniqueHashTransactionValidator.class);

		// Act:
		final List<BatchTransactionValidator> validators = new ArrayList<>();
		factory.visitBatchSubValidators(validators::add, Mockito.mock(DefaultHashCache.class));

		// Assert:
		Assert.assertThat(
				validators.stream().map(Object::getClass).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(expectedClasses));
	}

	private static TransactionValidatorFactory createFactory() {
		return new TransactionValidatorFactory(
				Mockito.mock(TimeProvider.class),
				Mockito.mock(PoiOptions.class));
	}
}