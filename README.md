# NIS client

This Java package provides a wrapper project for the [NIS repository](https://github.com/NemProject/nis) and all its dependencies. It is the simplest way to quickly build and launch a [NIS node](https://nemproject.github.io/nem-docs).

## Package Organization

The main folders are:

| Folder       | Content                                                                                         |
| ------------ | ----------------------------------------------------------------------------------------------- |
| `nem.core`   | Git submodule containing the [nem.core](https://github.com/NemProject/nem.core) dependency.     |
| `nem.deploy` | Git submodule containing the [nem.deploy](https://github.com/NemProject/nem.deploy) dependency. |
| `nem.peer`   | Git submodule containing the [nem.peer](https://github.com/NemProject/nem.peer) dependency.     |
| `nis`        | Git submodule containing the [nis](https://github.com/NemProject/nis) dependency.               |

## Building the package

The package uses [Apache Maven](https://maven.apache.org/) but before building it the Git submodules need to be prepared:

```bash
git submodule init
git submodule update
git submodule foreach 'git checkout dev'
```

Then, if you are using a version of Java higher than 8:

```bash
git submodule foreach './travis_prepare.sh'
```

Build the package as usual:

```bash
mvn package
```

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

- [NIS Developer Documentation](https://nemproject.github.io/nem-docs).
- [NIS Technical Reference](https://nemproject.github.io/nem-docs/pages/Whitepapers/NEM_techRef.pdf).
- Join the community [Discord server](https://discord.gg/xymcity).
- If you found a bug, [open a new issue](https://github.com/NemProject/nem.core/issues).

## License

Copyright (c) 2014-2021 NEM Contributors, licensed under the [MIT license](LICENSE).
