---
title: Change Mosaic Supply
tutorial_level: intermediate
---

# Changing Mosaic Supply

<mosaics:|Mosaics> created with a [mutable supply](../../textbook/mosaics.md#supply-mutability) can have their total
supply increased or decreased after creation.

This tutorial shows how to change a mosaic's supply by minting and burning units.

## Prerequisites

Before you start, make sure to:

* Set up your development environment.
    See [Setting Up a Development Environment](../start/setup.md).
* Create a mosaic with a [mutable supply](../../textbook/mosaics.md#supply-mutability),
    using the <account:> that will sign the supply changes.
    See the [Creating a Mosaic](./create-mosaic.md) tutorial.
* Keep the <namespace:> that holds the mosaic active.
    See [Lifetime](../../textbook/mosaics.md#lifetime) in the Textbook.
* Obtain <XEM:> to pay for the transaction fee.
    See [Getting Testnet Funds from the Faucet](../accounts/testnet-faucet.md).

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/mosaics/change_mosaic_supply', ['py', 'js']) }}

## Code Explanation

Changing a mosaic's supply uses the <ser:MosaicSupplyChangeTransactionV1> transaction.
This tutorial announces two of them: one to mint new units and one to burn them.

Because both transactions are submitted the same way, the snippet defines two helpers,
{{ tutorial.var('announce_transaction') }} and {{ tutorial.var('wait_for_confirmation') }}, which announce a transaction
and then poll the network until it is included in a block.

A third helper, {{ tutorial.var('fetch_supply') }}, reads the mosaic's current supply from <get:/mosaic/supply>, so
that the effect of each transaction can be observed.

### Setting Up the Account

{{ tutorial.code_snippet_tagged('step-1') }}

The snippet reads the signer's private key from the `SIGNER_PRIVATE_KEY` environment variable, which defaults to a test
key if not set.
The signer must be the creator of the mosaic.

The mosaic to update is read from the `NAMESPACE` and `MOSAIC` environment variables, which default to
`my_namespace:token`.

### Fetching Network Time

{{ tutorial.code_snippet_tagged('step-2') }}

Network time is fetched from <get:/time-sync/network-time>, and the transaction's `timestamp` and `deadline` fields
are derived from it, following the process described in the [Transfer XEM](../transactions/transfer-xem.md) tutorial.

### Increasing Supply (Minting)

{{ tutorial.code_snippet_tagged('step-3') }}

The snippet first reads the mosaic's supply, so that the newly minted units can be seen once the transaction is
confirmed.

To mint new units, the transaction sets:

* {{ tutorial.var('type') }}: Mosaic supply change transactions use the type <ser:MosaicSupplyChangeTransactionV1>.

* {{ tutorial.var('signer_public_key') }}: The account that signs the transaction and pays the fees.
    It must be the creator of the mosaic.

* {{ tutorial.var('timestamp') }} and {{ tutorial.var('deadline') }}: The values computed in the network time step.

* {{ tutorial.var('mosaic_id') }}: The [fully qualified name](../../textbook/mosaics.md#fully-qualified-name) of the
    mosaic to update.

* {{ tutorial.var('action') }}: The value `increase` mints new units.

* {{ tutorial.var('delta') }}: The number of
    [whole units](../../textbook/mosaics.md#divisibility) to add.
    The resulting total supply cannot exceed the [maximum supply](../../textbook/mosaics.md#initial-supply).

The transaction fee is then calculated and the transaction is signed, announced, and confirmed, following the same
process as in the [Transfer XEM](../transactions/transfer-xem.md) tutorial.

Once confirmed, the supply is read again to show the resulting supply.

### Decreasing Supply (Burning)

{{ tutorial.code_snippet_tagged('step-4') }}

To burn existing units, the same transaction type is used with {{ tutorial.var('action') }} set to `decrease` and
{{ tutorial.var('delta') }} set to the number of whole units to remove.

If the creator does not hold enough units, the transaction fails with a validation error.

Once confirmed, the supply is read again to show the burned units.
Because this tutorial increases and then decreases the supply by the same amount, the final supply matches the value
before the changes.

## Output

The output shown below corresponds to a typical run of the program.

```text linenums="1" hl_lines="8 25-26 35 54-55 64"
--8<-- 'devbook/mosaics/change_mosaic_supply.log'
```

Some highlights from the output:

* **Supply before minting** (line 8): The mosaic starts with a supply of `1000` whole units.

* **Supply increase** (lines 25-26): The `increase` action with a delta of `500` mints new units into the creator's
    balance.

* **Supply after minting** (line 35): The supply rises to `1500` whole units.

* **Supply decrease** (lines 54-55): The `decrease` action with the same delta burns those units.

* **Supply after burning** (line 64): The supply returns to `1000`, because the increase and decrease cancel out.

## Conclusion

This tutorial showed how to:

| Step                                                          | Related documentation                      |
| ------------------------------------------------------------- | ------------------------------------------ |
| [Mint mosaic supply](#increasing-supply-minting)              | <dy:TransactionFactory.create>             |
| [Burn mosaic supply](#decreasing-supply-burning)              | <dy:TransactionFactory.create>             |
| [Calculate the transaction fee](#increasing-supply-minting)   | <dy:FeeCalculator.calculateTransactionFee> |
| [Read the mosaic supply](#increasing-supply-minting)          | <get:/mosaic/supply>                       |

## Next Steps

Now that you can change a mosaic's supply, you can:

* [Send your mosaic with a transfer transaction](../transactions/transfer-mosaics.md) to distribute it to other
    accounts
* [Get mosaic information](./get-mosaic-info.md) to inspect the properties and supply of any mosaic
