---
title: Transaction Flow
tutorial_level: beginner
---

# Listening to Transaction Flow

NEM provides <WebSocket channels:> that send real-time notifications as a <transaction:> moves
through the confirmation process for a specific <account:>.
Compared to polling the <get:/transaction/get> endpoint, WebSockets push updates as they happen without the overhead
of repeated API calls.

This tutorial shows how to subscribe to transaction channels, announce a minimal
[Transfer Transaction](../transactions/transfer-xem.md), and wait for its confirmation using WebSockets.

!!! note "Alternative: Polling"

    For a polling-based approach, see the
    [Monitoring Transaction Status](../transactions/monitoring-status.md) tutorial.

## Prerequisites

Before you start, make sure to:

* Set up your development environment.
  See [Setting Up a Development Environment](../start/setup.md).
* Have the address of the account to monitor.
* Have an account with enough balance for transaction fees.
  See [Creating an Account from a Private Key](../accounts/create-from-private-key.md) or
  [Creating an Account by Using a Wallet](../../userbook/wallet/create-account.md).

Additionally, NEM serves WebSockets using the [STOMP](https://stomp.github.io/) messaging protocol over
[SockJS](https://github.com/sockjs/sockjs-client), so a STOMP client and a WebSocket transport are required:

=== ":simple-python: Python"

    Install the `stomper` and `websockets` libraries:

    ```bash
    pip install stomper websockets
    ```

=== ":simple-javascript: JavaScript"

    Install the `@stomp/stompjs` and `sockjs-client` libraries:

    ```bash
    npm install @stomp/stompjs sockjs-client
    ```

See the [WebSocket reference](../reference/websockets/index.md) for details on the connection protocol.

## Full Code

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/websockets/listen_transaction_flow', ['py', 'js']) }}

!!! note

    There is no SockJS client library for Python, so a few small helper methods are defined at the top of the file
    for convenience.

The snippet uses the `NODE_URL` environment variable to set the NEM <node:>.
If no value is provided, a default one is used.

`WS_URL` defines the WebSocket endpoint for the same node.
It is derived from `NODE_URL` by replacing port `7890` with `7778`.

## Code Explanation

### Setting Up the Monitored Address and Signer

{{ tutorial.code_snippet_tagged('step-1') }}

Each transaction channel is scoped to a specific address.
The channels send a notification whenever this address is involved in a transaction, as sender or recipient.
The `MONITOR_ADDRESS` environment variable sets the address to watch.
The WebSocket API expects it uppercase and without hyphens.

To trigger notifications, this tutorial sends a transfer transaction to the monitored address.
The sender's private key is read from `SIGNER_PRIVATE_KEY`.

If any of these environment variables is not provided, the tutorial provides default values.

### Connecting to the WebSocket

{{ tutorial.code_snippet_tagged('step-2') }}

The code opens a SockJS connection to the `/w/messages` endpoint on `WS_URL` and starts a <STOMP session:>
over it.

### Registering the Account

{{ tutorial.code_snippet_tagged('step-3') }}

To receive notifications on an account's transaction channels, the address must first be **registered** with the node.

Before sending the registration request, the code temporarily subscribes to the <ws:account&#47;{address}> channel
using `id-0`, so it can capture the notification that confirms registration.

The code then sends a <req:w&#47;api&#47;account&#47;get> request to register the address.

Once the address is registered, the node sends the account’s current state on the same <ws:account&#47;{address}>
channel, which serves as the registration confirmation.

When the notification arrives, the temporary subscription to the account messages channel is dropped using the same
`id-0` identifier.

### Subscribing to the Channels

{{ tutorial.code_snippet_tagged('step-4') }}

With the account's address registered, the code subscribes to two address-scoped channels:

* <ws:unconfirmed&#47;{address}>: Notifies when a transaction involving the address enters the <unconfirmed pool:>,
    waiting to be included in a block.
* <ws:transactions&#47;{address}>: Notifies when a transaction involving the address is included in a <block:>.

The subscriptions use IDs `id-1` and `id-2`, which identify them when the code unsubscribes after confirmation.

### Building and Signing a Transfer Transaction

{{ tutorial.code_snippet_tagged('step-5') }}

This tutorial builds a minimal <Transfer Transaction:> to the monitored address, with a zero amount, no mosaics, and no message.
A transfer is used for simplicity, but any transaction type triggers the same WebSocket notifications.

The transaction follows the same process described in the
[Transfer XEM](../transactions/transfer-xem.md) tutorial: fetching the network time, creating the transaction,
and signing it.
The hash is computed locally so it can be matched against incoming messages later.

### Announcing and Waiting for Confirmation

{{ tutorial.code_snippet_tagged('step-6') }}

The code announces the transaction to the <post:/transaction/announce> endpoint and checks the result.
If the node rejected it, the code prints the rejection reason and stops.

Otherwise, the code waits for confirmation, printing each message from the two subscribed channels.
Each message follows the [TransactionMetaDataPair](../reference/rest/nem.md#model/TransactionMetaDataPair) schema, whose
`meta.hash.data` field holds the transaction hash.

When a message from the <ws:transactions&#47;{address}> channel arrives whose hash matches the announced transaction,
the program prints a confirmation message and exits.

!!! warning "Announce after subscribing to channels"

    Always announce the transaction **after** subscribing to the WebSocket channels to ensure the listener is ready.
    Otherwise, notifications could arrive before the WebSocket is listening.

The expected sequence for a successful transaction is described in the
[Transaction Lifecycle](../../textbook/transactions.md#transaction-lifecycle) section:

1. `unconfirmed`: The transaction enters the <unconfirmed pool:>.
2. `confirmed`: The transaction is included in a <block:>.

### Unsubscribing from Channels

{{ tutorial.code_snippet_tagged('step-7') }}

After confirmation, the code unsubscribes from both channels and ends the STOMP session before the connection closes.

## Output

```text linenums="1" hl_lines="2 3 4 5 6 7 8 9 10 11"
--8<-- 'devbook/websockets/listen_transaction_flow.log'
```

The output shows:

* **Address** (line 2): The monitored address.
* **Connection** (line 3): The STOMP session is established over the node's WebSocket endpoint at port `7778`.
* **Registration** (line 4): The account registration is confirmed before subscribing.
* **Subscriptions** (lines 5-6): Both transaction channels are subscribed.
* **Announcement** (line 7): The transaction is announced and its hash is printed.
* **Transaction flow** (lines 8-9): The transaction moves from `unconfirmed` to `confirmed`, showing the confirmation
    lifecycle.
* **Confirmation** (line 10): The hash from the `/transactions/{address}` channel matches the announced transaction.
* **Unsubscribe** (line 11): The code unsubscribes from both channels.

## Conclusion

This tutorial showed how to:

| Step                                                                    | Related documentation                                                             |
| ----------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| [Register the account](#registering-the-account)                        | <req:w&#47;api&#47;account&#47;get>, <ws:account&#47;{address}>                   |
| [Subscribe to the unconfirmed channel](#subscribing-to-the-channels)    | <ws:unconfirmed&#47;{address}>                                                    |
| [Subscribe to the transactions channel](#subscribing-to-the-channels)   | <ws:transactions&#47;{address}>                                                   |
| [Handle transaction messages](#announcing-and-waiting-for-confirmation) | [TransactionMetaDataPair](../reference/rest/nem.md#model/TransactionMetaDataPair) |
