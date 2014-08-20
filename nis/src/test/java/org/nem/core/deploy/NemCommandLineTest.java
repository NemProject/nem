package org.nem.core.deploy;

import org.apache.commons.cli.Option;
import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.*;

public class NemCommandLineTest {

	//region constructor

	@Test
	public void nemCommandLineWithNoOptionsCanBeConstructed() {
		// Act:
		NemCommandLine commandLine = new NemCommandLine(new ArrayList<>());

		// Assert:
		Assert.assertThat(commandLine.optionsSize(), IsEqual.equalTo(0));

	}

	@Test
	public void nemCommandLineWithOptionsCanBeConstructed() {
		// Act:
		NemCommandLine commandLine = getTestNemCommandLine();

		// Assert:
		Assert.assertThat(commandLine.optionsSize(), IsEqual.equalTo(3));
	}

	//endregion

	//region parse

	@Test
	public void allParametersCanBeParsed() {
		// Arrange:
		NemCommandLine commandLine = getTestNemCommandLine();
		String[] parameters = {"-foo1", "foo1Param", "-foo2", "-foo3", "foo3Param" };

		// Act:
		boolean result = commandLine.parse(parameters);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(commandLine.hasParameter("foo1"), IsEqual.equalTo(true));
		Assert.assertThat(commandLine.getParameter("foo1"), IsEqual.equalTo("foo1Param"));
		Assert.assertThat(commandLine.hasParameter("foo2"), IsEqual.equalTo(true));
		Assert.assertThat(commandLine.hasParameter("foo3"), IsEqual.equalTo(true));
		Assert.assertThat(commandLine.getParameter("foo3"), IsEqual.equalTo("foo3Param"));
	}

	@Test
	public void partiallySuppliedParametersCanBeParsed() {
		// Arrange:
		NemCommandLine commandLine = getTestNemCommandLine();
		String[] parameters = {"-foo1", "foo1Param" };

		// Act:
		boolean result = commandLine.parse(parameters);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(commandLine.hasParameter("foo1"), IsEqual.equalTo(true));
		Assert.assertThat(commandLine.getParameter("foo1"), IsEqual.equalTo("foo1Param"));
	}

	@Test
	public void unknownParameterCannotBeParsed() {
		// Arrange:
		NemCommandLine commandLine = getTestNemCommandLine();
		String[] parameters = {"-foo1", "foo1Param", "-bazz", "bazzParam" };

		// Act:
		boolean result = commandLine.parse(parameters);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(false));
	}

	//endregion

	private NemCommandLine getTestNemCommandLine() {
		return new NemCommandLine(Arrays.asList(
				new Option("foo1", true, "bar1"),
				new Option("foo2", false, "bar2"),
				new Option("foo3", true, "bar3")));
	}
}
