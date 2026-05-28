# Class: Amount

Represents a base integer.

## Extends

- [`BaseValue`](../../../../index/classes/BaseValue.md)

## Constructors

### new Amount()

```ts
new Amount(amount?): Amount
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `amount`? | `bigint` |

#### Returns

`Amount`

#### Overrides

[`BaseValue`](../../../../index/classes/BaseValue.md).[`constructor`](../../../../index/classes/BaseValue.md#constructor)

## Properties

| Property | Modifier | Type | Description | Inherited from |
| ------ | ------ | ------ | ------ | ------ |
| <a id="issigned"></a> `isSigned` | `public` | `boolean` | \c true if the value should be treated as signed. | [`BaseValue`](../../../../index/classes/BaseValue.md).[`isSigned`](../../../../index/classes/BaseValue.md#issigned) |
| <a id="size"></a> `size` | `public` | `number` | Size of the integer. | [`BaseValue`](../../../../index/classes/BaseValue.md).[`size`](../../../../index/classes/BaseValue.md#size) |
| <a id="value"></a> `value` | `public` | `number` \| `bigint` | Value. | [`BaseValue`](../../../../index/classes/BaseValue.md).[`value`](../../../../index/classes/BaseValue.md#value) |
| <a id="size-1"></a> `SIZE` | `static` | `number` | - | - |

## Methods

### serialize()

```ts
serialize(): Uint8Array<ArrayBufferLike>
```

#### Returns

`Uint8Array`&lt;`ArrayBufferLike`&gt;

***

### toJson()

```ts
toJson(): string | number
```

Returns representation of this object that can be stored in JSON.

#### Returns

`string` \| `number`

JSON-safe representation of this object.

#### Inherited from

[`BaseValue`](../../../../index/classes/BaseValue.md).[`toJson`](../../../../index/classes/BaseValue.md#tojson)

***

### toString()

```ts
toString(): string
```

Converts base value to string.

#### Returns

`string`

String representation.

#### Inherited from

[`BaseValue`](../../../../index/classes/BaseValue.md).[`toString`](../../../../index/classes/BaseValue.md#tostring)

***

### deserialize()

```ts
static deserialize(payload): Amount
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`Amount`

***

### deserializeAligned()

```ts
static deserializeAligned(payload): Amount
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`Amount`
