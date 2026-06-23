# WebSockets

To get **live updates** when an event occurs on the blockchain, NEM publishes
[WebSockets](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API).

Client applications can open a WebSocket connection and subscribe to any of the available channels instead of needing
to constantly poll the [REST API](../rest/nem.md) for updates.

When an event occurs in a channel, the <node:> sends a notification to every subscribed client in real-time.

NEM serves WebSockets using the [STOMP](https://stomp.github.io/) messaging protocol over
[SockJS](https://github.com/sockjs/sockjs-client), on a dedicated port (`7778` by default) separate from the HTTP API
port.
The SockJS endpoint is `/w/messages`, for example: `http://localhost:7778/w/messages`.

!!! warning

    The WebSocket connection is dropped silently if idle for too long.

    Channels are not automatically resubscribed on reconnection.

## Request Format

After opening the SockJS connection, the client drives the session with STOMP frames.
A _frame_ is a plain-text message made of a command, optional `header:value` lines, and an optional body.

The client uses these frames:

| Frame         | Purpose                                                                           |
| ------------- | --------------------------------------------------------------------------------- |
| `CONNECT`     | Starts the STOMP session. Sent once, right after the connection opens.            |
| `SUBSCRIBE`   | Subscribes to a channel, with a client-chosen `id` and the channel `destination`. |
| `SEND`        | Sends a request to a `/w/api` destination, for example to register an account.    |
| `UNSUBSCRIBE` | Cancels a subscription by its `id`.                                               |
| `DISCONNECT`  | Ends the session.                                                                 |

To subscribe to a channel, send a `SUBSCRIBE` frame with a client-chosen `id` and the channel `destination`:

```text title="SUBSCRIBE frame"
SUBSCRIBE
id:sub-0
destination:/blocks
```

* `id` is unique only within a single connection (other clients can reuse the same value).
    It is echoed back as the `subscription` header on every message and used to `UNSUBSCRIBE` later.
* `destination` is one of the [channels](#channels) listed below.

## Response Format

Each update is delivered as a STOMP `MESSAGE` frame:

```text title="MESSAGE frame"
MESSAGE
destination:/blocks
subscription:sub-0
message-id:befkedjj-6247

{ ... }
```

* The `destination` header identifies the channel, so the same connection can monitor multiple channels.
* The `subscription` header is the `id` from the `SUBSCRIBE` frame.
* The `message-id` header is a unique identifier the server assigns to each message.
* The frame body is a channel-specific JSON object, described per [channel](#channels) below.

## Channels

The available channels are grouped by the type of event they report.

### Block channels

#### `/blocks`

ws:blocks
:   Notifies subscribed clients every time a new block is added to the chain.

=== "Request body"

    ```text
    SUBSCRIBE
    id:sub-0
    destination:/blocks
    ```

=== "Response body"

    [Block](../rest/nem.md#model/Block)

#### `/blocks/new`

ws:blocks-new
:   Notifies subscribed clients of the new chain height every time a new block is added.
    A lighter alternative to `/blocks` when only the height is needed.

=== "Request body"

    ```text
    SUBSCRIBE
    id:sub-0
    destination:/blocks/new
    ```

=== "Response body"

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

=== "Request body"

    ```text
    SUBSCRIBE
    id:sub-0
    destination:/unconfirmed
    ```

=== "Response body"

    [Transaction](../rest/nem.md#model/Transaction)

### Account channels

!!! note "Account channels require registration"

    Account channels only push after the account has been registered by sending a `SEND` frame to
    `/w/api/account/subscribe`:

    ```text
    SEND
    destination:/w/api/account/subscribe

    { "account": "{address}" }
    ```

#### `/account/{address}`

ws:account&#47;{address}
:   Notifies subscribed clients when the account's state, such as its balance, changes.

=== "Request body"

    ```text
    SUBSCRIBE
    id:sub-0
    destination:/account/{address}
    ```

=== "Response body"

    [AccountMetaDataPair](../rest/nem.md#model/AccountMetaDataPair)

#### `/unconfirmed/{address}`

ws:unconfirmed&#47;{address}
:   Notifies subscribed clients every time a transaction involving the registered account enters the
    <unconfirmed pool:>.

=== "Request body"

    ```text
    SUBSCRIBE
    id:sub-0
    destination:/unconfirmed/{address}
    ```

=== "Response body"

    [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair)

#### `/transactions/{address}`

ws:transactions&#47;{address}
:   Notifies subscribed clients when a transaction involving the registered account is confirmed.

=== "Request body"

    ```text
    SUBSCRIBE
    id:sub-0
    destination:/transactions/{address}
    ```

=== "Response body"

    [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair)
