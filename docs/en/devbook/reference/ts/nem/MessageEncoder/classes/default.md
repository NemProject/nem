# Class: default

Encrypts and encodes messages between two parties.

## Constructors

### new default()

```ts
new default(keyPair): MessageEncoder
```

Creates message encoder around key pair.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `keyPair` | [`KeyPair`](../../classes/KeyPair.md) | Key pair. |

#### Returns

`MessageEncoder`

## Accessors

### publicKey

#### Get Signature

```ts
get publicKey(): PublicKey
```

Public key used for message encoding.

##### Returns

[`PublicKey`](../../../index/classes/PublicKey.md)

Public key used for message encoding.

## Methods

### encode()

```ts
encode(recipientPublicKey, message): Message
```

Encodes message to recipient using recommended format.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `recipientPublicKey` | [`PublicKey`](../../../index/classes/PublicKey.md) | Recipient public key. |
| `message` | `Uint8Array` | Message to encode. |

#### Returns

[`Message`](../../namespaces/models/classes/Message.md)

Encrypted and encoded message.

***

### ~~encodeDeprecated()~~

```ts
encodeDeprecated(recipientPublicKey, message): Message
```

Encodes message to recipient using recommended format.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `recipientPublicKey` | [`PublicKey`](../../../index/classes/PublicKey.md) | Recipient public key. |
| `message` | `Uint8Array` | Message to encode. |

#### Returns

[`Message`](../../namespaces/models/classes/Message.md)

Encrypted and encoded message.

#### Deprecated

This function is only provided for compatability with older NEM messages.
            Please use `encode` in any new code.

***

### tryDecode()

```ts
tryDecode(recipientPublicKey, encodedMessage): TryDecodeResult
```

Tries to decode encoded message.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `recipientPublicKey` | [`PublicKey`](../../../index/classes/PublicKey.md) | Recipient public key. |
| `encodedMessage` | [`Message`](../../namespaces/models/classes/Message.md) | Encoded message. |

#### Returns

[`TryDecodeResult`](../type-aliases/TryDecodeResult.md)

Tuple containing decoded status and message.
