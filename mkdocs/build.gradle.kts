plugins {
	id("com.diffplug.spotless") version "6.25.0"
}

repositories {
	mavenCentral()
}

spotless {
	java {
		target("snippets/**/*.java")

		trimTrailingWhitespace()
		endWithNewline()
		removeUnusedImports()
		importOrder("java", "javax", "", "org.symbol")
	}
}

tasks.register("checkJavaSnippetLineLength") {
	group = "verification"
	description = "Checks Java snippets are wrapped to 75 columns."

	val snippets = fileTree("snippets") {
		include("**/*.java")
	}
	inputs.files(snippets)

	doLast {
		val maxLineLength = 75
		val tabWidth = 4
		val violations = snippets.files.flatMap { file ->
			file.readLines().mapIndexedNotNull { index, line ->
				val expandedLine = line.replace("\t", " ".repeat(tabWidth))
				if (expandedLine.length > maxLineLength)
					"${file.relativeTo(projectDir)}:${index + 1}: " +
						"${expandedLine.length} columns"
				else
					null
			}
		}

		if (violations.isNotEmpty())
			error("Java snippet lines exceed $maxLineLength columns:\n" +
				violations.joinToString("\n"))
	}
}
