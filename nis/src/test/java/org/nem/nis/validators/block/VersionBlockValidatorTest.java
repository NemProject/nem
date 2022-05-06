package org.nem.nis.validators.block;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nem.core.model.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.*;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BlockValidator;

import java.util.*;

@RunWith(Parameterized.class)
public class VersionBlockValidatorTest {
	private final int type;

	public VersionBlockValidatorTest(final int type) {
		this.type = type;
	}

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		return ParameterizedUtils.wrap(Arrays.asList(BlockTypes.NEMESIS, BlockTypes.REGULAR));
	}

	@Test
	public void versionOneBlockIsAllowed() {
		// Arrange:
		final Block block = changeBlockVersion(this.type, 1);

		// Assert:
		assertValidation(block, ValidationResult.SUCCESS);
	}

	@Test
	public void versionOneHundredBlockIsNotAllowed() {
		// Arrange:
		final Block block = changeBlockVersion(this.type, 100);

		// Assert:
		assertValidation(block, ValidationResult.FAILURE_ENTITY_INVALID_VERSION);
	}

	private static void assertValidation(final Block block, final ValidationResult expectedResult) {
		// Arrange:
		final BlockValidator validator = new VersionBlockValidator();

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static Block changeBlockVersion(final int type, final int version) {
		final Block block = NisUtils.createRandomBlock();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(block.asNonVerifiable());
		jsonObject.put("version", version | 0xFF000000);
		return new Block(type, VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, Utils.createDeserializer(jsonObject));
	}
}
