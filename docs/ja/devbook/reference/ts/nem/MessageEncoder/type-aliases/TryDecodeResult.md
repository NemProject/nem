# Type Alias: TryDecodeResult

```ts
type TryDecodeResult = object;
```

Result of a try decode operation.

## Properties

| Property | Type | Description |
| ------ | ------ | ------ |
| <a id="isdecoded"></a> `isDecoded` | `boolean` | \c true if message has been decoded and decrypted; \c false otherwise. |
| <a id="message"></a> `message` | `Uint8Array` \| [`Message`](../../namespaces/models/classes/Message.md) | Decoded message when `isDecoded` is \c true; encoded message otherwise. |
