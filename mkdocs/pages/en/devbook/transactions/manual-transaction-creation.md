---
title: Manual Transaction Creation
tutorial_level: intermediate
---

# Creating Transactions Manually

Most tutorials create transactions from descriptors through the facade.
This is the recommended approach: typed descriptors provide compile-time or type-checking support and better editor
assistance in languages that support them, and the facade fills the signer, timestamp, and deadline from a duration.

For completeness, this tutorial shows the lower-level alternative: creating a transaction manually with the
transaction factory.

The example mirrors the [Transfer XEM](./transfer-xem.md) tutorial, but replaces descriptor-based creation with manual
field assignment. It explicitly fetches network time, assigns the timestamp and absolute deadline, and calculates the
NEM transaction fee.

## Prerequisites

Before you start, make sure to:

* [Set Up your Development Environment](../start/setup.md).
* Create an <account:> to send the transfer transaction, either
    [from code](../accounts/create-from-private-key.md) or
    [by using a wallet](../../userbook/wallet/create-account.md).
* Obtain <XEM:> to pay for the transaction fee and transfer amount.
    See [Getting Testnet Funds from the Faucet](../accounts/testnet-faucet.md).

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/transactions/manual_transaction_creation', ['py', 'js']) }}

## Code Explanation

### Setting Up the Accounts

{{ tutorial.code_snippet_tagged('step-1') }}

The signer and recipient are configured in the same way as in the
[Transfer XEM](./transfer-xem.md#setting-up-the-accounts) tutorial.

### Defining the Transfer Amount

{{ tutorial.code_snippet_tagged('step-2') }}

The amount is expressed in atomic units. XEM has a <divisibility:> of 6, so the default transfer of 1 XEM is
`1_000_000` atomic units.

### Fetching Network Time

{{ tutorial.code_snippet_tagged('step-3') }}

Manual transaction creation requires an explicit `timestamp` and an absolute `deadline`, both expressed as the number
of seconds since the NEM <nemesis block:>.

The snippet fetches the current network time from <get:/time-sync/network-time>.
The endpoint returns milliseconds, so the value is divided by 1000 before it is wrapped in <dy:NetworkTimestamp>.
The deadline is set two hours after the timestamp.

Descriptor-based facade creation instead accepts a deadline duration in seconds and derives both values using the local
clock, so it does not require this request.

### Building the Transaction

{{ tutorial.code_snippet_tagged('step-4') }}

The transaction is created with <dy:TransactionFactory.create>, which accepts a plain descriptor dictionary or object.
Unlike the facade creation method, this lower-level factory does not fill common transaction fields.

The descriptor contains:

* {{ tutorial.var('type') }}: <ser:TransferTransactionV2>, the current transfer transaction version.
* {{ tutorial.var('signer_public_key') }}: The account that signs the transaction, pays the fee, and sends the XEM.
* {{ tutorial.var('timestamp') }}: The current network-time timestamp fetched in the previous step.
* {{ tutorial.var('deadline') }}: The absolute network-time deadline, two hours after the timestamp.
* {{ tutorial.var('recipient_address') }}: The address that receives the XEM.
* {{ tutorial.var('amount') }}: The amount to transfer in atomic units.

### Calculating the Transaction Fee

{{ tutorial.code_snippet_tagged('step-5') }}

The low-level factory does not calculate NEM's fixed-schedule fee.
The snippet calls <dy:FeeCalculator.calculateTransactionFee> after construction and assigns the result to
`transaction.fee` before signing.

### Signing and Serializing

{{ tutorial.code_snippet_tagged('step-6') }}

The transaction is signed with <dy:NemFacade.signTransaction>.
The signature is attached with <dy:TransactionFactory.attachSignature>, producing a JSON payload ready for announcement.

### Announcing the Transaction

{{ tutorial.code_snippet_tagged('step-7') }}

The signed payload is submitted to <post:/transaction/announce>.
A `SUCCESS` response means the transaction entered the unconfirmed pool.

### Waiting for Confirmation

{{ tutorial.code_snippet_tagged('step-8') }}

The transaction is polled through <get:/transaction/get> until it is included in a block or the two-minute retry limit
is reached.

## Output

```text linenums="1" hl_lines="2 3 4 11 14 20 21"
--8<-- 'devbook/transactions/manual_transaction_creation.log'
```

The first two highlighted lines show the explicit network-time request.
The output also shows the calculated fee, the manually assigned signer and absolute deadline, the announcement result,
and the transaction hash used for confirmation.

## Conclusion

This tutorial showed how to create a transaction manually:

| Step                                                  | Related documentation                                                      |
| ----------------------------------------------------- | -------------------------------------------------------------------------- |
| [Fetch network time](#fetching-network-time)          | <get:/time-sync/network-time>, <dy:NetworkTimestamp>                       |
| [Build the transaction](#building-the-transaction)    | <dy:TransactionFactory.create>, <ser:TransferTransactionV2>                |
| [Calculate the fee](#calculating-the-transaction-fee) | <dy:FeeCalculator.calculateTransactionFee>                                 |
| [Sign and serialize](#signing-and-serializing)        | <dy:NemFacade.signTransaction><br/><dy:TransactionFactory.attachSignature> |
| [Announce and confirm](#announcing-the-transaction)   | <post:/transaction/announce><br/><get:/transaction/get>                    |
