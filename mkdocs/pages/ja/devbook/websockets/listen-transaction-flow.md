---
title: トランザクションフロー
tutorial_level: intermediate
---

# トランザクションフローをリッスンする

NEM は、特定の [アカウント](default:アカウント) の [トランザクション](default:トランザクション) が承認プロセスを進むとき、リアルタイム通知を送信する [WebSocket チャネル](<default:WebSocket チャネル>) を提供します。
<get:/transaction/get> エンドポイントをポーリングする場合と比べ、WebSocket は API を繰り返し呼び出すオーバーヘッドなしに、発生した更新をプッシュします。

このチュートリアルでは、トランザクションチャネルをサブスクライブし、最小限の [転送トランザクション](../transactions/transfer-xem.md) をアナウンスして、WebSocket で承認を待つ方法を説明します。

!!! note "別の方法: ポーリング"

    ポーリングを使う方法については、[トランザクションのステータスを監視する](../transactions/monitoring-status.md) チュートリアルを参照してください。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* 開発環境をセットアップする。
  [開発環境のセットアップ](../start/setup.md) を参照してください。
* 監視するアカウントのアドレスを用意する。
* トランザクション手数料を支払える残高を持つアカウントを用意する。
  [秘密鍵からアカウントを作成する](../accounts/create-from-private-key.md) または [ウォレットでアカウントを作成する](../../userbook/wallet/create-account.md) を参照してください。

