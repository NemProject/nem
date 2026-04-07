# Glossary

AMA
:   Ask Me Anything. An open questions session.

AML
:   Anti Money Laundering.

APAC
:   Asia and Pacific region.

APR
:   Annual Percentage Rate.

Arbitrage
:   When a trader purchases an asset in a market and sells it in a different one, to profit from a deviation in prices between markets.

Backrunning
:   To broadcast ``transactionA`` with slightly lower gas (or fees) than an already pending ``transactionB`` so that ``transactionA`` gets mined *right after* ``transactionB`` in the same block.

BLS
:   A [Boneh–Lynn–Shacham](https://en.wikipedia.org/wiki/BLS_digital_signature) signature is a cryptographic signature scheme which allows a user to verify that a signer is authentic.

BTC
:   Bitcoin.

CBDC
:   Central Bank Digital Currency.

CEX
:   Centralized Exchange, as opposed to Decentralized Exchanges (<DEX:>).

CLI
:   Command-Line Interface. A Program which is entirely used from a terminal console, using only the keyboard. Symbol has a CLI tool to interact with the blockchain.

CMC
:   Coin Market Cap. A web page with cryptocurrency information.

CSD
:   Central Securities Deposit.

DAO
:   Decentralized Autonomous Organization. An organization whose governance happens completely on a blockchain.

Dapp
:   Decentralized Application. An application that runs on a blockchain instead of a single computer.
    The term is slightly abused so, in a more general sense, it also means any application which makes use of a blockchain.

DDH
:   Decisional [Diffie-Hellman](https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange).

DD
:   Due Diligence.

DeFi
:   Decentralized Finance, as opposed to Traditional Finance (<TradFi:>).

DEX
:   Decentralized Exchange, as opposed to traditional Centralized Exchanges (<CEX:>).

DoS
:   Denial of Service. An attack in which a single source floods a server or network with excessive requests,
    overwhelming its resources and rendering it unable to respond to legitimate traffic.

    The most common variant is the DDoS (Distributed Denial of Service) attack that involves multiple sources,
    often using compromised devices without the owners' knowledge.

DTC
:   Direct To Consumer, i.e. mass market.

E2E
:   End-To-End.

EMEA
:   Europe, Middle-East and Africa.

ERC
:   Ethereum Request for Comment. Commonly utilized to refer to a token standard on the EVM (such as ERC-20, ERC-721, or ERC-1155).

ETH
:   Ethereum.

EVM
:   Ethereum Virtual Machine.

FFT
:   [Fast Fourier Transform](https://en.wikipedia.org/wiki/Fast_Fourier_transform).

Frontrunning
:   To broadcast ``transactionA`` with slightly higher gas (or fees) than an already pending ``transactionB`` so that ``transactionA`` gets mined *right before* ``transactionB`` in the same block.
    This is important in case of <DeFi:> markets, where gains can be made from frontrunning.

Hardware wallet
:   A device designed to store <private keys:> and produce signatures with them.
    The keys are stored in an encrypted memory and never leave the device, so hardware wallets are deemed one of
    the most secure ways to access an account.
    They typically only provide signing functionality, so they must be paired with a software <wallet:> or
    application that creates the <transactions:> and announces them.

HTLC
:   Hashed Time-Lock Contract.

ICO
:   Initial Coin Offering.

Inflation
:   A small amount of <XYM:> that is freshly minted with each new <block:> to reward the <node:> that creates it.
    Inflation began 48 hours after network launch in March 2021, starting at approximately 200 XYM per block.
    The reward decreases gradually over time following a slow curve, reaching 1 XYM per block after 30 years,
    and disappearing entirely after 105 years.

IP
:   Intellectual Property.

IRS
:   Internal Revenue Service. Who you pay your taxes to if you live in the United States or are an American citizen.

KYC
:   Know Your Customer. Related to <AML:>.

LATAM
:   Latin America (Central and South America).

mainnet
:   Symbol's Main Network, where transactions with real value happen, as opposed to the <testnet:>.

MEV
:   Miner-Extractable Value, or Maximal-Extractable Value, is the process of reorganizing transactions inside a block
    by miners, to gain *something*. Uses <Frontrunning:>, <Backrunning:>, or <Sandwich:>.

NAM
:   North America.

NEM
:   The New Economy Movement.

NFT
:   A non-fungible <token:>, a way to represent individual entities as a blockchain-based asset.

NIS1
:   The first version of <NEM:>'s blockchain node that operates the public <mainnet:> with the native currency <XEM:>.
    First launched on March 31, 2015.

PoC
:   Proof of Concept, i.e., a prototype (not a consensus protocol).

PoI
:   Proof of Importance.
    The consensus protocol used by <NIS1:>. Similar to <PoS:> but measuring an account's activity besides its stake.

PoS
:   Proof of Stake. A consensus protocol, used, for example, by Ethereum.

PoS+
:   Proof-of-Stake Plus. Symbol's consensus mechanism.
    It is a modified <PoS:> algorithm which considers users' activity in the network in addition to their network stakes.
    The chance that accounts will have to <harvesting:|harvest> a block is calculated through their <importance:> score.

PoW
:   Proof of Work. A consensus protocol, used, for example, by Bitcoin.

Rug Pull
:   A malicious maneuver where cryptocurrency developers abandon a project and run off with the funds.

Sandwich
:   A type of <MEV:> technique that is popular in <DeFi:>.
    To make a sandwich, you find a pending transaction in the network and then try to surround it by placing one order
    *just* before the transaction (<frontrunning:>) and one order just after it (<backrunning:>).

SDK
:   Software Development Kit. A Software library used to simplify creating applications for a given platform.

Sharding
:   An Ethereum [scaling solution](https://ethereum.org/en/developers/docs/scaling/#sharding).

SXDH
:   Symmetric External [Diffie-Hellman](https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange).

Sybil Attack
:   An attack in which a single adversary creates many fake identities or accounts to gain disproportionate influence
    over a network or consensus process.
    Common countermeasures include <PoW:> or <PoS:>, which tie influence to scarce resources.

testnet
:   Symbol's Test Network, intended for development. Test <XYM:> can be freely obtained from a
    [faucet](../devbook/accounts/testnet-faucet.md), so transactions on this network do not have real value,
    as opposed to transactions on the <mainnet:>.

TLC
:   Tender Loving Care.

TLS
:   Security protocol used to encrypting communication between peers on a network.

Token
:   A representation of a digital asset. On Symbol they are called <mosaics:>.

TPS
:   Transactions Per Second.

TradFi
:   Traditional Finance, as opposed to Decentralized Finance (<DeFi:>).

USP
:   Unique Selling Proposition or Unique Selling Point.
    A characteristic of a product that can be used in advertising to differentiate it from its competitors.

VPS
:   Virtual Private Server.
    A virtual machine typically hosted on a data center which can be accessed remotely and treated as if it was
    a conventional physical machine.

VRF
:   Verifiable Random Function.

XEM
:   The native currency of the <NIS1:> blockchain.

XYM
:   The native currency of the Symbol blockchain.
