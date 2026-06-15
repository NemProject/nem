---
title: Transaction Status
tutorial_level: beginner
---

# Monitoring Transaction Status

After announcing a <transaction:> to the NEM network, it remains unconfirmed until it is included in a <block:>.

Monitoring status changes is essential for building responsive applications that can react to transaction confirmation
or failure.

This tutorial shows how to poll a transaction's status until it is confirmed, how to check whether it is still
waiting in the <unconfirmed pool:>, and how to decide when it will never confirm.

This kind of monitoring typically happens right after announcing a transaction, as shown in the
[Transfer XEM tutorial](./transfer-xem.md), to make sure it gets confirmed.

!!! note "Polling is not recommended for production"

    This tutorial uses polling to check the transaction status for illustration purposes,
    but it is not the recommended approach for production applications.

    [WebSockets](../reference/websockets/index.md) provide a more responsive solution without the overhead of repeated
    API calls.

## Prerequisites

This tutorial uses the [NEM REST API](../reference/rest/nem.md) without requiring an SDK.
You only need a way to make HTTP requests.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/transactions/monitoring_status', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set the NEM API node.
If no value is provided, a default one is used.

The tutorial first defines the following reusable functions:

* {{ tutorial.var('get_confirmation_height()') }}: Checks whether the transaction has been confirmed by <harvesting:>
    and is already part of the blockchain.
* {{ tutorial.var('is_in_unconfirmed_pool()') }}: Reports whether the transaction is waiting to be confirmed in the
    <unconfirmed pool:>.
* {{ tutorial.var('wait_for_confirmation()') }}: Repeats the confirmation check until the transaction is confirmed or
    the attempts run out.

