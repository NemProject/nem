# Class: CosignatureV1Descriptor

Type safe descriptor used to generate a descriptor map for CosignatureV1Descriptor.

binary layout for a cosignature transaction (V1, latest)

## Constructors

### new CosignatureV1Descriptor()

```ts
new CosignatureV1Descriptor(otherTransactionHash, multisigAccountAddress): CosignatureV1Descriptor
```

Creates a descriptor for CosignatureV1.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `otherTransactionHash` | [`Hash256`](../../../../index/classes/Hash256.md) | other transaction hash |
| `multisigAccountAddress` | [`Address`](../../../classes/Address.md) | multisig account address |

#### Returns

`CosignatureV1Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.multisigAccountAddress` | [`Address`](../../../classes/Address.md) |
| `rawDescriptor.otherTransactionHash` | [`Hash256`](../../../../index/classes/Hash256.md) |
| `rawDescriptor.type` | `string` |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
