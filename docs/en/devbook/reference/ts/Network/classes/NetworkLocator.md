# Class: NetworkLocator

Provides utility functions for finding a network.

## Constructors

### new NetworkLocator()

```ts
new NetworkLocator(): NetworkLocator
```

#### Returns

`NetworkLocator`

## Methods

### findByIdentifier()

```ts
static findByIdentifier<TNetwork>(networks, singleOrMultipleIdentifiers): TNetwork
```

Finds a network with a specified identifier within a list of networks.

#### Type Parameters

| Type Parameter | Description |
| ------ | ------ |
| `TNetwork` *extends* [`Network`](Network.md)&lt;`any`, `any`&gt; |  |

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `networks` | `TNetwork`[] | List of networks to search. |
| `singleOrMultipleIdentifiers` | `number` \| `number`[] | Identifiers for which to search. |

#### Returns

`TNetwork`

First network with an identifier in the supplied list.

***

### findByName()

```ts
static findByName<TNetwork>(networks, singleOrMultipleNames): TNetwork
```

Finds a network with a specified name within a list of networks.

#### Type Parameters

| Type Parameter | Description |
| ------ | ------ |
| `TNetwork` *extends* [`Network`](Network.md)&lt;`any`, `any`&gt; |  |

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `networks` | `TNetwork`[] | List of networks to search. |
| `singleOrMultipleNames` | `string` \| `string`[] | Names for which to search. |

#### Returns

`TNetwork`

First network with a name in the supplied list.
