---
title: Query Account Balance
tutorial_level: beginner
---

# Querying an Account Balance

<Accounts:|Accounts> on NEM can hold <mosaics:> (fungible tokens), including the native currency <XEM:>.

This tutorial shows how to query an account's mosaic balances and display them with the appropriate number of decimal
places.

## Prerequisites

This tutorial uses the [NEM REST API](../reference/rest/nem.md) without requiring an SDK.
You only need a way to make HTTP requests.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/accounts/query_balance', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set the NEM API node.
If no value is provided, a default one is used.

The tutorial defines the following functions:

* {{ tutorial.var('get_mosaic_balances()') }}: Fetches all <mosaics:> owned by an account.
* {{ tutorial.var('get_mosaic_definitions()') }}: Fetches mosaic definitions, including <divisibility:>.
* {{ tutorial.var('format_amount()') }}: Formats amounts with the appropriate number of decimal places, according to
    their <divisibility:>.

## Code Explanation

### Fetching Mosaic Balances

{{ tutorial.code_snippet_tagged('step-2') }}

The <get:/account/mosaic/owned> endpoint returns every mosaic the account holds, together with its quantity.

### Fetching Mosaic Definitions

{{ tutorial.code_snippet_tagged('step-3') }}

To format mosaic balances correctly, the snippet fetches their definitions from the network.
The key property required is <divisibility:>, which defines how many decimal places a mosaic supports.

The <get:/account/mosaic/owned/definition> endpoint returns the definition for every mosaic owned by the account in a
single request, including divisibility and other properties.

### Formatting Amounts

{{ tutorial.code_snippet_tagged('step-4') }}

This utility function converts _atomic_ amounts into human-friendly representations:

* **Atomic amount:** The raw value stored on the blockchain, expressed as an integer.
* **Formatted amount:** The display format with decimal places determined by the mosaic's divisibility.

The formatting splits the atomic amount into whole and fractional parts by dividing and taking the remainder with
respect to \(10^{\text{divisibility}}\).
The fractional part is then zero-padded to ensure it always displays the correct number of decimal places.

### Putting It All Together

{{ tutorial.code_snippet_tagged('step-5') }}

The main code reads the `ADDRESS` environment variable to determine which account to query.
If no value is provided, it uses a default sample address.

It orchestrates the helper functions to:

1. Fetch the mosaic balances for the account.
2. Retrieve the mosaic definitions to determine each mosaic's divisibility.
3. Iterate through each mosaic and format its balance with the appropriate number of decimal places.

## Output

The output shown below corresponds to a typical run of the program.

```text
--8<-- 'devbook/accounts/query_balance.log'
```

The output displays all mosaics the account holds. Notice how different mosaics have different divisibility values:

* The first mosaic is `nem:xem`, the network's native currency, which has divisibility 6 and is therefore displayed with
    six decimal places (`9883.200000`).
* The second mosaic is `company:token`, a user-defined mosaic with divisibility 0, displayed as an integer (`1000000`).

## Conclusion

This tutorial showed how to:

| Step                                                     | Related documentation                                         |
| -------------------------------------------------------- | ------------------------------------------------------------- |
| [Fetch mosaic balances](#fetching-mosaic-balances)       | <get:/account/mosaic/owned>                                   |
| [Fetch mosaic definitions](#fetching-mosaic-definitions) | <get:/account/mosaic/owned/definition>                        |
