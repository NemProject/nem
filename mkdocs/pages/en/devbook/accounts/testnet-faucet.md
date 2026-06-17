---
title: Fund via Faucet
tutorial_level: beginner
---

# Getting Testnet Funds from the Faucet

The NEM <testnet:> provides a faucet that distributes free <XEM:> to developer <accounts:> for testing purposes.
This guide explains how to claim testnet funds using the web-based faucet.

!!! note
    Testnet XEM has no real-world value.
    It exists only to let you experiment with NEM features without using real currency.

    If you need <mainnet:> XEM, you will need to buy it through an
    [exchange](https://coinmarketcap.com/currencies/nem/#Markets).

## Prerequisites

Before you start, make sure to:

* Create a testnet <account:> to receive funds, either
  [from code](../accounts/create-from-private-key.md) or
  [by using a wallet](../../userbook/wallet/create-account.md).
* Have an 𝕏 account to verify your identity with the faucet.

## How to Claim Testnet Funds

{% import 'tutorial.jinja2' as tutorial %}

{{ tutorial.list_begin() }}

{{ tutorial.step_begin("faucet-open.jpg") }}
Open your web browser and navigate to the NEM testnet faucet at [testnet.nem.tools](https://testnet.nem.tools).
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-sign-in.jpg") }}
Click **Sign in with Twitter** (now 𝕏) and follow the authentication flow.

This step limits the amount of test funds to 10,000 XEM per account, to help prevent abuse of the faucet.
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-authorize.jpg") }}
After signing in, 𝕏 will ask you to authorize the faucet application to access your account information.

Review the permissions and click **Authorize app** to continue.
Once authorized, you will be redirected again to the faucet.
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-address.jpg") }}
Enter the address where you want to receive the funds in the **Your Testnet Address** field.

Make sure the address starts with `T`, meaning it is a testnet account.
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-xem.jpg") }}
In the **XEM Amount** field, specify how much XEM you want to claim.
The maximum amount per request is 10,000 XEM.
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-claim.jpg") }}
Click **Claim** to submit your request.
If the request is successful, the faucet will transfer the specified amount of XEM to your address.
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-view-explorer.jpg") }}
Click **View in Explorer** in the top-right corner notification to verify that the transaction was processed.

The explorer will display the transaction details, including its confirmation status.
The transaction should confirm in less than a minute under normal network conditions.

You can also monitor the transfer from your <wallet:> if you have one set up.
{{ tutorial.step_end() }}

{{ tutorial.list_end() }}

## Returning Funds to the Faucet

When you are done testing, consider returning unused XEM back to the faucet.
The faucet address is the same address that sent you the funds.

You can find the sender address by checking the transaction in the [blockchain explorer](https://testnet.nem.fyi/)
or by searching your account transaction history.

Better yet, use the faucet address as the recipient for your test transactions.
This way, you practice sending transactions while helping keep the faucet stocked for other developers.

## Next Steps

Why not try [sending a transfer transaction](../transactions/transfer-xem.md)?
