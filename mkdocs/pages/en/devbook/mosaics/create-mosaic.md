---
title: Create Mosaic
tutorial_level: intermediate
---

# Creating a Mosaic

<mosaics:|Mosaics> represent assets on the NEM blockchain, such as currencies, collectibles, or access rights.
Unlike tokens on other platforms, NEM mosaics are supported directly at the protocol level
and require no additional coding to use.

Their properties are configurable to support various use cases, from simple currencies to tokens with custom supply
and transfer rules.

Every mosaic belongs to a registered <namespace:>, which provides the first half of its
[fully qualified name](../../textbook/mosaics.md#fully-qualified-name), such as `my_namespace:token`.
A namespace must therefore be registered before a mosaic can be created.

This tutorial shows how to create a mosaic under an existing namespace and configure its initial properties.

## Prerequisites

Before you start, make sure to:

* Set up your development environment.
    See [Setting Up a Development Environment](../start/setup.md).
* Create an <account:> to own the mosaic, either
    [from code](../accounts/create-from-private-key.md) or
    [by using a wallet](../../userbook/wallet/create-account.md).
* Register a <namespace:> to hold the mosaic.
    See [Registering a Root Namespace](../namespaces/register-root-namespace.md).
* Obtain <XEM:> to pay for the transaction and creation fees.
    See [Getting Testnet Funds from the Faucet](../accounts/testnet-faucet.md).

Additionally, review the [Transfer XEM](../transactions/transfer-xem.md) tutorial to understand how
transactions are announced and confirmed.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/mosaics/create_mosaic', ['py', 'js']) }}

## Code Explanation

### Setting Up the Account

{{ tutorial.code_snippet_tagged('step-1') }}

The snippet reads the signer's private key from the `SIGNER_PRIVATE_KEY` environment variable, which defaults to a
test key if not set.
The signer's address is derived from the public key.
This account will own the created mosaic and must also own the namespace that will hold it.

### Fetching Network Time

{{ tutorial.code_snippet_tagged('step-2') }}

Network time is fetched from <get:/time-sync/network-time>, and the transaction's `timestamp` and `deadline` fields
are derived from it, following the process described in the [Transfer XEM](../transactions/transfer-xem.md) tutorial.

### Choosing the Mosaic Name

{{ tutorial.code_snippet_tagged('step-3') }}

The mosaic ID is assembled from an existing namespace and a mosaic name.
See [Name](../../textbook/mosaics.md#name) in the Textbook for the naming rules.

To avoid collisions across multiple runs of the tutorial, a timestamp is added to the mosaic name.
In practice, however, programs would use a fixed name for their mosaics.
You can force the tutorial to use fixed names through the `NAMESPACE` and `MOSAIC` environment variables.

!!! warning "Use a namespace owned by the signer"

    By default, the code uses the test account referenced by `SIGNER_PRIVATE_KEY` and a namespace named
    `my_namespace`.

    If you come from the [Registering a Root Namespace](../namespaces/register-root-namespace.md) tutorial, set the
    `SIGNER_PRIVATE_KEY` and `NAMESPACE` environment variables to match the account and namespace you created there,
    or any other namespace that the signer owns.

### Defining the Mosaic

{{ tutorial.code_snippet_tagged('step-4') }}

The mosaic definition describes the asset itself, separately from the transaction that registers it:

* {{ tutorial.var('owner_public_key') }}: The <public key:> of the account creating the mosaic, which must match
    {{ tutorial.var('signer_public_key') }}.
    The network rejects transactions where the two differ.

* {{ tutorial.var('id') }}: The mosaic identifier, formed from the namespace and the mosaic name.

* {{ tutorial.var('description') }}: Text [describing](../../textbook/mosaics.md#description) the mosaic.

* {{ tutorial.var('properties') }}: A set of key-value pairs that configure the mosaic behavior:

    * {{ tutorial.var('divisibility') }}: The number of decimal places the mosaic supports.
        For example, a value of `2` means each whole unit can be divided into 100 (10^2^) atomic units.
        See [Divisibility](../../textbook/mosaics.md#divisibility) in the Textbook.
    * {{ tutorial.var('initialSupply') }}: The number of whole units minted to the creator when the mosaic is
        defined.
        See [Initial Supply](../../textbook/mosaics.md#initial-supply) in the Textbook.
    * {{ tutorial.var('supplyMutable') }}: Whether the total supply can be changed after creation.
        See [Supply Mutability](../../textbook/mosaics.md#supply-mutability) in the Textbook.
    * {{ tutorial.var('transferable') }}: Whether the mosaic can be sent between any two accounts other than the
        creator.
        See [Transferability](../../textbook/mosaics.md#transferability) in the Textbook.

    In this example, the mosaic is divisible to two decimal places and starts with a supply of `1000.00` whole
    units.
    Its supply can be changed after creation, and its units can be freely transferred between accounts.

!!! note "Optional levy"

    A mosaic definition can also include an optional [levy](../../textbook/mosaics.md#levy).
    For more information, see the [Creating a Mosaic with a Levy](./mosaic-levy.md) tutorial.

### Building the Mosaic Definition Transaction

{{ tutorial.code_snippet_tagged('step-5') }}

The mosaic definition transaction registers the mosaic on the network, specifying:

* {{ tutorial.var('type') }}: Mosaic definition transactions use the type <ser:MosaicDefinitionTransactionV1>.

* {{ tutorial.var('signer_public_key') }}: The account that signs the transaction and pays the fees, which must be the
    owner of the namespace that will hold the mosaic.
    It becomes the owner of the created mosaic.

* {{ tutorial.var('timestamp') }} and {{ tutorial.var('deadline') }}: The values computed in the network time step.

* {{ tutorial.var('rental_fee_sink') }}: The special account that collects mosaic
    [creation fees](../../textbook/mosaics.md#creation-fee).
    Each network has a fixed sink address:

    * <mainnet:>: `NBMOSAICOD4F54EE5CDMR23CCBGOAM2XSIUX6TRS`
    * <testnet:>: `TBMOSAICOD4F54EE5CDMR23CCBGOAM2XSJBR5OLC`

    The network rejects transactions that send the creation fee to any other address.

* {{ tutorial.var('rental_fee') }}: The creation fee, which is 10 XEM.
    The SDK's <dy:FeeCalculator.calculateMosaicRentalFee> helper returns the required amount.

    The network rejects transactions that pay less than this fee.
    Larger amounts are accepted, but the entire amount is transferred to the sink account.

* {{ tutorial.var('mosaic_definition') }}: The mosaic definition built in the previous step.

{{ tutorial.code_snippet_tagged('step-6') }}

Finally, the transaction fee is calculated with <dy:FeeCalculator.calculateTransactionFee> and attached to the
transaction.
Unlike the creation fee, the transaction fee is paid to the <harvester account:>.
Mosaic definition transactions pay a fixed transaction fee of 0.15 XEM, as shown in the
[fee schedule](../../textbook/transactions.md#fee-schedule).

### Submitting the Mosaic Definition

{{ tutorial.code_snippet_tagged('step-7') }}

The mosaic definition transaction is signed and announced following the same process as in the
[Transfer XEM](../transactions/transfer-xem.md#announcing-the-transaction) tutorial.

{{ tutorial.code_snippet_tagged('step-8') }}

The code then waits for the transaction to be confirmed by polling the <get:/transaction/get> endpoint until the
transaction is included in a block.

### Retrieving the Mosaic

{{ tutorial.code_snippet_tagged('step-9') }}

To verify the mosaic was created successfully, the code retrieves its definition from the <get:/mosaic/definition>
endpoint and displays its properties.

A successful response confirms the mosaic exists on the network with the expected properties.

!!! note "Mosaic lifetime"

    A mosaic has no duration of its own and becomes inactive when its parent namespace expires.
    [Extending the root namespace](../namespaces/extend-root-namespace.md) keeps its mosaics usable.
    See [Lifetime](../../textbook/mosaics.md#lifetime) in the Textbook.

## Output

The output shown below corresponds to a typical run of the program.

```text linenums="1" hl_lines="5 6 7 67 68 69 70"
--8<-- 'devbook/mosaics/create_mosaic.log'
```

Some highlights from the output:

* **Mosaic ID** (line 5): The mosaic is identified by its fully qualified name, combining the namespace
    `my_namespace` and a timestamped mosaic name.
    Search for this name in the [NEM testnet explorer](https://testnet.nem.fyi/) to view the mosaic details.

* **Creation fee and transaction fee** (lines 6-7): The creation fee is 10 XEM, while the transaction fee is
    0.15 XEM.

* **Verified properties** (lines 67-70): The mosaic is retrieved from the network, confirming the expected
    divisibility, the initial supply of `1000`, and that the mosaic is both supply mutable and transferable.

## Conclusion

This tutorial showed how to:

| Step                                                                      | Related documentation                                                       |
| ------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| [Define the mosaic](#defining-the-mosaic)                                 | <dy:TransactionFactory.create>, <ser:MosaicDefinitionTransactionV1>         |
| [Calculate the creation fee](#building-the-mosaic-definition-transaction) | <dy:FeeCalculator.calculateMosaicRentalFee>                                 |
| [Retrieve the mosaic](#retrieving-the-mosaic)                             | <get:/mosaic/definition>                                                    |

## Next Steps

Now that you have created a mosaic, you can:

* [Change the mosaic supply](./change-mosaic-supply.md) to mint or burn units if the mosaic was created with a
    [mutable supply](../../textbook/mosaics.md#supply-mutability)
* [Send your mosaic with a transfer transaction](../transactions/transfer-mosaics.md) to distribute it to other
    accounts
* [Get mosaic information](./get-mosaic-info.md) to inspect the properties and supply of any mosaic
* [Modify the mosaic definition](./modify-mosaic-definition.md) to change its properties before distributing
