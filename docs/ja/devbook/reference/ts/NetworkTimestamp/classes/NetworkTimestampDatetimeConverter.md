# Class: NetworkTimestampDatetimeConverter

Provides utilities for converting between network timestamps and datetimes.

## Constructors

### new NetworkTimestampDatetimeConverter()

```ts
new NetworkTimestampDatetimeConverter(epoch, timeUnits): NetworkTimestampDatetimeConverter
```

Creates a converter given an epoch and base time units.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `epoch` | `Date` | Date at which network started. |
| `timeUnits` | `string` | Time unit the network uses for progressing. |

#### Returns

`NetworkTimestampDatetimeConverter`

## Properties

| Property | Type | Description |
| ------ | ------ | ------ |
| <a id="epoch"></a> `epoch` | `Date` | Date at which network started |
| <a id="timeunits"></a> `timeUnits` | `number` | Number of milliseconds per time unit. |

## Methods

### toDatetime()

```ts
toDatetime(rawTimestamp): Date
```

Converts a network timestamp to a datetime.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `rawTimestamp` | `number` | Raw network timestamp. |

#### Returns

`Date`

Date representation of the network timestamp.

***

### toDifference()

```ts
toDifference(referenceDatetime): number
```

Subtracts the network epoch from the reference date.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `referenceDatetime` | `Date` | Reference date. |

#### Returns

`number`

Number of network time units between the reference date and the network epoch.
