---
title: Setup
---

# Setting Up a Development Environment

This page lists the dependencies required to run the tutorials in this documentation and explains how to run them.

Most tutorials use the Symbol SDK, which supports both the <NEM:> and <Symbol:> networks.
It is the recommended library for building NEM applications, so the steps below apply to every tutorial.

Select the language you prefer:

=== ":simple-python: Python"

    <table markdown class="setup">
    <tr markdown><td>Prerequisites</td><td markdown>[Python](https://www.python.org/downloads/) 3.10 or later</td></tr>
    <tr markdown><td>Installation</td><td markdown>
    Install the Symbol SDK version 3.3.1 with:
    ```bash
    pip install symbol-sdk-python --upgrade
    ```
    </td></tr>
    <tr markdown><td>Running the Sample Code</td><td markdown>
    Download a sample and run it with:
    ```bash
    python hello-world.py
    ```
    </td></tr></table>

    ??? warning "Troubleshooting"

        On some systems, installing the SDK may require additional system packages,
        because some Python dependencies are built from source.

        If installation fails with errors related to missing headers, libraries, or compiler tools,
        install the required **development packages** for your system and run the installation again.

        Common symptoms include errors mentioning `gcc` or `pysha3`.

        On Ubuntu and Debian, it is typically enough to install:

        ```bash
        sudo apt install python3-dev build-essential
        ```

        Then run the Symbol SDK installation again.

=== ":simple-javascript: JavaScript"

    <table markdown class="setup">
    <tr markdown><td>Prerequisites</td><td markdown>Any actively supported version of [Node.js](https://nodejs.org/)</td></tr>
    <tr markdown><td>Installation</td><td markdown>
    Create a project folder and install the Symbol SDK version 3.3.1 as a dependency:
    ```bash
    mkdir nem-dev && cd nem-dev
    npm init -y
    npm install symbol-sdk
    ```
    </td></tr>
    <tr markdown><td>Running the Sample Code</td><td markdown>
    Download a sample and run it with:
    ```bash
    node hello-world.mjs
    ```
    </td></tr></table>

## Next Steps

* Proceed to [Hello World](./hello-world.md)

<style>
.md-typeset .tabbed-labels a {
    font-size: large;
}
table.setup {
    border-collapse:collapse;
}
table.setup td {
    border: 1px solid var(--md-default-bg-color--light);
    padding: 0.5rem;
}
.md-typeset table.setup td:first-child {
    white-space:nowrap;
}
.md-typeset table.setup td:last-child {
    width: 100%;
}
.md-typeset table.setup pre {
    margin-bottom: 0;
}
</style>
