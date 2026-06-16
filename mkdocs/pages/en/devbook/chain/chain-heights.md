---
title: Chain and Irreversible Height
tutorial_level: beginner
---

# Querying Chain and Irreversible Height

The <get:/chain/height> endpoint returns the current chain height.

The **irreversible height** is the highest block that can no longer be rolled back.
On NEM, it is calculated by subtracting the <rewrite limit:> from the current chain height.

This tutorial shows how to poll the chain height in a loop, calculate the irreversible height, and track how long ago
the chain height last changed.

## Prerequisites

This tutorial uses the [NEM REST API](../reference/rest/nem.md) without requiring an SDK.
You only need a way to make HTTP requests.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/chain/chain_heights', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set the NEM API node.
If no value is provided, a default one is used.

The program runs in an infinite loop, printing a status line every second.
A keyboard interrupt (`Ctrl+C`) stops the loop.

## Code Explanation

### Fetching Chain Height

{{ tutorial.code_snippet_tagged('step-1') }}

On each iteration, the code sends a `GET` request to the <get:/chain/height> endpoint.
The response contains a single `height` field with the current chain height, the latest block known to the node.

The chain height increases each time a new block is produced (approximately every 60 seconds).

### Calculating the Irreversible Height

{{ tutorial.code_snippet_tagged('step-2') }}

The <rewrite limit:> is the maximum number of blocks a rollback can undo on NEM, set to **360 blocks** (approximately
six hours).

Subtracting the rewrite limit from the current chain height gives the **irreversible height**.

Any block at or below the irreversible height can no longer be rolled back.

See the [Consensus](../../textbook/consensus.md#conflict-resolution) textbook section for details on rollbacks and the
rewrite limit.

### Tracking Height Changes

{{ tutorial.code_snippet_tagged('step-3') }}

To show how long ago the chain height last changed, the code stores the previous height and the time at which it was
last updated.

Whenever a new block arrives and the height changes, the timestamp is refreshed.
The elapsed time is then displayed alongside the current chain height.

### Polling Loop

{{ tutorial.code_snippet_tagged('step-4') }}

Each iteration prints a single status line showing:

* The current chain height and how many seconds have elapsed since it last changed.
* The irreversible height.

The loop then sleeps for one second before querying the node again.

## Output

The following output shows a typical run monitoring the chain height and the irreversible height:

```text linenums="1" hl_lines="5"
--8<-- 'devbook/chain/chain_heights.log'
```

Some highlights from the output:

* **Before a new block** (lines 2 to 4): The chain height remains unchanged while the program continues polling.
* **A new block arrives** (line 5): The chain height advances from `659,471` to `659,472`.
    The irreversible height advances as well.

!!! note "Chain height vs. irreversible height"

    The irreversible height always trails the chain height by the <rewrite limit:>.
    Transactions near the chain tip may still be rolled back.
    Once their block falls below the rewrite limit, they become irreversible.

## Conclusion

This tutorial showed how to:

| Step                                         | Related documentation |
| -------------------------------------------- | --------------------- |
| [Fetch chain height](#fetching-chain-height) | <get:/chain/height>   |

## Next steps

For an event-driven approach to monitoring new blocks, see the
[Listening to New Blocks](../websockets/listen-new-blocks.md) WebSocket tutorial.
