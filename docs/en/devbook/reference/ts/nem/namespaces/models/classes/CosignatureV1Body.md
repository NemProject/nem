# Class: CosignatureV1Body

## Constructors

### new CosignatureV1Body()

```ts
new CosignatureV1Body(): CosignatureV1Body
```

#### Returns

`CosignatureV1Body`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="_multisigaccountaddress"></a> `_multisigAccountAddress` | `public` | [`Address`](Address.md) |
| <a id="_multisigaccountaddresssize"></a> `_multisigAccountAddressSize` | `public` | `number` |
| <a id="_othertransactionhash"></a> `_otherTransactionHash` | `public` | [`Hash256`](Hash256.md) |
| <a id="_othertransactionhashoutersize"></a> `_otherTransactionHashOuterSize` | `public` | `number` |
| <a id="_othertransactionhashsize"></a> `_otherTransactionHashSize` | `public` | `number` |
| <a id="transaction_type"></a> `TRANSACTION_TYPE` | `static` | [`TransactionType`](TransactionType.md) |
| <a id="transaction_version"></a> `TRANSACTION_VERSION` | `static` | `number` |
| <a id="type_hints"></a> `TYPE_HINTS` | `static` | `object` |
| `TYPE_HINTS.multisigAccountAddress` | `public` | `string` |
| `TYPE_HINTS.otherTransactionHash` | `public` | `string` |

## Accessors

### multisigAccountAddress

#### Get Signature

```ts
get multisigAccountAddress(): Address
```

##### Returns

[`Address`](Address.md)

#### Set Signature

```ts
set multisigAccountAddress(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`Address`](Address.md) |

##### Returns

`void`

***

### otherTransactionHash

#### Get Signature

```ts
get otherTransactionHash(): Hash256
```

##### Returns

[`Hash256`](Hash256.md)

#### Set Signature

```ts
set otherTransactionHash(value): void
```

##### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | [`Hash256`](Hash256.md) |

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
static deserialize(payload): CosignatureV1Body
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`CosignatureV1Body`
