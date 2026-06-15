---
title: Create from Private Keys
tutorial_level: beginner
---

# Creating Accounts from Private Keys

This tutorial shows how to create <accounts:> for the NEM blockchain, either by using an existing <private key:>
or by generating a new random account.

## Prerequisites

If you have not done so already, start with [Setting Up a Development Environment](../start/setup.md).

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/accounts/create_from_private_key', ['py', 'js']) }}

## Code Explanation

### Initializing the Facade

{{ tutorial.code_snippet_tagged('step-1') }}

The <dy:NemFacade> provides access to NEM's cryptographic operations and network utilities.
It is initialized with a network name (`testnet` or `mainnet`) to ensure that network-specific values,
such as <addresses:>, are generated correctly.

### Defining a Private Key

{{ tutorial.code_snippet_tagged('step-2') }}

The example starts by retrieving a private key from the environment variable `PRIVATE_KEY` as a hexadecimal string.
If the variable is set, the value is converted into a <dy:PrivateKey> object.
Otherwise, a new random private key is generated using <dy:PrivateKey.random> instead.

!!! warning "Store your private key securely"
    The private key gives full control over the account and any assets it holds.
    If you lose the private key, you lose access to the account permanently.
    If someone else obtains the private key, they can control the account.

    Never share your private key with anyone, and always store it in a secure location.

### Creating the Account

{{ tutorial.code_snippet_tagged('step-3') }}

After defining the private key, an account is created by deriving its public key and address.

1. **Key pair creation:** The <dy:KeyPair> constructor takes the private key and mathematically derives the
    corresponding <public key:>.
    While the private key must remain secret, the public key can be safely shared with anyone.

2. **Address derivation:** The <dy:network.publicKeyToAddress> method converts the public key into an
    <address:>, a shorter, human-readable, network-specific identifier for the account.

## Output

The output shown below corresponds to a typical run of the program.

```text
--8<-- 'devbook/accounts/create_from_private_key.log'
```

Each time the program runs without the environment variable, it generates a different random account.
If a private key is provided, the same public key and address are always derived.

## Conclusion

This tutorial showed how to:

| Step                                                          | Related documentation                    |
| ------------------------------------------------------------- | ---------------------------------------- |
| [Load a private key](#defining-a-private-key)                 | <dy:PrivateKey>                          |
| [Create a random private key](#defining-a-private-key)        | <dy:PrivateKey.random>                   |
| [Get the public key](#creating-the-account)                   | <dy:KeyPair.publicKey>                   |
| [Get the address](#creating-the-account)                      | <dy:network.publicKeyToAddress>          |

## Next Steps

Now that you have an account, you can:

* [Get testnet funds from the faucet](./testnet-faucet.md)
* [Send your first transaction](../transactions/transfer-xem.md)
