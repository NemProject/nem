---
title: Create from Mnemonic
tutorial_level: beginner
---

# Creating Accounts from Mnemonics

This tutorial shows how to create <accounts:> for the NEM blockchain using a <mnemonic phrase:>,
also known simply as _mnemonic_.

This approach is commonly used by <HD wallets:> to manage multiple accounts from a single seed.

## Prerequisites

If you have not done so already, start with [Setting Up a Development Environment](../start/setup.md).

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/accounts/create_from_mnemonic', ['py', 'js']) }}

## Code Explanation

### Initializing the Facade

{{ tutorial.code_snippet_tagged('step-1') }}

The <dy:NemFacade> provides access to NEM's cryptographic operations and network utilities.
It is initialized with a network name (`testnet` or `mainnet`) to ensure that network-specific values,
such as <addresses:>, are generated correctly.

### Defining a Mnemonic

{{ tutorial.code_snippet_tagged('step-2') }}

The example checks for an existing mnemonic in the `MNEMONIC` environment variable.
If the variable is set, the mnemonic is loaded from it.
Otherwise, a new random mnemonic is generated using <dy:Bip32.random>.

NEM uses the [BIP39](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki) standard, which represents
mnemonics as 24 English words selected from a standardized word list.
These words encode the entropy (randomness) used to create all derived private keys.

!!! warning "Store your mnemonic phrase securely"
    The mnemonic phrase can be used to regenerate all derived accounts and private keys.
    Anyone with access to it can control your accounts, and losing it means losing access permanently.

    Never share your mnemonic with anyone, and always store it in a secure location.

### Deriving the Root Node

{{ tutorial.code_snippet_tagged('step-3') }}

After defining the mnemonic, <dy:Bip32.fromMnemonic> converts the mnemonic and a password into a root node,
which serves as the starting point for deriving child accounts.

The password (sometimes called a "25th word") is an optional string that extends the mnemonic seed.
It can be left empty or set to any value.
When used, it adds another layer of security.
Different passwords with the same mnemonic produce completely different accounts.

!!! note "Password security"
    The password is part of the account derivation.
    Both the mnemonic and password are required to regenerate the accounts.
    If you lose either one, you lose access to all derived accounts.

In this example, the password is loaded from the `PASSWORD` environment variable.
If not set, the snippet uses a default one.

### Deriving the Child Account

{{ tutorial.code_snippet_tagged('step-4') }}

The root node can generate multiple accounts, each with its own unique keys and address.
This allows a single mnemonic to manage many accounts while keeping them cryptographically isolated.

Deriving an account requires specifying an account index.
<dy:NemFacade.bip32Path> generates the derivation path
(a standardized string that specifies which account to derive) for that index,
and <dy:Bip32Node.derivePath> follows that path to create the account.

In this example, the account at index `0` is derived.
Additional accounts can be derived by using different indices (e.g., `1`, `2`, `3`, ...).
Each index produces a completely different account.

### Creating the Account

{{ tutorial.code_snippet_tagged('step-5') }}

Once the child node is derived, it is converted into a usable key pair and address.

1. **Key pair creation:** <dy:NemFacade.bip32NodeToKeyPair> extracts the <private key:> and <public key:> from the
   child node.
   The private key must remain secret, while the public key can be safely shared.

2. **Address derivation:** <dy:network.publicKeyToAddress> converts the public key into an <address:>, a shorter,
   human-readable, network-specific identifier for the account.

## Output

The output shown below corresponds to a typical run of the program.

```text
--8<-- 'devbook/accounts/create_from_mnemonic.log'
```

Each time the code runs without environment variables, it generates a different random mnemonic and account.
If the same mnemonic and password are provided, the same account is always derived for the given account index.

## Conclusion

This tutorial showed how to:

| Step                                                         | Related documentation                                                                           |
| -----------------------------------------------------------  | ----------------------------------------------------------------------------------------------- |
| [Create a random mnemonic](#defining-a-mnemonic)             | <dy:Bip32.random>                                                                               |
| [Derive an account from a mnemonic](#deriving-the-root-node) | <dy:Bip32.fromMnemonic>, <dy:NemFacade.bip32Path>, and <dy:Bip32Node.derivePath>                |
| [Get the key pair of the account](#creating-the-account)     | <dy:NemFacade.bip32NodeToKeyPair>, <dy:network.publicKeyToAddress>                              |
