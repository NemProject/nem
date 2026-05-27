# Transactions

Transaction
:   A transaction represents an action to perform on the NEM blockchain,
    like moving funds from one <account:> to another, or registering a new mosaic.

These actions are expressed in a signed message, which is then announced to the network.
<Nodes:|Nodes> in the network validate it and, if accepted, include the transaction in a block, updating the state of
the blockchain.

## Fundamental Transaction Types

NEM supports two core transaction types: basic and multisig.

```dot
digraph "Fundamental Transaction Types" {
    node [fontsize=12];
    Transaction;
    Basic [URL="#basic-transactions"];
    Multisig [URL="#multisig-transactions"];

    Transaction -> Basic;
    Transaction -> Multisig;
}
```

### Basic Transactions

Basic Transaction
:   A basic <transaction:> represents a single action, initiated by a single account,
    requiring only that account's <signature:>.

Examples include transferring funds from an account or registering a new <namespace:>.

### Multisig Transactions

Multisig Transaction
:   A multisig transaction wraps a single <inner transaction:> issued on behalf of a
    <multisignature account:|multisig account>, and requires signatures from the configured number of cosignatories
    before it can be included in a block.

Multisig transactions are initiated by one cosignatory, but require additional signatures from other cosignatories
to be valid.

Cosignature
:   When a transaction requires signatures from multiple accounts, the additional signatures are called _cosignatures_.

On NEM, cosignatures are delivered as separate multisig cosignature transactions that reference the
<inner transaction:> by hash.
Cosignatories can submit their signatures independently and over multiple blocks until the required threshold is
reached.

Multiple coordinated actions must therefore be issued as separate multisig transactions, one per inner transaction.

When a multisig transaction is included in a block, the multisig account pays every fee in the composition:
the inner transaction's fee, the multisig transaction's fee, and every cosignature's fee.
Cosignatories never spend from their own balance when cosigning.

!!! tip "Multisig Transaction Example"

    A treasury account `T` is a 2-of-3 multisig controlled by cosignatories `C1`, `C2`, and `C3`.
    To pay a supplier `S`, `C1` announces a multisig transaction wrapping a transfer from `T` to `S`.
    `C2` then submits a cosignature, meeting the 2-of-3 threshold.
    `C3` does not need to sign.
    Once the threshold is reached, the transfer executes, and `S` receives the funds.

    ```dot
    digraph {
        rankdir="LR";
        fontsize=12;
        compound=true;
        node [fontsize=12];

        C1 [label="C1"];
        C2 [label="C2"];
        C3 [label="C3"];

        subgraph clusterMultisig {
            label = "Multisig Transaction";
            fontsize = 12;
            style = dashed;
            T [label="T\nMultisig Account\n2 of 3"];
            S [label="S"];
            T -> S [label="Transfer"];
        }

        C1 -> T [label="signature" lhead=clusterMultisig minlen=2 labelfloat=true];
        C2 -> T [label="cosignature" lhead=clusterMultisig minlen=2 labelfloat=true];
        C3 -> T [style=dashed lhead=clusterMultisig minlen=2];
    }
    ```

### Inner Transactions

Inner Transaction
:   The <basic transaction:> wrapped inside a <multisig transaction:> is called the _inner transaction_.

Inner transactions behave like basic transactions, with the following differences:

* They are not individually signed.
    The multisig transaction is signed by the initiating cosignatory, and additional cosignatories provide
    their approvals through separate multisig cosignature transactions.

* They cannot themselves be multisig transactions.
    Multisig hierarchies are only one layer deep.

* They retain their own fee and deadline fields.
    Inner transaction fees are billed to the multisig account along with the multisig transaction's fee
    and each cosignature's fee.

## Transaction Lifecycle

Each NEM transaction moves through six stages, from creation by a client to confirmation by the network:

```dot
digraph "Transaction Lifecycle" {
    node [shape=box, style=rounded, fontsize=12, margin="0.2,0.1"];
    edge [fontsize=12];
    nodesep=0.3;
    ranksep=0.3;

    Creation     [label="1. Transaction is created and signed", URL="#1-creation-and-signature"];
    Announcement [label="2. Transaction is announced to a node", URL="#2-announcement"];
    Validation   [label="3. Is it
valid?", shape=diamond, style="", URL="#3-validation"];
    Propagation  [label="4. Propagate to other nodes", URL="#4-propagation"];
    Harvesting   [label="5. Inclusion in a block", URL="#5-harvesting"];
    Confirmation [label="6. Confirmed?", shape=diamond, style="", URL="#6-confirmation"];
    Confirmed    [label="Confirmed"];

    Rejection1   [label="Rejected" style="rounded,dashed"];
    Rejection2   [label="Rejected" style="rounded,dashed"];

    Creation ->     Announcement;
    Announcement -> Validation;
    Validation ->   Propagation [label="   Yes", labelfloat=true];
    Propagation ->  Harvesting;
    Harvesting ->   Confirmation;
    Confirmation -> Confirmed [label="   Yes", labelfloat=true];

    Validation ->   Rejection1 [label=No, style=dashed, minlen=2];
    Confirmation ->   Rejection2 [label=No, style=dashed, minlen=2];

    { rank = same; Validation; Rejection1 }
    { rank = same; Confirmation; Rejection2 }
}
```

### 1. Creation and signature

A software client, typically an app, creates the transaction and fills in all its parameters.
For example, a transfer transaction requires the source <account:>, destination account, and amount.

This step also involves signing the transaction.
Signatures prove that the signing account has authorized the transaction, since only the holder of an account's
<private key:> can produce a valid signature.

