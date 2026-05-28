# Class: MultisigAccountModificationTransactionV2Descriptor

Type safe descriptor used to generate a descriptor map for MultisigAccountModificationTransactionV2Descriptor.

binary layout for a multisig account modification transaction (V2, latest)

## Constructors

### new MultisigAccountModificationTransactionV2Descriptor()

```ts
new MultisigAccountModificationTransactionV2Descriptor(minApprovalDelta, modifications?): MultisigAccountModificationTransactionV2Descriptor
```

Creates a descriptor for MultisigAccountModificationTransactionV2.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `minApprovalDelta` | `number` | relative change of the minimal number of cosignatories required when approving a transaction |
| `modifications`? | [`SizePrefixedMultisigAccountModificationDescriptor`](SizePrefixedMultisigAccountModificationDescriptor.md)[] | multisig account modifications |

#### Returns

`MultisigAccountModificationTransactionV2Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.minApprovalDelta` | `number` |
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
