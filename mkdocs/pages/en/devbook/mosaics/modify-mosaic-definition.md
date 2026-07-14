---
title: Modify Mosaic Definition
tutorial_level: intermediate
---

# Modifying a Mosaic Definition

After a <mosaic:> is created, its creator can modify some of its properties by sending another mosaic definition
transaction that uses the same mosaic identifier.
Instead of creating a new mosaic, the network updates the existing one.

This tutorial shows how to modify an existing mosaic definition.
To change the mosaic's supply instead, see [Changing Mosaic Supply](./change-mosaic-supply.md).

## Prerequisites

Before you start, make sure to:

* Create a <mosaic:> with the <account:> that will sign the modification.
    Only the creator can modify a mosaic.
    See the [Creating a Mosaic](./create-mosaic.md) tutorial.
* Keep the <namespace:> that holds the mosaic active.
    See [Lifetime](../../textbook/mosaics.md#lifetime) in the Textbook.
* Obtain <XEM:> to pay for the transaction and creation fees.
    See [Getting Testnet Funds from the Faucet](../accounts/testnet-faucet.md).

## What Can Be Changed

The [description](../../textbook/mosaics.md#description) can be changed at any time.

The [transferability](../../textbook/mosaics.md#transferability) and the [name](../../textbook/mosaics.md#name) can
never be changed.

Changing any other property requires the creator to still own the entire mosaic supply.
In practice, most mosaic definitions can only be modified before the mosaic is distributed.

For the complete rules, see [Modifying a Mosaic](../../textbook/mosaics.md#modifying-a-mosaic) in the Textbook.

!!! tip "Consider creating a new mosaic instead"

    [Creating a new mosaic](./create-mosaic.md) is easier than modifying an existing one.
    Understanding how modification works is still important to know when a mosaic's definition can no longer be edited.

## Procedure

To modify a mosaic definition, reuse the steps from the [Creating a Mosaic](./create-mosaic.md) tutorial:

1. Retrieve the current definition from <get:/mosaic/definition>, as described in
    [Retrieving the Mosaic](./create-mosaic.md#retrieving-the-mosaic).

2. Build a new <ser:MosaicDefinitionTransactionV1>, as described in
    [Building the Mosaic Definition Transaction](./create-mosaic.md#building-the-mosaic-definition-transaction),
    using the **same mosaic identifier**.

    The transaction must include the complete mosaic definition.
    Resend the definition retrieved in the previous step, changing only the values that should be updated.

    !!! warning "Resend all properties"

        Properties omitted from the transaction are reset to their default values:

        * `divisibility`: `0`
        * `initialSupply`: `1000`
        * `supplyMutable`: `false`
        * `transferable`: `true`

        An omitted [levy](../../textbook/mosaics.md#levy) is removed from the mosaic.

3. Submit the transaction, as described in
    [Submitting the Mosaic Definition](./create-mosaic.md#submitting-the-mosaic-definition).

4. Retrieve the definition again to confirm that the mosaic holds the updated values.

The transaction pays the full [creation fee](../../textbook/mosaics.md#creation-fee) of 10 XEM, the same amount as
the transaction that created the mosaic, in addition to the regular transaction fee.
For example, changing only the description costs the same as creating a new mosaic.

## Outcome

If only the description changes, the network preserves the existing supply and the balances of all accounts that hold
the mosaic.

If any other property changes, the network rebuilds the mosaic from the new definition.
The network resets the total supply to `initialSupply` and assigns the entire supply to the creator account.

Because of this behavior, the creator must own the entire supply before changing any property other than the
description.
