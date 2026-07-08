---
title: Get Mosaic Information
tutorial_level: beginner
---

# Getting Mosaic Information

Every <mosaic:> on NEM has a set of on-chain properties such as supply, divisibility, and transfer rules.

This tutorial shows how to retrieve a mosaic's properties and its current supply.

## Prerequisites

This tutorial only reads data from the network. No <account:> is required.

Before you start, make sure to [set up your development environment](../start/setup.md).

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/mosaics/get_mosaic_info', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set the NEM API node.
If no value is provided, a default <testnet:> node is used.

The `MOSAIC_ID` environment variable specifies which mosaic to query, given as its
[fully qualified name](../../textbook/mosaics.md#fully-qualified-name).
If not set, it defaults to the <XEM:> mosaic (`nem:xem`).

## Code Explanation

### Fetching Mosaic Information

{{ tutorial.code_snippet_tagged('step-1') }}

The <get:/mosaic/definition> endpoint retrieves the definition of a mosaic, including:

* **Description:** Text [describing](../../textbook/mosaics.md#description) the mosaic.
* **Creator:** The <public key:> of the account that created the mosaic.
* **Properties:** The mosaic's [behavioral properties](../../textbook/mosaics.md#properties):
    * **[Divisibility](../../textbook/mosaics.md#divisibility):** The number of decimal places the mosaic supports.
        For example, XEM has a divisibility of `6`, meaning 1 XEM equals 1'000'000 atomic units.
    * **[Initial supply](../../textbook/mosaics.md#initial-supply):** The supply at creation time, expressed in
        whole units.
    * **[Supply mutability](../../textbook/mosaics.md#supply-mutability):** Whether the creator can change the
        supply after creation.
    * **[Transferability](../../textbook/mosaics.md#transferability):** Whether the mosaic can be freely sent
        between accounts or only to and from the creator.
* **Levy:** An optional [extra fee](../../textbook/mosaics.md#levy) paid to a third account whenever the mosaic is
    transferred.

### Fetching the Current Supply

{{ tutorial.code_snippet_tagged('step-2') }}

The definition only records the **initial** supply.
For mosaics with mutable supply, the current value can differ, so the <get:/mosaic/supply> endpoint returns the supply
currently in circulation, expressed in [whole units](../../textbook/mosaics.md#divisibility).

### Converting to Atomic Units

{{ tutorial.code_snippet_tagged('step-3') }}

The endpoint reports supply in whole units, but transaction quantities are expressed in
[atomic units](../../textbook/mosaics.md#divisibility).
To convert from whole to atomic units, the code multiplies the supply by 10 raised to the mosaic's divisibility.

For XEM (divisibility `6`), a supply of `8'999'999'999` whole units equals `8'999'999'999'000'000'` atomic units.

## Output

The output shown below corresponds to a typical run of the program, querying the XEM mosaic on testnet.

```text linenums="1" hl_lines="5 6 7 8 9 10 11 12 15 17"
--8<-- 'devbook/mosaics/get_mosaic_info.log'
```

Some highlights from the output:

* **Mosaic ID** (line 5): The XEM mosaic identifier, the fully qualified name `nem:xem`.

* **Description** (line 6): Text that describes the mosaic, set by its creator.

* **Creator** (line 7): The public key of the account that created the mosaic.

* **Divisibility** (line 8): The value `6` means 1 XEM = 1,000,000 (10^6^) atomic units.

* **Initial supply** (line 9): The supply at creation time, in whole units.

* **Supply mutable** (line 10): The value `false` means the XEM supply can never change.

* **Transferable** (line 11): The value `true` means XEM can be freely sent between accounts.

* **Levy** (line 12): XEM transfers carry no additional mosaic fee.

* **Current supply** (line 15): The supply currently in circulation, identical to the initial supply because XEM
    is not mutable.

* **Supply in atomic units** (line 17): The supply converted from whole units to atomic units using the mosaic's
    divisibility.

## Conclusion

This tutorial showed how to:

| Step                                                           | Related documentation    |
| -------------------------------------------------------------- | ------------------------ |
| [Fetch the mosaic definition](#fetching-mosaic-information)    | <get:/mosaic/definition> |
| [Fetch the current supply](#fetching-the-current-supply)       | <get:/mosaic/supply>     |

## Next Steps

* [Transfer mosaics](../transactions/transfer-mosaics.md) to send a mosaic between accounts
* [Query an account balance](../accounts/query-balance.md) to see how much of a mosaic an account holds
