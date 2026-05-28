# Class: Message

## Constructors

### new Message()

```ts
new Message(): Message
```

#### Returns

`Message`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_message"></a> `_message` | `public` | `Uint8Array`&lt;`ArrayBuffer`&gt; |
| <a id="_messagetype"></a> `_messageType` | `public` | [`MessageType`](MessageType.md) |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.message` | `public` | `string` |
| `TYPE_HINTS.messageType` | `public` | `string` |

## Accessors

### message

#### Get Signature

```ts
get message(): Uint8Array<ArrayBuffer>
```

##### Returns

`Uint8Array`&lt;`ArrayBuffer`&gt;

#### Set Signature

```ts
set message(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `Uint8Array`&lt;`ArrayBuffer`&gt; |

##### Returns

`void`

***

### messageType

#### Get Signature

```ts
get messageType(): MessageType
```

##### Returns

[`MessageType`](MessageType.md)

#### Set Signature

```ts
set messageType(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`MessageType`](MessageType.md) |

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
static deserialize(payload): Message
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`Message`
