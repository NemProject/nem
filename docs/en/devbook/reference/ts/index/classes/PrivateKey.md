# Class: PrivateKey

Represents a private key.

## Extends

- [`ByteArray`](ByteArray.md)

## Constructors

### new PrivateKey()

```ts
new PrivateKey(privateKey): PrivateKey
```

Creates a private key from bytes or a hex string.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `privateKey` | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | Input string or byte array. |

#### Returns

`PrivateKey`

#### Overrides

[`ByteArray`](ByteArray.md).[`constructor`](ByteArray.md#constructor)

## Properties

| Property | Modifier | Type | Description | Inherited from |
| ------ | ------ | ------ | ------ | ------ |
| <a id="bytes"></a> `bytes` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; | Underlying bytes. | [`ByteArray`](ByteArray.md).[`bytes`](ByteArray.md#bytes) |
| <a id="name"></a> `NAME` | `static` | `string` | Byte array name (required because `constructor.name` is dropped during minification). | [`ByteArray`](ByteArray.md).[`NAME`](ByteArray.md#name) |
| <a id="size"></a> `SIZE` | `static` | `number` | Byte size of raw private key. | - |

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

***

### random()

```ts
static random(): PrivateKey
```

Creates a random private key.

#### Returns

`PrivateKey`

Random private key.
