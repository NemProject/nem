# Class: Address

Represents a NEM address.

## Extends

- [`ByteArray`](../../index/classes/ByteArray.md)

## Constructors

### new Address()

```ts
new Address(addressInput): Address
```

Creates a NEM address.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `addressInput` | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; \| `Address` | Input string, byte array or address. |

#### Returns

`Address`

#### Overrides

[`ByteArray`](../../index/classes/ByteArray.md).[`constructor`](../../index/classes/ByteArray.md#constructor)

## Properties

| Property | Modifier | Type | Description | Inherited from |
| ------ | ------ | ------ | ------ | ------ |
| <a id="bytes"></a> `bytes` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; | Underlying bytes. | [`ByteArray`](../../index/classes/ByteArray.md).[`bytes`](../../index/classes/ByteArray.md#bytes) |
| <a id="encoded_size"></a> `ENCODED_SIZE` | `static` | `number` | Length of encoded address string. | - |
| <a id="name"></a> `NAME` | `static` | `string` | Byte array name (required because `constructor.name` is dropped during minification). | [`ByteArray`](../../index/classes/ByteArray.md).[`NAME`](../../index/classes/ByteArray.md#name) |
| <a id="size"></a> `SIZE` | `static` | `number` | Byte size of raw address. | - |

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

[`ByteArray`](../../index/classes/ByteArray.md).[`toJson`](../../index/classes/ByteArray.md#tojson)

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

[`ByteArray`](../../index/classes/ByteArray.md).[`toString`](../../index/classes/ByteArray.md#tostring)