The tutorial then calls them together to monitor the transaction,
as shown in [Putting It All Together](#putting-it-all-together).

## Code Explanation

### Finding the Transaction Hash, Address, and Signature

{{ tutorial.code_snippet_tagged('step-1') }}

To monitor a transaction, you need its hash, which is generated after signing.
The hash uniquely identifies the transaction on the NEM network.

The snippet also reads a **signer's address** and the **transaction signature**.
Neither is needed to detect confirmation, but both are used to check whether a transaction is still waiting in the
<unconfirmed pool:>.

The snippet uses sample values.
Set `TRANSACTION_HASH`, `SIGNER_ADDRESS`, and `TRANSACTION_SIGNATURE` environment variables to override them.
All three values are produced when signing a transaction, as shown in the [Transfer XEM tutorial](./transfer-xem.md).

### Checking for Confirmation

{{ tutorial.code_snippet_tagged('step-2') }}

The {{ tutorial.var('get_confirmation_height') }} function checks whether the transaction is confirmed by querying
<get:/transaction/get> with the transaction hash.

When a transaction has been included in a block, this endpoint returns its contents together with the block height
in `meta.height`, which the function returns.

Otherwise, the endpoint responds with HTTP `400` ("Hash was not found in cache") and the function returns no height,
meaning the transaction is not confirmed.
A transaction that is not confirmed may still be waiting in the <unconfirmed pool:>, which the next function inspects.

!!! warning "Hash lookup is short-lived"

    <get:/transaction/get> reads from a cache with a default retention of 36 hours.
    The lookup is enabled by default, but node operators can disable it or change the retention.

    Querying for a transaction hash older than the retention period returns an HTTP `400` error, even if the
    transaction is in fact confirmed.

    Therefore, when announcing a transaction that might need to be looked up later than this retention period,
    store its confirmation block height along with its hash.
    In this way, the transaction can be directly retrieved from the block via the <post:/block/at/public> endpoint.

    Otherwise, the transaction needs to be located by paging through the signer's full history with
    <get:/account/transfers/all>, or by searching the blockchain block by block.

### Inspecting the Unconfirmed Pool

{{ tutorial.code_snippet_tagged('step-3') }}

{{ tutorial.var('is_in_unconfirmed_pool') }} queries <get:/account/unconfirmedTransactions> for the signer's address and
reports whether the monitored transaction is among the pending list.

!!! note "Invalid transactions never enter the pool"

    A transaction that fails [validation](../../textbook/transactions.md#3-validation) does not reach the unconfirmed
    pool: the receiving <node:> rejects it immediately when announced, as shown in the
    [Transfer XEM tutorial](./transfer-xem.md#announcing-the-transaction).

The above endpoint response omits each entry's hash, so the function matches by **signature** instead.
A transaction's signature is unique and appears under `transaction.signature` in every pool entry.
For multisig transactions, this is the signature of the announced wrapper, not of the inner transaction.

The function returns:

* `True`: the transaction is still in the unconfirmed pool, waiting to be included in a block.
* `False`: the transaction is not in the response.
    Possible causes include: it has not arrived at this node yet, it has already been confirmed,
    it was dropped from the pool, or it was left out of the response.

    !!! warning "The response is limited to 25 transactions"

        The endpoint returns at most the 25 most recent transactions involving the address.
        Incoming transactions count toward this limit too, so on a busy account the monitored transaction can be missing
        from the response while it is still in the unconfirmed pool.

### Waiting for Confirmation

{{ tutorial.code_snippet_tagged('step-4') }}

The {{ tutorial.var('wait_for_confirmation') }} function calls {{ tutorial.var('get_confirmation_height') }} every
second until the transaction is confirmed, or two minutes ellapse (configurable timeout).

The function returns `True` as soon as a check reports a confirmation.
When the attempts run out, it returns `False` instead.
This means the transaction was not confirmed within the polling window, not that it failed, but this is a rare case.

### Putting It All Together

{{ tutorial.code_snippet_tagged('step-5') }}

The snippet starts with a call to {{ tutorial.var('get_confirmation_height') }} to check if the transaction is already
confirmed.

!!! warning "Confirmed transactions can still be reversed"

    A confirmed transaction has been included in a block but is not yet irreversible.
    Until enough subsequent blocks are added to surpass the <rewrite limit:>, <rollbacks:> are still possible.

If the transaction is not already part of a block, {{ tutorial.var('is_in_unconfirmed_pool') }} looks for it in the
unconfirmed pool.
A transaction that is neither confirmed nor in this pool is reported as not found.

Only when the transaction is waiting in the unconfirmed pool does the snippet call
{{ tutorial.var('wait_for_confirmation') }} to poll until the transaction is confirmed or the polling window ends.

!!! note "Only rejection or a passed deadline means failure"

    There are only two ways to know a transaction will never confirm: it was rejected when announced, or its
    **deadline** has passed.

    A transaction disappearing from one node's unconfirmed pool does **not** mean it has failed.
    Each node maintains its own pool, and another peer may still hold and eventually confirm the transaction.
    For example, a node may restart with an empty pool or remove older transactions while managing pool capacity.

    Because announcement rejections are returned immediately, the **deadline** provides the final verdict.
    Once <network time:> passes the deadline, the transaction can no longer be included in a block and it is safe
    to announce a replacement transaction.

    Compare the deadline chosen when [building the transaction](./transfer-xem.md#fetching-network-time) against the
    network time returned by <get:/time-sync/network-time>.

## Output

The following output shows a typical run monitoring a freshly-announced transaction:

```text linenums="1" hl_lines="2 4 10 12"
--8<-- 'devbook/transactions/monitoring_status.log'
```

Some highlights from the output:

* **Transaction hash** (line 2): The hash of the transaction to monitor, which uniquely identifies it on the network.

* **Polling start** (line 4): Polling starts because the transaction was not already present in the blockchain and was
    found waiting in the unconfirmed pool.

* **Polling attempts** (lines 5-9): Each attempt reports `pending` while the transaction waits to be included in a
    block.

* **Confirmation** (line 10): A polling attempt finally reports the inclusion in block `652601`.

* **Final outcome** (line 12): The transaction is confirmed and monitoring ends.

The number of attempts and timing vary depending on network conditions and block production rate.

To see the transaction from the network's perspective, visit the [NEM Testnet Explorer](https://testnet.nem.fyi/) and
search for the transaction hash.

## Conclusion

This tutorial showed how to:

| Step                                                                      | Related documentation                   |
| ------------------------------------------------------------------------- | --------------------------------------- |
| [Check for confirmation](#checking-for-confirmation)                      | <get:/transaction/get>                  |
| [Inspect the unconfirmed pool](#inspecting-the-unconfirmed-pool)          | <get:/account/unconfirmedTransactions>  |
| [Wait for confirmation](#waiting-for-confirmation)                        | <get:/transaction/get>                  |
| [Detect when a transaction will never confirm](#putting-it-all-together)  | <get:/time-sync/network-time>           |

## Next steps

For production applications, consider these improvements:

* **Wait past the rewrite limit.** A confirmed transaction can still be rolled back until enough subsequent
    blocks have been added.
    See the <rewrite limit:> for the practical threshold.
* **Query multiple nodes.** Check status across several <nodes:> for greater reliability and protection against
    single-node issues.
* **Use WebSockets:** Replace polling with WebSocket subscriptions for real-time updates without repeated API calls.
    See the [Listening to Transaction Flow](../websockets/listen-transaction-flow.md) WebSocket tutorial.
