# Class: MultisigAccountModification

## Constructors

### new MultisigAccountModification()

```ts
new MultisigAccountModification(): MultisigAccountModification
```

#### Returns

`MultisigAccountModification`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_cosignatorypublickey"></a> `_cosignatoryPublicKey` | `public` | [`PublicKey`](PublicKey.md) |
| <a id="_cosignatorypublickeysize"></a> `_cosignatoryPublicKeySize` | `public` | `number` |
| <a id="_modificationtype"></a> `_modificationType` | `public` | [`MultisigAccountModificationType`](MultisigAccountModificationType.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.cosignatoryPublicKey` | `public` | `string` |
| `TYPE_HINTS.modificationType` | `public` | `string` |

## Accessors

### cosignatoryPublicKey

#### Get Signature

```ts
get cosignatoryPublicKey(): PublicKey
```

##### Returns

[`PublicKey`](PublicKey.md)

#### Set Signature

```ts
set cosignatoryPublicKey(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`PublicKey`](PublicKey.md) |

##### Returns

`void`

***

### modificationType

#### Get Signature

```ts
get modificationType(): MultisigAccountModificationType
```

##### Returns

[`MultisigAccountModificationType`](MultisigAccountModificationType.md)

#### Set Signature

```ts
set modificationType(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`MultisigAccountModificationType`](MultisigAccountModificationType.md) |

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

### comparer()

```ts
comparer(): (
  | Uint8Array<ArrayBufferLike>
  | MultisigAccountModificationType)[]
```

#### Returns

(
  \| `Uint8Array`&lt;`ArrayBufferLike`&gt;
  \| [`MultisigAccountModificationType`](MultisigAccountModificationType.md))[]

***

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
static deserialize(payload): MultisigAccountModification
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`MultisigAccountModification`
