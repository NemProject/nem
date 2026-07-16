---
title: Register Subnamespace
tutorial_level: intermediate
---

# Registering a Subnamespace

<Subnamespaces:> (also called "child" namespaces) extend the hierarchical structure of <namespaces:>.

This tutorial shows how to register a subnamespace under an existing <root namespace:>.

## Prerequisites

Before you start, make sure to:

* Set up your development environment.
  See [Setting Up a Development Environment](../start/setup.md).
* Have an <account:> with an existing root namespace.
  See [Registering a Root Namespace](./register-root-namespace.md).

    !!! note
        The examples in this tutorial use a root namespace named `ns_root`.
        Make sure to update the code to use your own root namespace name.

* Obtain <XEM:> to pay for the transaction and lease fees.
  See [Getting Testnet Funds from the Faucet](../accounts/testnet-faucet.md).

Additionally, review the [Transfer XEM](../transactions/transfer-xem.md) tutorial to understand how
transactions are announced and confirmed.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/namespaces/register_subnamespace', ['py', 'js']) }}

## Code Explanation

The code follows the same pattern as the [Registering a Root Namespace](./register-root-namespace.md) tutorial.
This section focuses only on the key differences.

For detailed explanations of the common steps (setting up the account, fetching network time, announcing) and the
transaction descriptor fields shared with a root namespace,
see [Registering a Root Namespace](./register-root-namespace.md).

### Choosing the Subnamespace Name

{{ tutorial.code_snippet_tagged('step-1') }}

A subnamespace is identified by its full name, which joins the parent namespace name and the child name with a dot,
such as `ns_root.product`.
See [Name](../../textbook/namespaces.md#name) in the Textbook for the naming rules.

To avoid collisions across multiple runs of the tutorial, a timestamp is added to the child name.
In practice, however, programs would use a fixed name for their subnamespaces.
You can force the tutorial to use fixed names through the `ROOT_NAMESPACE` and `SUBNAMESPACE` environment variables.

!!! warning "Use a parent namespace owned by the signer"

    By default, the code uses the test account referenced by `SIGNER_PRIVATE_KEY` and a parent namespace named
    `ns_root`.

    If you come from the [Registering a Root Namespace](./register-root-namespace.md) tutorial, set the
    `SIGNER_PRIVATE_KEY` and `ROOT_NAMESPACE` environment variables to match the account and namespace you created
    there, or any other namespace that the signer owns.

### Building the Transaction

{{ tutorial.code_snippet_tagged('step-2') }}

The main difference when registering a subnamespace is in the transaction descriptor:

* {{ tutorial.var('parent_name') }}: The name of the parent namespace, defined in the previous step.
    It can be a root namespace or another subnamespace.

* {{ tutorial.var('name') }}: The name of the subnamespace, chosen in the previous step.

    Note that this is just the name of the subnamespace, not the full path.
    For example, to create `company.product`, where `company` is the root, you would set
    {{ tutorial.var("`name: 'product'`") }} and {{ tutorial.var("`parent_name: 'company'`") }}.

* {{ tutorial.var('rental_fee') }}: The lease fee, which is 10 XEM for subnamespaces, paid to the same
    [sink account](./register-root-namespace.md#building-the-transaction) as root namespaces.

    The SDK's <dy:FeeCalculator.calculateNamespaceRentalFee> helper returns the required amount.
    The {{ tutorial.lit('False') }} argument requests the fee for a subnamespace.

The transaction is then signed, announced, and confirmed following the same process as in the
[Registering a Root Namespace](./register-root-namespace.md#submitting-the-transaction) tutorial.

### Retrieving the Subnamespace

{{ tutorial.code_snippet_tagged('step-3') }}

To verify the subnamespace was registered, the code retrieves it from the network using the <get:/namespace> endpoint
and displays its properties.

The subnamespace is queried by its full name, which joins the parent and child names with a dot
(for example, `ns_root.sub_1783411728`).

A successful response confirms the subnamespace is registered and active.

The response also shows the registration height, which is the block in which the root namespace was registered,
because subnamespaces inherit their root namespace's [lease](../../textbook/namespaces.md#duration).

!!! note "Subnamespace duration"

    A subnamespace expires when its root namespace expires and cannot be renewed on its own.
    [Renewing the root namespace](./extend-root-namespace.md) also renews the subnamespace.

## Output

The output shown below corresponds to a typical run of the program.

```text linenums="1" hl_lines="5 6 7 32-34"
--8<-- 'devbook/namespaces/register_subnamespace.log'
```

Some highlights from the output:

* **Full namespace path** (line 5): `ns_root.sub_1783411728` combines the parent namespace `ns_root` with the
    subnamespace name set in the transaction.

* **Lease fee and transaction fee** (lines 6-7): The lease fee is 10 XEM because this is a subnamespace
    (root namespaces pay 100 XEM instead), while the transaction fee is 0.15 XEM.

* **Namespace information** (lines 32-34): The registered subnamespace, its owner (the signer's address), and the
    registration height, which is the block at which the root namespace's lease began, inherited by the subnamespace.

## Conclusion

This tutorial showed how to:

| Step                                                                       | Related documentation                                                    |
| -------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| [Build a subnamespace registration transaction](#building-the-transaction) | <dy:TransactionFactory.create>, <ser:NamespaceRegistrationTransactionV1> |
| [Calculate the lease fee](#building-the-transaction)                       | <dy:FeeCalculator.calculateNamespaceRentalFee>                           |
| [Retrieve the subnamespace](#retrieving-the-subnamespace)                  | <get:/namespace>                                                         |

## Next Steps

Now that you have a subnamespace, you can:

* Register additional subnamespaces to expand your hierarchical structure
* [Define mosaics](../mosaics/create-mosaic.md) under the subnamespace to create custom assets
