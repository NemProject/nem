---
title: Supernode Program
---

# Supernode Program

The <Supernode Program:> rewards public NEM nodes that help secure the network and provide reliable data access for
applications.
Because all <XEM:> was created in the genesis block, NEM has no block subsidy.
A quantity of XEM was set aside for the Supernode Program.

Reward eligibility is checked daily in four rounds, one round every six hours.
A participating node must pass every test in every round to qualify for that day's reward.

Testing is performed by a centrally managed Supernode monitoring service called the _controller_.
Some tests compare the participating node with a trusted _reference node_.

Participating nodes also run the _Node Servant_, a lightweight application on the same machine as the NIS client.
The Servant performs Supernode Program tests and exposes the `/nr/...` endpoints used by some of those tests.

## Eligibility Tests

Each testing round includes the following checks:

* **Chain height:** The node reports its current height through the <get:/chain/height> API.
    The test passes if the height is no more than 4 blocks behind the reference node.

* **Chain part:** The node serves a random group of 60 to 100 recent blocks through the
    <post:/local/chain/blocks-after> API.
    The controller verifies the block signatures and compares a composite hash of those blocks with the same hash
    calculated from the reference node.

* **Balance:** The node's main account has at least 10,000 XEM.
    The controller checks this through the <get:/account/get/forwarded> API, routed through the reference node.

* **Computing power:** The node completes 10,000 repeated public-key derivations from a random hash.
    The test passes if the final public key matches the controller's result and the full round trip takes 5 seconds or
    less.

* **Version:** The node reports a NIS client version through the <get:/node/info> API.
    The test passes if the version is at least as recent as the reference node's version.

* **Ping:** The controller gives the Servant 5 random partner nodes through the `/nr/task/ping` API.
    The Servant calls the `/nr/ping` API on each partner 5 times.
    The test passes if no more than one ping fails and the average successful round-trip time is less than 200 ms.

* **Bandwidth:** The controller gives the Servant the fastest partner from the ping test and a random hash seed through
    the `/nr/task/bandwidth` API.
    The Servant and partner perform the same 30,000-hash calculation.
    The test passes if both hash results match and the measured transfer speed is at least 5 Mbit/s.

* **Responsiveness:** The controller sends 10 requests to the <get:/chain/height> API.
    The test passes if at least 9 requests succeed and all requests complete in 1 second or less.
    If only the time limit fails, the controller retries this test up to 4 times.

## Enrolling in the Program

Before enrolling, make sure you have:

* [NEM NanoWallet](https://github.com/NemProject/NanoWallet/releases) installed.

* A NEM account with at least 10,010 XEM.
    The program requires 10,000 XEM to participate, plus approximately 10 XEM for delegated harvesting and enrollment
    transaction fees.

* <Delegated harvesting:|Delegated harvesting> activated on the main account.
    The remote account becomes usable after approximately 360 blocks, or about six hours.
    Enrollment can start as soon as the <remote key:|remote account private key> is available.

* A synchronized public node.
    Follow the [node installation guide](install.md) if you do not have one yet.

    Make sure the `nis.shouldAutoHarvestOnBoot` property is set to `true` or not set.

* A stable public IP address or domain name for the node.

!!! warning "Use the Delegated Private Key"

    Use the delegated private key for the node and Servant configuration.
    Do not use the private key of the main account.

    The main private key controls the account's funds.
    The delegated private key only controls delegated harvesting and can be replaced if it is compromised.

To find the delegated private key in NEM NanoWallet, open **Services**, then **Delegated Harvesting**, then
**Manage Delegated Account**.
Select **Show delegated account keys** and enter the wallet password to reveal the delegated private key.

### Manually

#### Configuring the Servant

1. Download the [Node Servant](https://bob.nem.ninja/servant_0_0_4.zip).

2. Unzip the file in the folder that becomes the Servant installation folder.
    It does not need to be the same as the NIS1 installation folder.
    Open the `servant` folder.

3. Open the Servant `config-user.properties` file.

4. Set `nem.host` to the node's static IP address or domain name.
    This value must remain stable so the controller can test the same node.

5. Set `servant.key` to the delegated private key.

6. Save the file.

7. Open inbound and outbound TCP port **7880** so the Servant can receive Supernode Program tests.

#### Starting the Servant

Start NIS and let it synchronize before starting the Servant with:

=== "Windows"

    ```bash
    runservant.bat
    ```

=== "Linux and macOS"

    ```bash
    sh startservant.sh
    ```

    Run the Servant in the background with a tool such as `screen` or `nohup`.

### Using Docker

These instructions only work for Linux systems, including the Windows Subsystem for Linux.

#### Configuring the Servant

If you installed your node using Docker as explained in the [Installation guide for Docker](./install.md#using-docker),
you already have the Servant installed and you only need to configure it.

Stop the NIS1 client if it is running, as explained in [Controlling the node](./install.md#controlling-the-node).

Inside the `custom-configs` folder:

* Copy the `servant.config.properties.sample` file into `servant.config.properties` and edit it.
    Provide values for at least the `nem.host` and `servant.key` properties.
* Copy the `supervisord.conf.sample` file into `supervisord.conf` and edit it.
    Set `autostart=true` in both the `[program:nis]` and `[program:servant]` sections.

#### Starting the Servant

The Servant now starts every time the NIS1 client is started with:

```bash
./boot.sh
```

You can verify it with:

```bash
./service.sh status
```

### Sending the Enrollment

The monthly enrollment address is announced through the NEM community channels.

#### Enrolling with NEM NanoWallet

1. Open **Services**.

2. Open **SuperNode Program**.

3. Open **Check & Enroll in Program**.

4. Select **Enroll in Program**.

5. Enter the current enrollment address and the node host.

    The host must match the host returned by the node's <get:/node/info> endpoint.

6. Send the enrollment transaction.

#### Enrolling Manually

Enrolling manually requires sending a transfer transaction to the current enrollment address with the following
unencrypted message:

```text
enroll <NODE_HOST> <CODEWORD_HASH>
```

* `<NODE_HOST>` as returned by the node's <get:/node/info> endpoint.
* `<CODEWORD_HASH>` as returned by the following query:

    ```text
    https://nem.io/supernode/api/codeword/<MAIN_PUBLIC_KEY>
    ```

    !!! warning "Use the Public Key"

        * Use the main <public key:> in the codeword API URL, not the <private key:>.

        * Use the <main key:|main public key> of the account enrolled in the program, not the <remote key:> used to
            harvest on the node.

            Please note that <get:/node/info> returns the remote key.

## Reviewing Test Results

Review the node's test results on the [Supernode Program page](https://nem.io/supernode/).
Results do not appear immediately after enrollment.

## Editing the Supernode Host

If the node's IP address or domain name changes, update the node and Servant configuration.
Then send a new enrollment transaction with the changed host.

## Monthly Re-Enrollment

The Supernode Program requires monthly re-enrollment.
Each month has a new enrollment address, announced through the NEM community channels.

Enrollment for the next month opens 4 days before the end of the current month.
Repeat the [enrollment process](#sending-the-enrollment) each month with the new enrollment address.
