package org.nem.nis.validators;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.poi.PoiOptions;

import java.util.*;
import java.util.stream.Collectors;

public class TransactionValidatorFactoryTest {

//	@Test
//	public void createReturnsValidValidator() {
//		// Arrange:
//		final TransactionValidatorFactory factory = createFactory();
//
//		// Act:
//		final SingleTransactionValidator validator = factory.create(Mockito.mock(ReadOnlyNisCache.class));
//
//		// Assert:
//		Assert.assertThat(validator, IsNull.notNullValue());
//	}

	@Test
	public void createSingleReturnsValidValidator() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();

		// Act:
		final SingleTransactionValidator validator = factory.createSingle(Mockito.mock(ReadOnlyAccountStateCache.class), false);

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	@Test
	public void createBatchReturnsValidValidator() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();

		// Act:
		final BatchTransactionValidator validator = factory.createBatch(Mockito.mock(DefaultHashCache.class));

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	@Test
	public void createSingleAddsDesiredSingleValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final List<Class<?>> expectedClasses = Arrays.asList(
				UniversalTransactionValidator.class,
				MultisigNonOperationalValidator.class,
				NonFutureEntityValidator.class,
				TransferTransactionValidator.class,
				ImportanceTransferTransactionValidator.class,
				MultisigSignaturesPresentValidator.class,
				MultisigSignerModificationTransactionValidator.class);

		// Act:
		final List<SingleTransactionValidator> validators = new ArrayList<>();
		factory.visitSingleSubValidators(validators::add, Mockito.mock(ReadOnlyAccountStateCache.class), false);

		// Assert:
		Assert.assertThat(
				validators.stream().map(Object::getClass).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(expectedClasses));
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