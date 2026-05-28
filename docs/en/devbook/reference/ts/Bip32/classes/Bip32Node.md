# Class: Bip32Node

Representation of a BIP32 node.

## Constructors

### new Bip32Node()

```ts
new Bip32Node(hmacKey, data): Bip32Node
```

Creates a BIP32 node around a key and data.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `hmacKey` | `Uint8Array` | BIP32 HMAC key. |
| `data` | `Uint8Array` | BIP32 seed. |

#### Returns

`Bip32Node`

## Properties

| Property | Type | Description |
| ------ | ------ | ------ |
| <a id="chaincode"></a> `chainCode` | `Uint8Array` | Chain code associated with this node. |
| <a id="privatekey"></a> `privateKey` | [`PrivateKey`](../../index/classes/PrivateKey.md) | Private key associated with this node. |

## Methods

### deriveOne()

```ts
deriveOne(identifier): Bip32Node
```

Derives a direct child node with specified identifier.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `identifier` | `number` | Child identifier. |

#### Returns

`Bip32Node`

BIP32 child node.

***

### derivePath()

```ts
derivePath(path): Bip32Node
```

Derives a descendent node with specified path.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `path` | `number`[] | BIP32 path. |

#### Returns

`Bip32Node`

BIP32 node at the end of the path.
