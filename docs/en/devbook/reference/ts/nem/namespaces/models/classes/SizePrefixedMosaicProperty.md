# Class: SizePrefixedMosaicProperty

## Constructors

### new SizePrefixedMosaicProperty()

```ts
new SizePrefixedMosaicProperty(): SizePrefixedMosaicProperty
```

#### Returns

`SizePrefixedMosaicProperty`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_property"></a> `_property` | `public` | [`MosaicProperty`](MosaicProperty.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.property` | `public` | `string` |

## Accessors

### property

#### Get Signature

```ts
get property(): MosaicProperty
```

##### Returns

[`MosaicProperty`](MosaicProperty.md)

#### Set Signature

```ts
set property(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`MosaicProperty`](MosaicProperty.md) |

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
static deserialize(payload): SizePrefixedMosaicProperty
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`SizePrefixedMosaicProperty`
