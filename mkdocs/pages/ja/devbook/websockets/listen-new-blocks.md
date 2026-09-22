---
title: 新しいブロック
tutorial_level: beginner
---

# 新しいブロックをリッスンする

<ws:blocks> [WebSocket チャネル](<default:WebSocket チャネル>) は、新しい [ブロック](default:ブロック) がチェーンに追加されるたびにリアルタイム通知を送信します。
<get:/chain/height> エンドポイントをポーリングする場合と比べ、WebSocket は API を繰り返し呼び出すオーバーヘッドなしに、発生した更新をプッシュします。

このチュートリアルでは、チャネルをサブスクライブして、受け取った更新を表示する方法を説明します。

!!! note "ポーリングによる方法"

    ポーリングを使う方法については、[チェーン高と不可逆高を照会する](../chain/chain-heights.md) チュートリアルを参照してください。

## 前提条件 {: #prerequisites }

NEM は [SockJS](https://github.com/sockjs/sockjs-client) 上で [STOMP](https://stomp.github.io/) メッセージングプロトコルを使って WebSocket を提供するため、STOMP クライアントと WebSocket トランスポートが必要です。

=== ":simple-python: Python"

    `stomper` と `websockets` ライブラリをインストールします。

    ```bash
    pip install stomper websockets
    ```

=== ":simple-javascript: JavaScript"

    `@stomp/stompjs` と `sockjs-client` ライブラリをインストールします。

    ```bash
    npm install @stomp/stompjs sockjs-client
    ```

=== ":fontawesome-brands-java: Java"

    このチュートリアルでは Tyrus WebSocket クライアントを使用します。これはスニペット内の JBang 依存関係の行によって読み込まれます。

接続プロトコルの詳細については、[WebSocket リファレンス](../reference/websockets/index.md) を参照してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/websockets/listen_new_blocks') }}

スニペットでは、`NODE_URL` 環境変数を使って NEM [ノード](default:ノード) を指定します。
値が指定されていない場合は、デフォルト値を使用します。

`WS_URL` は同じノードの WebSocket エンドポイントを定義します。
`NODE_URL` のポート `7890`（デフォルトの HTTP API ポート）を `7778`（デフォルトの NIS WebSocket ポート）に置き換えて導出します。

プログラムは `Ctrl+C` で割り込みを受けるまで実行され、接続を閉じる前にサブスクライブ解除の手順が実行されます。

!!! note "Python の SockJS ヘルパー"

    Python 用の SockJS クライアントライブラリはないため、便宜上、ファイルの先頭に小さなヘルパーメソッドをいくつか定義しています。

## コードの説明 {: #code-explanation }

### WebSocket に接続する {: #connecting-to-the-websocket }

{{ tutorial.code_snippet_tagged('step-1') }}

最初の手順では、ノードの `/w/messages` エンドポイントへの接続を開き、その上で [STOMP セッション](<default:STOMP セッション>) を開始します。

### チャネルをサブスクライブする {: #subscribing-to-the-channel }

{{ tutorial.code_snippet_tagged('step-2') }}

コードは <ws:blocks> チャネルをサブスクライブします。
ノードは新しいブロックがチェーンに追加されるたびに、サブスクライバーへ通知します（およそ 1 分ごとです）。

通知は常に均等な間隔で届くとは限りません。
例えば、ノードがピアに追いつくために一度に複数のブロックを追加すると、チャネルはブロックごとに 1 通知を、短い間隔でまとめて配信します。

サブスクリプションには `id`（`id-0`）が付与されます。終了時のサブスクライブ解除に使います。

受信した各メッセージは、以下の整形処理に渡されます。

### メッセージを整形する {: #formatting-the-message }

{{ tutorial.code_snippet_tagged('step-3') }}

受信する各メッセージの本文は、ブロック情報です。[Block](../reference/rest/nem.md#model/Block) スキーマに従います。

スニペットでは、各メッセージから次の 2 フィールドを表示します。

* `height`: 新しいブロックの高さ。
* `signer`: ブロックを生成した [ハーベスターアカウント](default:ハーベスターアカウント) の公開鍵。

NEM のブロックは、このペイロードに自身のハッシュを含まないため、このチュートリアルでは各ブロックを `height` で識別し、ハーベスターの `signer` も表示します。

!!! warning "新しいブロックはまだ確定していません"

    新しいブロックはすでにチェーンの一部ですが、まだ不可逆ではありません。
    後続ブロックが [書き換え制限](default:書き換え制限) を超えるのに十分な数だけ追加されるまでは、[ロールバック](default:ロールバック) が発生する可能性があります。
    ロールバック後は、受信済みのブロックを置き換えるブロックが届くため、チャネルがすでに受け取ったブロックより低い高さのブロックを報告することもあります。

    [チェーン高と不可逆高を照会する](../chain/chain-heights.md) チュートリアルでは、不可逆高の計算方法を説明しています。

### 終了時にサブスクライブ解除する {: #unsubscribing-on-exit }

{{ tutorial.code_snippet_tagged('step-4') }}

プログラムが割り込み（`Ctrl+C`）を受けると、コードはチャネルのサブスクライブを解除し、接続を閉じる前に STOMP セッションを終了します。
これにより、ノードからきれいに切断できます。

## 出力 {: #output }

以下の出力は、新しいブロックをリッスンした場合の実行例です。

```text linenums="1" hl_lines="2 3 4 9"
--8<-- 'devbook/websockets/listen_new_blocks.log'
```

出力には次の内容が表示されます。

* **接続**（2 行目）: ノードのポート `7778` の WebSocket エンドポイント上で STOMP セッションが確立されます。
* **サブスクリプション**（3 行目）: `/blocks` チャネルをサブスクライブします。
* **新しいブロック**（4～8 行目）: およそ 1 分ごとに新しいブロック通知が届きます。
* **サブスクライブ解除**（9 行目）: `Ctrl+C` でコードがサブスクライブを解除して切断します。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [ブロックチャネルをサブスクライブする](#subscribing-to-the-channel) | <ws:blocks> |
| [ブロックメッセージを整形する](#formatting-the-message) | [Block](../reference/rest/nem.md#model/Block) |
