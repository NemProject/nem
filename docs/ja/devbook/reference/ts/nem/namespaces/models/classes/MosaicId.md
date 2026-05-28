# Class: MosaicId

## Constructors

### new MosaicId()

```ts
new MosaicId(): MosaicId
```

#### Returns

`MosaicId`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_name"></a> `_name` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; |
| <a id="_namespaceid"></a> `_namespaceId` | `public` | [`NamespaceId`](NamespaceId.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.name` | `public` | `string` |
| `TYPE_HINTS.namespaceId` | `public` | `string` |

## Accessors

### name

#### Get Signature

```ts
get name(): Uint8Array<ArrayBuffer>
```

##### Returns

`Uint8Array`&lt;`ArrayBuffer`&gt;

#### Set Signature

```ts
set name(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `Uint8Array`&lt;`ArrayBuffer`&gt; |

##### Returns

`void`

***

### namespaceId

#### Get Signature

```ts
get namespaceId(): NamespaceId
```

##### Returns

[`NamespaceId`](NamespaceId.md)

#### Set Signature

```ts
set namespaceId(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`NamespaceId`](NamespaceId.md) |

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
static deserialize(payload): MosaicId
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`MosaicId`
