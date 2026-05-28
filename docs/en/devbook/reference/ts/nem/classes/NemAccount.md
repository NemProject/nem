# Class: NemAccount

NEM account.

## Extends

- [`NemPublicAccount`](NemPublicAccount.md)

## Constructors

### new NemAccount()

```ts
new NemAccount(facade, keyPair): NemAccount
```

Creates a NEM account.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `facade` | [`NemFacade`](NemFacade.md) | NEM facade. |
| `keyPair` | [`KeyPair`](KeyPair.md) | Account key pair. |

#### Returns

`NemAccount`

#### Overrides

[`NemPublicAccount`](NemPublicAccount.md).[`constructor`](NemPublicAccount.md#constructor)

## Properties

| Property | Modifier | Type | Description | Inherited from |
| ------ | ------ | ------ | ------ | ------ |
| <a id="_facade"></a> `_facade` | `protected` | [`NemFacade`](NemFacade.md) | - | [`NemPublicAccount`](NemPublicAccount.md).[`_facade`](NemPublicAccount.md#_facade) |
| <a id="address"></a> `address` | `public` | [`Address`](Address.md) | Account address. | [`NemPublicAccount`](NemPublicAccount.md).[`address`](NemPublicAccount.md#address) |
| <a id="keypair"></a> `keyPair` | `public` | [`KeyPair`](KeyPair.md) | Account key pair. | - |
| <a id="publickey"></a> `publicKey` | `public` | [`PublicKey`](../../index/classes/PublicKey.md) | Account public key. | [`NemPublicAccount`](NemPublicAccount.md).[`publicKey`](NemPublicAccount.md#publickey) |

## Methods

### messageEncoder()

```ts
messageEncoder(): default
```

Creates a message encoder that can be used for encrypting and encoding messages between two parties.

#### Returns

[`default`](../MessageEncoder/classes/default.md)

Message encoder using this account as one party.

***

### signTransaction()

```ts
signTransaction(transaction): Signature
```

Signs a NEM transaction.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `transaction` | [`Transaction`](../namespaces/models/classes/Transaction.md) | Transaction object. |

#### Returns

[`Signature`](../../index/classes/Signature.md)

Transaction signature.
