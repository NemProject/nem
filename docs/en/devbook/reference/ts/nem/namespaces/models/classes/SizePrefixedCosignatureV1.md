# Class: SizePrefixedCosignatureV1

## Constructors

### new SizePrefixedCosignatureV1()

```ts
new SizePrefixedCosignatureV1(): SizePrefixedCosignatureV1
```

#### Returns

`SizePrefixedCosignatureV1`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_cosignature"></a> `_cosignature` | `public` | [`CosignatureV1`](CosignatureV1.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.cosignature` | `public` | `string` |

## Accessors

### cosignature

#### Get Signature

```ts
get cosignature(): CosignatureV1
```

##### Returns

[`CosignatureV1`](CosignatureV1.md)

#### Set Signature

```ts
set cosignature(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`CosignatureV1`](CosignatureV1.md) |

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
static deserialize(payload): SizePrefixedCosignatureV1
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`SizePrefixedCosignatureV1`
