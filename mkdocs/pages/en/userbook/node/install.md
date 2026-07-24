---
title: Node Installation
---

# Installing the Node Client

This guide explains how to deploy a NEM node, either [manually](#manual-installation) or [using Docker](#using-docker).

Hardware requirements:

* A machine connected to the internet with approximately 30 GB of disk space for database and log files, and 16 GB of
    RAM as of July 2026.

* If the node needs to participate in <consensus:> and <harvesting:>, it must have a publicly reachable IP
    address.

    Without a public IP address, the node can still synchronize with the rest of the network and answer local queries.

## Manual Installation

### Prerequisites

* Install [Java JRE 11](https://docs.oracle.com/en/java/javase/11/) or [OpenJDK 11](https://openjdk.org/projects/jdk/11/).

The NIS1 client works on any operating system that supports Java, including Linux, Windows, and macOS.

### Installation

* [Download the latest binary](https://github.com/NemProject/nem/releases).

* Unzip the file in the folder that becomes the NIS1 home folder.

### Configuration

Edit the `nis/config.properties` file:

* Set the `nem.folder` property to point to the NIS1 home folder.
    Backslashes on Windows need to be doubled: `\\`.
    For example, use `D:\\NEM\\nis1-home`, or `%h/nem` to start from the user's home folder.

* Set `nis.bootName` to the name you want for your server.
    This is merely informational.

* Set `nis.bootKey` to the <private key:> of the account managing this node.
    If you do not have an account yet, use a <wallet:> such as NEM NanoWallet to create one.

    * When performing <delegated harvesting:>, this is the private key of the <remote key:|remote account>.
        This is the **recommended** setup.

    * When performing <local harvesting:>, this is directly the private key of your account.
        This setup is **not recommended**.

    !!! warning "This key must be kept secret at all times"

!!! note "Speed Up the First Run of the Node"

    You can optionally download a database snapshot to speed up the first run of the node:

    * Go to [https://bob.nem.ninja/](https://bob.nem.ninja/) and download the most recent `nis5_mainnet-*.mv.db` file.
        For example, [nis5_mainnet-5-565-850.mv.db.gz](https://bob.nem.ninja/nis5_mainnet-5-565-850.mv.db.gz).
    * Unzip the file inside a folder named `nis/data` inside the NIS1 home folder.
        You should get a file named `{nem.folder}/nis/data/nis5_mainnet.mv.db`.

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
    [increase the `-Xmx` parameter](https://docs.oracle.com/cd/E13150_01/jrockit_jvm/jrockit/jrdocs/refman/optionX.html).

Launch the script.
The console output indicates that the node is running.

## Using Docker

These instructions only work for Linux systems, including the Windows Subsystem for Linux.

### Prerequisites

* [Docker](https://docs.docker.com/get-docker/).

* [Git](https://git-scm.com).

### Installation

Clone the [nem-docker](https://github.com/NemProject/nem-docker) repository:

```bash
git clone https://github.com/NemProject/nem-docker.git
cd nem-docker
```

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
    of nodes at [nemnodes.org](https://nemnodes.org/nodes/).
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

* Stop the server by pressing `Ctrl+C` or killing the process.

* Remove the old package. This means all files you [installed previously](#installation) **except** the `*.config` files and the `nis/data` folder.

* [Download the latest binary](https://github.com/NemProject/nem/releases) and extract it in the same folder.

* Start the server again with [the same command used to launch it](#launch).
