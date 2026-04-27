# Transfer Transactions

Transfer Transaction
:   A <transaction:> that allows sending <XEM:>, <mosaics:>, and optional messages from one account to another.

Transfer transactions are the most common type of transaction on NEM, enabling both asset transfers and simple
communication.

Adapts [Transfer Transactions](https://docs.symboltest.net/en/textbook/transfer_transactions/) concepts to NEM technology.

## Key Features

* **Mosaic Transfer**

    You can attach one or more mosaics to a transfer transaction.
    All mosaics in a transfer transaction are sent from one sender to one recipient.
    This makes transfer transactions ideal for simple, direct asset transfers.

* **Message Support**

    Optional plaintext or encrypted messages can be included.
    A transfer transaction does not require mosaics, so messages can also be sent on their own.
    This allows for simple communication alongside the mosaic transfers.

* **Multisig Compatibility**

    Transfer transactions, like all other transactions, support <multisignature accounts:>.
    This allows them to require authorization from more than one account, allowing complex governance schemes.

## Structure

Besides the [common transaction structure](./transactions.md#common-transaction-structure),
a transfer transaction contains the following attributes:

| Attribute                                                       | Description                                                                                                                                                             |
| --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [**Recipient's address**](#recipients-address)                  | Address of the receiving account.                                                                                                                                       |
| [**XEM amount**](#xem-amount)                                   | Either the XEM sent to the recipient (when the mosaic list is empty) or a multiplier applied to each attached mosaic (when the mosaic list is non-empty).               |
| [**List of transferred mosaics**](#list-of-transferred-mosaics) | Zero or more mosaics to transfer.                                                                                                                                       |
| [**Optional message**](#optional-message)                       | Plaintext or encrypted message, up to 1024 bytes.                                                                                                                       |

### Recipient's Address

The recipient is specified as a 40-character base32-encoded <address:>.

!!! warning "Assets sent to an unowned address are lost"

    It is possible to send XEM or mosaics to any valid address, including one that has never appeared on-chain.
    If no one holds the <private key:> corresponding to that address, the transferred assets are unrecoverable.

### XEM Amount

The **XEM amount** is expressed in micro-XEM, where `1 XEM = 1 000 000 micro-XEM`.
It serves two purposes depending on the [mosaic list](#list-of-transferred-mosaics):

* **Transfer amount.**
    When the mosaic list is empty, the XEM amount is the XEM sent to the recipient.
* **Mosaic multiplier.**
    When the mosaic list is non-empty, the XEM amount transfers no XEM on its own.
    Instead, it acts as a whole-number **multiplier** applied to every attached mosaic's quantity.
    The same multiplier applies to all mosaics in the list.

    For example, `2 000 000` doubles every mosaic quantity in the list, so a list entry of `500` units is delivered as
    `1 000` units to the recipient.

!!! note "Multiplier rules"

    When the XEM amount acts as a multiplier:

    * The multiplier must be a whole number of XEM, so the stored micro-XEM value must be divisible by `1 000 000`.
        Fractional amounts are rejected.
    * Wallets set it to `1 000 000` by convention, transferring each mosaic quantity as specified.
    * A multiplier of `0` produces a valid transaction that transfers no mosaics.

### List of Transferred Mosaics

A transfer transaction can include up to **10 mosaics**.
The list can also be empty, which lets the sender attach a message without moving any assets.

Each entry specifies a mosaic ID and a quantity.
Quantities are raw integers counted in the mosaic's **atomic units**.

A mosaic's `divisibility` property, set when the mosaic is created and ranging from `0` to `6`,
defines how many atomic units make one whole unit.
XEM, for example, has divisibility `6`, so 1 000 000 atomic units (micro-XEM) equal one whole XEM.

Full details appear in the [Mosaics](./mosaics.md) chapter.

The network rejects the transaction if the sender does not hold enough of any listed mosaic.

!!! tip "Sending XEM alongside other mosaics"

    To transfer XEM in the same transaction as other mosaics, include XEM as an entry in this list.
    Its quantity is then scaled by the XEM-amount multiplier along with every other mosaic in the list.

### Optional Message

A transfer transaction may carry an optional message of up to **1024 bytes**.
With no attached mosaics and a `0` XEM amount, the transfer carries only the message.

Every message contains a **type** field identifying its payload as plaintext (`0x0001`) or secure (`0x0002`).

Nodes enforce the type field and the 1024-byte size limit.
They do not interpret the payload bytes.
The protocol does not define a plaintext encoding, and it does not standardize an encryption scheme for secure
payloads.
Both are conventions agreed between sender and recipient.

#### Plaintext conventions

Plaintext payloads are stored as-is.
Sender and recipient agree on a format such as UTF-8, JSON, or hex.

NEM wallets and applications typically assume UTF-8.

#### Secure message conventions

Secure payloads are encrypted so that only the recipient can decrypt them.

The protocol does not standardize an encryption scheme, but two are widely used in existing wallets and SDKs:

* **AES-CBC.**
    AES in [CBC mode](https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#CBC), using a shared key derived
    via Elliptic Curve Diffie-Hellman (ECDH).
    The payload is a 32-byte salt, a 16-byte initialization vector (IV), and the ciphertext, leaving up to
    **960 bytes** of usable plaintext.

* **AES-GCM.**
    AES in [GCM mode](https://en.wikipedia.org/wiki/Galois/Counter_Mode) with its own ECDH-based key derivation, under
    the same `0x0002` type flag.
    The payload is a 16-byte authentication tag, a 12-byte IV, and the ciphertext, leaving up to **996 bytes** of
    usable plaintext.

!!! warning "CBC and GCM are not interoperable"

    Nodes accept and store both schemes under the same `0x0002` flag without inspecting the payload.
    A recipient can only decrypt a secure message when its tooling implements the same scheme the sender used.
    Messages produced by GCM tooling cannot be decrypted by CBC-only tooling, and vice versa.
