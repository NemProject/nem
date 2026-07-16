---
title: Transfer XEM
tutorial_level: beginner
---

# Sending XEM with a Transfer Transaction

Sending <XEM:> from one <account:> to another is the most basic action on the NEM blockchain, and every other type of
<transaction:> follows the same general pattern.

```dot
digraph "Transfer XEM" {
    rankdir="LR";
    node [fontsize=12];

    A [label="A"];
    B [label="B"];

    A -> B [label="1 XEM"];
}
```

This tutorial shows how to create, sign, and announce a <transfer transaction:> that sends 1 XEM between two accounts,
and then poll the transaction's status until it is confirmed.

## Prerequisites

Before you start, make sure to:

* [Set Up your Development Environment](../start/setup.md).
* Create an <account:> to send the transfer transaction, either
    [from code](../accounts/create-from-private-key.md) or
    [by using a wallet](../../userbook/wallet/create-account.md).
* Obtain <XEM:> to pay for the transaction fee and transfer amount.
    See [Getting Testnet Funds from the Faucet](../accounts/testnet-faucet.md).

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/transactions/transfer_xem', ['py', 'js']) }}

The whole code is wrapped in a single `try` block to provide simple error handling,
but applications will probably want to use more fine-grained control.

## Code Explanation

### Setting Up the Accounts

{{ tutorial.code_snippet_tagged('step-1') }}

Every transfer transaction involves two accounts: a **sender** and a **recipient**.

The **sender** is the <account:> that signs the transaction and pays the fee.
Its private key is loaded from the `SIGNER_PRIVATE_KEY` environment variable.
If not provided, a test key is used as default.

The **recipient** is the account that receives the XEM.
Its <address:> is loaded from the `RECIPIENT_ADDRESS` environment variable.
If not provided, a test address is used as default.

### Defining the Transfer Amount

{{ tutorial.code_snippet_tagged('step-2') }}

The snippet defines the transfer amount in the `xem` variable, loaded as a number from the `XEM_AMOUNT` environment
variable.
If not provided, a default of 1 XEM is used.

The transaction's `amount` field requires [atomic units](../../textbook/mosaics.md#divisibility), not whole XEM.
XEM has a <divisibility:> of 6, so one XEM equals one million atomic units.
The snippet derives `amount` by multiplying `xem` by 1'000'000.

### Fetching Network Time

{{ tutorial.code_snippet_tagged('step-3') }}

Every NEM transaction contains two time fields, both expressed in <network time:>,
the number of seconds since the NEM nemesis block:

