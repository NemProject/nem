# Class: BaseValue

Represents a base integer.

## Extended by

- [`Amount`](../../nem/namespaces/models/classes/Amount.md)
- [`Height`](../../nem/namespaces/models/classes/Height.md)
- [`Timestamp`](../../nem/namespaces/models/classes/Timestamp.md)

## Constructors

### new BaseValue()

```ts
new BaseValue(
   size, 
   value, 
   isSigned?): BaseValue
```

Creates a base value.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `size` | `number` | Size of the integer. |
| `value` | `number` \| `bigint` | Value. |
| `isSigned`? | `boolean` | \c true if the value should be treated as signed. |

#### Returns

`BaseValue`

## Properties

| Property | Type | Description |
| ------ | ------ | ------ |
| <a id="issigned"></a> `isSigned` | `boolean` | \c true if the value should be treated as signed. |
| <a id="size"></a> `size` | `number` | Size of the integer. |
| <a id="value"></a> `value` | `number` \| `bigint` | Value. |

## Methods

### toJson()

```ts
toJson(): string | number
```

Returns representation of this object that can be stored in JSON.

#### Returns

`string` \| `number`

JSON-safe representation of this object.

***

### toString()

```ts
toString(): string
```

Converts base value to string.

#### Returns

`string`

String representation.
