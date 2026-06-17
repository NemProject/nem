# Basic Cryptography

These are the basic cryptography concepts that underpin NEM's technology.

## Hashes

Hash
:   A cryptographic hash is a fixed-size string of characters produced by a mathematical function
    (called a _hash function_) that maps input data of any size to a unique output.

Several such functions exist, such as [Keccak](https://keccak.team/keccak.html) or
[RIPEMD-160](https://en.wikipedia.org/wiki/RIPEMD), but they all share the same essential properties:

* **Determinism**: The same input always produces the same hash.
* **Collision resistance**: It is extremely difficult to find two different inputs that produce the same hash.
* **Irreversibility**: The original input cannot be reconstructed from the hash.

These properties are critical for ensuring data integrity, verifying authenticity,
and linking <blocks:> together in a blockchain.

NEM uses **Keccak-256**, **Keccak-512**, and **RIPEMD-160** across key derivation, address generation, signing, and
block hashing.

!!! warning "NEM uses Keccak, not SHA-3"

    NEM adopted Keccak before it was finalized as SHA-3.
    The two algorithms use different padding and therefore produce different outputs for the same input.
    As a result, a standard SHA-3 library cannot verify NEM signatures or regenerate NEM addresses.
    A Keccak implementation such as [Bouncy Castle](https://www.bouncycastle.org/) is required instead.

    NEM's Java source names its helper methods `sha3_256` and `sha3_512`, but both internally call
    `Keccak-*`.
    The `sha3_` prefix is historical and does not refer to the final SHA-3 specification.

## Keys

Private Key
:   A very long, secret number.
    The actual value of the private key is meaningless, and it is meant to be kept secret.
    It should be impossible to guess by unauthorized parties, and, although it is commonly randomly-generated,
    it is extremely unlikely that the same number is generated twice by chance.

NEM private keys are 32 bytes long, typically represented as 64-character hexadecimal strings.

Public Key
:   A very long number that serves as the public identifier of a <private key:> and can be disseminated widely.
    It can be used to prove that the private key is known without revealing it.

    Although mathematically derived from the private key, the reverse operation is practically impossible with
    current technology.

NEM public keys are 32 bytes long, typically represented as 64-character hexadecimal strings.

Key Pair
:   A matched set consisting of a <private key:> and its corresponding <public key:>.
    The private key is kept secret by the owner, while the public key is distributed openly.
    Together, they enable secure cryptographic operations such as digital signatures and encryption.

NEM uses key pairs in two places:

Main Key
:   <Key pair:|Key pair> associated with every <account:>, identifying its owner.

Remote Key
:   Key pair associated with an account that has delegated harvesting to a remote node (see <harvesting:>).
    The remote key signs blocks on behalf of the main account without exposing the main private key.

??? warning "Key Security"

    The **private key** in any key pair should be kept secret at all times.

    However, the severity of having a secret key revealed depends on the purpose of that key:

    | Key        | Severity | Impact |
    | ---------- | -------- | ------ |
    | **Main**   | 🔴 HIGH  | Assets can be transferred out of the account. |
    | **Remote** | 🟠 MED   | Harmless to the delegating account's funds. An attacker gathering a large number of remote keys could gain substantial harvesting power and influence which blocks are added to the blockchain. Easily reverted by linking another remote account. |

On NEM, both the private and the public key are 256-bit (32-byte) integers.
The public key is obtained via [Elliptic Curve Cryptography](https://en.wikipedia.org/wiki/Elliptic-curve_cryptography)
using [Ed25519](https://ed25519.cr.yp.to), which is defined over a
[twisted Edwards curve](https://en.wikipedia.org/wiki/Twisted_Edwards_curve).

## Signatures

Signature
:   A digital attachment to a document that certifies that the document is approved by a given <account:>.

The signature is obtained by processing the document with the <private key:> of the account,
so that anybody can use the associated public key to verify that the signature matches the document,
but only the owner of the private key can produce an identical signature.

All transactions on NEM are signed, but the signatures required depend on the transaction type and its participants.
For example, transferring assets from a single-owner account to another only requires the signature of the
source account's private key.

However, transferring assets from a <multisignature account:|multiple-owner account> requires the approval of enough
cosignatories to meet the multisig threshold, and must therefore gather multiple signatures before it is considered
valid.

Signatures on NEM are 512-bit (64-byte) long and are generated using the [Ed25519](https://ed25519.cr.yp.to) algorithm
with the **Keccak-512** hash function.

## Addresses

Address
:   A convenient, shorter form of a <public key:>, that simplifies sharing it by requiring only
    letters and numbers. It is typically a synonym for <account:>.

Keys, both public and private, are binary data which is hard to print and share, whereas addresses are made up of
only latin letters and numbers.

Moreover, NEM keys require 32 bytes of binary data, or 64 hexadecimal characters.
Addresses, on the other hand, only require 40 characters, reaching a compromise between length and practicality.

On NEM, addresses are obtained from public keys by:

1. Applying [Keccak-256](https://keccak.team/keccak.html) to the public key to produce a 32-byte hash.
2. Applying [RIPEMD-160](https://en.wikipedia.org/wiki/RIPEMD) to the result to produce a 20-byte hash.
3. Generating a 25-byte **raw address** by joining:

    * A 1-byte network version: `0x68` for <mainnet:> (`N`), `0x98` for <testnet:> (`T`), or `0x60` for
        <mijinnet:> (`M`).
    * The 20-byte RIPEMD-160 hash from step 2.
    * A 4-byte checksum to detect mistyped addresses, computed as the first 4 bytes of the `Keccak-256` hash of the
        previous 21 bytes (network version + RIPEMD-160 hash).

4. Generating a 40-character **encoded address** by [Base32-encoding](https://en.wikipedia.org/wiki/Base32) the raw
    address.

    The encoded address is the most common way of sharing addresses because it only uses uppercase letters and digits.

    Example: `NBHK6WHL5TGBMCLVW4RSFMRO4ZYXCJFRAVO2B4FU`

5. Optionally, for easier reading, hyphens can be added every 6 characters to create a 46-character **pretty address**.

    Example: `NBHK6W-HL5TGB-MCLVW4-RSFMRO-4ZYXCJ-FRAVO2-B4FU`

!!! note "Address generation is an offline process"

    Note that address generation does not require interaction with the blockchain.

    In fact, NEM only tracks addresses and associated public keys when they first appear in a transaction.

## Vanity Addresses

While keys, and therefore <addresses:> too, are normally generated randomly, it is possible to create
**vanity addresses** that include specific patterns or prefixes.

This involves generating <key pairs:> repeatedly until one produces an address that meets the desired criteria.
The process usually requires substantial time and computation depending on the complexity of the pattern.

Vanity addresses can be useful for branding, visibility, or personal preference, but they offer no security advantage.
