---
title: セットアップ
---

# 開発環境のセットアップ

このページでは、このドキュメントのチュートリアルを実行するために必要な依存関係と、実行方法を説明します。

ほとんどのチュートリアルでは、[NEM](default:NEM) と [Symbol](default:Symbol) の両方のネットワークをサポートする Symbol SDK を使用します。
NEM アプリケーションの構築に推奨されるライブラリなので、以下の手順はすべてのチュートリアルに適用できます。

使用する言語を選択してください。

=== ":simple-python: Python"

    <table markdown class="setup">
    <tr markdown><td>前提条件</td><td markdown>[Python](https://www.python.org/downloads/) 3.10 以降</td></tr>
    <tr markdown><td>インストール</td><td markdown>
    次のコマンドで Symbol SDK バージョン 3.3.1 をインストールします。
    ```bash
    pip install symbol-sdk-python --upgrade
    ```
    </td></tr>
    <tr markdown><td>サンプルコードの実行</td><td markdown>
    サンプルをダウンロードして、次のコマンドで実行します。
    ```bash
    python hello-world.py
    ```
    </td></tr></table>

    ??? warning "トラブルシューティング"

        システムによっては、Python の依存関係の一部がソースからビルドされるため、SDK のインストールに追加のシステムパッケージが必要になる場合があります。

        ヘッダー、ライブラリ、コンパイラツールが見つからないというエラーでインストールに失敗した場合は、使用しているシステムに必要な **開発用パッケージ** をインストールして、もう一度インストールを実行してください。

        よくある症状として、`gcc` または `pysha3` に関するメッセージが表示されます。

        Ubuntu と Debian では、通常、次のコマンドで十分です。

        ```bash
        sudo apt install python3-dev build-essential
        ```

        その後、Symbol SDK のインストールをもう一度実行してください。

=== ":simple-javascript: JavaScript"

    <table markdown class="setup">
    <tr markdown><td>前提条件</td><td markdown>[Node.js](https://nodejs.org/) の現在サポートされているバージョン</td></tr>
    <tr markdown><td>インストール</td><td markdown>
    プロジェクトフォルダーを作成し、依存関係として Symbol SDK バージョン 3.3.1 をインストールします。
    ```bash
    mkdir nem-dev && cd nem-dev
    npm init -y
    npm install symbol-sdk
    ```
    </td></tr>
    <tr markdown><td>サンプルコードの実行</td><td markdown>
    サンプルをダウンロードして、次のコマンドで実行します。
    ```bash
    node hello-world.mjs
    ```
    </td></tr></table>

## 次のステップ {: #next-steps }

* [Hello World](./hello-world.md) に進む

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