* `timestamp`: The moment the transaction is created, set here to the current network time.
* `deadline`: How long the network keeps trying to confirm the transaction before discarding it.
    It must be after the timestamp and no more than
    [24 hours](../../textbook/transactions.md#common-transaction-structure) later.
    Otherwise, the node rejects the transaction.
    This example sets it two hours after the timestamp, well within the limit.

Building a transfer therefore needs an accurate network time.

The <get:/time-sync/network-time> endpoint reports the node's current network time.
The node returns this value in milliseconds, so the code divides it by 1000 to obtain the seconds that transactions
expect.

However, applications do not need to query the network time before every transaction.
It can be fetched once and then adjusted using the local system clock when needed.
This provides a good balance between accuracy and performance.

### Building the Transaction

{{ tutorial.code_snippet_tagged('step-4') }}

The snippet calls <dy:TransactionFactory.create> with a descriptor that supplies the transfer transaction's
properties:

* {{ tutorial.var('type') }}: This tutorial uses <ser:TransferTransactionV2>, the current transfer version, which can carry both XEM and
    other <mosaics:>.
    No mosaics are attached here, so the transaction sends XEM only.

* {{ tutorial.var('signer_public_key') }}: The signer is the account that will pay the fee.
    In a transfer transaction, it is also the source of the transferred XEM.

* {{ tutorial.var('timestamp') }} and {{ tutorial.var('deadline') }}: The values computed in the network time step.

* {{ tutorial.var('recipient_address') }}: The address that will receive the XEM.

* {{ tutorial.var('amount') }}: The atomic-unit value computed earlier. For 1 XEM, this is `1_000_000`.

!!! info "Sending a mosaic or a message"

    A <ser:TransferTransactionV2> can also carry other <mosaics:> instead of XEM, or include a message, with the fee
    calculated differently in each case.
    See the [Transfer Mosaics](./transfer-mosaics.md) and [Transfer with a Message](./messages.md) tutorials.

### Calculating the Transaction Fee

{{ tutorial.code_snippet_tagged('step-5') }}

Every transaction pays a fee to the <harvester account:> that includes it in a block.

Rather than implement NEM's [fixed fee schedule](../../textbook/transfer_transactions.md#fees) by hand,
the snippet calls the SDK's <dy:FeeCalculator.calculateTransactionFee> helper, which reads the XEM amount directly from
the transaction built in the previous step.

The returned fee is assigned to `transaction.fee` before signing.
The fee starts at 0.05 XEM for small amounts and grows with the XEM sent, up to a cap of 1.25 XEM.

### Signing and Serializing

{{ tutorial.code_snippet_tagged('step-6') }}

Once the transaction is created, it must be signed with the signing account's private key.
Signing ensures the transaction is authentic and authorized by the sender.

<dy:NemFacade.signTransaction> returns a <signature:>.
<dy:TransactionFactory.attachSignature> adds the signature to the transaction and serializes it into a JSON payload
ready to be submitted directly to a node for announcement.

### Announcing the Transaction

{{ tutorial.code_snippet_tagged('step-7') }}

The signed payload is submitted to the <post:/transaction/announce> endpoint of any NEM <node:>.

The node validates the transaction as soon as it is announced and reports the outcome in the response.
A result of `SUCCESS` means the transaction passed this first check and was added to the <unconfirmed pool:>.
Any other result means the node did not accept it, and the response message explains why, for example that the
account does not hold enough XEM to cover the amount and the fee.

!!! warning "Do not rely on unconfirmed transactions"

    A `SUCCESS` result only means the transaction reached the unconfirmed pool.
    It is not yet guaranteed to be included in a block.
    Wait until it is [confirmed](#waiting-for-confirmation), and ideally past the <rewrite limit:>, before relying
    on it.

### Waiting for Confirmation

{{ tutorial.code_snippet_tagged('step-8') }}

The snippet above repeatedly queries the <get:/transaction/get> endpoint using the hash of the announced transaction.

!!! note "Polling vs WebSockets"

    This step uses polling to check whether the transaction has been confirmed.
    Polling is used here for illustration purposes, but it is not the recommended approach for real applications.

    [WebSockets](../websockets/listen-transaction-flow.md) provide a more responsive solution without the overhead of
    repeated API calls.

While the transaction is still unconfirmed, the endpoint responds with an error, and the code waits one second
before retrying, for up to 120 attempts (about two minutes).

Once the transaction is included in a block, the endpoint returns it together with the block height, and the loop
ends.

NEM produces a block roughly once per minute, so confirmation usually takes from a few seconds to a couple of minutes.

## Output

The output shown below corresponds to a typical run of the program.

```text linenums="1" hl_lines="11 13 15 16 17 20 21 34"
--8<-- 'devbook/transactions/transfer_xem.log'
```

Some highlights from the output:

* **Signer public key** (line 11): The account that signs the transaction and sends the XEM.

* **Transaction fee** (line 13): `50000` atomic units (`0.05` XEM), the fee for sending the default amount of 1 XEM.

* **Recipient address** (line 15): The account that receives the XEM.
    This is the same `RECIPIENT_ADDRESS`, but it looks different because NEM's transaction format encodes each character
    of its Base32 text as an ASCII code in hexadecimal, so `5442...` decodes back to `TBUL...`
    (`54` is `T`, `42` is `B`, and so on).

* **Transfer amount** (line 16): `1000000` atomic units, equal to 1 XEM.

* **No mosaics** (line 17): An empty mosaics array means the transaction sends XEM only.

* **Announcement result** (line 20): A result of `SUCCESS` means the node accepted the transaction into the unconfirmed
    pool.

* **Transaction hash** (line 21): The hash that uniquely identifies the transaction on the network.

* **Confirmation** (line 34): The transaction is included in block `626588`.

The number of `pending` checks depends on how soon the next block is harvested, so it varies between runs.

To see the transaction from the network's perspective, you can search for the transaction hash on the
[NEM testnet explorer](https://testnet.nem.fyi/).
The hash is printed in the line that says `Waiting for confirmation from /transaction/get?hash=...`.

## Conclusion

This tutorial showed how to:

| Step                                                              | Related documentation                                                      |
| ----------------------------------------------------------------- | -------------------------------------------------------------------------- |
| [Obtain the network time](#fetching-network-time)                 | <get:/time-sync/network-time>                                              |
| [Build the transaction](#building-the-transaction)                | <dy:TransactionFactory.create>, <ser:TransferTransactionV2>                |
| [Calculate the transaction fee](#calculating-the-transaction-fee) | <dy:FeeCalculator.calculateTransactionFee>                                 |
| [Sign the transaction](#signing-and-serializing)                  | <dy:NemFacade.signTransaction><br/><dy:TransactionFactory.attachSignature> |
| [Announce the transaction](#announcing-the-transaction)           | <post:/transaction/announce>                                               |
| [Wait for confirmation](#waiting-for-confirmation)                | <get:/transaction/get>                                                     |

Most other NEM transaction types are created, signed, and announced in the same way.
