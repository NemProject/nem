# Class: Network

Represents a NEM network.

## Extends

- [`Network`](../../Network/classes/Network.md)&lt;`any`, `any`&gt;

## Constructors

### new Network()

```ts
new Network(
   name, 
   identifier, 
   epochTime): Network
```

Creates a new network with the specified name, identifier byte and generation hash seed.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `name` | `string` | Network name. |
| `identifier` | `number` | Network identifier byte. |
| `epochTime` | `Date` | Network epoch time. |

#### Returns

`Network`

#### Overrides

[`Network`](../../Network/classes/Network.md).[`constructor`](../../Network/classes/Network.md#constructor)

## Properties

| Property | Modifier | Type | Description | Inherited from |
| ------ | ------ | ------ | ------ | ------ |
| <a id="datetimeconverter"></a> `datetimeConverter` | `public` | [`NetworkTimestampDatetimeConverter`](../../NetworkTimestamp/classes/NetworkTimestampDatetimeConverter.md) | Network timestamp datetime converter associated with this network. | [`Network`](../../Network/classes/Network.md).[`datetimeConverter`](../../Network/classes/Network.md#datetimeconverter) |
| <a id="identifier"></a> `identifier` | `public` | `number` | Network identifier byte. | [`Network`](../../Network/classes/Network.md).[`identifier`](../../Network/classes/Network.md#identifier) |
| <a id="name"></a> `name` | `public` | `string` | Network name. | [`Network`](../../Network/classes/Network.md).[`name`](../../Network/classes/Network.md#name) |
| <a id="mainnet"></a> `MAINNET` | `static` | `Network` | NEM main network. | - |
| <a id="networks"></a> `NETWORKS` | `static` | `Network`[] | NEM well known networks. | - |
| <a id="testnet"></a> `TESTNET` | `static` | `Network` | NEM test network. | - |

## Methods

### fromDatetime()

```ts
fromDatetime(referenceDatetime): any
```

Converts a datetime to a network timestamp.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `referenceDatetime` | `Date` | Reference datetime to convert. |

#### Returns

`any`

Network timestamp representation of the reference datetime.

#### Inherited from

[`Network`](../../Network/classes/Network.md).[`fromDatetime`](../../Network/classes/Network.md#fromdatetime)

***

### isValidAddress()

```ts
isValidAddress(address): boolean
```

Checks if an address is valid and belongs to this network.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `address` | `any` | Address to check. |

#### Returns

`boolean`

\c true if address is valid and belongs to this network.

#### Inherited from

[`Network`](../../Network/classes/Network.md).[`isValidAddress`](../../Network/classes/Network.md#isvalidaddress)

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

#### Inherited from

[`Network`](../../Network/classes/Network.md).[`isValidAddressString`](../../Network/classes/Network.md#isvalidaddressstring)

***

### publicKeyToAddress()

```ts
publicKeyToAddress(publicKey): any
```

Converts a public key to an address.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `publicKey` | [`PublicKey`](../../index/classes/PublicKey.md) | Public key to convert. |

#### Returns

`any`

Address corresponding to the public key input.

#### Inherited from

[`Network`](../../Network/classes/Network.md).[`publicKeyToAddress`](../../Network/classes/Network.md#publickeytoaddress)

***

### toDatetime()

```ts
toDatetime(referenceNetworkTimestamp): Date
```

Converts a network timestamp to a datetime.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `referenceNetworkTimestamp` | `any` | Reference network timestamp to convert. |

#### Returns

`Date`

Datetime representation of the reference network timestamp.

#### Inherited from

[`Network`](../../Network/classes/Network.md).[`toDatetime`](../../Network/classes/Network.md#todatetime)

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

[`Network`](../../Network/classes/Network.md).[`toString`](../../Network/classes/Network.md#tostring)
