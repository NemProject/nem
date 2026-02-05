# nem-openapi

OpenAPI specification for **NEM Infrastructure Server (NIS)** API. The API is implemented in [NemProject/nem](https://github.com/NemProject/nem/tree/dev/nis).

**Canonical API documentation:** [https://nemproject.github.io/](https://nemproject.github.io/) — use it for endpoint descriptions and verification. See also [docs/DOCUMENTATION_SOURCE.md](docs/DOCUMENTATION_SOURCE.md).

## Requirements

* Node.js 20 LTS

## Installation

1. Clone the ``nem-openapi`` repository.

```
git clone https://github.com/nem/nem-openapi.git
```

2. Install dependencies.

```
npm install
```

## Commands

### Build

Compile the specification.
The generated output is saved under ``_build`` directory.

```
npm run build
```

### Test

Check if the specification is valid.

```
npm run test
```