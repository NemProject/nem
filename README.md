# NIS client

This project contains [NEM Infrastructure Server](nis) and all its dependencies. It is the simplest way to quickly build and launch a [NEM node](https://nemproject.github.io/nem-docs).

## Package Organization

The main folders are:

| Folder   | Content                          |
|----------|----------------------------------|
| `core`   | The [core](core) dependency.     |
| `deploy` | The [deploy](deploy) dependency. |
| `peer`   | The [peer](peer) dependency.     |
| `nis`    | The [nis](nis) dependency.       |

## Building the package

The package uses [Apache Maven](https://maven.apache.org/) and  minimum required Java SDK version to build is **Java 11**.

Please make sure that the Java version is 11+ by running the following command:

```bash
java -version
# should print sth similar to: openjdk version "11.0.2" 2019-01-15
```

Build the package as usual:

```bash
mvn package
```

Then check that unit tests are passing by running:

```bash
mvn test
```

Optionally, check if the slower integration tests are passing by running:

```bash
mvn failsafe:integration-test
````

## Running the package

NIS nodes are configured through [property files](https://nemproject.github.io/nem-docs/pages/Guides/node-operation/docs.en.html#configuration). Create a folder named `staging` and add any required property files inside.

Then run the node with:

```bash
java -Xms6G -Xmx6G -cp ./staging:./nis/target/libs/*:./nis/target/* org.nem.deploy.CommonStarter
```

Read the [NEM node documentation](https://nemproject.github.io/nem-docs/pages/Guides/node-operation/docs.en.html) to know more about handling NIS nodes.

## Contributing

Before contributing please [read the CONTRIBUTING instructions](CONTRIBUTING.md).

## Getting Help

- [NEM Developer Documentation](https://nemproject.github.io/nem-docs).
- [NEM Technical Reference](https://nemproject.github.io/nem-docs/pages/Whitepapers/NEM_techRef.pdf).
- Join the community [Discord server](https://discord.gg/xymcity).
- If you found a bug, [open a new issue](https://github.com/NemProject/nem.core/issues).

## License

Copyright (c) 2014-2021 NEM Contributors, licensed under the [MIT license](LICENSE).
