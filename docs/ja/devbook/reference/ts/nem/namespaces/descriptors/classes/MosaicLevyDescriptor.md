# Class: MosaicLevyDescriptor

Type safe descriptor used to generate a descriptor map for MosaicLevyDescriptor.

binary layout for a mosaic levy

## Constructors

### new MosaicLevyDescriptor()

```ts
new MosaicLevyDescriptor(
   transferFeeType, 
   recipientAddress, 
   mosaicId, 
   fee): MosaicLevyDescriptor
```

Creates a descriptor for MosaicLevy.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `transferFeeType` | [`MosaicTransferFeeType`](../../models/classes/MosaicTransferFeeType.md) | mosaic fee type |
| `recipientAddress` | [`Address`](../../../classes/Address.md) | recipient address |
| `mosaicId` | [`MosaicIdDescriptor`](MosaicIdDescriptor.md) | levy mosaic id |
| `fee` | [`Amount`](../../models/classes/Amount.md) | amount of levy mosaic to transfer |

#### Returns

`MosaicLevyDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.fee` | [`Amount`](../../models/classes/Amount.md) |
| `rawDescriptor.mosaicId` | `any` |
| `rawDescriptor.recipientAddress` | [`Address`](../../../classes/Address.md) |
| `rawDescriptor.transferFeeType` | [`MosaicTransferFeeType`](../../models/classes/MosaicTransferFeeType.md) |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
