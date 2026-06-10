---
title: Transaction Status
tutorial_level: beginner
---

# Monitoring Transaction Status

When a <transaction:> is announced to the NEM network and accepted, it enters the <unconfirmed pool:>, where it remains
until it is included in a <block:>.
An invalid transaction never gets that far: the receiving <node:> rejects it immediately when announced, as shown in the
[Transfer XEM tutorial](./transfer-xem.md#announcing-the-transaction).

Acceptance is not confirmation, though.
The network exposes the transaction's progress but does not push it to the sender, so applications that need to react to
confirmation or failure must check for status changes themselves.

This tutorial shows how to monitor a transaction's status until it is confirmed, how to check whether a transaction that
has not yet confirmed is still in the unconfirmed pool, and how to decide when a transaction will never confirm.
Along the way, you will gain a practical understanding of the
[transaction lifecycle](../../textbook/transactions.md#transaction-lifecycle).

!!! note "Confirmed transactions can still be reversed"
    A confirmed transaction has been included in a block but is not yet irreversible.
    Until enough subsequent blocks are added to surpass the <rewrite limit:>, <rollbacks:> are still possible.

## Prerequisites

This tutorial uses the [NEM REST API](../reference/rest/nem.md) without requiring an SDK.
You only need a way to make HTTP requests.

## Full Code

This tutorial uses polling to check the transaction status.
Polling is used here for illustration purposes, but it is not the recommended approach for production applications.

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/transactions/monitoring_status', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set the NEM API node.
If no value is provided, a default one is used.

## Code Explanation

### Finding the Transaction Hash, Address, and Signature

{{ tutorial.code_snippet_tagged('step-1') }}

To monitor a transaction, you need its hash, which is generated after signing.
The hash uniquely identifies the transaction on the NEM network.

The snippet also reads a **signer's address** and the **transaction signature**.
Neither is needed to detect confirmation, but both are used to diagnose a transaction that does not confirm within
the polling window.

The snippet uses sample values.
Set `TRANSACTION_HASH`, `SIGNER_ADDRESS`, and `TRANSACTION_SIGNATURE` environment variables to override them.
All three values are produced when signing a transaction, as shown in the [Transfer XEM tutorial](./transfer-xem.md).

### Polling for Confirmation

{{ tutorial.code_snippet_tagged('step-2') }}

The {{ tutorial.var('wait_for_transaction_confirmation') }} function polls a transaction until it is confirmed.
It queries <get:/transaction/get> with the transaction hash up to 120 times by default, sleeping 1 second between
attempts (about two minutes in total), so monitoring eventually stops even if the transaction never confirms.

When the transaction has been included in a block, the endpoint returns it together with the block height under
`meta.height`, and the function returns successfully.

Otherwise, the endpoint responds with HTTP `400` ("Hash was not found in cache") and the function logs the attempt as
`pending` and continues polling.

!!! warning "Hash lookup is short-lived"

    <get:/transaction/get> reads from an in-memory cache with a default retention of 36 hours.
    The lookup is enabled by default, but node operators can disable it or change the retention.
    After the retention period, a confirmed transaction returns the same HTTP `400` error.

    For long-term lookups, store `meta.height` on confirmation and fetch the block by height via
    <post:/block/at/public>.

If the loop ends without a confirmation, the function returns `False`.
This means the transaction was not confirmed within the polling window, not that it failed, and the next function
answers whether it is still in the <unconfirmed pool:>.

### Inspecting the Unconfirmed Pool

{{ tutorial.code_snippet_tagged('step-3') }}

{{ tutorial.var('is_in_unconfirmed_pool') }} queries <get:/account/unconfirmedTransactions> for the signer's address and
reports whether the monitored transaction is among them.

The endpoint response omits each entry's hash, so the function matches by **signature** instead.
A transaction's signature is unique and appears under `transaction.signature` in every pool entry.
For multisig transactions, this is the signature of the announced wrapper, not of the inner transaction.

The function returns:

* `True`: the transaction is still in the unconfirmed pool, waiting for a block.
* `False`: the transaction is not in the response, because it has not arrived at this node yet, because it was dropped
    from the pool, or because it was pushed out by the entry cap.

    !!! warning "The pool view is capped at 25 entries"

        The endpoint returns at most the 25 most recent entries involving the address.
        Incoming transactions count toward this cap too, so on a busy account the monitored transaction can drop out of
        the response while it is still in the pool.

??? info "Alternative: hash-based matching"

    Each pool entry can be rebuilt using <dy:TransactionFactory.create> and hashed with <dy:NemFacade.hashTransaction>
    to compare against the monitored hash.
    However, rebuilding requires mapping the REST fields to a typed descriptor for every transaction type, which is
    why the snippet matches by signature instead.

### Putting Both Functions Together

{{ tutorial.code_snippet_tagged('step-4') }}

The snippet combines both functions: when {{ tutorial.var('wait_for_transaction_confirmation') }} ends without a
confirmation, {{ tutorial.var('is_in_unconfirmed_pool') }} diagnoses whether the transaction is still in the unconfirmed
pool.

!!! note "Detecting rejected transactions"

    A node keeps no record of dropped transactions, and other nodes may still hold the transaction as unconfirmed,
    so a missing transaction is not necessarily a failed one.

    The transaction's **deadline** gives the definitive verdict.
    Blocks cannot include a transaction whose deadline has passed, so once the current <network time:> moves past
    the deadline, the transaction will never confirm.

    To decide whether to keep waiting or to announce a new transaction, compare the deadline chosen when
    [building the transaction](./transfer-xem.md#fetching-network-time) against the network time from
    <get:/time-sync/network-time>.

## Output

The following output shows a typical run monitoring a freshly-announced transaction:

```text
--8<-- 'devbook/transactions/monitoring_status.log'
```

The output shows:

1. The transaction hash being monitored.
2. Several polling attempts reporting `pending` while the transaction is being processed.
3. The successful inclusion in a block on a later attempt.
4. A success message when the transaction is confirmed.

The number of attempts and timing vary depending on network conditions and block production rate.

To see the transaction from the network's perspective, visit the [NEM Testnet Explorer](https://testnet.nem.fyi/) and
search for the transaction hash.

## Conclusion

This tutorial showed how to:

| Step                                                                             | Related documentation                  |
| -------------------------------------------------------------------------------- | -------------------------------------- |
| [Poll for confirmation](#polling-for-confirmation)                               | <get:/transaction/get>                 |
| [Inspect the unconfirmed pool](#inspecting-the-unconfirmed-pool)                 | <get:/account/unconfirmedTransactions> |
| [Detect when a transaction will never confirm](#putting-both-functions-together) | <get:/time-sync/network-time>          |

## Next steps

For production applications, consider these improvements:

* **Re-check confirmation after a negative pool result.** The transaction may confirm in the gap between the
    polling timeout and the pool check, so query <get:/transaction/get> once more before reacting.
* **Wait past the rewrite limit.** A confirmed transaction can still be rolled back until enough subsequent
    blocks have been added.
    See the <rewrite limit:> for the practical threshold.
* **Query multiple nodes.** Check status across several <nodes:> for greater reliability and protection against
    single-node issues.
