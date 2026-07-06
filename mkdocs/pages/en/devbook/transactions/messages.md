---
title: Messages
tutorial_level: intermediate
---

# Sending Messages with Transfer Transactions

<Transfer transaction:|Transfer transactions> can include an optional message field, which allows attaching up to 1,024
bytes of data to the transaction.
Messages can be sent as plain text or encrypted using the recipient's public key, ensuring only the intended recipient
can read them.

This tutorial shows how to send both plain and encrypted messages and how to decode received messages.

## Prerequisites

Before you start, make sure to:

* Set up your development environment.
    See [Setting Up a Development Environment](../start/setup.md).
* Create an <account:> to send the transfer transaction, either
    [from code](../accounts/create-from-private-key.md) or
    [by using a wallet](../../userbook/wallet/create-account.md).
* Obtain <XEM:> to pay for the transaction fee.
    See [Getting Testnet Funds from the Faucet](../accounts/testnet-faucet.md).

Additionally, check the [Transfer transaction](./transfer-xem.md) tutorial to understand how fee
calculation, network time, and transaction confirmation work.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/transactions/messages', ['py', 'js']) }}

## Code Explanation

This tutorial focuses on the message-specific aspects of transfer transactions.
The parts about fetching network time, calculating fees, and announcing transactions have been explained in the
[Transfer Transaction](./transfer-xem.md) tutorial and are skipped here for brevity.

### Setting Up Accounts

{{ tutorial.code_snippet_tagged('step-1') }}

To send a message, you need the sender's <private key:> and the recipient's <address:>.
To encrypt a message, you additionally need the recipient's <public key:>.

This tutorial uses two accounts (sender and recipient) to demonstrate both sending and receiving plain and encrypted
messages.
The snippet reads their private keys from the `SENDER_PRIVATE_KEY` and `RECIPIENT_PRIVATE_KEY` environment variables,
which default to test keys if not set.
The recipient's public key and address are derived from their private key.

!!! note "Retrieving public keys"

    When only the address is known, you can retrieve the public key from the network using the
    <get:/account/get> endpoint.
    An account's public key becomes available only after it has broadcast at least one transaction.

### Sending a Plain Text Message

{{ tutorial.code_snippet_tagged('step-2') }}

You can combine mosaic transfers with messages by including both the `mosaics` and `message` fields in the transaction
descriptor.

The transaction is then signed and announced following the same process as in
[Sending XEM with a Transfer Transaction](./transfer-xem.md).

**Message constraints:**

* **Maximum size:** 1,024 bytes (the network rejects larger messages).
* **Encoding:** UTF-8 by convention, though the protocol does not enforce a standard.
* **Privacy:** All messages are publicly visible on the blockchain unless encrypted.

!!! tip "Handling larger data"

    For applications requiring more than 1,024 bytes of data, common approaches include:

    * **On-chain storage:** Split the data across multiple <transfer transactions:>, allowing you to keep everything on
        the blockchain.
    * **Off-chain storage:** Store the data off-chain and include a hash and a reference in the message field.
        The hash verifies data integrity while the reference enables retrieval.

### Receiving a Plain Text Message

{{ tutorial.code_snippet_tagged('step-3') }}

After announcing the transaction, the {{ tutorial.var('retrieve_confirmed_transaction') }} helper function polls the
<get:/transaction/get> endpoint until the transaction is confirmed.

The confirmed transaction contains the message as a hex string.
To retrieve the original message, it converts the hex string to bytes and decodes it as UTF-8.

### Sending an Encrypted Message

{{ tutorial.code_snippet_tagged('step-4') }}

Encrypted messages provide confidentiality by protecting the message content using a shared secret derived from the
sender's private key and the recipient's public key.
Both the sender and recipient can decrypt the message using their own private key and the other party's public key.

The <dy:MessageEncoder> class handles message encryption:

1. A <dy:MessageEncoder> is created with the sender's key pair.
2. The message is encoded using the recipient's public key and the message bytes with <dy:MessageEncoder.encode>.
3. The encrypted payload is attached to the transaction's `message` field.

The transaction is then signed and announced following the same process as in
[Sending XEM with a Transfer Transaction](./transfer-xem.md).

!!! note "Message encryption is a convention"

    The NEM protocol does not define a standard for message encryption.
    Sender and recipient must agree in advance on whether messages are encrypted and the cipher used.

    <dy:MessageEncoder.encode> implements **AES-GCM**, the convention used by most wallets and applications.
    The class also provides <dy:MessageEncoder.encodeDeprecated> for compatibility with legacy AES-CBC messages.
    <dy:MessageEncoder.tryDecode> automatically detects and decodes both schemes.


    For more details, see [Optional Messages](../../textbook/transfer_transactions.md#optional-message) in the Textbook.

### Receiving an Encrypted Message

{{ tutorial.code_snippet_tagged('step-5') }}

After announcing the encrypted message transaction, the {{ tutorial.var('retrieve_confirmed_transaction') }} helper
function polls for confirmation.

To decrypt the message from the confirmed transaction, a <dy:MessageEncoder> is created with the recipient's key pair,
then <dy:MessageEncoder.tryDecode> is called with the sender's public key (obtained from the transaction's
`signer` field) and the encrypted payload.

The method returns a tuple {{ tutorial.var('(is_decoded, message)') }} indicating whether decryption was successful,
and, if so, contains the original plaintext bytes, which still need to be decoded.

!!! note "Decryption works both ways"

    Because the encryption uses a shared secret derived from both key pairs, the sender can also decrypt the message
    using their own private key and the recipient's public key.
    This allows both parties to verify the message content after it has been published on the blockchain.

If decryption fails, possible causes include:

* The message was encrypted for a different recipient.
* The message is corrupted or tampered with.
* The message is plain text, not encrypted.
* An incorrect public key was used for the other party.
* The same convention was not used when encrypting and decrypting.

## Output

The output shown below corresponds to a typical run of the program.

```text linenums="1" hl_lines="9 16 19 20 27"
--8<-- 'devbook/transactions/messages.log'
```

Some highlights from the output:

* **Plain message** (line 9): The message attached to the first transaction.
    Because it is not encrypted, anyone inspecting the blockchain can read it.

* **Received plain message** (line 16): The same message, recovered from the confirmed transaction by converting the
    hexadecimal payload back to UTF-8.

* **Original message** (line 19): The secret message before encryption.

* **Encrypted payload** (line 20): The result of encrypting the previous message with <dy:MessageEncoder.encode>,
    shown as a hexadecimal string.
    This is what gets stored on the blockchain.

* **Recipient decrypted message** (line 27): The original message, retrieved from the confirmed transaction and
    decrypted by <dy:MessageEncoder.tryDecode> using the recipient's private key and the sender's public key.

You can view the transactions on the [NEM testnet explorer](https://testnet.nem.fyi/) by searching for the
transaction hashes printed in the output.

The explorer cannot decrypt encrypted messages because it does not have access to the private keys.

## Conclusion

This tutorial showed how to:

| Step                                                           | Related documentation                       |
| -------------------------------------------------------------- | ------------------------------------------- |
| [Convert text into UTF-8 bytes](#sending-a-plain-text-message) | `TextEncoder` (JS) and `str.encode`/`bytes.decode` (Python) <br> System methods, not part of the SDK |
| [Encrypt a message](#sending-an-encrypted-message)             | <dy:MessageEncoder.encode>                  |
| [Decrypt a message](#receiving-an-encrypted-message)           | <dy:MessageEncoder.tryDecode>               |
