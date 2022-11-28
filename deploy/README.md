# NEM Deploy

[![Build Status](https://travis-ci.org/NemProject/nem.deploy.svg?branch=dev)](https://travis-ci.org/NemProject/nem.deploy)

This Java package provides the bootstrapping functions used by [NEM](https://nemproject.github.io/nem-docs) nodes, including reading property files. To deploy a complete node please examine the [build script](../infra/docker).

## Package Organization

The main folders are:

| Folder                  | Content                                                        |
| ----------------------- | -------------------------------------------------------------- |
| `org.nem.deploy`        | Logging, bootstrapping, serialization, property file handling. |
| `org.nem.deploy.server` | Server instantiation.                                          |

## Building the package

The package uses [Apache Maven](https://maven.apache.org/) and  minimum required Java SDK version to build is **Java 11**.

Please make sure that the Java version is 11+ by running the following command:

```bash
java -version
# openjdk version "11.0.2" 2019-01-15
```


Then build and install the package as usual:

```bash
mvn install
```

Then check that unit tests are passing by running:

```bash
mvn test
```

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
