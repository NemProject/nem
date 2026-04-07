# WebSockets

To get **live updates** when an event occurs on the blockchain, NEM publishes
[WebSockets](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API).

Client applications can open a WebSocket connection and get a unique identifier.
With this identifier, applications can subscribe to any of the available channels instead of needing to constantly
poll the [REST API](../rest/nem.md) for updates.

When an event occurs in a channel, the [REST Gateway](../../../textbook/nodes.md#rest-gateway) sends a notification to
every subscribed client in real-time.

WebSocket URIs share the same host and port as the HTTP requests URIs, but use the ``ws://`` protocol.
The endpoint is `/ws`, for example: `ws://localhost:3001/ws`

Both outgoing subscription messages and incoming updates use the JSON format and are described next.

!!! warning

    The WebSocket connection is dropped silently if idle for too long.

    Channels are not automatically resubscribed on reconnection.
