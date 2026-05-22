# Nodes

Node
:   A computer running the NEM software which shares information with peer nodes, validates incoming <transactions:>,
    and participates in <consensus:> and block creation.

Nodes form the backbone of the blockchain, ensuring the network remains functional as long as enough nodes are active.

Anyone can run a NEM node.
Operators do so to <harvesting:|harvest> blocks with their own account, to host <delegated harvesting:> for others,
or to qualify for the <Supernode Program:>.

## Node Structure

Every NEM node runs the same application, called _NIS_.

NIS
:   NEM Infrastructure Server.
    A single Java process that implements all node functionality.

NIS has four parts: an [engine](#engine), exposed by a [REST API](#rest-api) and a [WebSocket](#websocket) service, with an embedded [database](#database) for storage.

Over the network, NIS exchanges data with other nodes and with clients:

* _Other nodes_ are NIS peers on the network.
* _Clients_ are external programs such as wallets, explorers, and applications.

```dot
digraph NemNode {
    compound=true;
    splines=ortho;
    nodesep=0.7;
    ranksep=0.7;
    node [shape=box];
    edge [penwidth=1.5 dir=both];

    // External actors
    OtherNodes [label="Other nodes" shape=plaintext];
    Clients    [label="Clients" shape=plaintext];

    subgraph cluster_nis {
        label="NIS";
        labelloc=b;
        labeljust=c;
        style=dashed;
        penwidth=1.5;
        margin=25;

        // Core components
        REST   [label="REST + WebSocket" style=filled fixedsize=true width=6.0 height=0.8 URL="#rest-api"];
        Engine [label="Engine" style=filled width=2.6 height=0.9 URL="#engine"];
        H2     [label="Blocks\n(H2)" style=filled width=2.6 height=0.9 shape=cylinder URL="#database"];

        { rank=same; Engine; H2; }

        // Internal connections
        REST -> Engine;
        REST -> H2 [style=invis];
        Engine -> H2;
    }

    // External connections
    OtherNodes -> REST;
    Clients -> REST;
}
```

### Engine

The engine performs the blockchain work: it validates incoming data, runs <consensus:> and <harvesting:>, handles
[peer-to-peer networking](#peer-to-peer-communication), and maintains the
<unconfirmed pool:|unconfirmed transactions pool>.

The engine is an internal component and is not exposed directly.
Every inbound request, from a peer or from a client, arrives through the REST API described below and is then handed
to the engine.

### REST API

Both peers and clients reach NIS through a single HTTP API:

* _Peer requests_ handle block synchronization, transaction relay, and node discovery.
* _Client requests_ handle reading blockchain data and submitting <transactions:>.

### WebSocket

NIS publishes block and transaction events through a built-in
[WebSocket](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API) service.
Subscribed clients receive notifications in real time without polling.

### Database

NIS stores the blockchain in an [H2](https://www.h2database.com) relational database that is _embedded_, meaning it runs
inside the NIS process rather than as a separate database server.

The database holds only the chain itself: every block and the transactions inside it.
It does not store the current blockchain state, such as account balances and <importance:> scores.
NIS keeps that state in memory and rebuilds it at every start by replaying the chain from the <nemesis block:> onward,
which is why a node stays unavailable for a while after it starts.

NIS reads and writes data through [Hibernate](https://hibernate.org), a library that maps Java objects to database rows.
When NIS is upgraded to a newer release, [Flyway](https://flywaydb.org) automatically applies any required schema
changes.

## Peer To Peer Communication

NEM nodes communicate directly with one another in a decentralized, peer-to-peer fashion.
There is no central coordinator: instead, each node establishes connections with a subset of other nodes,
forming a distributed network.

Nodes share their lists of known peers, allowing a newly connected node to quickly discover others and integrate
into the network.
This process ensures robust connectivity and helps the network remain resilient, even if individual nodes go offline.

```dot
graph P2PNetwork {
    layout=circo;
    mindist=0.5;
    node [style=filled];
    edge [dir=both len=1];

    N1 [label="Node 1"];
    N2 [label="Node 2"];
    N3 [label="Node 3"];
    N4 [label="Node 4"];
    N5 [label="Node 5"];
    N6 [label="Node 6"];
    N7 [label="Node 7"];
    N8 [label="Node 8"];

    // Random peer-to-peer connections
    N1 -- N2 -- N3 -- N4 -- N5 -- N6 -- N7 -- N8;
    N1 -- N5;
    N2 -- N6;
    N4 -- N1;
    N8 -- N3;
}
```

To facilitate bootstrapping, an initial list of peers is bundled with <NIS:>.
This allows a new node to make its first connections and begin discovering others.
However, nodes on this list receive no special treatment:
once connected, all peers are treated equally by the protocol.

### Node Reputation

In a decentralized network like NEM, nodes must decide which peers to trust and maintain connections with.
Rather than relying on static whitelists or manually curated connections, NEM nodes use a _reputation_ system
to dynamically score and rank their peers based on observed behavior over time.

Each node calculates reputation independently, using metrics such as communication success, response time,
and the validity of received data.
Nodes that behave correctly and respond consistently are given higher scores.
Those that send invalid data, fail to respond, or otherwise misbehave may be penalized or temporarily blacklisted.

When a node needs to establish a new connection, it selects from the available peers, prioritizing those with higher
reputation based on past interactions.

Note that reputation scores are local: each node maintains its own view of the network,
based solely on its direct experience.

The implementation is based on the [EigenTrust++](https://en.wikipedia.org/wiki/EigenTrust) algorithm.

!!! note "Node rotation"

    To prevent the formation of isolated or stagnant node groups,
    NEM nodes periodically refresh their peer set, pulling peer lists from a sample of current peers
    and merging newly discovered nodes into the candidate pool, regardless of how well existing peers are scoring.

    This forced churn ensures that nodes continually discover and evaluate new peers,
    maintaining a well-connected and adaptive network topology.

    By balancing reputation-based stability with deliberate connection turnover,
    the protocol avoids network fragmentation and promotes long-term decentralization.

Finally, note that this reputation score is an internal metric used by nodes to decide which other nodes to connect to.
When using <delegated harvesting:>, accounts may delegate to any node they choose, based on reputation factors
that may or may not be related to the score described in this page.

## Supernodes

A supernode is a node enrolled in the _Supernode Program_.

Supernode Program
:   An off-chain, community-funded program that rewards reliable public nodes.

NEM has no block subsidy or inflation.
Nodes are paid exclusively from transaction fees, which can be small in periods of low activity.
The Supernode Program offsets this by paying daily rewards to nodes that prove themselves reliable.

The program runs entirely off-chain, and NIS itself plays no part: a separate, centrally operated service called the
_controller_ tests participating nodes and pays out the rewards.

A node qualifies for a day's reward by holding at least 10'000 <XEM:> and passing a battery of automated tests that
confirm it is in sync, up to date, and running on hardware and a network connection capable of serving peers reliably.

Testing happens in four rounds spaced six hours apart, and a node must pass every test in every round to be eligible.
Each round runs the eight tests below, several of them measured against a trusted _reference node_ that the controller
knows to be healthy:

| Test                | A node passes when                                                                                     |
| ------------------- | -------------------------------------------------------------------------------------------------------|
| **Chain height**    | Its chain is no more than 4 blocks behind the reference node.                                          |
| **Chain part**      | A random sequence of 60 to 100 recent blocks matches the reference node exactly, including signatures. |
| **Balance**         | Its main account holds at least 10'000 XEM.                                                            |
| **Computing power** | It completes 10'000 successive key derivations, round trip included, in 5 seconds or less.             |
| **Version**         | It runs a client version at least as recent as the reference node's.                                   |
| **Ping**            | Across pings to 5 random peers, at most one ping fails and the average round trip stays under 200 ms.  |
| **Bandwidth**       | A bulk hashing exchange with a peer runs at an effective transfer speed of at least 5 Mbit/s.          |
| **Responsiveness**  | It answers 10 chain-height requests in 1 second or less, with at least 9 succeeding.                   |

Rewards come from a fixed daily pool of 25'500 XEM, and each qualifying node earns at most 500 XEM per day.
While 51 or fewer nodes qualify, each receives the full 500 XEM.
Beyond that, the pool is divided equally, so per-node rewards fall as the network grows.

Enrollment is optional.
Step-by-step instructions are available on the [NEM Supernode page](https://nem.io/supernode/).
