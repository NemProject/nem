# Class: NamespaceIdDescriptor

Type safe descriptor used to generate a descriptor map for NamespaceIdDescriptor.

binary layout for a namespace id

## Constructors

### new NamespaceIdDescriptor()

```ts
new NamespaceIdDescriptor(name?): NamespaceIdDescriptor
```

Creates a descriptor for NamespaceId.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `name`? | `string` \| `Uint8Array`&lt;`ArrayBufferLike`&gt; | name |

#### Returns

`NamespaceIdDescriptor`

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
