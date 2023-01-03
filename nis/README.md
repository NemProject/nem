# NIS: NEM INFRASTRUCTURE SERVER

This Java package provides everything needed to launch a [NEM](https://nemproject.github.io/nem-docs) node.

It depends on the packages [core](../core), [peer](../peer) and [deploy](../deploy).

## Package Organization

The main folders are:

| Folder                   | Content                                                                        |
| ------------------------ | ------------------------------------------------------------------------------ |
| `org.nem.nis.audit`      | Provides auditing of HTTP calls.                                               |
| `org.nem.nis.boot`       | Boots a node.                                                                  |
| `org.nem.nis.cache`      | Copy on write cache implementations that store the blockchain state in memory. |
| `org.nem.nis.chain`      | Processes blocks for commit and rollback.                                      |
| `org.nem.nis.connect`    | Manages HTTP connections to peer nodes.                                        |
| `org.nem.nis.controller` | NEM REST api implementation.                                                   |
| `org.nem.nis.dao`        | (H2) data access layer.                                                        |
| `org.nem.nis.dbmodel`    | Entity models stored in database.                                              |
| `org.nem.nis.pox`        | Proof of Importance calculations.                                              |
| `org.nem.nis.secret`     | Observers that change the blockchain state.                                    |
| `org.nem.nis.service`    | Commons services that are injected in the various places to decouple the code. |
| `org.nem.nis.state`      | Entity models stored in the caches.                                            |
| `org.nem.nis.sync`       | Syncs the chain with peers and resolves forks.                                 |
| `org.nem.nis.time`       | P2P Time synchronization algorithm.                                            |
| `org.nem.nis.validators` | Validators that validate the blockchain state.                                 |
| `org.nem.nis.visitors`   | Block by block visitors that allow block by block processing.                  |
| `org.nem.nis.websocket`  | NEM websocket implementation.                                                  |

## Building the package

> **NOTE:**
> It is far more convenient to use the [nem repository](https://github.com/NemProject/nem) to build and run this package.

The package uses [Apache Maven](https://maven.apache.org/) and  minimum required Java SDK version to build is **Java 11**.

Please make sure that the Java version is 11+ by running the following command:

```bash
java -version
# openjdk version "11.0.2" 2019-01-15
```

First build and install (with ``mvn install -DskipTests=true``) all the dependency packages [nem.core](https://github.com/NemProject/nem.core), [nem.peer](https://github.com/NemProject/nem.peer) and [nem.deploy](https://github.com/NemProject/nem.deploy). Make sure they are all accessible through the ``CLASSPATH`` environment variable.

Then build and install the package as usual:

```bash
mvn install
```

Optionally, check if the slower integration tests are passing by running:

```bash
mvn failsafe:integration-test
````

## Coding style

If you want your changes to be considered for inclusion in the repository (see [CONTRIBUTING.md](CONTRIBUTING.md)), they must adhere to the coding style. You can check that the coding style and format are correct by running:

```bash
mvn spotless:check
```

If there is any problem, you can try to fix it automatically using:

```bash
mvn spotless:apply
```

## Running a node

If all dependencies are accessible through the ``CLASSPATH``, the command to launch the node is:

```bash
java -Xms6G -Xmx6G org.nem.deploy.CommonStarter
```

Otherwise you will need to use the ``-cp`` parameter and point to each individual dependency JAR file.

For more information on how to run a node see the [NEM documentation](https://nemproject.github.io/nem-docs/pages/Guides/node-operation/docs.en.html).

## Running as a Docker container

### Build the image
```bash
# note that build context must be parent directory(..)
docker build -t nis-client -f ./Dockerfile ..
```
### Run the container
```bash
# create the nem config folder and put the config files you want to overwrite
mkdir -p nem-config
# see ReplaceWithYourPrivateKey below
echo 'nis.bootKey=ReplaceWithYourPrivateKey' > nem-config/config-user.properties
# you can overwrite other properties in the same file as well as overwriting other properties files
docker run -d -p 7890:7890 -p 7778:7778 -v $(pwd)/nem-config:/usersettings nis-client
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
