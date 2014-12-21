package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class MappingRepositoryTest {

	@Test
	public void canAddSingleMapping() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);

		// Assert:
		Assert.assertThat(repository.map(7, String.class), IsEqual.equalTo("7"));
	}

	@Test
	public void canAddMultipleMappingsForSourceType() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);
		repository.addMapping(Integer.class, Double.class, Integer::doubleValue);

		// Assert:
		Assert.assertThat(repository.map(7, String.class), IsEqual.equalTo("7"));
		Assert.assertThat(repository.map(7, Double.class), IsEqual.equalTo(7.0));
	}

	@Test
	public void canAddMultipleMappingsForTargetType() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);
		repository.addMapping(Double.class, String.class, Object::toString);

		// Assert:
		Assert.assertThat(repository.map(7, String.class), IsEqual.equalTo("7"));
		Assert.assertThat(repository.map(7.1, String.class), IsEqual.equalTo("7.1"));
	}

	@Test
	public void cannotAddMappingForSameTypePairMoreThanOnce() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> repository.addMapping(Integer.class, String.class, Object::toString),
				MappingException.class);
	}

	@Test
	public void cannotMapUnregisteredTypePair() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> repository.map(7, Double.class),
				MappingException.class);
	}
}