# Class: ByteArray

Represents a fixed size byte array.

## Extended by

- [`Hash256`](Hash256.md)
- [`PrivateKey`](PrivateKey.md)
- [`PublicKey`](PublicKey.md)
- [`SharedKey256`](SharedKey256.md)
- [`Signature`](Signature.md)
- [`Address`](../../nem/classes/Address.md)
- [`Address`](../../nem/namespaces/models/classes/Address.md)
- [`Hash256`](../../nem/namespaces/models/classes/Hash256.md)
- [`PublicKey`](../../nem/namespaces/models/classes/PublicKey.md)
- [`Signature`](../../nem/namespaces/models/classes/Signature.md)

## Constructors

### new ByteArray()

```ts
new ByteArray(fixedSize, arrayInput): ByteArray
```

Creates a byte array.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `fixedSize` | `number` | Size of the array. |
| `arrayInput` | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | Byte array or hex string. |

#### Returns

`ByteArray`

## Properties

| Property | Modifier | Type | Description |
| ------ | ------ | ------ | ------ |
| <a id="bytes"></a> `bytes` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; | Underlying bytes. |
| <a id="name"></a> `NAME` | `static` | `string` | Byte array name (required because `constructor.name` is dropped during minification). |

## Methods

### toJson()

```ts
toJson(): string
```

Returns representation of this object that can be stored in JSON.

#### Returns

`string`

JSON-safe representation of this object.

***

### toString()

```ts
toString(): string
```

Returns string representation of this object.

#### Returns

`string`

String representation of this object
