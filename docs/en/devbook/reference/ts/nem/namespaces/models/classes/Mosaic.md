# Class: Mosaic

## Constructors

### new Mosaic()

```ts
new Mosaic(): Mosaic
```

#### Returns

`Mosaic`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_amount"></a> `_amount` | `public` | [`Amount`](Amount.md) |
| <a id="_mosaicid"></a> `_mosaicId` | `public` | [`MosaicId`](MosaicId.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.amount` | `public` | `string` |
| `TYPE_HINTS.mosaicId` | `public` | `string` |

## Accessors

### amount

#### Get Signature

```ts
get amount(): Amount
```

##### Returns

[`Amount`](Amount.md)

#### Set Signature

```ts
set amount(value): void
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
static deserialize(payload): Mosaic
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`Mosaic`
