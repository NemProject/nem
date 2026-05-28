# Class: Bip32

Factory of BIP32 root nodes.

## Constructors

### new Bip32()

```ts
new Bip32(curveName?, mnemonicLanguage?): Bip32
```

Creates a BIP32 root node factory.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `curveName`? | `string` | Elliptic curve to use. |
| `mnemonicLanguage`? | `string` | Language of constructed mnemonics. |

#### Returns

`Bip32`

## Methods

### fromMnemonic()

```ts
fromMnemonic(mnemonic, password): Bip32Node
```

Creates a BIP32 root node from a BIP39 mnemonic and password.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mnemonic` | `string` | BIP32 mnemonic. |
| `password` | `string` | BIP32 mnemonic password. |

#### Returns

[`Bip32Node`](Bip32Node.md)

BIP32 root node.

***

### fromSeed()

```ts
fromSeed(seed): Bip32Node
```

Creates a BIP32 root node from a seed.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `seed` | `Uint8Array` | BIP32 seed. |

#### Returns

[`Bip32Node`](Bip32Node.md)

BIP32 root node.

***

### random()

```ts
random(seedLength?): string
```

Creates a random BIP32 mnemonic.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `seedLength`? | `number` | Length of random seed to use when generating mnemonic. |

#### Returns

`string`

Random mnemonic created with the specified entropy.
