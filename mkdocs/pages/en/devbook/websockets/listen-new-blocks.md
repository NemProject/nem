---
title: New Blocks
tutorial_level: beginner
---

# Listening to New Blocks

The <ws:blocks> WebSocket channel sends a real-time notification every time a new <block:> is added to the chain.
Compared to polling the <get:/chain/height> endpoint, WebSockets push updates as they happen without the overhead of
repeated API calls.

This tutorial shows how to subscribe to the channel and display each update as it arrives.

!!! note "Polling alternative"

    For a polling-based approach, see the
    [Querying Chain and Irreversible Height](../chain/chain-heights.md) tutorial.

## Prerequisites

=== ":simple-python: Python"

    Install the `websockets` and `stomper` libraries:

    ```bash
    pip install websockets stomper
    ```

=== ":simple-javascript: JavaScript"

    Install the `@stomp/stompjs` and `sockjs-client` libraries:

    ```bash
    npm install @stomp/stompjs sockjs-client
    ```

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/websockets/listen_new_blocks', ['py', 'js']) }}

The snippet uses the `NODE_URL` environment variable to set the NEM <node:>.
WebSockets are served on a dedicated port (`7778` by default), separate from the REST API port (`7890`).
If no value is provided, a default one is used.

The program runs until interrupted with `Ctrl+C`, which triggers the unsubscribe step before closing the connection.

## Code Explanation

### Connecting to the WebSocket

{{ tutorial.code_snippet_tagged('step-1') }}

The first step is to open a connection to the node's `/w/messages` endpoint and start a STOMP session over it.

See the [WebSocket reference](../reference/websockets/index.md) for details on the connection protocol.

### Subscribing to the Channel

{{ tutorial.code_snippet_tagged('step-2') }}

The code subscribes to the <ws:blocks> channel, which fires every time a new block is added to the chain
(approximately every minute).

The subscription is given an `id` (`sub-blocks`) when subscribing, which is reused to unsubscribe on exit.

Each incoming message is then passed to the formatting logic below.

### Formatting the Message

{{ tutorial.code_snippet_tagged('step-3') }}

The body of each incoming STOMP message is the new block, following the [Block](../reference/rest/nem.md#model/Block)
schema.

For each message, the snippet prints two of its fields:

* `height`: The height of the new block.
* `signer`: The public key of the <harvester account:> that produced the block.

A NEM block does not include its own hash in this payload, so this tutorial identifies each block by its `height`
and also prints the harvester's `signer`.

!!! warning "New blocks are not yet final"

    New blocks are already part of the chain but not yet irreversible.
    Until enough subsequent blocks surpass the <rewrite limit:>, <rollbacks:> are still possible.
    The [Querying Chain and Irreversible Height](../chain/chain-heights.md) tutorial shows how to calculate the
    irreversible height.

### Unsubscribing on Exit

{{ tutorial.code_snippet_tagged('step-4') }}

When the program is interrupted (`Ctrl+C`), the code unsubscribes from the channel and ends the STOMP session before
closing the connection.
This ensures a clean disconnection from the node.

## Output

The following output shows a typical run listening to new blocks:

```text linenums="1" hl_lines="2 3 4 9"
--8<-- 'devbook/websockets/listen_new_blocks.log'
```

The output shows:

* **Connection** (line 2): The STOMP session is established over the SockJS endpoint.
* **Subscription** (line 3): The `/blocks` channel is subscribed.
* **New blocks** (lines 4-8): New block notifications arrive approximately every minute.
* **Unsubscribe** (line 9): On `Ctrl+C`, the code unsubscribes and disconnects.

## Conclusion

This tutorial showed how to:

| Step                                                          | Related documentation                         |
| ------------------------------------------------------------- | --------------------------------------------- |
| [Subscribe to the block channel](#subscribing-to-the-channel) | <ws:blocks>                                   |
| [Format block messages](#formatting-the-message)              | [Block](../reference/rest/nem.md#model/Block) |
