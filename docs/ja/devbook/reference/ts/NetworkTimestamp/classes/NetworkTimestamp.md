# Class: NetworkTimestamp

Represents a network timestamp.

## Extended by

- [`NetworkTimestamp`](../../nem/classes/NetworkTimestamp.md)

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

## Properties

| Property | Type | Description |
| ------ | ------ | ------ |
| <a id="timestamp"></a> `timestamp` | `bigint` | Underlying timestamp. |

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

`NetworkTimestamp`

New timestamp that is the specified number of hours past this timestamp.

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

`NetworkTimestamp`

New timestamp that is the specified number of minutes past this timestamp.

***

### addSeconds()

```ts
abstract addSeconds(count): NetworkTimestamp
```

Adds a specified number of seconds to this timestamp.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `count` | `number` \| `bigint` | Number of seconds to add. |

#### Returns

`NetworkTimestamp`

New timestamp that is the specified number of seconds past this timestamp.

***

### toString()

```ts
toString(): string
```

Returns string representation of this object.

#### Returns

`string`

String representation of this object
