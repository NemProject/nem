OpenAPI specification for **NEM Infrastructure Server (NIS)** REST API.
The API is implemented in the [`nis`](../nis) module in this repository.

**Canonical API documentation:** [https://nemproject.github.io/](https://nemproject.github.io/) — use it for endpoint descriptions and verification.

## Requirements

* Node.js 20 LTS

## Installation (in the monorepo)

From the root of the `nem` repository:

```bash
cd openapi
npm install
```

## Commands

### Build

Compile the OpenAPI specification.
The generated output is saved under the `_build` directory.

```bash
npm run build
```

### Test

Check if the specification is valid.

```bash
npm run test
```