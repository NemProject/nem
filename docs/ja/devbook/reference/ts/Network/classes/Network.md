# Class: Network&lt;TAddress, TNetworkTimestamp&gt;

Represents a network.

## Extended by

- [`Network`](../../nem/classes/Network.md)

## Type Parameters

| Type Parameter | Description |
| ------ | ------ |
| `TAddress` *extends* `object` |  |
| `TNetworkTimestamp` *extends* [`NetworkTimestamp`](../../NetworkTimestamp/classes/NetworkTimestamp.md) |  |

## Constructors

### new Network()

```ts
new Network<TAddress, TNetworkTimestamp>(
   name, 
   identifier, 
   datetimeConverter, 
   addressHasher, 
   createAddress, 
   AddressClass, 
   NetworkTimestampClass): Network<TAddress, TNetworkTimestamp>
```

Creates a new network with the specified name and identifier byte.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `name` | `string` | Network name. |
| `identifier` | `number` | Network identifier byte. |
| `datetimeConverter` | [`NetworkTimestampDatetimeConverter`](../../NetworkTimestamp/classes/NetworkTimestampDatetimeConverter.md) | Network timestamp datetime converter associated with this network. |
| `addressHasher` | `Function` | Gets the primary hasher to use in the public key to address conversion. |
| `createAddress` | `Function` | Creates an encoded address from an address without checksum and checksum bytes. |
| `AddressClass` | [`AddressConstructable`](../type-aliases/AddressConstructable.md) | Address class associated with this network. |
| `NetworkTimestampClass` | [`Constructable`](../type-aliases/Constructable.md) | Network timestamp class associated with this network. |

#### Returns

`Network`&lt;`TAddress`, `TNetworkTimestamp`&gt;

## Properties

| Property | Type | Description |
| ------ | ------ | ------ |
| <a id="datetimeconverter"></a> `datetimeConverter` | [`NetworkTimestampDatetimeConverter`](../../NetworkTimestamp/classes/NetworkTimestampDatetimeConverter.md) | Network timestamp datetime converter associated with this network. |
| <a id="identifier"></a> `identifier` | `number` | Network identifier byte. |
| <a id="name"></a> `name` | `string` | Network name. |

## Methods

### fromDatetime()

```ts
fromDatetime(referenceDatetime): TNetworkTimestamp
```

Converts a datetime to a network timestamp.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `referenceDatetime` | `Date` | Reference datetime to convert. |

#### Returns

`TNetworkTimestamp`

Network timestamp representation of the reference datetime.

***

### isValidAddress()

```ts
isValidAddress(address): boolean
```

Checks if an address is valid and belongs to this network.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `address` | `TAddress` | Address to check. |

#### Returns

`boolean`

\c true if address is valid and belongs to this network.

***

### isValidAddressString()

```ts
isValidAddressString(addressString): boolean
```

Checks if an address string is valid and belongs to this network.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `addressString` | `string` | Address to check. |

#### Returns

`boolean`

\c true if address is valid and belongs to this network.

***

### publicKeyToAddress()

```ts
publicKeyToAddress(publicKey): TAddress
```

Converts a public key to an address.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `publicKey` | [`PublicKey`](../../index/classes/PublicKey.md) | Public key to convert. |

#### Returns

`TAddress`

Address corresponding to the public key input.

***

### toDatetime()

```ts
toDatetime(referenceNetworkTimestamp): Date
```

Converts a network timestamp to a datetime.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `referenceNetworkTimestamp` | `TNetworkTimestamp` | Reference network timestamp to convert. |

#### Returns

`Date`

Datetime representation of the reference network timestamp.

***

### toString()

```ts
toString(): string
```

Returns string representation of this object.

#### Returns

`string`

String representation of this object
