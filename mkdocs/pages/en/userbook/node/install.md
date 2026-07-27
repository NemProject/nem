---
title: Node Installation
---

# Installing the Node Client

This guide explains how to deploy a NEM node, either [manually](#manual-installation) or [using Docker](#using-docker).

## Hardware Requirements

* A machine connected to the internet with approximately 30 GB of disk space for database and log files, and 16 GB of
    RAM as of July 2026.

* For optimum participation in <consensus:> and <harvesting:>, the node must have a publicly reachable IP address
    and TCP port **7890** must be open for inbound and outbound connections.

    Without a public IP address, your node can still produce new blocks and submit them to the network,
    but peer nodes will not be able to notify your node about new blocks and transactions they discover.
    In this case, your node resorts to periodically polling its peers, which is slower.

    Additionally, a public IP address is required to participate in the <Supernode program:>.

* For applications like wallets to be able to communicate with the network through your node,
    the following TCP ports should be open:

    * **7890** for [REST](../../devbook/reference/rest/nem.md) requests.
    * **7778** for [WebSockets](../../devbook/reference/websockets/index.md) requests.

## Manual Installation

### Prerequisites

* Install [Java JRE 11](https://docs.oracle.com/en/java/javase/11/) or [OpenJDK 11](https://openjdk.org/projects/jdk/11/).

The NIS1 client works on any operating system that supports Java, including Linux, Windows, and macOS.

### Installation

* [Download the latest binary](https://github.com/NemProject/nem/releases).

* Unzip the file in the folder that becomes the NIS1 _installation_ folder.

### Configuration {: #manual-configuration }

Create a new `nis/config-user.properties` file with the following content, adapted to your case:

```ini
nem.folder = %h/nem
nis.bootName = my-server
nis.bootKey = 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
```

* Set the `nem.folder` property to point to the NIS1 _home_ folder.
    This can be different from the installation folder and will store the program's logs and database files.

    Do not use `~` to refer to your user's home folder, use `%h` instead.
    The default location is `%h/nem`.

    Backslashes on Windows need to be doubled: `\\`.
    For example, `D:\\NEM\\nis1-home`.

* Set `nis.bootName` to the name you want for your server.
    This is merely informational.
    Leading and trailing spaces are removed, but the name can contain spaces in the middle.
    Avoid using characters outside the ASCII range, even though UTF8 is supported via escaping
    (e.g. `\u3053\u3093\u306B\u3061\u306F`).

* Set `nis.bootKey` to the <private key:> of the account managing this node.
    If you do not have an account yet, use a <wallet:> such as NEM NanoWallet to create one.

    * When performing <delegated harvesting:>, this is the private key of the <remote key:|remote account>.
        This is the **recommended** setup.

    * When performing <local harvesting:>, this is directly the private key of your account.
        This setup is **not recommended**.

    !!! warning "This key must be kept secret at all times"

* Set `nis.shouldAutoHarvestOnBoot` to `true` if you want the node to harvest.

!!! note "Speed Up the First Run of the Node"

    You can optionally download a database snapshot to speed up the first run of the node:

    * Go to [https://bob.nem.ninja/](https://bob.nem.ninja/) and download the most recent `nis5_mainnet-*.mv.db` file.
        For example, [nis5_mainnet-5-565-850.mv.db.gz](https://bob.nem.ninja/nis5_mainnet-5-565-850.mv.db.gz).
    * Unzip the file inside a folder named `nis/data` inside the NIS1 home folder.
        Rename the extracted file to `nis5_mainnet.mv.db`.

!!! example "Running a Testnet Node"

    If you want to run a <testnet:> node, add the following properties to your `config-user.properties` file:

    ```ini
    nem.network = testnet

    nis.treasuryReissuanceForkHeight = 1
    nis.treasuryReissuanceForkTransactionHashes =
    nis.treasuryReissuanceForkFallbackTransactionHashes =
    nis.multisigMOfNForkHeight = 1
    nis.mosaicsForkHeight = 1
    nis.firstFeeForkHeight = 1
    nis.secondFeeForkHeight = 1
    nis.remoteAccountForkHeight = 1
    nis.mosaicRedefinitionForkHeight = 1
    ```

### Launch

Open a terminal and locate the appropriate launch script for your operating system:

=== "Windows"

    ```bash
    runNis.bat
    ```

=== "Linux"

    ```bash
    nix.runNis.sh
    ```

!!! note "Out of Memory Issues"

    If you encounter memory issues, edit the launch script and
    [increase the `-Xmx` parameter](https://docs.oracle.com/en/java/javase/11/tools/java.html#GUID-3B1CE181-CD30-4178-9602-230B800D4FAE__GUID-98AC4535-A539-406D-9AC5-390C1AF143F0).

Launch the script.
The console output indicates that the node is running.

## Using Docker

These instructions only work for Linux systems, including the Windows Subsystem for Linux.

### Prerequisites

* [Docker](https://docs.docker.com/get-docker/).

* [Git](https://git-scm.com).

### Installation {: #docker-installation }

Clone the [nem-docker](https://github.com/NemProject/nem-docker) repository:

```bash
git clone https://github.com/NemProject/nem-docker.git
cd nem-docker
```

### Configuration

Upon first run, the client asks for the **boot name** and **boot key** properties (described in the manual
[Configuration](#manual-configuration) section above) and stores them.

If you want to edit these settings manually, before starting the client create a new file called
`custom-configs/nis.config-user.properties` with the content described above.

### Controlling the Node

* To start the node:

    ```bash
    ./boot.sh
    ```

* To stop the node:

    ```bash
    ./stop.sh
    ```

For additional commands, read [the nem-docker GitHub project](https://github.com/NemProject/nem-docker).

## Synchronization

When the node first starts, it downloads the whole blockchain from its peers.

**This is a long process that can take up to 48 hours.**

If you downloaded the optional database snapshot, the node reads the database first and then downloads the remaining
blocks, significantly reducing the synchronization time.

Meanwhile, you can check:

* If you have a public IP, a few minutes after launching your node it should appear in the public list
    of nodes at [nodewatch.symbol.tools](https://nodewatch.symbol.tools/nem/nodes).
    Its reported chain height increases as the node catches up with the rest of the network.

* You can also ask your node its current chain height by pointing a browser to
    [localhost:7890/chain/height](http://localhost:7890/chain/height).

## Monitoring the Node

NIS listens for queries on port 7890, so the first way to monitor your node is to point a browser to
[localhost:7890/node/info](http://localhost:7890/node/info).

If you get any response, even an error such as `NIS_ILLEGAL_STATE_LOADING_CHAIN`, the node is running.

For the full list of URLs that can be queried, see the [REST API specification](../../devbook/reference/rest/nem.md).

## Updating a Node

Updating the NIS1 client to the latest protocol version is straightforward:

### Manually

* Stop the server by pressing `Ctrl+C` or killing the process.

* Remove the old package.
    This means all files in the [NIS1 installation folder](#installation) **except** the `config-user.properties`.
    Everything in the NIS1 home folder should remain.

* [Download the latest binary](https://github.com/NemProject/nem/releases) and extract it in the same folder.

* Start the server again with [the same command used to launch it](#launch).

### Using Docker

* Stop the server with `./stop.sh`

* Update the repository that you cloned in the [Installation](#docker-installation) step with `git pull`

* Restart the server with `./boot.sh`
