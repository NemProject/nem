# Class: MosaicProperty

## Constructors

### new MosaicProperty()

```ts
new MosaicProperty(): MosaicProperty
```

#### Returns

`MosaicProperty`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_name"></a> `_name` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; |
| <a id="_value"></a> `_value` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.name` | `public` | `string` |
| `TYPE_HINTS.value` | `public` | `string` |

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

***

### value

#### Get Signature

```ts
get value(): Uint8Array<ArrayBuffer>
```

##### Returns

`Uint8Array`&lt;`ArrayBuffer`&gt;

#### Set Signature

```ts
set value(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `Uint8Array`&lt;`ArrayBuffer`&gt; |

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
static deserialize(payload): MosaicProperty
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`MosaicProperty`
