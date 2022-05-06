package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class MappingRepositoryTest {

	// region add success

	@Test
	public void canAddSingleMapping() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);

		// Assert:
		MatcherAssert.assertThat(repository.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(repository.map(7, String.class), IsEqual.equalTo("7"));
	}

	@Test
	public void canAddMultipleMappingsForSourceType() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);
		repository.addMapping(Integer.class, Double.class, Integer::doubleValue);

		// Assert:
		MatcherAssert.assertThat(repository.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(repository.map(7, String.class), IsEqual.equalTo("7"));
		MatcherAssert.assertThat(repository.map(7, Double.class), IsEqual.equalTo(7.0));
	}

	@Test
	public void canAddMultipleMappingsForTargetType() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);
		repository.addMapping(Double.class, String.class, Object::toString);

		// Assert:
		MatcherAssert.assertThat(repository.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(repository.map(7, String.class), IsEqual.equalTo("7"));
		MatcherAssert.assertThat(repository.map(7.1, String.class), IsEqual.equalTo("7.1"));
	}

	// endregion

	// region add failure

	@Test
	public void cannotAddMappingForSameTypePairMoreThanOnce() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);

		// Assert:
		ExceptionAssert.assertThrows(v -> repository.addMapping(Integer.class, String.class, Object::toString), MappingException.class);
		MatcherAssert.assertThat(repository.size(), IsEqual.equalTo(1));
	}

	@Test
	public void cannotMapUnregisteredTypePair() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);

		// Assert:
		ExceptionAssert.assertThrows(v -> repository.map(7, Double.class), MappingException.class);
		MatcherAssert.assertThat(repository.size(), IsEqual.equalTo(1));
	}

	// endregion

	// region size / isSupported

	@Test
	public void sizeReturnsTotalNumberOfKnownMappings() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);
		repository.addMapping(Double.class, String.class, Object::toString);
		repository.addMapping(String.class, String.class, Object::toString);
		repository.addMapping(Integer.class, Double.class, Integer::doubleValue);

		// Assert:
		MatcherAssert.assertThat(repository.size(), IsEqual.equalTo(4));
	}

	@Test
	public void isSupportedReturnsTrueForSupportedMappings() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);
		repository.addMapping(Double.class, String.class, Object::toString);
		repository.addMapping(String.class, String.class, Object::toString);
		repository.addMapping(Integer.class, Double.class, Integer::doubleValue);

		// Assert:
		MatcherAssert.assertThat(repository.isSupported(Integer.class, String.class), IsEqual.equalTo(true));
		MatcherAssert.assertThat(repository.isSupported(Double.class, String.class), IsEqual.equalTo(true));
		MatcherAssert.assertThat(repository.isSupported(String.class, String.class), IsEqual.equalTo(true));
		MatcherAssert.assertThat(repository.isSupported(Integer.class, Double.class), IsEqual.equalTo(true));
	}

	@Test
	public void isSupportedReturnsFalseForUnsupportedMappings() {
		// Act:
		final MappingRepository repository = new MappingRepository();
		repository.addMapping(Integer.class, String.class, Object::toString);
		repository.addMapping(Double.class, String.class, Object::toString);
		repository.addMapping(String.class, String.class, Object::toString);
		repository.addMapping(Integer.class, Double.class, Integer::doubleValue);

		// Assert:
		MatcherAssert.assertThat(repository.isSupported(Integer.class, Object.class), IsEqual.equalTo(false));
		MatcherAssert.assertThat(repository.isSupported(String.class, Double.class), IsEqual.equalTo(false));
		MatcherAssert.assertThat(repository.isSupported(Object.class, Object.class), IsEqual.equalTo(false));
		MatcherAssert.assertThat(repository.isSupported(Long.class, Double.class), IsEqual.equalTo(false));
	}

	// endregion
}
