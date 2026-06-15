---
title: Chain and Irreversible Height
tutorial_level: beginner
---

# Querying Chain and Irreversible Height

The <get:/chain/height> endpoint returns the current chain height.

A <block:> becomes effectively irreversible once enough newer blocks sit on top of it that the network can no longer
roll it back.
The height of the most recent irreversible block is the **irreversible height**.

Comparing the chain height with the irreversible height shows which blocks can still be rolled back and which are
already settled, which is useful for applications that need to know when a transaction can no longer be reversed.

This tutorial shows how to poll the chain height in a loop and track how long ago it last changed.

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
Any block at or below `height - 360` has enough blocks on top of it that it can no longer be replaced by an alternative
chain.

Because the rewrite limit is constant, the irreversible height advances by one block each time a new block is produced,
keeping a fixed gap below the chain tip.

See the [Consensus](../../textbook/consensus.md#conflict-resolution) textbook section for details on how rollbacks and
the rewrite limit work.

### Tracking Height Changes

{{ tutorial.code_snippet_tagged('step-3') }}

To show how long ago the height last changed, the code stores the previous value and its timestamp.
When the height differs from the previous value, the timestamp is updated to the current time.

Until a change is observed, the timestamp remains unset and the output displays `-` instead of a number.
Once a change occurs, the counter starts from `0s ago` and increments each second until the next change.

### Polling Loop

{{ tutorial.code_snippet_tagged('step-4') }}

Each iteration prints a single status line showing:

* The current chain height and how many seconds have elapsed since it last changed.
* The irreversible height, the highest block that can no longer be rolled back.

The loop then sleeps for one second between iterations.

## Output

The following output shows a typical run monitoring the chain height and the irreversible height:

```text linenums="1" hl_lines="5"
--8<-- 'devbook/chain/chain_heights.log'
```

Some highlights from the output:

* **Before a new block** (lines 2 to 4): The change counter shows `-` because no change has been observed yet.
* **A new block arrives** (line 5): The chain height advances from `659,471` to `659,472` and the change counter starts
    from `0s`.
    The irreversible height advances to `659,112`.

The gap between the chain height and the irreversible height is normal.
A transaction included in a block at the chain tip is confirmed but not yet irreversible.
Once it falls below the rewrite limit, the block can no longer be rolled back and the transaction is guaranteed to
remain in the chain.

## Conclusion

This tutorial showed how to:

| Step                                         | Related documentation |
| -------------------------------------------- | --------------------- |
| [Fetch chain height](#fetching-chain-height) | <get:/chain/height>   |

## Next steps

For an event-driven approach to monitoring new blocks, see the
[Listening to New Blocks](../websockets/listen-new-blocks.md) WebSocket tutorial.