For multisig transactions, the initiating cosignatory signs the multisig transaction that wraps the inner transaction.
Other cosignatories provide their cosignatures separately, via multisig cosignature transactions.

### 2. Announcement

The client application submits the transaction to a connected <node:> on the network.

For multisig transactions, cosignatures are announced as separate multisig cosignature transactions, each submitted
independently by its signer.

### 3. Validation

The node checks that the transaction is well-formed and includes a valid signature.
For multisig transactions, the node also verifies that any referenced cosignatures come from valid
cosignatories of the multisig account.

Some transaction types require additional semantic checks.
For example, a transfer transaction verifies that the source account has enough funds.

If any of these checks fail, the transaction is rejected and not propagated further.
If all checks pass, the process continues.

### 4. Propagation

Once the node considers the transaction to be valid, it is broadcast to other peer <nodes:> in the network,
and added to every node's _unconfirmed pool_.

Unconfirmed pool
:   A list of validated transactions awaiting inclusion in a block, maintained by each node in the network.

When a peer receives a propagated transaction, it runs the full validation again before adding the transaction
to its own pool, because no node trusts another's validation.
If the transaction passes, the peer forwards it to its own peers, and propagation continues until the transaction
is distributed across the network.

!!! warning "Do not rely on unconfirmed transactions"

    A transaction in the unconfirmed pool is not yet guaranteed to be included in a block.
    Wait until it is [confirmed](#6-confirmation), and ideally past the <rewrite limit:>, before treating it as final.

For multisig transactions, the multisig transaction and its accompanying multisig cosignature transactions propagate
independently.

### 5. Harvesting

Once in the unconfirmed pool, the transaction will eventually be picked up by the <harvesting:> process and
_included_ in a block.

For multisig transactions, harvesters do not include the transaction in a block until enough cosignatures have
been collected to meet the multisig account's signature threshold.
If the deadline expires first, the multisig transaction and its accumulated cosignatures are dropped from the pool.

### 6. Confirmation

Newly created blocks are propagated to other nodes that validate them and either accept or reject them.
The <consensus:> mechanism ensures that all nodes on the network ultimately agree on the same blocks.
Once the block containing a transaction is accepted by consensus, the transaction is _confirmed_.

Occasionally, a block already accepted by a node is later rejected by the majority of the network and must be
<rollback:|rolled back>.
In this case, the block's transactions are reverted and returned to the unconfirmed pool.

NEM bounds how far back a rollback can reach with the <rewrite limit:>.

If a transaction's deadline expires while it is still in the unconfirmed pool, it is dropped from the pool.
This may happen, for example, if the transaction fee offered is too low to be included by any harvester.

## Common Transaction Structure

All transaction types in NEM share a set of common attributes:

| Attribute             | Description                                                                           |
| --------------------- | ------------------------------------------------------------------------------------- |
| **Signer public key** | Public key of the account that created and signed the transaction.                    |
| **Signature**         | Cryptographic proof that the signer authorized the transaction and its content.       |
| **Deadline**          | Timestamp indicating when the transaction expires if not confirmed.                   |
| **Fee**               | Fee the signer pays to have the transaction included in a block.                      |
| **Type**              | Transaction type, which determines which additional attributes, if any, are present.  |

## Validation Details

Before a transaction is included in a block, each node independently validates it using the following checks:

| **Check**            | **Description**                                                                                                                                    |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Signature check**  | Verifies the signature is valid and matches the signer's public key and the transaction's contents.                                                |
| **Fee check**        | Confirms the fee meets the node's minimum threshold and that the signer has enough XEM to pay the fee.                                             |
| **Deadline check**   | Discards the transaction if its deadline has already passed.                                                                                       |
| **Timestamp check**  | Rejects transactions whose timestamp lies too far in the future, protecting against clock manipulation.                                            |
| **Network check**    | Rejects transactions that target a different network, for example a testnet transaction sent to mainnet.                                           |
| **Uniqueness check** | Rejects transactions whose hash already appears in the recent chain history, preventing replay.                                                    |
| **Semantic checks**  | Validates that the transaction is logically correct based on its type. Example: a transfer transaction fails if the sender lacks sufficient funds. |

Transactions that fail any of these checks are rejected and not propagated further.

## Supported Transaction Types

NEM supports the following transaction types, each tailored to a specific kind of operation.
All transaction types share the same [common structure](#common-transaction-structure) and follow the same processing
and validation steps, but differ in purpose and required fields.

<div class="subsections" markdown>

| **Transaction Type**                                                | **Description**                                                                                               |
| ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| **[Transfer Transactions](default:transfer transaction)**           |                                                                                                               |
| `Transfer`                                                          | Send XEM or <mosaics:> and an optional message between two <accounts:>.                                       |
| **[Harvesting](default:harvesting)**                                |                                                                                                               |
| `Account Key Link`                                                  | Activate or deactivate delegated harvesting by linking a remote account.                                      |
| **[Multisig](default:multisignature account)**                      |                                                                                                               |
| `Multisig Account Modification`                                     | Create a multisig account, add or remove cosignatories, and change the minimum number of required signatures. |
| `Multisig Cosignature`                                              | Provide a cosignature for a pending multisig transaction.                                                     |
| `Multisig`                                                          | Wrap an inner transaction issued on behalf of a multisig account.                                             |
| **[Namespaces](default:namespace)**                                 |                                                                                                               |
| `Namespace Registration`                                            | Register or renew a namespace.                                                                                |
| **[Mosaics](default:mosaic)**                                       |                                                                                                               |
| `Mosaic Definition`                                                 | Create a new mosaic.                                                                                          |
| `Mosaic Supply Change`                                              | Change the total supply of a mosaic.                                                                          |

</div>
