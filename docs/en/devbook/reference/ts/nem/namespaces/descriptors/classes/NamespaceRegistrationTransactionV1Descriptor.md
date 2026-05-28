# Class: NamespaceRegistrationTransactionV1Descriptor

Type safe descriptor used to generate a descriptor map for NamespaceRegistrationTransactionV1Descriptor.

binary layout for a namespace registration transaction (V1, latest)

## Constructors

### new NamespaceRegistrationTransactionV1Descriptor()

```ts
new NamespaceRegistrationTransactionV1Descriptor(
   rentalFeeSink, 
   rentalFee, 
   name?, 
   parentName?): NamespaceRegistrationTransactionV1Descriptor
```

Creates a descriptor for NamespaceRegistrationTransactionV1.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `rentalFeeSink` | [`Address`](../../../classes/Address.md) | mosaic rental fee sink public key |
| `rentalFee` | [`Amount`](../../models/classes/Amount.md) | mosaic rental fee |
| `name`? | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | new namespace name |
| `parentName`? | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | parent namespace name |

#### Returns

`NamespaceRegistrationTransactionV1Descriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |
| `rawDescriptor.rentalFee` | [`Amount`](../../models/classes/Amount.md) |
| `rawDescriptor.rentalFeeSink` | [`Address`](../../../classes/Address.md) |
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
