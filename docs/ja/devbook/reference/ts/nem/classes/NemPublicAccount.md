# Class: NemPublicAccount

NEM public account.

## Extended by

- [`NemAccount`](NemAccount.md)

## Constructors

### new NemPublicAccount()

```ts
new NemPublicAccount(facade, publicKey): NemPublicAccount
```

Creates a NEM public account.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `facade` | [`NemFacade`](NemFacade.md) | NEM facade. |
| `publicKey` | [`PublicKey`](../../index/classes/PublicKey.md) | Account public key. |

#### Returns

`NemPublicAccount`

## Properties

| Property | Modifier | Type | Description |
| ------ | ------ | ------ | ------ |
| <a id="_facade"></a> `_facade` | `protected` | [`NemFacade`](NemFacade.md) | - |
| <a id="address"></a> `address` | `public` | [`Address`](Address.md) | Account address. |
| <a id="publickey"></a> `publicKey` | `public` | [`PublicKey`](../../index/classes/PublicKey.md) | Account public key. |
