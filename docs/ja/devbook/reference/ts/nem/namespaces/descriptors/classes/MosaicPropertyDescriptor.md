# Class: MosaicPropertyDescriptor

Type safe descriptor used to generate a descriptor map for MosaicPropertyDescriptor.

binary layout for a mosaic property supported property names are: divisibility, initialSupply, supplyMutable, transferable

## Constructors

### new MosaicPropertyDescriptor()

```ts
new MosaicPropertyDescriptor(name?, value?): MosaicPropertyDescriptor
```

Creates a descriptor for MosaicProperty.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `name`? | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | property name |
| `value`? | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | property value |

#### Returns

`MosaicPropertyDescriptor`

## Properties

| Property | Type |
| ------ | ------ |
| <a id="rawdescriptor"></a> `rawDescriptor` | `object` |

## Methods

### toMap()

```ts
toMap(): object
```

Builds a representation of this descriptor that can be passed to a factory function.

#### Returns

`object`

Descriptor that can be passed to a factory function.