さらに、NEM は [SockJS](https://github.com/sockjs/sockjs-client) 上で [STOMP](https://stomp.github.io/) メッセージングプロトコルを使って WebSocket を提供するため、STOMP クライアントと WebSocket トランスポートが必要です。

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

{{ tutorial.code_full_tagged('devbook/websockets/listen_transaction_flow') }}

スニペットでは、`NODE_URL` 環境変数を使って NEM [ノード](default:ノード) を指定します。
値が指定されていない場合は、デフォルト値を使用します。

`WS_URL` は同じノードの WebSocket エンドポイントを定義します。
`NODE_URL` のポート `7890`（デフォルトの HTTP API ポート）を `7778`（デフォルトの NIS WebSocket ポート）に置き換えて導出します。

!!! note "Python の SockJS ヘルパー"

    Python 用の SockJS クライアントライブラリはないため、便宜上、ファイルの先頭に小さなヘルパーメソッドをいくつか定義しています。

## コードの説明 {: #code-explanation }

### 監視対象アドレスと署名者をセットアップする {: #setting-up-the-monitored-address-and-signer }

{{ tutorial.code_snippet_tagged('step-1') }}

この手順では、監視するアドレスと、そこへ送金するアカウントをセットアップします。

`MONITOR_ADDRESS` は監視するアドレスです。
このチュートリアルがサブスクライブするチャネルはこのアドレスに対応付けられ、送金の送信者や受取人など、トランザクションに関係するたびに通知します。
WebSocket API では、アドレスを大文字かつハイフンなしで指定します。

`SIGNER_PRIVATE_KEY` は送金を送信するアカウントの秘密鍵で、通知を発生させます。

これらの環境変数が設定されていない場合は、チュートリアルがデフォルト値を用意します。

### 転送トランザクションを構築して署名する {: #building-and-signing-a-transfer-transaction }

{{ tutorial.code_snippet_tagged('step-2') }}

このチュートリアルでは、監視対象アドレスへ、金額 0、モザイクなし、メッセージなしの最小限の [転送トランザクション](default:転送トランザクション) を構築します。
簡単にするため送金を使いますが、どのトランザクションタイプでも同じ WebSocket 通知を発生させます。

トランザクションは [XEM を送信する](../transactions/transfer-xem.md) チュートリアルと同じ方法で作成して署名します。
ファサードは署名者を追加し、2 時間のデッドライン期間からタイムスタンプとデッドラインを導出します。

トランザクションに署名するとハッシュが生成され、一意に識別できるようになります。
コードはこのハッシュを保存します。トランザクションチャネルの通知にはトランザクションハッシュが含まれるためです。
後で、受信した各ハッシュを保存値と比較して、このトランザクションの通知を特定します。

トランザクションは準備されますが、まだ [アナウンス](#announcing-the-transaction) されません。
チャネルのサブスクリプションを確立した後でアナウンスするため、結果の通知を取り逃しません。

### WebSocket に接続する {: #connecting-to-the-websocket }

{{ tutorial.code_snippet_tagged('step-3') }}

コードは `WS_URL` の `/w/messages` エンドポイントへ SockJS 接続を開き、その上で [STOMP セッション](<default:STOMP セッション>) を開始します。

### チャネルをサブスクライブする {: #subscribing-to-the-channels }

{{ tutorial.code_snippet_tagged('step-4') }}

コードは、アドレスに対応付けられた次の 3 チャネルをサブスクライブします。

* <ws:account&#47;{address}>: アドレスを含む [ブロック](default:ブロック) が承認されると、アカウントの現在の状態を通知します。
* <ws:unconfirmed&#47;{address}>: アドレスに関係するトランザクションが [未承認トランザクションプール](default:未承認トランザクションプール) に入り、ブロックへの包含を待つ状態になると通知します。
* <ws:transactions&#47;{address}>: アドレスに関係するトランザクションが [ブロック](default:ブロック) に含まれると通知します。

サブスクリプションには `id-0`、`id-1`、`id-2` を使います。プログラムが最後にサブスクライブを解除するときに、それぞれを識別します。

!!! note "メッセージ処理の違い"

    JavaScript では、各チャネルを専用のハンドラー関数でサブスクライブします。関数は下の [承認を待つ](#waiting-for-confirmation) 手順で定義します。
    Python では、接続から到着したメッセージを順番に読み取ります。

3 つのチャネルは、次の手順でアドレスを登録するまで何も送信しません。

### アカウントを登録する {: #registering-the-account }

{{ tutorial.code_snippet_tagged('step-5') }}

アカウントのチャネルから通知を受け取るには、まずアドレスをノードに **登録** する必要があります。

コードは <req:w&#47;api&#47;account&#47;get> にリクエストを送信します。アドレスを登録するとともに、<ws:account&#47;{address}> チャネルでアカウントの現在の状態を送信するようノードに要求します。

コードは最初のアカウント通知を待ち、登録が有効になったことを確認します。
通知は [AccountMetaDataPair](../reference/rest/nem.md#model/AccountMetaDataPair) スキーマに従います。

アカウントチャネルのサブスクリプションは実行中ずっと開いたままなので、トランザクションの承認で発生するアカウント通知も出力に表示されます。

### トランザクションをアナウンスする {: #announcing-the-transaction }

{{ tutorial.code_snippet_tagged('step-6') }}

!!! warning "チャネルをサブスクライブしてからアナウンスしてください"

    リスナーの準備ができていることを確実にするため、トランザクションは必ず WebSocket チャネルをサブスクライブした**後**にアナウンスしてください。
    そうしないと、WebSocket がリッスンする前に通知が届く可能性があります。

コードは <post:/transaction/announce> エンドポイントへトランザクションをアナウンスし、結果を確認します。
ノードが拒否した場合は、拒否理由を表示して停止します。

### 承認を待つ {: #waiting-for-confirmation }

{{ tutorial.code_snippet_tagged('step-7') }}

受け付けられた場合、コードは承認を待ち、サブスクライブしたチャネルからの各メッセージを表示します。

トランザクションチャネルからのメッセージは [TransactionMetaDataPair](../reference/rest/nem.md#model/TransactionMetaDataPair) スキーマに従い、`meta.hash.data` フィールドにトランザクションハッシュを持ちます。
メッセージが届くたびに、コードはそのハッシュを保存値と比較して、チャネル通知の中からこのトランザクションを認識します。

成功したトランザクションで想定される順序は、[トランザクションのライフサイクル](../../textbook/transactions.md#transaction-lifecycle) に説明されています。

1. `unconfirmed`: トランザクションが [未承認トランザクションプール](default:未承認トランザクションプール) に入る。
2. `confirmed`: トランザクションが [ブロック](default:ブロック) に含まれる。

トランザクションを含むブロックは、<ws:account&#47;{address}> チャネルでも最後の通知を発生させます。
トランザクションチャネルとは異なり、この通知にはトランザクションハッシュではなくアカウントの更新後の状態が含まれるため、特定のトランザクションとは照合できません。

この最後の通知が届くと、プログラムは後片付けの手順へ進みます。

### チャネルのサブスクライブを解除する {: #unsubscribing-from-channels }

{{ tutorial.code_snippet_tagged('step-8') }}

承認後、コードは 3 つのチャネルのサブスクライブを解除し、接続を閉じる前に STOMP セッションを終了します。

## 出力 {: #output }

```text linenums="1" hl_lines="2-14"
--8<-- 'devbook/websockets/listen_transaction_flow.log'
```

出力には次の内容が表示されます。

* **アドレス**（2 行目）: 監視対象のアドレス。
* **接続**（3 行目）: ノードのポート `7778` の WebSocket エンドポイント上で STOMP セッションが確立されます。
* **サブスクリプション**（4～6 行目）: アカウントチャネルと 2 つのトランザクションチャネルがサブスクライブされます。
* **登録**（7～8 行目）: アカウントの現在の状態がアカウントチャネルに届き、登録を確認します。
* **アナウンス**（9 行目）: トランザクションがアナウンスされ、そのハッシュが表示されます。
* **トランザクションフロー**（10～11 行目）: トランザクションが `unconfirmed` から `confirmed` へ進み、承認のライフサイクルが表示されます。
* **承認**（12 行目）: <ws:transactions&#47;{address}> チャネルのハッシュが、アナウンスしたトランザクションと一致します。
* **アカウント更新**（13 行目）: トランザクションを含むブロックが最後のアカウント通知を発生させます。送金額が 0 なので残高は変わりません。
* **サブスクライブ解除**（14 行目）: コードが 3 つのチャネルのサブスクライブを解除します。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [アカウントチャネルをサブスクライブする](#subscribing-to-the-channels) | <ws:account&#47;{address}> |
| [未承認チャネルをサブスクライブする](#subscribing-to-the-channels) | <ws:unconfirmed&#47;{address}> |
| [トランザクションチャネルをサブスクライブする](#subscribing-to-the-channels) | <ws:transactions&#47;{address}> |
| [アカウントを登録する](#registering-the-account) | <req:w&#47;api&#47;account&#47;get> |
| [トランザクションメッセージを処理する](#waiting-for-confirmation) | [TransactionMetaDataPair](../reference/rest/nem.md#model/TransactionMetaDataPair) |
