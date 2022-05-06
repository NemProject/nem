# NEM Core

[![Build Status](https://travis-ci.org/NemProject/nem.core.svg?branch=dev)](https://travis-ci.org/NemProject/nem.core)

This Java package provides the cryptographic and serialization base methods used by [NEM](https://nemproject.github.io/nem-docs) nodes. To deploy a complete node please examine the [nis-client repository](https://github.com/NemProject/nis-client).

## Package Organization

The main folders are:

| Folder                       | Content                                                          |
| ---------------------------- | ---------------------------------------------------------------- |
| `org.nem.core.async`         | Utilities for working with asynchronous timers.                  |
| `org.nem.core.connect`       | HTTP connection handling.                                        |
| `org.nem.core.crypto`        | Hashing, key management, signing and verification.               |
| `org.nem.core.function`      | Generic functional prototypes.                                   |
| `org.nem.core.i18n`          | Handling of Unicode (UTF8) files.                                |
| `org.nem.core.math`          | Linear algebra matrix and vector utilities.                      |
| `org.nem.core.messages`      | Implementation of plain and encrypted transfer messages.         |
| `org.nem.core.metadata`      | Application metadata handling and embedding into JAR files.      |
| `org.nem.core.model`         | Basic data types for transactions and states.                    |
| `org.nem.core.node`          | Basic data types for node information.                           |
| `org.nem.core.serialization` | Binary and JSON (de)serialization implementations and utilities. |
| `org.nem.core.time`          | Basic data types for time synchronization.                       |
| `org.nem.core.utils`         | Other utilities like collections or encoders.                    |

## Building the package

The package uses [Apache Maven](https://maven.apache.org/) and the ``pom.xml`` file is initially configured to work with **Java 8**. To work on more recent versions of Java run the ``setup_java9.sh`` script first.

Then build and install the package as usual:

```bash
mvn install
```

Then check that unit tests are passing by running:

```bash
mvn test
```

Optionally, check if the slower integration tests are passing by running:

```bash
mvn failsafe:integration-test
````

## Coding style

If you want your changes to be considered for inclusion in the repository (see [CONTRIBUTING.md](CONTRIBUTING.md)), they must addhere to the coding style. You can check that the coding style and format are correct by running:

```bash
mvn spotless:check
```

If there is any problem, you can try to fix it automatically using:

```bash
mvn spotless:apply
```

## Contributing

Before contributing please [read the CONTRIBUTING instructions](CONTRIBUTING.md).

## Getting Help

- [NEM Developer Documentation](https://nemproject.github.io/nem-docs).
- [NEM Technical Reference](https://nemproject.github.io/nem-docs/pages/Whitepapers/NEM_techRef.pdf).
- Join the community [Discord server](https://discord.gg/xymcity).
- If you found a bug, [open a new issue](https://github.com/NemProject/nem.core/issues).

## License

Copyright (c) 2014-2021 NEM Contributors, licensed under the [MIT license](LICENSE).
