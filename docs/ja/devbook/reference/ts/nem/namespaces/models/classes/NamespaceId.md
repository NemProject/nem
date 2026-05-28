# Class: NamespaceId

## Constructors

### new NamespaceId()

```ts
new NamespaceId(): NamespaceId
```

#### Returns

`NamespaceId`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_name"></a> `_name` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.name` | `public` | `string` |

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
static deserialize(payload): NamespaceId
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`NamespaceId`
