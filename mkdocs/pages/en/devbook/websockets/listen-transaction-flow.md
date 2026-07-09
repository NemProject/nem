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
It is derived from `NODE_URL` by replacing port `7890`, the default HTTP API port, with `7778`, the default NIS
WebSocket port.

## Code Explanation

### Setting Up the Monitored Address and Signer

{{ tutorial.code_snippet_tagged('step-1') }}

This step sets up the address to monitor and the account that sends a transfer to it.

`MONITOR_ADDRESS` is the address to watch.
The channels this tutorial subscribes to are scoped to this address and notify whenever it is involved in a transaction,
for example as the sender or recipient of a transfer.
The WebSocket API expects the address uppercase and without hyphens.

`SIGNER_PRIVATE_KEY` is the private key of the account that sends the transfer, which triggers the notifications.

If any of these environment variables is not provided, the tutorial provides default values.

### Building and Signing a Transfer Transaction

{{ tutorial.code_snippet_tagged('step-2') }}

This tutorial builds a minimal <Transfer Transaction:> to the monitored address, with a zero amount, no mosaics, and
no message.
A transfer is used for simplicity, but any transaction type triggers the same WebSocket notifications.

The transaction is built the same way as in the
[Transfer XEM](../transactions/transfer-xem.md) tutorial: fetching the network time, creating the transaction, and
signing it.

Signing the transaction produces its hash, which uniquely identifies it.
The code stores this hash because transaction channel notifications include the transaction hash.
The message handler, defined later, compares each received hash with the stored value to identify notifications for this
transaction.

The transaction is prepared, but it is not [announced](#announcing-and-waiting-for-confirmation) yet.
The announcement happens after the channel subscriptions are established, so the notifications it triggers are not
missed.

### Connecting to the WebSocket

{{ tutorial.code_snippet_tagged('step-3') }}

The code opens a SockJS connection to the `/w/messages` endpoint on `WS_URL` and starts a <STOMP session:>
over it.

### Subscribing to the Channels

{{ tutorial.code_snippet_tagged('step-4') }}

The code subscribes to three address-scoped channels:

* <ws:account&#47;{address}>: Notifies of the account's current state when a <block:> involving the account's address is
    confirmed.
* <ws:unconfirmed&#47;{address}>: Notifies of a transaction involving the account's address when it enters the
    <unconfirmed pool:>, waiting to be included in a block.
* <ws:transactions&#47;{address}>: Notifies of a transaction involving the account's address when it is included in a
    <block:>.

The subscriptions use the IDs `id-0`, `id-1` and `id-2`, which identify them when the code unsubscribes at the end.

All three channels stay silent until the address is registered, which the next step performs.

### Registering the Account

{{ tutorial.code_snippet_tagged('step-5') }}

To receive notifications on an account's channels, the address must first be **registered** with the node.

The code sends a <req:w&#47;api&#47;account&#47;get> request, which registers the address and also forces the node to
send the account's current state on the <ws:account&#47;{address}> channel.

The code waits for this first account notification, which confirms that the registration is active.
The notification follows the [AccountMetaDataPair](../reference/rest/nem.md#model/AccountMetaDataPair) schema.

The subscription to the account channel stays open for the rest of the run, so the account notification triggered by
the transaction confirmation also appears in the output.

### Announcing and Waiting for Confirmation

{{ tutorial.code_snippet_tagged('step-6') }}

!!! warning "Announce after subscribing to channels"

    Always announce the transaction **after** subscribing to the WebSocket channels to ensure the listener is ready.
    Otherwise, notifications could arrive before the WebSocket is listening.

The code announces the transaction to the <post:/transaction/announce> endpoint and checks the result.
If the node rejects it, the code prints the rejection reason and stops.

Otherwise, the code waits for confirmation, printing each message from the subscribed channels.
Messages from the transaction channels follow the
[TransactionMetaDataPair](../reference/rest/nem.md#model/TransactionMetaDataPair) schema, whose
`meta.hash.data` field holds the transaction hash.
As each one arrives, the message handler compares that hash against the stored value to recognize this transaction
among the channel notifications.

The expected sequence for a successful transaction is described in the
[Transaction Lifecycle](../../textbook/transactions.md#transaction-lifecycle) section:

1. `unconfirmed`: The transaction enters the <unconfirmed pool:>.
2. `confirmed`: The transaction is included in a <block:>.

The block that includes the transaction also triggers a final notification on the <ws:account&#47;{address}>
channel.
Unlike the transaction channels, this notification contains the account's updated state rather than a transaction hash,
so it cannot be matched to a specific transaction.

Once this final notification arrives, the program moves on to the cleanup step.

### Unsubscribing from Channels

{{ tutorial.code_snippet_tagged('step-7') }}

After confirmation, the code unsubscribes from the three channels and ends the STOMP session before the connection
closes.

## Output

```text linenums="1" hl_lines="2-14"
--8<-- 'devbook/websockets/listen_transaction_flow.log'
```

The output shows:

* **Address** (line 2): The monitored address.
* **Connection** (line 3): The STOMP session is established over the node's WebSocket endpoint at port `7778`.
* **Subscriptions** (lines 4-6): The account channel and both transaction channels are subscribed.
* **Registration** (lines 7-8): The account's current state arrives on the account channel, confirming the
    registration.
* **Announcement** (line 9): The transaction is announced and its hash is printed.
* **Transaction flow** (lines 10-11): The transaction moves from `unconfirmed` to `confirmed`, showing the
    confirmation lifecycle.
* **Confirmation** (line 12): The hash from the <ws:transactions&#47;{address}> channel matches the announced
    transaction.
* **Account update** (line 13): The block containing the transaction triggers a final account notification.
    The balance is unchanged, since the transfer amount is zero.
* **Unsubscribe** (line 14): The code unsubscribes from the three channels.

## Conclusion

This tutorial showed how to:

| Step                                                                    | Related documentation                                                             |
| ----------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| [Subscribe to the account channel](#subscribing-to-the-channels)        | <ws:account&#47;{address}>                                                        |
| [Subscribe to the unconfirmed channel](#subscribing-to-the-channels)    | <ws:unconfirmed&#47;{address}>                                                    |
| [Subscribe to the transactions channel](#subscribing-to-the-channels)   | <ws:transactions&#47;{address}>                                                   |
| [Register the account](#registering-the-account)                        | <req:w&#47;api&#47;account&#47;get>                                               |
| [Handle transaction messages](#announcing-and-waiting-for-confirmation) | [TransactionMetaDataPair](../reference/rest/nem.md#model/TransactionMetaDataPair) |
