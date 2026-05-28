# Class: SizePrefixedMultisigAccountModification

## Constructors

### new SizePrefixedMultisigAccountModification()

```ts
new SizePrefixedMultisigAccountModification(): SizePrefixedMultisigAccountModification
```

#### Returns

`SizePrefixedMultisigAccountModification`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_modification"></a> `_modification` | `public` | [`MultisigAccountModification`](MultisigAccountModification.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.modification` | `public` | `string` |

## Accessors

### modification

#### Get Signature

```ts
get modification(): MultisigAccountModification
```

##### Returns

[`MultisigAccountModification`](MultisigAccountModification.md)

#### Set Signature

```ts
set modification(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`MultisigAccountModification`](MultisigAccountModification.md) |

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
static deserialize(payload): SizePrefixedMultisigAccountModification
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`SizePrefixedMultisigAccountModification`
