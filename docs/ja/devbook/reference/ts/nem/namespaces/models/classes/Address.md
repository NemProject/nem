# Class: Address

Represents a fixed size byte array.

## Extends

- [`ByteArray`](../../../../index/classes/ByteArray.md)

## Constructors

### new Address()

```ts
new Address(address?): Address
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `address`? | `Uint8Array`&lt;`ArrayBuffer`&gt; |

#### Returns

`Address`

#### Overrides

[`ByteArray`](../../../../index/classes/ByteArray.md).[`constructor`](../../../../index/classes/ByteArray.md#constructor)

## Properties

| Property | Modifier | Type | Description | Inherited from |
| ------ | ------ | ------ | ------ | ------ |
| <a id="bytes"></a> `bytes` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; | Underlying bytes. | [`ByteArray`](../../../../index/classes/ByteArray.md).[`bytes`](../../../../index/classes/ByteArray.md#bytes) |
| <a id="name"></a> `NAME` | `static` | `string` | Byte array name (required because `constructor.name` is dropped during minification). | [`ByteArray`](../../../../index/classes/ByteArray.md).[`NAME`](../../../../index/classes/ByteArray.md#name) |
| <a id="size"></a> `SIZE` | `static` | `number` | - | - |

## Accessors

### size

#### Get Signature

```ts
get size(): number
```

##### Returns

`number`

## Methods

### serialize()

```ts
serialize(): Uint8Array<ArrayBuffer>
```

#### Returns

`Uint8Array`&lt;`ArrayBuffer`&gt;

***

### toJson()

```ts
toJson(): string
```

Returns representation of this object that can be stored in JSON.

#### Returns

`string`

JSON-safe representation of this object.

#### Inherited from

[`ByteArray`](../../../../index/classes/ByteArray.md).[`toJson`](../../../../index/classes/ByteArray.md#tojson)

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

[`ByteArray`](../../../../index/classes/ByteArray.md).[`toString`](../../../../index/classes/ByteArray.md#tostring)

***

### deserialize()

```ts
static deserialize(payload): Address
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`Address`
