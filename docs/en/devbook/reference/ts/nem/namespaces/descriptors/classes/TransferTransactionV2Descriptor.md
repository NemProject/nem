# Class: TransferTransactionV2Descriptor

Type safe descriptor used to generate a descriptor map for TransferTransactionV2Descriptor.

binary layout for a transfer transaction (V2, latest)

## Constructors

### new TransferTransactionV2Descriptor()

```ts
new TransferTransactionV2Descriptor(
   recipientAddress, 
   amount, 
   message?, 
   mosaics?): TransferTransactionV2Descriptor
```

Creates a descriptor for TransferTransactionV2.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `recipientAddress` | [`Address`](../../../classes/Address.md) | recipient address |
| `amount` | [`Amount`](../../models/classes/Amount.md) | XEM amount |
| `message`? | [`MessageDescriptor`](MessageDescriptor.md) | optional message |
| `mosaics`? | [`SizePrefixedMosaicDescriptor`](SizePrefixedMosaicDescriptor.md)[] | attached mosaics notice that mosaic amount is multipled by transfer amount to get effective amount |

#### Returns

`TransferTransactionV2Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.amount` | [`Amount`](../../models/classes/Amount.md) |
| `rawDescriptor.recipientAddress` | [`Address`](../../../classes/Address.md) |
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
