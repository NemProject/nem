# Class: NonVerifiableAccountKeyLinkTransactionV1

## Extends

- [`NonVerifiableTransaction`](NonVerifiableTransaction.md)

## Constructors

### new NonVerifiableAccountKeyLinkTransactionV1()

```ts
new NonVerifiableAccountKeyLinkTransactionV1(): NonVerifiableAccountKeyLinkTransactionV1
```

#### Returns

`NonVerifiableAccountKeyLinkTransactionV1`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`constructor`](NonVerifiableTransaction.md#constructor)

## Properties

| Property | Modifier | Type | Overrides | Inherited from |
| ------ | ------ | ------ | ------ | ------ |
| <a id="_deadline"></a> `_deadline` | `public` | [`Timestamp`](Timestamp.md) | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_deadline`](NonVerifiableTransaction.md#_deadline) |
| <a id="_entitybodyreserved_1"></a> `_entityBodyReserved_1` | `public` | `number` | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_entityBodyReserved_1`](NonVerifiableTransaction.md#_entitybodyreserved_1) |
| <a id="_fee"></a> `_fee` | `public` | [`Amount`](Amount.md) | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_fee`](NonVerifiableTransaction.md#_fee) |
| <a id="_linkaction"></a> `_linkAction` | `public` | [`LinkAction`](LinkAction.md) | - | - |
| <a id="_network"></a> `_network` | `public` | [`NetworkType`](NetworkType.md) | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_network`](NonVerifiableTransaction.md#_network) |
| <a id="_remotepublickey"></a> `_remotePublicKey` | `public` | [`PublicKey`](PublicKey.md) | - | - |
| <a id="_remotepublickeysize"></a> `_remotePublicKeySize` | `public` | `number` | - | - |
| <a id="_signerpublickey"></a> `_signerPublicKey` | `public` | [`PublicKey`](PublicKey.md) | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_signerPublicKey`](NonVerifiableTransaction.md#_signerpublickey) |
| <a id="_signerpublickeysize"></a> `_signerPublicKeySize` | `public` | `number` | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_signerPublicKeySize`](NonVerifiableTransaction.md#_signerpublickeysize) |
| <a id="_timestamp"></a> `_timestamp` | `public` | [`Timestamp`](Timestamp.md) | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_timestamp`](NonVerifiableTransaction.md#_timestamp) |
| <a id="_type"></a> `_type` | `public` | [`TransactionType`](TransactionType.md) | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_type`](NonVerifiableTransaction.md#_type) |
| <a id="_version"></a> `_version` | `public` | `number` | - | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_version`](NonVerifiableTransaction.md#_version) |
| <a id="transaction_type"></a> `TRANSACTION_TYPE` | `static` | [`TransactionType`](TransactionType.md) | - | - |
| <a id="transaction_version"></a> `TRANSACTION_VERSION` | `static` | `number` | - | - |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` | [`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`TYPE_HINTS`](NonVerifiableTransaction.md#type_hints) | - |
| `TYPE_HINTS.deadline` | `public` | `string` | - | - |
| `TYPE_HINTS.fee` | `public` | `string` | - | - |
| `TYPE_HINTS.linkAction` | `public` | `string` | - | - |
| `TYPE_HINTS.network` | `public` | `string` | - | - |
| `TYPE_HINTS.remotePublicKey` | `public` | `string` | - | - |
| `TYPE_HINTS.signerPublicKey` | `public` | `string` | - | - |
| `TYPE_HINTS.timestamp` | `public` | `string` | - | - |
| `TYPE_HINTS.type` | `public` | `string` | - | - |

## Accessors

### deadline

#### Get Signature

```ts
get deadline(): Timestamp
```

##### Returns

[`Timestamp`](Timestamp.md)

#### Set Signature

```ts
set deadline(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`Timestamp`](Timestamp.md) |

##### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`deadline`](NonVerifiableTransaction.md#deadline)

***

### fee

#### Get Signature

```ts
get fee(): Amount
```

##### Returns

[`Amount`](Amount.md)

#### Set Signature

```ts
set fee(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`Amount`](Amount.md) |

##### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`fee`](NonVerifiableTransaction.md#fee)

***

### linkAction

#### Get Signature

```ts
get linkAction(): LinkAction
```

##### Returns

[`LinkAction`](LinkAction.md)

#### Set Signature

```ts
set linkAction(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`LinkAction`](LinkAction.md) |

##### Returns

`void`

***

### network

#### Get Signature

```ts
get network(): NetworkType
```

##### Returns

[`NetworkType`](NetworkType.md)

#### Set Signature

```ts
set network(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`NetworkType`](NetworkType.md) |

##### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`network`](NonVerifiableTransaction.md#network)

***

### remotePublicKey

#### Get Signature

```ts
get remotePublicKey(): PublicKey
```

##### Returns

[`PublicKey`](PublicKey.md)

#### Set Signature

```ts
set remotePublicKey(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`PublicKey`](PublicKey.md) |

##### Returns

`void`

***

### signerPublicKey

#### Get Signature

```ts
get signerPublicKey(): PublicKey
```

##### Returns

[`PublicKey`](PublicKey.md)

#### Set Signature

```ts
set signerPublicKey(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`PublicKey`](PublicKey.md) |

##### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`signerPublicKey`](NonVerifiableTransaction.md#signerpublickey)

***

### size

#### Get Signature

```ts
get size(): number
```

##### Returns

`number`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`size`](NonVerifiableTransaction.md#size)

***

### timestamp

#### Get Signature

```ts
get timestamp(): Timestamp
```

##### Returns

[`Timestamp`](Timestamp.md)

#### Set Signature

```ts
set timestamp(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`Timestamp`](Timestamp.md) |

##### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`timestamp`](NonVerifiableTransaction.md#timestamp)

***

### type

#### Get Signature

```ts
get type(): TransactionType
```

##### Returns

[`TransactionType`](TransactionType.md)

#### Set Signature

```ts
set type(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`TransactionType`](TransactionType.md) |

##### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`type`](NonVerifiableTransaction.md#type)

***

### version

#### Get Signature

```ts
get version(): number
```

##### Returns

`number`

#### Set Signature

```ts
set version(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `number` |

##### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`version`](NonVerifiableTransaction.md#version)

## Methods

### \_serialize()

```ts
_serialize(buffer): void
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `buffer` | `any` |

#### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_serialize`](NonVerifiableTransaction.md#_serialize)

***

### serialize()

```ts
serialize(): Uint8Array<ArrayBufferLike>
```

#### Returns

`Uint8Array`&lt;`ArrayBufferLike`&gt;

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`serialize`](NonVerifiableTransaction.md#serialize)

***

### sort()

```ts
sort(): void
```

#### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`sort`](NonVerifiableTransaction.md#sort)

***

### toJson()

```ts
toJson(): object
```

#### Returns

`object`

JSON-safe representation of this object.

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`toJson`](NonVerifiableTransaction.md#tojson)

***

### toString()

```ts
toString(): string
```

#### Returns

`string`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`toString`](NonVerifiableTransaction.md#tostring)

***

### \_deserialize()

```ts
static _deserialize(view, instance): void
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `view` | `any` |
| `instance` | `any` |

#### Returns

`void`

#### Inherited from

[`NonVerifiableTransaction`](NonVerifiableTransaction.md).[`_deserialize`](NonVerifiableTransaction.md#_deserialize)

***

### deserialize()

```ts
static deserialize(payload): NonVerifiableAccountKeyLinkTransactionV1
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`NonVerifiableAccountKeyLinkTransactionV1`
