---
title: Register Root Namespace
tutorial_level: intermediate
---

# Registering a Root Namespace

<Namespaces:|Namespaces> provide labels that group related <mosaics:> under a meaningful name, like the `nem` prefix
in the native `nem:xem` mosaic.

Namespaces can be nested under other namespaces, and this tutorial shows how to register a <root namespace:> for one
year.

To learn how to register a <subnamespace:> instead, read the [Registering a Subnamespace](./register-subnamespace.md)
guide.

## Prerequisites

Before you start, make sure to:

* Set up your development environment.
  See [Setting Up a Development Environment](../start/setup.md).
* Create an <account:> to register the namespace, either [from code](../accounts/create-from-private-key.md) or
  [by using a wallet](../../userbook/wallet/create-account.md).
* Obtain <XEM:> to pay for the transaction and lease fees.
  See [Getting Testnet Funds from the Faucet](../accounts/testnet-faucet.md).

Additionally, review the [Transfer XEM](../transactions/transfer-xem.md) tutorial to understand how transactions are
announced and confirmed.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/namespaces/register_root_namespace', ['py', 'js']) }}

## Code Explanation

### Setting Up the Account

{{ tutorial.code_snippet_tagged('step-1') }}

The snippet reads the signer's private key from the `SIGNER_PRIVATE_KEY` environment variable, which defaults to a test
key if not set.
The signer's address is derived from the public key.
This account will own the registered namespace.

### Fetching Network Time

{{ tutorial.code_snippet_tagged('step-2') }}

Network time is fetched from <get:/time-sync/network-time>, and the transaction's `timestamp` and `deadline` fields
are derived from it, following the process described in the [Transfer XEM](../transactions/transfer-xem.md) tutorial.

### Building the Transaction

{{ tutorial.code_snippet_tagged('step-3') }}

A namespace is identified by its name, which the transaction registers on the network for one year.

The namespace registration transaction specifies:

* {{ tutorial.var('type') }}: Namespace registration transactions use the type <ser:NamespaceRegistrationTransactionV1>.

* {{ tutorial.var('signer_public_key') }}: The account that signs the transaction and pays the fees.
    It becomes the owner of the registered namespace.

* {{ tutorial.var('timestamp') }} and {{ tutorial.var('deadline') }}: The values computed in the network time step.

* {{ tutorial.var('rental_fee_sink') }}: The special account that collects namespace
    [lease fees](../../textbook/namespaces.md#lease-fee).
    Each network has a fixed sink address:

    * <mainnet:>: `NAMESPACEWH4MKFMBCVFERDPOOP4FK7MTBXDPZZA`
    * <testnet:>: `TAMESPACEWH4MKFMBCVFERDPOOP4FK7MTDJEYP35`

    The network rejects transactions that send the lease fee to any other address.

* {{ tutorial.var('rental_fee') }}: The lease fee, which is 100 XEM for root namespaces.
    The SDK's <dy:FeeCalculator.calculateNamespaceRentalFee> helper returns the required amount.
    The {{ tutorial.lit('True') }} argument requests the fee for a root namespace.

    The network rejects transactions that pay less than this fee.
    Larger amounts are accepted, but the entire amount is transferred to the sink account.

* {{ tutorial.var('name') }}: The name of the root namespace.
    See [Name](../../textbook/namespaces.md#name) in the Textbook for the naming rules.

    To avoid collisions across multiple runs of the tutorial, a timestamp is added to the name.
    In practice, however, programs would use a fixed name for their namespaces.
    You can force the tutorial to use a fixed name through the `ROOT_NAMESPACE` environment variable.

Finally, the transaction fee is calculated with <dy:FeeCalculator.calculateTransactionFee> and attached to the
transaction.
Unlike the lease fee, the transaction fee is paid to the <harvester account:>.
Namespace registration transactions pay a fixed transaction fee of 0.15 XEM, as shown in the
[fee schedule](../../textbook/transactions.md#fee-schedule).

### Submitting the Transaction

{{ tutorial.code_snippet_tagged('step-4') }}

The transaction is signed and announced following the same process as in the
[Transfer XEM](../transactions/transfer-xem.md#announcing-the-transaction) tutorial.

{{ tutorial.code_snippet_tagged('step-5') }}

The code then waits for the transaction to be confirmed by polling the <get:/transaction/get> endpoint until the
transaction is included in a block.

### Retrieving the Namespace

{{ tutorial.code_snippet_tagged('step-6') }}

To verify the namespace was registered, the code retrieves it from the network using the <get:/namespace> endpoint and
displays its properties.

A successful response confirms that the namespace is registered and active.

The response also shows the registration height, which is the block in which the namespace was registered,
marking the start of the one-year lease.

## Output

The output shown below corresponds to a typical run of the program.

```text linenums="1" hl_lines="5 6 7 31-33"
--8<-- 'devbook/namespaces/register_root_namespace.log'
```

Some highlights from the output:

* **Namespace name** (line 5): The chosen name `ns_1783091378` includes a timestamp to ensure uniqueness.
    Search for this name in the [NEM testnet explorer](https://testnet.nem.fyi/) to view the namespace details.

* **Lease fee and transaction fee** (lines 6-7): The lease fee is 100 XEM because this is a root namespace
    (<subnamespaces:> pay 10 XEM instead), while the transaction fee is 0.15 XEM.

* **Namespace information** (lines 31-33): The registered namespace, its owner (the signer's address), and the
    registration height, which is the block at which the lease began.

## Conclusion

This tutorial showed how to:

| Step                                                                     | Related documentation                          |
|--------------------------------------------------------------------------|------------------------------------------------|
| [Build a namespace registration transaction](#building-the-transaction)  | <dy:TransactionFactory.create>                 |
| [Calculate the lease fee](#building-the-transaction)                     | <dy:FeeCalculator.calculateNamespaceRentalFee> |
| [Retrieve the namespace](#retrieving-the-namespace)                      | <get:/namespace>                               |

## Next Steps

Now that you have a root namespace, you can:

* [Define mosaics](../mosaics/create-mosaic.md) under the namespace to create custom assets
* [Register a subnamespace](./register-subnamespace.md) to create a hierarchical structure
* [Extend the namespace](./extend-root-namespace.md) before it expires to keep it active
