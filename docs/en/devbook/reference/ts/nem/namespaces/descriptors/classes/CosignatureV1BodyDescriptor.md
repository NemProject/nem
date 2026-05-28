# Class: CosignatureV1BodyDescriptor

Type safe descriptor used to generate a descriptor map for CosignatureV1BodyDescriptor.

shared content between V1 verifiable and non-verifiable cosignature transactions

## Constructors

### new CosignatureV1BodyDescriptor()

```ts
new CosignatureV1BodyDescriptor(otherTransactionHash, multisigAccountAddress): CosignatureV1BodyDescriptor
```

Creates a descriptor for CosignatureV1Body.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `otherTransactionHash` | [`Hash256`](../../../../index/classes/Hash256.md) | other transaction hash |
| `multisigAccountAddress` | [`Address`](../../../classes/Address.md) | multisig account address |

#### Returns

`CosignatureV1BodyDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.multisigAccountAddress` | [`Address`](../../../classes/Address.md) |
| `rawDescriptor.otherTransactionHash` | [`Hash256`](../../../../index/classes/Hash256.md) |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
