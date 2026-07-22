---
title: Create Mosaic with Levy
tutorial_level: advanced
---

# Creating a Mosaic with a Levy

A <mosaic:> can include an optional <levy:>, a fee charged to the sender on every transfer and credited to a designated
account.

A typical use of levies is funding the account behind an asset, for example by charging a commission or a royalty on
every transfer.

This tutorial shows how to create a mosaic with a levy.

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

Additionally, review the [Creating a Mosaic](./create-mosaic.md) tutorial to understand how a mosaic definition is
built, announced, and confirmed.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/mosaics/mosaic_levy', ['py', 'js']) }}

## Code Explanation

### Setting Up the Account and the Mosaic

{{ tutorial.code_snippet_tagged('step-1') }}

The snippet reads the signer's private key from the `SIGNER_PRIVATE_KEY` environment variable, which defaults to a
test key if not set.
This account signs the transaction and becomes the owner of the mosaic, so it must also own the namespace that will
hold it.
The mosaic identifier is assembled from that namespace and a mosaic name.
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

{{ tutorial.code_snippet_tagged('step-2') }}

Network time is fetched from <get:/time-sync/network-time>, and the transaction's `timestamp` and `deadline` fields
are derived from it, following the process described in the [Transfer XEM](../transactions/transfer-xem.md) tutorial.

### Describing the Levy

{{ tutorial.code_snippet_tagged('step-3') }}

The levy is a <ser:MosaicLevy> structure with four fields:

* {{ tutorial.var('transfer_fee_type') }}: How the levy amount is calculated:

    * `absolute`: A fixed quantity charged on every transfer, regardless of the amount transferred.
    * `percentile`: A quantity proportional to the amount transferred.

    This tutorial uses an `absolute` levy, so every transfer is charged the same amount.

* {{ tutorial.var('recipient_address') }}: The account credited with the levy on every transfer.
    It can be the mosaic creator or any other account.

* {{ tutorial.var('mosaic_id') }}: The mosaic in which the levy is paid.
    This tutorial charges the levy in `nem:xem`, so senders pay in the network currency.

    The levy can also be paid in the mosaic being defined itself.
    Any other levy mosaic must already exist on the network and be
    [transferable](../../textbook/mosaics.md#transferability).

* {{ tutorial.var('fee') }}: The levy amount.
    For an `absolute` levy, it is expressed in the [atomic units](../../textbook/mosaics.md#divisibility) of the
    levy mosaic.
    Because `nem:xem` has a <divisibility:> of 6, a value of 1'000'000 charges 1 XEM per transfer.

    For a `percentile` levy, the fee is interpreted in basis points instead: a `fee` of `100` charges 1% of the
    amount transferred.
    See the [percentile levy calculation](../../textbook/mosaics.md#percentile-levy-calculation) in the Textbook for
    the full rules.

### Attaching the Levy to the Mosaic Definition

{{ tutorial.code_snippet_tagged('step-4') }}

A levy is part of the mosaic definition, so it is set with the same <ser:MosaicDefinitionTransactionV1> used in
[Creating a Mosaic](./create-mosaic.md#building-the-mosaic-definition-transaction).
This tutorial reuses the same transaction, with the levy added to the {{ tutorial.var('mosaic_definition') }} field.

The [creation fee](../../textbook/mosaics.md#creation-fee) is 10 XEM, and the transaction fee is a fixed 0.15 XEM, as
shown in the [fee schedule](../../textbook/transactions.md#fee-schedule).

### Submitting the Mosaic Definition

{{ tutorial.code_snippet_tagged('step-5') }}

The transaction is then signed, announced, and confirmed following the same process as in the
[Transfer XEM](../transactions/transfer-xem.md#announcing-the-transaction) tutorial.

### Verifying the Levy

{{ tutorial.code_snippet_tagged('step-6') }}

To verify the mosaic with the levy was created, the code retrieves the mosaic definition from the
<get:/mosaic/definition> endpoint, which returns the levy alongside the mosaic properties.

A levy in the response confirms that future transfers of the mosaic will be charged the levy.

## How the Levy Is Charged

After the mosaic is created, the levy applies to every
[transfer](../transactions/transfer-mosaics.md) of the mosaic, and no extra field is needed in the transfer
transaction.

!!! warning "A levy is not a guaranteed charge"

    Levies are not recursive, therefore they can be sidestepped.
    See the [example in the Textbook](../../textbook/mosaics.md#limitations).

The levy is charged on top of the transferred amount, so a sender who transfers 50 units of the mosaic created in this
tutorial is debited:

* 50 units of the mosaic, credited to the recipient of the transfer.
* 1 XEM, credited to the levy recipient.
* The transaction fee, credited to the <harvester account:>.

The network rejects the transfer if the sender cannot cover both the transferred amount and the levy.
Because this levy is paid in `nem:xem`, the sender must also have enough XEM to cover both the levy and the transaction
fee.
If the levy is paid in another mosaic, the sender must also hold a sufficient balance of that mosaic.

## Output

The output shown below corresponds to a typical run of the program.

```text linenums="1" hl_lines="3 6-10 58-68 82-85"
--8<-- 'devbook/mosaics/mosaic_levy.log'
```

Some highlights from the output:

* **Mosaic ID** (line 3): The mosaic is identified by its fully qualified name, combining the namespace
    `my_namespace` and a timestamped mosaic name.
    Search for this name in the [NEM testnet explorer](https://testnet.nem.fyi/) to view the mosaic details.

* **Levy fields** (lines 6-10): The levy to create, an `absolute` fee of 1'000'000 atomic units of `nem:xem` (1 XEM),
    paid to the levy recipient on every transfer.

* **Levy in the transaction** (lines 58-68): The levy is defined inside the mosaic definition.
    The recipient address, the levy mosaic name, and the mosaic name are hex-encoded in the payload, while the fee of
    this `absolute` levy is expressed in atomic units.

* **Verified levy** (lines 82-85): The mosaic is retrieved from the network, confirming the levy type, its recipient,
    the mosaic in which it is paid, and its amount.

## Conclusion

This tutorial showed how to:

| Step                                                                        | Related documentation                                               |
| --------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| [Describe the levy](#describing-the-levy)                                   | <ser:MosaicLevy>                                                    |
| [Attach the levy to a mosaic](#attaching-the-levy-to-the-mosaic-definition) | <dy:TransactionFactory.create>, <ser:MosaicDefinitionTransactionV1> |
| [Verify the levy](#verifying-the-levy)                                      | <get:/mosaic/definition>                                            |

## Next Steps

Now that you have created a mosaic with a levy, you can:

* [Send your mosaic with a transfer transaction](../transactions/transfer-mosaics.md) to see the levy charged to the
    sender.
* [Get mosaic information](./get-mosaic-info.md) to inspect the levy of any mosaic.
