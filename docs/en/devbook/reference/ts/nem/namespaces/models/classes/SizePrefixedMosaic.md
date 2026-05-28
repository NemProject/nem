# Class: SizePrefixedMosaic

## Constructors

### new SizePrefixedMosaic()

```ts
new SizePrefixedMosaic(): SizePrefixedMosaic
```

#### Returns

`SizePrefixedMosaic`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_mosaic"></a> `_mosaic` | `public` | [`Mosaic`](Mosaic.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.mosaic` | `public` | `string` |

## Accessors

### mosaic

#### Get Signature

```ts
get mosaic(): Mosaic
```

##### Returns

[`Mosaic`](Mosaic.md)

#### Set Signature

```ts
set mosaic(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`Mosaic`](Mosaic.md) |

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
static deserialize(payload): SizePrefixedMosaic
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`SizePrefixedMosaic`
