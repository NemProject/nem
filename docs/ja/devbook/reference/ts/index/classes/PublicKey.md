# Class: PublicKey

Represents a public key.

## Extends

- [`ByteArray`](ByteArray.md)

## Constructors

### new PublicKey()

```ts
new PublicKey(publicKey): PublicKey
```

Creates a public key from bytes or a hex string.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `publicKey` | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; \| `PublicKey` | Input string, byte array or public key. |

#### Returns

`PublicKey`

#### Overrides

[`ByteArray`](ByteArray.md).[`constructor`](ByteArray.md#constructor)

## Properties

| Property | Modifier | Type | Description | Inherited from |
| ------ | ------ | ------ | ------ | ------ |
| <a id="bytes"></a> `bytes` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; | Underlying bytes. | [`ByteArray`](ByteArray.md).[`bytes`](ByteArray.md#bytes) |
| <a id="name"></a> `NAME` | `static` | `string` | Byte array name (required because `constructor.name` is dropped during minification). | [`ByteArray`](ByteArray.md).[`NAME`](ByteArray.md#name) |
| <a id="size"></a> `SIZE` | `static` | `number` | Byte size of raw public key. | - |

## Methods

### toJson()

```ts
toJson(): string
```

Returns representation of this object that can be stored in JSON.

#### Returns

`string`

JSON-safe representation of this object.

#### Inherited from

[`ByteArray`](ByteArray.md).[`toJson`](ByteArray.md#tojson)

***

### toString()

```ts
toString(): string
```

Returns string representation of this object.

#### Returns

`string`

String representation of this object

#### Inherited from

[`ByteArray`](ByteArray.md).[`toString`](ByteArray.md#tostring)
