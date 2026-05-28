# Class: MosaicDefinition

## Constructors

### new MosaicDefinition()

```ts
new MosaicDefinition(): MosaicDefinition
```

#### Returns

`MosaicDefinition`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_description"></a> `_description` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; |
| <a id="_id"></a> `_id` | `public` | [`MosaicId`](MosaicId.md) |
| <a id="_levy"></a> `_levy` | `public` | `any` |
| <a id="_ownerpublickey"></a> `_ownerPublicKey` | `public` | [`PublicKey`](PublicKey.md) |
| <a id="_ownerpublickeysize"></a> `_ownerPublicKeySize` | `public` | `number` |
| <a id="_properties"></a> `_properties` | `public` | `any`[] |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.description` | `public` | `string` |
| `TYPE_HINTS.id` | `public` | `string` |
| `TYPE_HINTS.levy` | `public` | `string` |
| `TYPE_HINTS.ownerPublicKey` | `public` | `string` |
| `TYPE_HINTS.properties` | `public` | `string` |

## Accessors

### description

#### Get Signature

```ts
get description(): Uint8Array<ArrayBuffer>
```

##### Returns

`Uint8Array`&lt;`ArrayBuffer`&gt;

#### Set Signature

```ts
set description(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `Uint8Array`&lt;`ArrayBuffer`&gt; |

##### Returns

`void`

***

### id

#### Get Signature

```ts
get id(): MosaicId
```

##### Returns

[`MosaicId`](MosaicId.md)

#### Set Signature

```ts
set id(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`MosaicId`](MosaicId.md) |

##### Returns

`void`

***

### levy

#### Get Signature

```ts
get levy(): any
```

##### Returns

`any`

#### Set Signature

```ts
set levy(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `any` |

##### Returns

`void`

***

### levySizeComputed

#### Get Signature

```ts
get levySizeComputed(): any
```

##### Returns

`any`

***

### ownerPublicKey

#### Get Signature

```ts
get ownerPublicKey(): PublicKey
```

##### Returns

[`PublicKey`](PublicKey.md)

#### Set Signature

```ts
set ownerPublicKey(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`PublicKey`](PublicKey.md) |

##### Returns

`void`

***

### properties

#### Get Signature

```ts
get properties(): any[]
```

##### Returns

`any`[]

#### Set Signature

```ts
set properties(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `any`[] |

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
static deserialize(payload): MosaicDefinition
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`MosaicDefinition`
