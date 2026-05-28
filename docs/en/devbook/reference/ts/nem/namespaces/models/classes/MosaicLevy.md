# Class: MosaicLevy

## Constructors

### new MosaicLevy()

```ts
new MosaicLevy(): MosaicLevy
```

#### Returns

`MosaicLevy`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_fee"></a> `_fee` | `public` | [`Amount`](Amount.md) |
| <a id="_mosaicid"></a> `_mosaicId` | `public` | [`MosaicId`](MosaicId.md) |
| <a id="_recipientaddress"></a> `_recipientAddress` | `public` | [`Address`](Address.md) |
| <a id="_recipientaddresssize"></a> `_recipientAddressSize` | `public` | `number` |
| <a id="_transferfeetype"></a> `_transferFeeType` | `public` | [`MosaicTransferFeeType`](MosaicTransferFeeType.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.fee` | `public` | `string` |
| `TYPE_HINTS.mosaicId` | `public` | `string` |
| `TYPE_HINTS.recipientAddress` | `public` | `string` |
| `TYPE_HINTS.transferFeeType` | `public` | `string` |

## Accessors

### fee

#### Get Signature

```ts
get fee(): Amount
```

##### Returns

[`Amount`](Amount.md)

#### Set Signature

```ts
set fee(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`Amount`](Amount.md) |

##### Returns

`void`

***

### mosaicId

#### Get Signature

```ts
get mosaicId(): MosaicId
```

##### Returns

[`MosaicId`](MosaicId.md)

#### Set Signature

```ts
set mosaicId(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`MosaicId`](MosaicId.md) |

##### Returns

`void`

***

### recipientAddress

#### Get Signature

```ts
get recipientAddress(): Address
```

##### Returns

[`Address`](Address.md)

#### Set Signature

```ts
set recipientAddress(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`Address`](Address.md) |

##### Returns

`void`

***

### size

#### Get Signature

```ts
get size(): number
```

##### Returns

`number`

***

### transferFeeType

#### Get Signature

```ts
get transferFeeType(): MosaicTransferFeeType
```

##### Returns

[`MosaicTransferFeeType`](MosaicTransferFeeType.md)

#### Set Signature

```ts
set transferFeeType(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`MosaicTransferFeeType`](MosaicTransferFeeType.md) |

##### Returns

`void`

## Methods

### serialize()

```ts
serialize(): Uint8Array<ArrayBufferLike>
```

#### Returns

`Uint8Array`&lt;`ArrayBufferLike`&gt;

***

### sort()

```ts
sort(): void
```

#### Returns

`void`

***

### toJson()

```ts
toJson(): object
```

#### Returns

`object`

JSON-safe representation of this object.

***

### toString()

```ts
toString(): string
```

#### Returns

`string`

***

### deserialize()

```ts
static deserialize(payload): MosaicLevy
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`MosaicLevy`
