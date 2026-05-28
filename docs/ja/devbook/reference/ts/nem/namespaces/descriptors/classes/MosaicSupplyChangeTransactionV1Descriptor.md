# Class: MosaicSupplyChangeTransactionV1Descriptor

Type safe descriptor used to generate a descriptor map for MosaicSupplyChangeTransactionV1Descriptor.

binary layout for a mosaic supply change transaction (V1, latest)

## Constructors

### new MosaicSupplyChangeTransactionV1Descriptor()

```ts
new MosaicSupplyChangeTransactionV1Descriptor(
   mosaicId, 
   action, 
   delta): MosaicSupplyChangeTransactionV1Descriptor
```

Creates a descriptor for MosaicSupplyChangeTransactionV1.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mosaicId` | [`MosaicIdDescriptor`](MosaicIdDescriptor.md) | mosaic id |
| `action` | [`MosaicSupplyChangeAction`](../../models/classes/MosaicSupplyChangeAction.md) | supply change action |
| `delta` | [`Amount`](../../models/classes/Amount.md) | change amount |

#### Returns

`MosaicSupplyChangeTransactionV1Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.action` | [`MosaicSupplyChangeAction`](../../models/classes/MosaicSupplyChangeAction.md) |
| `rawDescriptor.delta` | [`Amount`](../../models/classes/Amount.md) |
| `rawDescriptor.mosaicId` | `any` |
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
