# Changelog
All notable changes to this project will be documented in this file.

The changelog format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## next

## [0.6.102] - 01-Apr-2025

### Added
- add new NEM testnet
- add 'beneficiary' and 'totalFee' to result from `/local/block/at` to determine block beneficiary directly
- add route `/mosaic/definition/last` to get the last mosaic definition
- add local endpoint `/local/mosaic/definition/supply` for querying historical mosaic supplies, including expiration height
- add a new `TRACK_EXPIRED_MOSAICS` node feature to track expired mosaics
- add `/local/mosaics/expired` to report expired mosaics, and include a new 'expiredMosaicType' property in the output to differentiate expired and restored mosaics

### Changed
- mosaic description change would effectively zero all balances prior to mosaic redefinition fork. Update MosaicDefinitionCreationObserver to track these balance resets as mosaic expirations
- replace removeAll with removeExpiration in ExpiredNamespacesObserver

### Fixed
- only 'restore' mosaic balances if the root is inactive
- MosaicDefinitionRetriever needs SQL parameters
- block lessor is not set correctly in the database when (future) remote link is pending. Update RemoteLinks to retrieve the active (not current) remote link

### Changed
 - complete SDK rewrite, see details in [readme](README.md)

[0.6.102]: https://github.com/symbol/symbol/releases/tag/v0.6.102
