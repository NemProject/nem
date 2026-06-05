# Transfer Transactions

Transfer Transaction
:   A <transaction:> that allows sending <XEM:>, <mosaics:>, and optional messages from one account to another.

Transfer transactions are the most common type of transaction on NEM, enabling both asset transfers and simple
communication.

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

The recipient is specified as an <address:>.

!!! warning "Assets sent to an unowned address are lost"

    It is possible to send XEM or mosaics to any valid address, including one that has never appeared on-chain.
    If no one holds the <private key:> corresponding to that address, the transferred assets are unrecoverable.

### XEM Amount

The **XEM amount** is expressed in micro-XEM, where `1 XEM = 1'000'000 micro-XEM`.
It serves two purposes depending on the [mosaic list](#list-of-transferred-mosaics):

* **Transfer amount.**
    When the mosaic list is empty, the XEM amount is the XEM sent to the recipient.
* **Mosaic multiplier.**
    When the mosaic list is non-empty, the XEM amount transfers no XEM on its own.
    Instead, the value is divided by `1'000'000` to obtain a **multiplier** that scales every attached mosaic's
    quantity.
    The same multiplier applies to all mosaics in the list.

    For example, `2'000'000` yields a multiplier of `2`, doubling every mosaic quantity in the list.
    A list entry of `500` units is delivered as `1000` units to the recipient.

!!! note "Multiplier rules"

    When the XEM amount acts as a multiplier:

    * The value must be divisible by `1'000'000` so the resulting multiplier is an integer.
        Fractional amounts are rejected.
    * Wallets set it to `1'000'000` by convention, transferring each mosaic quantity as specified.
    * A multiplier of `0` produces a valid transaction that transfers no mosaics.

### List of Transferred Mosaics

A transfer transaction can include up to **10 mosaics**.
The list can also be empty, which lets the sender attach a message without moving any assets.

Each entry specifies a mosaic ID and a quantity.
Quantities are integers counted in the mosaic's **atomic units**.

A mosaic's `divisibility` property, set when the mosaic is created and ranging from `0` to `6`,
defines how many atomic units make one whole unit.
XEM, for example, has divisibility `6`, so `1'000'000` atomic units equal one whole XEM.

Full details appear in the [Mosaics](./mosaics.md) page.

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
The protocol does not standardize an encryption scheme.

Two schemes are widely used in existing wallets and SDKs:
**AES-CBC** and **AES-GCM**, both with a shared key derived via Elliptic Curve Diffie-Hellman (ECDH).
Each scheme reserves part of the 1024-byte payload for cryptographic metadata, leaving up to **960 bytes** of usable
plaintext under AES-CBC and **996 bytes** under AES-GCM.

!!! warning "CBC and GCM are not interoperable"

    Nodes accept and store both schemes under the same `0x0002` flag without inspecting the payload.
    A recipient can only decrypt a secure message when its tooling implements the same scheme the sender used.
    Messages produced by GCM tooling cannot be decrypted by CBC-only tooling, and vice versa.

## Fees

A transfer transaction's fee depends on what is sent.
It is the sum of two components:

* The **transfer fee**, based on the XEM amount or the attached mosaics.
* The **message fee**, based on the length of any attached message.

### Transfer Fee

For a XEM-only transfer, the fee scales with the amount sent:

| Amount sent                  | Cost      |
| ---------------------------- | --------- |
| Up to 10'000 XEM             | 0.05 XEM  |
| Each additional 10'000 XEM   | +0.05 XEM |
| 250'000 XEM or more          | 1.25 XEM  |

For a mosaic transfer, the fee is the sum of every attached mosaic's individual fee.
Each mosaic is priced as follows:

* **Tiny, indivisible mosaics** (supply ≤ 10'000 and divisibility 0) pay a flat **0.05 XEM**.
* **All other mosaics** are priced from their **XEM-equivalent value**, derived from the transferred quantity and the
    mosaic's total supply.
    This value maps to the same 0.05-to-1.25 XEM fee tiers used for XEM-only transfers, with a **supply discount**
    that grows as the mosaic's total supply shrinks.

The minimum per-mosaic fee is **0.05 XEM**.

??? info "Mosaic Fee Calculation"

    Computing a non-tiny mosaic's fee takes three steps: compute the transferred quantity's XEM-equivalent value,
    look that value up on the fee tiers to get a base fee, then subtract the supply discount.

    **1. XEM-equivalent value**

    \[
    \text{xem\_equivalent} \approx \frac{8.999 \times 10^9 \cdot \text{atomic\_quantity} \cdot \text{multiplier}}{\text{total\_atomic\_supply}}
    \]

    where:

    * `8.999 × 10^9` is the initial XEM supply, in whole units.
    * `atomic_quantity` is the amount of the mosaic being transferred, in atomic units.
    * `multiplier` is the [XEM-amount multiplier](#xem-amount) (typically 1).
    * `total_atomic_supply` is the mosaic's total supply, in atomic units: `supply × 10^divisibility`.

    **2. Base fee**

    The resulting value is then priced on the same 0.05-to-1.25 XEM fee tiers as a XEM-only transfer,
    yielding the mosaic's **base fee**.

    **3. Supply discount**

    A **supply discount** is then subtracted from that base fee:

    \[
    \text{discount} = \left\lfloor 0.8 \cdot \ln \!\left( \frac{9 \times 10^{15}}{\text{total\_atomic\_supply}} \right) \right\rfloor \cdot 0.05 \text{ XEM}
    \]

    where `9 × 10^15` is the largest mosaic quantity NEM allows.

    Scarcer mosaics receive a larger discount, because the logarithm grows as the supply shrinks.

    The final fee is **never less than 0.05 XEM**, even when the discount exceeds the base fee.

### Message Fee

A non-empty message costs **0.05 XEM** as a base, plus **0.05 XEM** for every additional 32 bytes of
payload, up to the 1024-byte maximum:

| Message length         | Added cost |
| ---------------------- | ---------- |
| No message             | —          |
| 1 to 31 bytes          | 0.05 XEM   |
| 32 to 63 bytes         | 0.10 XEM   |
| 64 to 95 bytes         | 0.15 XEM   |
| …                      | …          |
| 1024 bytes (maximum)   | 1.65 XEM   |

The fee is calculated on the stored payload size, so [secure messages](#secure-message-conventions) are billed on their
encrypted payload, not the plaintext.
