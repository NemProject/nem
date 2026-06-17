---
title: Query Currency Supply
tutorial_level: beginner
---

# Querying Currency Supply

Exchanges and market data aggregators need accurate supply figures to display market capitalization and token metrics.

NEM exposes the supply of <XEM:>, the native currency, through the REST API.
This tutorial shows how to query the total supply and derive the circulating supply from it.

## Prerequisites

This tutorial uses the [NEM REST API](../reference/rest/nem.md) without requiring an SDK.
You only need a way to make HTTP requests.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/network-currency/query_currency_supply', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set a NEM <mainnet:> node.

!!! note "Why mainnet?"
    Other tutorials typically run against <testnet:> to avoid spending real funds.
    This one is different because it queries fixed mainnet account addresses to calculate the circulating supply, so
    `NODE_URL` must point to a mainnet node.

## Code Explanation

### Fetching the Total Supply

{{ tutorial.code_snippet_tagged('step-1') }}

The total supply of <XEM:> is fixed.
All 8'999'999'999 XEM were created in the <nemesis block:> and no new XEM is ever minted.

This tutorial reads the supply from the API rather than hard-coding it, so the same approach also works for
<mosaics:> whose supply can change.

The code sends a `GET` request to the <get:/mosaic/supply> endpoint, passing the XEM mosaic identifier `nem:xem` as
the `mosaicId` query parameter.

The response is a JSON object with the mosaic identifier and its current `supply`, expressed in
[whole units](../../textbook/mosaics.md#divisibility).

### Fetching the Non-Circulating Supply

{{ tutorial.code_snippet_tagged('step-2') }}

A portion of the total supply is held by accounts that are not part of the open market:

* **Treasury:** A reserve account that holds undistributed XEM.
* **Nemesis:** The account that signed the nemesis block.
    It cannot send transactions after the nemesis block, so any XEM held by this account is effectively burnt.
* **Namespace rental sink:** Collects the fees paid to register <namespaces:>.
* **Mosaic rental sink:** Collects the fees paid to create <mosaics:>.

The code queries each account with the <get:/account/get> endpoint and sums their balances.
Balances are returned in [atomic units](../../textbook/mosaics.md#divisibility).
Because `nem:xem` has a <divisibility:> of 6, each balance is divided by 1'000'000 to match the supply in whole units.

### Deriving the Circulating Supply

{{ tutorial.code_snippet_tagged('step-3') }}

The circulating supply is the total supply minus the non-circulating balances.
This is the amount of XEM that is freely available on the open market.

## Output

The following output shows a typical run querying the currency supply:

```text linenums="1" hl_lines="2 7 8"
--8<-- 'devbook/network-currency/query_currency_supply.log'
```

The output shows the full breakdown of the XEM supply:

* **Total supply** (line 2): All the XEM that exists.
* **Non-circulating supply** (line 7): The sum of the treasury, nemesis, and rental sink balances.
* **Circulating supply** (line 8): The XEM actually available in circulation.

## Conclusion

This tutorial showed how to:

| Step                                                                 | Related documentation |
| -------------------------------------------------------------------- | --------------------- |
| [Fetch total supply](#fetching-the-total-supply)                     | <get:/mosaic/supply>  |
| [Fetch non-circulating supply](#fetching-the-non-circulating-supply) | <get:/account/get>    |

## Next Steps

To check a specific account's XEM balance, see the [Query Account Balance](../accounts/query-balance.md) tutorial.
