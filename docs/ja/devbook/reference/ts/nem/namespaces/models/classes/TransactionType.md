# Class: TransactionType

## Constructors

### new TransactionType()

```ts
new TransactionType(value): TransactionType
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `any` |

#### Returns

`TransactionType`

## Properties

| Property | Modifier | Type |
| ------ | ------ | ------ |
| <a id="value"></a> `value` | `public` | `any` |
| <a id="account_key_link"></a> `ACCOUNT_KEY_LINK` | `static` | `TransactionType` |
| <a id="mosaic_definition"></a> `MOSAIC_DEFINITION` | `static` | `TransactionType` |
| <a id="mosaic_supply_change"></a> `MOSAIC_SUPPLY_CHANGE` | `static` | `TransactionType` |
| <a id="multisig"></a> `MULTISIG` | `static` | `TransactionType` |
| <a id="multisig_account_modification"></a> `MULTISIG_ACCOUNT_MODIFICATION` | `static` | `TransactionType` |
| <a id="multisig_cosignature"></a> `MULTISIG_COSIGNATURE` | `static` | `TransactionType` |
| <a id="namespace_registration"></a> `NAMESPACE_REGISTRATION` | `static` | `TransactionType` |
| <a id="transfer"></a> `TRANSFER` | `static` | `TransactionType` |

## Accessors

### size

#### Get Signature

```ts
get size(): number
```

##### Returns

`number`

## Methods

### serialize()

```ts
serialize(): Uint8Array<ArrayBufferLike>
```

#### Returns

`Uint8Array`&lt;`ArrayBufferLike`&gt;

***

### toJson()

```ts
toJson(): any
```

#### Returns

`any`

***

### toString()

```ts
toString(): string
```

#### Returns

`string`

***

### deserialize()

```ts
static deserialize(payload): any
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`any`

***

### deserializeAligned()

```ts
static deserializeAligned(payload): any
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `payload` | `any` |

#### Returns

`any`

***

### fromValue()

```ts
static fromValue(value): any
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `any` |

#### Returns

`any`

***

### valueToKey()

```ts
static valueToKey(value): string
```

#### Parameters

| Parameter | Type |
| ------ | ------ |
| `value` | `any` |

#### Returns

`string`
