# Class: NetworkTimestamp

Represents a NEM network timestamp with second resolution.

## Extends

- [`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md)

## Constructors

### new NetworkTimestamp()

```ts
new NetworkTimestamp(timestamp): NetworkTimestamp
```

Creates a timestamp.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `timestamp` | `number` \| `bigint` | Raw network timestamp. |

#### Returns

`NetworkTimestamp`

#### Inherited from

[`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md).[`constructor`](../../NetworkTimestamp/classes/NetworkTimestamp.md#constructor)

## Properties

| Property | Type | Description | Inherited from |
| ------ | ------ | ------ | ------ |
| <a id="timestamp"></a> `timestamp` | `bigint` | Underlying timestamp. | [`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md).[`timestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md#timestamp) |

## Accessors

### isEpochal

#### Get Signature

```ts
get isEpochal(): boolean
```

Determines if this is the epochal timestamp.

##### Returns

`boolean`

\c true if this is the epochal timestamp.

#### Inherited from

[`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md).[`isEpochal`](../../NetworkTimestamp/classes/NetworkTimestamp.md#isepochal)

## Methods

### addHours()

```ts
addHours(count): NetworkTimestamp
```

Adds a specified number of hours to this timestamp.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `count` | `number` \| `bigint` | Number of hours to add. |

#### Returns

[`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md)

New timestamp that is the specified number of hours past this timestamp.

#### Inherited from

[`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md).[`addHours`](../../NetworkTimestamp/classes/NetworkTimestamp.md#addhours)

***

### addMinutes()

```ts
addMinutes(count): NetworkTimestamp
```

Adds a specified number of minutes to this timestamp.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `count` | `number` \| `bigint` | Number of minutes to add. |

#### Returns

[`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md)

New timestamp that is the specified number of minutes past this timestamp.

#### Inherited from

[`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md).[`addMinutes`](../../NetworkTimestamp/classes/NetworkTimestamp.md#addminutes)

***

### addSeconds()

```ts
addSeconds(count): NetworkTimestamp
```

Adds a specified number of seconds to this timestamp.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `count` | `number` | Number of seconds to add. |

#### Returns

`NetworkTimestamp`

New timestamp that is the specified number of seconds past this timestamp.

#### Overrides

[`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md).[`addSeconds`](../../NetworkTimestamp/classes/NetworkTimestamp.md#addseconds)

***

### toString()

```ts
toString(): string
```

Returns string representation of this object.

#### Returns

`string`

String representation of this object

#### Inherited from

[`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md).[`toString`](../../NetworkTimestamp/classes/NetworkTimestamp.md#tostring)
