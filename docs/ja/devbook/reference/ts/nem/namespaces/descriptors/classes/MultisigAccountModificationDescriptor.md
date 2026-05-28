# Class: MultisigAccountModificationDescriptor

Type safe descriptor used to generate a descriptor map for MultisigAccountModificationDescriptor.

binary layout for a multisig account modification

## Constructors

### new MultisigAccountModificationDescriptor()

```ts
new MultisigAccountModificationDescriptor(modificationType, cosignatoryPublicKey): MultisigAccountModificationDescriptor
```

Creates a descriptor for MultisigAccountModification.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `modificationType` | [`MultisigAccountModificationType`](../../models/classes/MultisigAccountModificationType.md) | modification type |
| `cosignatoryPublicKey` | [`PublicKey`](../../../../index/classes/PublicKey.md) | cosignatory public key |

#### Returns

`MultisigAccountModificationDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.cosignatoryPublicKey` | [`PublicKey`](../../../../index/classes/PublicKey.md) |
| `rawDescriptor.modificationType` | [`MultisigAccountModificationType`](../../models/classes/MultisigAccountModificationType.md) |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
