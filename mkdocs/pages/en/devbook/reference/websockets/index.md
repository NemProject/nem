# WebSockets

To get **live updates** when an event occurs on the blockchain, NEM publishes
[WebSockets](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API).

Client applications can open a WebSocket connection and subscribe to any of the available channels instead of needing
to constantly poll the [REST API](../rest/nem.md) for updates.

When an event occurs in a channel, the <node:> sends a notification to every subscribed client in real-time.

NEM serves WebSockets using the [STOMP](https://stomp.github.io/) messaging protocol over
[SockJS](https://github.com/sockjs/sockjs-client), on a dedicated port (`7778` by default) separate from the HTTP API
port.

## Session

The SockJS endpoint is `/w/messages`, for example `http://localhost:7778/w/messages`.
A client that does not use SockJS can open a plain WebSocket to `/w/messages/websocket` directly.

Once the socket is open, the client drives the session with STOMP <frames:>:

1. Send a `CONNECT` frame to start the STOMP session.
2. Send a `SUBSCRIBE` frame for each <channel:> to monitor, each with a client-chosen `id`.

    !!! note "Some channels require registration"

        Some channels, like [account channels](#account-channels), deliver nothing until they receive a registration
        request.
        A client must send this request before subscribing to the channels it enables.

        See [registration requests](#registration-requests) for the full list.

3. Receive a `MESSAGE` frame from the node for every event on a subscribed channel.
4. Send an `UNSUBSCRIBE` frame to leave a channel, then a `DISCONNECT` frame to end the session.

!!! warning "Connections can drop silently"

    The WebSocket connection can drop without notice, for example after being idle for too long.
    Most STOMP clients report this through a connection-closed callback, which is a good place to reconnect.

    Reconnection starts a fresh session, so every channel must be subscribed again.

## Frames

Frame
:   A plain-text message made of a command, optional `header:value` lines, and an optional body.

The client and node exchange these frames.

### `CONNECT`

Starts the STOMP session. Sent once, right after the connection opens.

```stomp
CONNECT
accept-version:1.2
heart-beat:0,0
```

### `SUBSCRIBE`

Subscribes to a <channel:>, with a client-chosen `id` and `destination`.

```stomp
SUBSCRIBE
id:sub-0
destination:/blocks
```

* `id` is unique only within a single connection (other clients can reuse the same value).
    It is echoed back as the `subscription` header on every message and used to `UNSUBSCRIBE` later.
* `destination` identifies the channel, so the same connection can monitor multiple channels.

### `MESSAGE`

Delivers channel data from the node, and is the only frame the node sends.

```stomp
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

Sends a <request:> to a `/w/api` destination.

```stomp
SEND
destination:/w/api/account/subscribe

{ "account": "{address}" }
```

### `UNSUBSCRIBE`

Cancels a subscription by its `id`.

```stomp
UNSUBSCRIBE
id:sub-0
```

### `DISCONNECT`

Ends the session.

```stomp
DISCONNECT
```

## Channels

Channel
:   A destination the client subscribes to in order to receive messages the node pushes to it.

The available channels are grouped here by the type of event they report.

!!! note "Address format"

    Wherever an address appears, in a channel `destination` or a request body, it is uppercase and without hyphens.

    **Example:** `TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4`.

### Block channels

#### `/blocks`

ws:blocks
:   Notifies subscribed clients every time a new block is added to the chain.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/blocks
    ```

=== "Message body"

    [Block](../rest/nem.md#model/Block)

#### `/blocks/new`

ws:blocks&#47;new
:   Notifies subscribed clients of the new chain height every time a new block is added.
    A lighter alternative to `/blocks` when only the height is needed.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/blocks/new
    ```

=== "Message body"

    ```json
    {
        "height": 1234567
    }
    ```

### Transaction channels

#### `/unconfirmed`

ws:unconfirmed
:   Notifies subscribed clients every time a transaction enters the <unconfirmed pool:>, regardless of the accounts
    involved.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/unconfirmed
    ```

=== "Message body"

    [Transaction](../rest/nem.md#model/Transaction)

### Account channels

#### `/account/{address}`

ws:account&#47;{address}
:   Notifies subscribed clients when the account's state, such as its balance, changes.
    Requires the address to be [registered](#registration-requests) first.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/account/{address}
    ```

=== "Message body"

    [AccountMetaDataPair](../rest/nem.md#model/AccountMetaDataPair)

#### `/unconfirmed/{address}`

ws:unconfirmed&#47;{address}
:   Notifies subscribed clients every time a transaction involving the account enters the <unconfirmed pool:>.
    Requires the address to be [registered](#registration-requests) first.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/unconfirmed/{address}
    ```

=== "Message body"

    [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair)

#### `/transactions/{address}`

ws:transactions&#47;{address}
:   Notifies subscribed clients when a transaction involving the account is confirmed.
    Requires the address to be [registered](#registration-requests) first.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/transactions/{address}
    ```

=== "Message body"

    [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair)

#### `/account/mosaic/owned/{address}`

ws:account&#47;mosaic&#47;owned&#47;{address}
:   Notifies subscribed clients when a block changes the mosaics the account owns.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/account/mosaic/owned/{address}
    ```

=== "Message body"

    [Mosaic](../rest/nem.md#model/Mosaic)

#### `/account/mosaic/owned/definition/{address}`

ws:account&#47;mosaic&#47;owned&#47;definition&#47;{address}
:   Notifies subscribed clients when a block changes the mosaic definitions the account owns.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/account/mosaic/owned/definition/{address}
    ```

=== "Message body"

    [MosaicDefinitionSupplyTuple](../rest/nem.md#model/MosaicDefinitionSupplyTuple)

#### `/account/namespace/owned/{address}`

ws:account&#47;namespace&#47;owned&#47;{address}
:   Notifies subscribed clients when a block changes the namespaces the account owns.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/account/namespace/owned/{address}
    ```

=== "Message body"

    [Namespace](../rest/nem.md#model/Namespace)

#### `/recenttransactions/{address}`

ws:recenttransactions&#47;{address}
:   Notifies subscribed clients of the account's 25 most recent confirmed transactions, only in response to
    <req:w&#47;api&#47;account&#47;transfers&#47;all>.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/recenttransactions/{address}
    ```

=== "Message body"

    A list of [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair), wrapped in a `data` field.

### System channels

These channels report node status and request errors, rather than blockchain events.

#### `/node/info`

ws:node&#47;info
:   Notifies subscribed clients of the node's information, only in response to <req:w&#47;api&#47;node&#47;info>.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/node/info
    ```

=== "Message body"

    [Node](../rest/nem.md#model/Node)

#### `/errors`

ws:errors
:   Notifies subscribed clients when a `/w/api` <request:> fails, for example when its address payload is invalid.
    A client can subscribe to this channel right after connecting, so problems surface here instead of being silently
    dropped.

=== "Frame"

    ```stomp
    SUBSCRIBE
    id:sub-0
    destination:/errors
    ```

=== "Message body"

    ```json
    {
        "timeStamp": 67191609,
        "status": 400,
        "error": "Bad Request",
        "message": "account is not valid"
    }
    ```

## Requests

Request
:   A message the client sends to a `/w/api/...` destination to make the node act once.
    It either **registers** (a prerequisite some channels require before they deliver) or pushes a **snapshot** of
    current data to a channel.

For example, a request lets an application read an account's current state on demand, over the same WebSocket it uses
for live channel updates, instead of polling the [REST API](../rest/nem.md).

Every request is **read-only**.
It never changes the chain, only registers interest in receiving events or replays existing data.

### Registration requests

#### `/w/api/account/subscribe`

req:w&#47;api&#47;account&#47;subscribe
:   Registers the address so that [account channels](#account-channels) begin delivering events.
    It pushes nothing back to any channel.

```stomp title="SEND frame"
SEND
destination:/w/api/account/subscribe

{ "account": "{address}" }
```

#### `/w/api/account/get`

req:w&#47;api&#47;account&#47;get
:   Registers the address and pushes its current state to <ws:account&#47;{address}>.

```stomp title="SEND frame"
SEND
destination:/w/api/account/get

{ "account": "{address}" }
```

### Snapshot requests

#### `/w/api/account/transfers/all`

req:w&#47;api&#47;account&#47;transfers&#47;all
:   Pushes the account's 25 most recent confirmed transactions to <ws:recenttransactions&#47;{address}>, and up to 10
    pending ones to <ws:unconfirmed&#47;{address}>.

```stomp title="SEND frame"
SEND
destination:/w/api/account/transfers/all

{ "account": "{address}" }
```

#### `/w/api/account/transfers/unconfirmed`

req:w&#47;api&#47;account&#47;transfers&#47;unconfirmed
:   Pushes up to 10 of the account's most recent pending transactions to <ws:unconfirmed&#47;{address}>.

```stomp title="SEND frame"
SEND
destination:/w/api/account/transfers/unconfirmed

{ "account": "{address}" }
```

#### `/w/api/account/mosaic/owned`

req:w&#47;api&#47;account&#47;mosaic&#47;owned
:   Pushes the mosaics the account owns to <ws:account&#47;mosaic&#47;owned&#47;{address}>.

```stomp title="SEND frame"
SEND
destination:/w/api/account/mosaic/owned

{ "account": "{address}" }
```

#### `/w/api/account/mosaic/owned/definition`

req:w&#47;api&#47;account&#47;mosaic&#47;owned&#47;definition
:   Pushes the mosaic definitions the account owns to <ws:account&#47;mosaic&#47;owned&#47;definition&#47;{address}>.

```stomp title="SEND frame"
SEND
destination:/w/api/account/mosaic/owned/definition

{ "account": "{address}" }
```

#### `/w/api/account/namespace/owned`

req:w&#47;api&#47;account&#47;namespace&#47;owned
:   Pushes the namespaces the account owns to <ws:account&#47;namespace&#47;owned&#47;{address}>.

```stomp title="SEND frame"
SEND
destination:/w/api/account/namespace/owned

{ "account": "{address}" }
```

#### `/w/api/block/last`

req:w&#47;api&#47;block&#47;last
:   Pushes the latest block to <ws:blocks>.

```stomp title="SEND frame"
SEND
destination:/w/api/block/last
```

#### `/w/api/node/info`

req:w&#47;api&#47;node&#47;info
:   Pushes the node's information to <ws:node&#47;info>.

```stomp title="SEND frame"
SEND
destination:/w/api/node/info
```
