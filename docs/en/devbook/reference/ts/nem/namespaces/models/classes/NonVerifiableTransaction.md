# Class: NonVerifiableTransaction

## Extended by

- [`NonVerifiableAccountKeyLinkTransactionV1`](NonVerifiableAccountKeyLinkTransactionV1.md)
- [`NonVerifiableMosaicDefinitionTransactionV1`](NonVerifiableMosaicDefinitionTransactionV1.md)
- [`NonVerifiableMosaicSupplyChangeTransactionV1`](NonVerifiableMosaicSupplyChangeTransactionV1.md)
- [`NonVerifiableMultisigAccountModificationTransactionV1`](NonVerifiableMultisigAccountModificationTransactionV1.md)
- [`NonVerifiableMultisigAccountModificationTransactionV2`](NonVerifiableMultisigAccountModificationTransactionV2.md)
- [`NonVerifiableCosignatureV1`](NonVerifiableCosignatureV1.md)
- [`NonVerifiableMultisigTransactionV1`](NonVerifiableMultisigTransactionV1.md)
- [`NonVerifiableNamespaceRegistrationTransactionV1`](NonVerifiableNamespaceRegistrationTransactionV1.md)
- [`NonVerifiableTransferTransactionV1`](NonVerifiableTransferTransactionV1.md)
- [`NonVerifiableTransferTransactionV2`](NonVerifiableTransferTransactionV2.md)

## Constructors

### new NonVerifiableTransaction()

```ts
new NonVerifiableTransaction(): NonVerifiableTransaction
```

#### Returns

`NonVerifiableTransaction`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_deadline"></a> `_deadline` | `public` | [`Timestamp`](Timestamp.md) |
| <a id="_entitybodyreserved_1"></a> `_entityBodyReserved_1` | `public` | `number` |
| <a id="_fee"></a> `_fee` | `public` | [`Amount`](Amount.md) |
| <a id="_network"></a> `_network` | `public` | [`NetworkType`](NetworkType.md) |
| <a id="_signerpublickey"></a> `_signerPublicKey` | `public` | [`PublicKey`](PublicKey.md) |
| <a id="_signerpublickeysize"></a> `_signerPublicKeySize` | `public` | `number` |
| <a id="_timestamp"></a> `_timestamp` | `public` | [`Timestamp`](Timestamp.md) |
| <a id="_type"></a> `_type` | `public` | [`TransactionType`](TransactionType.md) |
| <a id="_version"></a> `_version` | `public` | `number` |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.deadline` | `public` | `string` |
| `TYPE_HINTS.fee` | `public` | `string` |
| `TYPE_HINTS.network` | `public` | `string` |
| `TYPE_HINTS.signerPublicKey` | `public` | `string` |
| `TYPE_HINTS.timestamp` | `public` | `string` |
| `TYPE_HINTS.type` | `public` | `string` |

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

***

### size

#### Get Signature

```ts
get size(): number
```

##### Returns

`number`

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

***

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
