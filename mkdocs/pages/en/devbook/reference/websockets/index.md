# WebSockets

NEM publishes blockchain events over
[WebSockets](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API), so applications can receive live updates
without constantly polling the [REST API](../rest/nem.md).

Client applications open a WebSocket connection to any <node:> in the network and subscribe to the [channels](#channels)
they want to monitor.
When an event occurs on a channel, the node notifies every subscribed client in real time.

Some channels also accept [requests](#requests) for immediate data, similar to the REST API.
This can simplify applications that use WebSockets as their only API for both live notifications and on-demand updates.

## Connection

NEM serves WebSockets using the [STOMP](https://stomp.github.io/) messaging protocol over
[SockJS](https://github.com/sockjs/sockjs-client), on a dedicated port (`7778` by default) separate from the HTTP API
port.

The SockJS endpoint is `/w/messages`, for example `http://localhost:7778/w/messages`.

Clients typically connect using either a SockJS client library or the native WebSocket API, together with a STOMP client
library to handle messaging.

??? note "Connecting using native WebSockets"

    SockJS provides a WebSocket-like transport with cross-browser support and HTTP-based fallback options when native
    WebSockets are unavailable.

    Clients with native WebSocket support can connect directly to the SockJS WebSocket transport endpoint at
    `/w/messages/websocket`, for example: `ws://localhost:7778/w/messages/websocket`.

    This uses the WebSocket transport of SockJS without requiring the SockJS client library,
    while still relying on the SockJS server.

## STOMP Session

STOMP Session
:   A client-node conversation over a WebSocket connection, following the [STOMP](https://stomp.github.io/) messaging
    protocol.

A client controls the session by exchanging <STOMP Frames:> with the node:

1. Send a `CONNECT` frame to start the STOMP session.
2. Send a `SUBSCRIBE` frame for each <WebSocket Channel:|channel> to monitor.
    Each subscription requires a client-defined `id`.
3. Send an optional [registration request](#registration-requests) with a `SEND` frame to enable notifications for
    channels that require explicit registration.
4. Receive a `MESSAGE` frame from the node for every event on a subscribed channel.
5. Send an `UNSUBSCRIBE` frame for each subscribed channel to stop receiving its notifications.
6. Send a `DISCONNECT` frame to end the session.

!!! warning "Connections can drop silently"

    The WebSocket connection can drop without notice, for example after being idle for too long.
    Most STOMP clients report this through a connection-closed callback, which is a good place to reconnect.

    Reconnection starts a fresh session, so every channel must be subscribed again.

## STOMP Frames

STOMP Frame
:   A plain-text message adhering to the [STOMP](https://stomp.github.io/) protocol,
    made of a command, optional `header:value` lines, and an optional body.

The client and node exchange the following frame types.

### `CONNECT`

Starts the STOMP session.
The client must send this frame once, right after the connection opens.
See the [STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#CONNECT_or_STOMP_Frame) for full details.

```stomp title="Example"
CONNECT
accept-version:1.2
heart-beat:0,0
```

### `SUBSCRIBE`

Subscribes to a <WebSocket Channel:|channel>, with a client-chosen `id` and `destination`.
See the [STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE) for full details.

```stomp title="Example"
SUBSCRIBE
id:sub-0
destination:/blocks
```

* `id` is unique only within a single connection (other clients can reuse the same value).
    It is echoed back as the `subscription` header on every message and used to `UNSUBSCRIBE` later.
* `destination` identifies the channel, so the same connection can monitor multiple channels.

### `MESSAGE`

Delivers channel data from the node.
It is the only frame type the node sends.
See the [STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#MESSAGE) for full details.

```stomp title="Example"
MESSAGE
destination:/blocks
subscription:sub-0
message-id:befkedjj-6247

{ ... }
```

* `destination` matches the channel from the `SUBSCRIBE` frame.
* `subscription` matches the `id` from the `SUBSCRIBE` frame.
* `message-id` is a unique identifier the server assigns to each message.
* `{ ... }` is the body, a JSON object whose shape depends on the [channel](#channels).
    See the **Message body** tabs below.

### `SEND`

Sends a <WebSocket Request:|request> to a `/w/api` destination.
See the [STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#SEND) for full details.

```stomp title="Example"
SEND
destination:/w/api/account/subscribe

{ "account": "{address}" }
```

### `UNSUBSCRIBE`

Cancels a subscription by its `id`.
See the [STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#UNSUBSCRIBE) for full details.

```stomp title="Example"
UNSUBSCRIBE
id:sub-0
```

### `DISCONNECT`

Ends the session.
See the [STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT) for full details.

```stomp title="Example"
DISCONNECT
```

## Channels

WebSocket Channel
:   Node notifications are grouped into channels.
    Clients subscribe to each channel whose notifications they want to receive.

Every channel is subscribed to with a [`SUBSCRIBE`](#subscribe) frame.

The available channels are grouped here by the type of event they report.

### Block Channels

These channels report new blocks as they are added to the chain.

#### `/blocks`

ws:blocks
:   Notifies subscribed clients every time a new block is added to the chain.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/blocks
```
</td><td markdown>
```stomp
MESSAGE
destination:/blocks
subscription:sub-0
message-id:...

{ ... }
```
Followed by a [Block](../rest/nem.md#model/Block) JSON body.
</td></tr></table></div>

#### `/blocks/new`

ws:blocks&#47;new
:   Notifies subscribed clients of the new chain height every time a new block is added.
    A lighter alternative to `/blocks` when only the height is needed.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/blocks/new
```
</td><td markdown>
```stomp
MESSAGE
destination:/blocks/new
subscription:sub-0
message-id:...

{ "height": 1234567 }
```
</td></tr></table></div>

### Transaction Channels

These channels report transaction activity, regardless of the accounts involved.

#### `/unconfirmed`

ws:unconfirmed
:   Notifies subscribed clients every time a transaction enters the <unconfirmed pool:>, regardless of the accounts
    involved.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/unconfirmed
```
</td><td markdown>
```stomp
MESSAGE
destination:/unconfirmed
subscription:sub-0
message-id:...

{ ... }
```
Followed by a [Transaction](../rest/nem.md#model/Transaction) JSON body.
</td></tr></table></div>

### Account Channels

These channels report activity for a specific account, such as its balance, transactions, and the mosaics and
namespaces it owns.

!!! note "Address format"

    Wherever an address appears, either in a channel `destination` or a request body, it uses the
    [encoded address](../../../textbook/cryptography.md#addresses) format:
    uppercase letters and digits, without hyphens.

    **Example:** `TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4`.

#### `/account/{address}`

ws:account&#47;{address}
:   Notifies subscribed clients when the account's state, such as its balance, changes.
    Requires the address to be [registered](#registration-requests) first.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/account/{address}
```
</td><td markdown>
```stomp
MESSAGE
destination:/account/{address}
subscription:sub-0
message-id:...

{ ... }
```
Followed by an [AccountMetaDataPair](../rest/nem.md#model/AccountMetaDataPair) JSON body.
</td></tr></table></div>

#### `/unconfirmed/{address}`

ws:unconfirmed&#47;{address}
:   Notifies subscribed clients every time a transaction involving the account enters the <unconfirmed pool:>.
    Requires the address to be [registered](#registration-requests) first.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/unconfirmed/{address}
```
</td><td markdown>
```stomp
MESSAGE
destination:/unconfirmed/{address}
subscription:sub-0
message-id:...

{ ... }
```
Followed by a [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair) JSON body.
</td></tr></table></div>

#### `/transactions/{address}`

ws:transactions&#47;{address}
:   Notifies subscribed clients when a transaction involving the account is confirmed.
    Requires the address to be [registered](#registration-requests) first.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/transactions/{address}
```
</td><td markdown>
```stomp
MESSAGE
destination:/transactions/{address}
subscription:sub-0
message-id:...

{ ... }
```
Followed by a [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair) JSON body.
</td></tr></table></div>

#### `/account/mosaic/owned/{address}`

ws:account&#47;mosaic&#47;owned&#47;{address}
:   Notifies subscribed clients when a block changes the mosaics the account owns.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/account/mosaic/owned/{address}
```
</td><td markdown>
```stomp
MESSAGE
destination:/account/mosaic/owned/{address}
subscription:sub-0
message-id:...

{ ... }
```
Followed by a [Mosaic](../rest/nem.md#model/Mosaic) JSON body.
</td></tr></table></div>

#### `/account/mosaic/owned/definition/{address}`

ws:account&#47;mosaic&#47;owned&#47;definition&#47;{address}
:   Notifies subscribed clients when a block changes the mosaic definitions the account owns.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/account/mosaic/owned/definition/{address}
```
</td><td markdown>
```stomp
MESSAGE
destination:/account/mosaic/owned/definition/{address}
subscription:sub-0
message-id:...

{ ... }
```
Followed by a [MosaicDefinitionSupplyTuple](../rest/nem.md#model/MosaicDefinitionSupplyTuple) JSON body.
</td></tr></table></div>

#### `/account/namespace/owned/{address}`

ws:account&#47;namespace&#47;owned&#47;{address}
:   Notifies subscribed clients when a block changes the namespaces the account owns.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/account/namespace/owned/{address}
```
</td><td markdown>
```stomp
MESSAGE
destination:/account/namespace/owned/{address}
subscription:sub-0
message-id:...

{ ... }
```
Followed by a [Namespace](../rest/nem.md#model/Namespace) JSON body.
</td></tr></table></div>

#### `/recenttransactions/{address}`

ws:recenttransactions&#47;{address}
:   Notifies subscribed clients of the account's 25 most recent confirmed transactions, only in response to
    <req:w&#47;api&#47;account&#47;transfers&#47;all>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/recenttransactions/{address}
```
</td><td markdown>
```stomp
MESSAGE
destination:/recenttransactions/{address}
subscription:sub-0
message-id:...

{ ... }
```
Followed by a list of [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair) wrapped in a `data` field.
</td></tr></table></div>

### System Channels

These channels report node status and request errors, rather than blockchain events.

#### `/node/info`

ws:node&#47;info
:   Notifies subscribed clients of the node's information, only in response to <req:w&#47;api&#47;node&#47;info>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/node/info
```
</td><td markdown>
```stomp
MESSAGE
destination:/node/info
subscription:sub-0
message-id:...

{ ... }
```
Followed by a [Node](../rest/nem.md#model/Node) JSON body.
</td></tr></table></div>

#### `/errors`

ws:errors
:   Notifies subscribed clients when a `/w/api` <WebSocket Request:|request> fails, for example when its address payload is invalid.
    A client can subscribe to this channel right after connecting, so problems surface here instead of being silently
    dropped.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Subscription frame</th><th markdown>:material-arrow-down-bold: Notification frame</th></tr>
<tr markdown><td markdown>
```stomp
SUBSCRIBE
id:sub-0
destination:/errors
```
</td><td markdown>
```stomp
MESSAGE
destination:/errors
subscription:sub-0
message-id:...

{
    "timeStamp": 67191609,
    "status": 400,
    "error": "Bad Request",
    "message": "account is not valid"
}
```
</td></tr></table></div>

## Requests

WebSocket Request
:   A message sent by the client to make the node deliver:
    either notifications on channels that require **registration**, or an immediate notification containing a
    **snapshot** of the state of the blockchain.

Requests are **read-only** and do not modify the chain state.

All requests are sent with a [`SEND`](#send) frame to a destination that begins with `/w/api/`.
Some requests return no answer, while others return results through one of the [channels](#channels) above.

### Registration Requests

Some [account channels](#account-channels) stay silent until the address is registered.
These requests perform that registration, so those channels begin delivering notifications.

#### `/w/api/account/subscribe`

req:w&#47;api&#47;account&#47;subscribe
:   Registers the address so the node starts sending notifications on [account channels](#account-channels).

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/account/subscribe

{ "account": "{address}" }
```
</td></tr></table></div>

#### `/w/api/account/get`

req:w&#47;api&#47;account&#47;get
:   Registers the address like <req:w&#47;api&#47;account&#47;subscribe>, and forces the node to send a
    notification containing the account's current state to <ws:account&#47;{address}>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/account/get

{ "account": "{address}" }
```
</td></tr></table></div>

### Snapshot Requests

Each request forces the node to send an immediate snapshot of current data to a channel, without waiting for a new
event.

This allows applications to fetch current data on demand through the same channels used for live updates, instead of polling the [REST API](../rest/nem.md).

#### `/w/api/account/transfers/all`

req:w&#47;api&#47;account&#47;transfers&#47;all
:   Forces the node to send a notification containing the account's 25 most recent confirmed transactions
    to <ws:recenttransactions&#47;{address}>, and another with up to 10 pending ones to <ws:unconfirmed&#47;{address}>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/account/transfers/all

{ "account": "{address}" }
```
</td></tr></table></div>

#### `/w/api/account/transfers/unconfirmed`

req:w&#47;api&#47;account&#47;transfers&#47;unconfirmed
:   Forces the node to send a notification containing up to 10 of the account's most recent pending
    transactions to <ws:unconfirmed&#47;{address}>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/account/transfers/unconfirmed

{ "account": "{address}" }
```
</td></tr></table></div>

#### `/w/api/account/mosaic/owned`

req:w&#47;api&#47;account&#47;mosaic&#47;owned
:   Forces the node to send a notification containing the mosaics the account owns to
    <ws:account&#47;mosaic&#47;owned&#47;{address}>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/account/mosaic/owned

{ "account": "{address}" }
```
</td></tr></table></div>

#### `/w/api/account/mosaic/owned/definition`

req:w&#47;api&#47;account&#47;mosaic&#47;owned&#47;definition
:   Forces the node to send a notification containing the mosaic definitions the account owns to
    <ws:account&#47;mosaic&#47;owned&#47;definition&#47;{address}>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/account/mosaic/owned/definition

{ "account": "{address}" }
```
</td></tr></table></div>

#### `/w/api/account/namespace/owned`

req:w&#47;api&#47;account&#47;namespace&#47;owned
:   Forces the node to send a notification containing the namespaces the account owns to
    <ws:account&#47;namespace&#47;owned&#47;{address}>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/account/namespace/owned

{ "account": "{address}" }
```
</td></tr></table></div>

#### `/w/api/block/last`

req:w&#47;api&#47;block&#47;last
:   Forces the node to send a notification containing the latest block to <ws:blocks>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/block/last
```
</td></tr></table></div>

#### `/w/api/node/info`

req:w&#47;api&#47;node&#47;info
:   Forces the node to send a notification containing its own information to <ws:node&#47;info>.

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: Request frame</th></tr>
<tr markdown><td markdown>
```stomp
SEND
destination:/w/api/node/info
```
</td></tr></table></div>
