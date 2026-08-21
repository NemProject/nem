# WebSocket {: #websockets }

NEM は [WebSocket](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API) でブロックチェーンイベントを公開します。
そのため、[REST API](../rest/nem.md) を常にポーリングしなくても、アプリケーションでライブ更新を受信できます。

クライアントアプリケーションは、ネットワーク内の任意の [ノード](default:ノード) へ WebSocket 接続を開き、監視したい [チャネル](#channels) をサブスクライブします。
チャネルでイベントが発生すると、ノードはサブスクライブしているすべてのクライアントへリアルタイムで通知します。

一部のチャネルは、REST API と同様に、即時データを取得する [リクエスト](#requests) も受け付けます。
これにより、WebSocket だけを API として使い、ライブ通知とオンデマンド更新の両方を処理するアプリケーションを簡単にできます。

## 接続 {: #connection }

NEM は、HTTP API のポートとは別の専用ポート（デフォルト `7778`）で、[SockJS](https://github.com/sockjs/sockjs-client) 上の [STOMP](https://stomp.github.io/) メッセージングプロトコルを使って WebSocket を提供します。

SockJS エンドポイントは `/w/messages` です。例えば `http://localhost:7778/w/messages` です。

クライアントは通常、SockJS クライアントライブラリまたはネイティブ WebSocket API と、メッセージングを処理する STOMP クライアントライブラリを組み合わせて接続します。

??? note "ネイティブ WebSocket で接続する"

    SockJS は WebSocket に似たトランスポートを提供し、クロスブラウザー対応と、ネイティブ WebSocket が利用できない場合の HTTP ベースのフォールバックを備えています。

    ネイティブ WebSocket をサポートするクライアントは、SockJS の WebSocket トランスポートエンドポイント `/w/messages/websocket` に直接接続できます。例えば `ws://localhost:7778/w/messages/websocket` です。

    これは SockJS サーバーを利用しながら、SockJS クライアントライブラリなしで SockJS の WebSocket トランスポートを使う方法です。

## STOMP セッション {: #stomp-session }

STOMP セッション
:   [STOMP](https://stomp.github.io/) メッセージングプロトコルに従い、WebSocket 接続上で行うクライアントとノードの会話です。

クライアントは、[STOMP フレーム](<default:STOMP フレーム>) をノードと交換してセッションを制御します。

1. `CONNECT` フレームを送信して STOMP セッションを開始する。
2. 監視する各 [WebSocket チャネル](<default:WebSocket チャネル>) に `SUBSCRIBE` フレームを送信する。
    各サブスクリプションには、クライアントが定義する `id` が必要です。
3. 明示的な登録が必要なチャネルの通知を有効にするため、オプションの [登録リクエスト](#registration-requests) を `SEND` フレームで送信する。
4. サブスクライブしたチャネルのイベントごとに、ノードから `MESSAGE` フレームを受信する。
5. 通知の受信を停止するため、サブスクライブした各チャネルに `UNSUBSCRIBE` フレームを送信する。
6. セッションを終了するため、`DISCONNECT` フレームを送信する。

!!! warning "接続が静かに切断される可能性があります"

    WebSocket 接続は、例えば長時間アイドル状態だった後など、通知なしに切断される可能性があります。
    ほとんどの STOMP クライアントは接続終了コールバックでこれを報告するため、再接続に適した場所です。

    再接続すると新しいセッションが開始されるため、すべてのチャネルをもう一度サブスクライブする必要があります。

## STOMP フレーム {: #stomp-frames }

STOMP フレーム
:   [STOMP](https://stomp.github.io/) プロトコルに従うプレーンテキストメッセージです。
    コマンド、オプションの `header:value` 行、オプションの本文で構成されます。

クライアントとノードは、次のフレームタイプを交換します。

### `CONNECT` {: #connect }

STOMP セッションを開始します。
接続が開いた直後に、クライアントはこのフレームを 1 回送信する必要があります。
詳細は [STOMP 仕様](https://stomp.github.io/stomp-specification-1.2.html#CONNECT_or_STOMP_Frame) を参照してください。

```stomp title="Example"
CONNECT
accept-version:1.2
heart-beat:0,0
```

### `SUBSCRIBE` {: #subscribe }

クライアントが選んだ `id` と `destination` で [WebSocket チャネル](<default:WebSocket チャネル>) をサブスクライブします。
詳細は [STOMP 仕様](https://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE) を参照してください。

```stomp title="Example"
SUBSCRIBE
id:sub-0
destination:/blocks
```

* `id` は 1 つの接続内でのみ一意です（他のクライアントは同じ値を再利用できます）。
    すべてのメッセージで `subscription` ヘッダーとして返され、後で `UNSUBSCRIBE` に使われます。
* `destination` はチャネルを識別するため、同じ接続で複数のチャネルを監視できます。

### `MESSAGE` {: #message }

ノードからチャネルデータを配信します。
ノードが送信する唯一のフレームタイプです。
詳細は [STOMP 仕様](https://stomp.github.io/stomp-specification-1.2.html#MESSAGE) を参照してください。

```stomp title="Example"
MESSAGE
destination:/blocks
subscription:sub-0
message-id:befkedjj-6247

{ ... }
```

* `destination` は `SUBSCRIBE` フレームのチャネルと一致します。
* `subscription` は `SUBSCRIBE` フレームの `id` と一致します。
* `message-id` はサーバーが各メッセージに割り当てる一意の識別子です。
* `{ ... }` は本文で、形状は [チャネル](#channels) によって異なる JSON オブジェクトです。
    下の **メッセージ本文** タブを参照してください。

### `SEND` {: #send }

`/w/api` 宛先へ [WebSocket リクエスト](<default:WebSocket リクエスト>) を送信します。
詳細は [STOMP 仕様](https://stomp.github.io/stomp-specification-1.2.html#SEND) を参照してください。

```stomp title="Example"
SEND
destination:/w/api/account/subscribe

{ "account": "{address}" }
```

### `UNSUBSCRIBE` {: #unsubscribe }

`id` でサブスクリプションをキャンセルします。
詳細は [STOMP 仕様](https://stomp.github.io/stomp-specification-1.2.html#UNSUBSCRIBE) を参照してください。

```stomp title="Example"
UNSUBSCRIBE
id:sub-0
```

### `DISCONNECT` {: #disconnect }

セッションを終了します。
詳細は [STOMP 仕様](https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT) を参照してください。

```stomp title="Example"
DISCONNECT
```

## チャネル {: #channels }

WebSocket チャネル
:   ノードの通知をチャネルにまとめます。
    クライアントは受信したい通知を持つ各チャネルをサブスクライブします。

すべてのチャネルは [`SUBSCRIBE`](#subscribe) フレームでサブスクライブします。

ここでは、報告するイベントの種類ごとに利用可能なチャネルをまとめます。

### ブロックチャネル {: #block-channels }

チェーンに追加された新しいブロックを報告するチャネルです。

#### `/blocks` {: #blocks }

ws:blocks
:   新しいブロックがチェーンに追加されるたびに、サブスクライブしたクライアントへ通知します。

    ノードがピアに追いついている場合や [ロールバック](default:ロールバック) の後など、一度に複数のブロックが追加されると、チャネルは **バースト** を送信します。ブロックごとに 1 通知を、チェーン順に短い間隔で配信します。

    ロールバック後は、新しいブロックがすでに報告したブロックを置き換えるため、以前の通知より低いブロック高さを通知することがあります。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/blocks
```

</td><td markdown>

```stomp
MESSAGE
destination:/blocks
subscription:sub-0
message-id:...

{ ... }
```

[Block](../rest/nem.md#model/Block) JSON 本文が続きます。
</td></tr></table></div>

#### `/blocks/new` {: #blocksnew }

ws:blocks&#47;new
:   チェーンが変更されるたびに、追加または置き換えられた最初のブロックの高さを含む通知を、更新ごとに 1 つ送信します。

    <ws:blocks> とは異なり、含まれるブロック数に関係なく、チェーン更新ごとに 1 通知だけ送信します。
    例えば 5 ブロックが一度に追加された場合、<ws:blocks> は 5 通知を送信しますが、このチャネルは最初のブロックの高さを含む 1 通知だけを送信します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/blocks/new
```

</td><td markdown>

```stomp
MESSAGE
destination:/blocks/new
subscription:sub-0
message-id:...

{ "height": 1234567 }
```

</td></tr></table></div>

### トランザクションチャネル {: #transaction-channels }

関係するアカウントにかかわらず、トランザクションの活動を報告するチャネルです。

#### `/unconfirmed` {: #unconfirmed }

ws:unconfirmed
:   関係するアカウントにかかわらず、トランザクションが [未承認トランザクションプール](default:未承認トランザクションプール) に入るたびにサブスクライブしたクライアントへ通知します。

    ここには [連署](default:連署) を含むすべてのトランザクションタイプが現れます。連署は [アカウントチャネル](#account-channels) では報告されません。
    [マルチシグトランザクション](default:マルチシグトランザクション) は外側のマルチシグトランザクションとして現れ、その内部に内部トランザクションがネストされます。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/unconfirmed
```

</td><td markdown>

```stomp
MESSAGE
destination:/unconfirmed
subscription:sub-0
message-id:...

{ ... }
```

[Transaction](../rest/nem.md#model/Transaction) JSON 本文が続きます。
</td></tr></table></div>

### アカウントチャネル {: #account-channels }

残高、トランザクション、所有するモザイクやネームスペースなど、特定のアカウントの活動を報告するチャネルです。

!!! note "アドレス形式"

    チャネルの `destination` またはリクエスト本文にアドレスが現れる場合は、[エンコードされたアドレス](../../../textbook/cryptography.md#addresses) 形式を使います。
    大文字と数字のみで、ハイフンはありません。

    **例:** `TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4`。

アカウントチャネルは、アカウントの状態が変わらない場合でも、アカウントがトランザクションに **関係する** たびに通知します。

関係するとみなされるアカウントは、トランザクションタイプによって異なります。

| トランザクションタイプ | 関係するアカウント |
| --- | --- |
| 転送 | 署名者と受取人。 |
| インポータンス転送 | 署名者とリモートアカウント。 |
| マルチシグアカウント変更 | 追加または削除されたすべての連署者と署名者。 |
| ネームスペース登録 | 署名者。 |
| モザイク定義の作成 | 署名者、および定義に徴収手数料が含まれる場合は徴収手数料の受取人。 |
| モザイク供給量の変更 | 署名者、およびモザイク定義に徴収手数料が含まれる場合は徴収手数料の受取人。 |
| マルチシグ | 開始した連署者、マルチシグアカウント、内部トランザクションに含まれるその他のアカウント。 |

!!! note "マルチシグトランザクション"

    開始した連署者と内部送金の受取人など、複数の役割を持つアカウントには、役割ごとに 1 通知が届きます。

    [マルチシグトランザクション](default:マルチシグトランザクション) に複数の署名が必要な場合、追加の各連署者は別の [連署](default:連署) を送信して承認します。
    連署はグローバルな <ws:unconfirmed> チャネルにだけ現れます。
    マルチシグアカウントや送信した連署者のチャネルを含め、アカウントチャネルには届きません。

#### `/account/{address}` {: #accountaddress }

ws:account&#47;{address}
:   アドレスが関係する承認済みブロックごとに、アカウントの現在の状態を通知します。トランザクションで [関係する](#account-channels) 場合と、ブロックをハーベスティングした場合が含まれます。
    先にアドレスを [登録](#registration-requests) する必要があります。

    トランザクションに関係しても、アカウントが変化したとは限りません。
    例えば XEM を 0 転送した受取人は、残高が変わらなくても通知されます。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/account/{address}
```

</td><td markdown>

```stomp
MESSAGE
destination:/account/{address}
subscription:sub-0
message-id:...

{ ... }
```

[AccountMetaDataPair](../rest/nem.md#model/AccountMetaDataPair) JSON 本文が続きます。
</td></tr></table></div>

#### `/unconfirmed/{address}` {: #unconfirmedaddress }

ws:unconfirmed&#47;{address}
:   アカウントに [関係する](#account-channels) トランザクションが [未承認トランザクションプール](default:未承認トランザクションプール) に入るたびに、サブスクライブしたクライアントへ通知します。
    先にアドレスを [登録](#registration-requests) する必要があります。

    トランザクションはまだブロックに含まれていないため、`meta.height` フィールドには JSON パーサーが安全に表現できる最大の整数であるプレースホルダー `9007199254740991` が含まれます。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/unconfirmed/{address}
```

</td><td markdown>

```stomp
MESSAGE
destination:/unconfirmed/{address}
subscription:sub-0
message-id:...

{ ... }
```

[TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair) JSON 本文が続きます。
</td></tr></table></div>

#### `/transactions/{address}` {: #transactionsaddress }

ws:transactions&#47;{address}
:   アカウントに [関係する](#account-channels) トランザクションを承認済みブロックが含むたびに、サブスクライブしたクライアントへ通知します。
    先にアドレスを [登録](#registration-requests) する必要があります。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/transactions/{address}
```

</td><td markdown>

```stomp
MESSAGE
destination:/transactions/{address}
subscription:sub-0
message-id:...

{ ... }
```

[TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair) JSON 本文が続きます。
</td></tr></table></div>

#### `/account/mosaic/owned/{address}` {: #accountmosaicownedaddress }

ws:account&#47;mosaic&#47;owned&#47;{address}
:   承認済みブロックによってアカウントのモザイクが変わった可能性があるたびに、アカウントのモザイクを通知します。
    アカウントのモザイクとは、残高を保有するモザイクと、作成したモザイクです。

    モザイク関連のトランザクションがアカウントに [関係する](#account-channels) ブロックに含まれると、通知バーストを送信します。
    モザイクを運ぶ転送は、署名者、受取人、徴収手数料の受取人へ通知します。
    モザイク定義の作成とモザイク供給量の変更は、モザイク作成者と徴収手数料の受取人へ通知します。

    トランザクションがアカウントのモザイクを変更する必要はありません。
    例えばモザイクを 0 単位転送しても、送信者と受取人の両方へ通知します。

    各バーストにはアカウントのモザイク一覧全体が含まれ、変更されたモザイクにかかわらずモザイクごとに 1 通知が届きます。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/account/mosaic/owned/{address}
```

</td><td markdown>

```stomp
MESSAGE
destination:/account/mosaic/owned/{address}
subscription:sub-0
message-id:...

{ ... }
```

[Mosaic](../rest/nem.md#model/Mosaic) JSON 本文が続きます。
</td></tr></table></div>

#### `/account/mosaic/owned/definition/{address}` {: #accountmosaicowneddefinitionaddress }

ws:account&#47;mosaic&#47;owned&#47;definition&#47;{address}
:   承認済みブロックによって変わった可能性があるたびに、アカウントのモザイク定義を通知します。

    <ws:account/mosaic/owned/{address}> と同じトランザクションによって発生し、同じモザイクを対象とします。
    各バーストにはアカウントのモザイク定義一覧全体が含まれ、変更されたモザイクにかかわらず定義ごとに 1 通知が届きます。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/account/mosaic/owned/definition/{address}
```

</td><td markdown>

```stomp
MESSAGE
destination:/account/mosaic/owned/definition/{address}
subscription:sub-0
message-id:...

{ ... }
```

[MosaicDefinitionSupplyTuple](../rest/nem.md#model/MosaicDefinitionSupplyTuple) JSON 本文が続きます。
</td></tr></table></div>

#### `/account/namespace/owned/{address}` {: #accountnamespaceownedaddress }

ws:account&#47;namespace&#47;owned&#47;{address}
:   承認済みブロックによって変わった可能性があるたびに、アカウントが所有するネームスペースを通知します。

    アカウントが署名したネームスペース提供トランザクションをブロックが含むと、通知バーストを送信します。
    各バーストには所有するネームスペース一覧全体が含まれ、ネームスペースごとに 1 通知が届きます。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/account/namespace/owned/{address}
```

</td><td markdown>

```stomp
MESSAGE
destination:/account/namespace/owned/{address}
subscription:sub-0
message-id:...

{ ... }
```

[Namespace](../rest/nem.md#model/Namespace) JSON 本文が続きます。
</td></tr></table></div>

#### `/recenttransactions/{address}` {: #recenttransactionsaddress }

ws:recenttransactions&#47;{address}
:   <req:w&#47;api&#47;account&#47;transfers&#47;all> への応答としてのみ、アカウントの最新 25 件の承認済みトランザクションを通知します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/recenttransactions/{address}
```

</td><td markdown>

```stomp
MESSAGE
destination:/recenttransactions/{address}
subscription:sub-0
message-id:...

{ ... }
```

`data` フィールドにラップされた [TransactionMetaDataPair](../rest/nem.md#model/TransactionMetaDataPair) の一覧が続きます。
</td></tr></table></div>

### システムチャネル {: #system-channels }

ブロックチェーンイベントではなく、ノードの状態とリクエストエラーを報告するチャネルです。

#### `/node/info` {: #nodeinfo }

ws:node&#47;info
:   <req:w&#47;api&#47;node&#47;info> への応答としてのみ、ノードの情報をサブスクライブしたクライアントへ通知します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/node/info
```

</td><td markdown>

```stomp
MESSAGE
destination:/node/info
subscription:sub-0
message-id:...

{ ... }
```

[Node](../rest/nem.md#model/Node) JSON 本文が続きます。
</td></tr></table></div>

#### `/errors` {: #errors }

ws:errors
:   `/w/api` の [WebSocket リクエスト](<default:WebSocket リクエスト>) が失敗したとき、例えばアドレスのペイロードが無効なときに、サブスクライブしたクライアントへ通知します。
    クライアントは接続直後にこのチャネルをサブスクライブできるため、問題が静かに破棄されず、ここに現れます。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: サブスクリプションフレーム</th><th markdown>:material-arrow-down-bold: 通知フレーム</th></tr>
<tr markdown><td markdown>

```stomp
SUBSCRIBE
id:sub-0
destination:/errors
```

</td><td markdown>

```stomp
MESSAGE
destination:/errors
subscription:sub-0
message-id:...

{
    "timeStamp": 67191609,
    "status": 400,
    "error": "Bad Request",
    "message": "account is not valid"
}
```

</td></tr></table></div>

## リクエスト {: #requests }

WebSocket リクエスト
:   クライアントから送信するメッセージで、ノードに次のいずれかを配信させます。
    **登録** が必要なチャネルの通知、またはブロックチェーンの状態の **スナップショット** を含む即時通知です。

リクエストは **読み取り専用** で、チェーンの状態を変更しません。

すべてのリクエストは [`SEND`](#send) フレームで、`/w/api/` から始まる宛先へ送信します。
応答を返さないリクエストもあれば、上の [チャネル](#channels) のいずれかを通して結果を返すリクエストもあります。

### 登録リクエスト {: #registration-requests }

一部の [アカウントチャネル](#account-channels) は、アドレスを登録するまで何も送信しません。
これらのリクエストで登録すると、チャネルが通知を配信し始めます。

!!! note "登録は共有されます"

    ノードは、接続しているすべてのクライアントで共有する登録アドレスの一覧を 1 つ保持します。
    どのクライアントかがアドレスを登録すると、そのアカウントのチャネルをサブスクライブしているすべてのクライアントが、再登録なしでそのアドレスの通知を受け取ります。
    登録はノードが再起動するまで有効です。

#### `/w/api/account/subscribe` {: #wapiaccountsubscribe }

req:w&#47;api&#47;account&#47;subscribe
:   アドレスを登録し、ノードが [アカウントチャネル](#account-channels) に通知を送信し始めるようにします。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/account/subscribe

{ "account": "{address}" }
```

</td></tr></table></div>

#### `/w/api/account/get` {: #wapiaccountget }

req:w&#47;api&#47;account&#47;get
:   <req:w&#47;api&#47;account&#47;subscribe> と同じようにアドレスを登録し、アカウントの現在の状態を含む通知を <ws:account&#47;{address}> に送信するようノードに要求します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/account/get

{ "account": "{address}" }
```

</td></tr></table></div>

### スナップショットリクエスト {: #snapshot-requests }

各リクエストは、新しいイベントを待たずに、現在のデータのスナップショットをノードからチャネルへ即時送信させます。

これにより、[REST API](../rest/nem.md) をポーリングする代わりに、ライブ更新と同じチャネルを通してオンデマンドで現在のデータを取得できます。

#### `/w/api/account/transfers/all` {: #wapiaccounttransfersall }

req:w&#47;api&#47;account&#47;transfers&#47;all
:   アカウントの最新 25 件までの承認済みトランザクションを <ws:recenttransactions&#47;{address}> に送信し、<req:w&#47;api&#47;account&#47;transfers&#47;unconfirmed> と同じ方法で保留中のトランザクションも送信するようノードに要求します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/account/transfers/all

{ "account": "{address}" }
```

</td></tr></table></div>

#### `/w/api/account/transfers/unconfirmed` {: #wapiaccounttransfersunconfirmed }

req:w&#47;api&#47;account&#47;transfers&#47;unconfirmed
:   アカウントの最新の保留中トランザクションを最大 10 件、<ws:unconfirmed&#47;{address}> とグローバルな <ws:unconfirmed> チャネルに送信するようノードに要求します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/account/transfers/unconfirmed

{ "account": "{address}" }
```

</td></tr></table></div>

#### `/w/api/account/mosaic/owned` {: #wapiaccountmosaicowned }

req:w&#47;api&#47;account&#47;mosaic&#47;owned
:   アカウントが所有するモザイクを含む通知を <ws:account&#47;mosaic&#47;owned&#47;{address}> に送信するようノードに要求します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/account/mosaic/owned

{ "account": "{address}" }
```

</td></tr></table></div>

#### `/w/api/account/mosaic/owned/definition` {: #wapiaccountmosaicowneddefinition }

req:w&#47;api&#47;account&#47;mosaic&#47;owned&#47;definition
:   アカウントが所有するモザイク定義を含む通知を <ws:account&#47;mosaic&#47;owned&#47;definition&#47;{address}> に送信するようノードに要求します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/account/mosaic/owned/definition

{ "account": "{address}" }
```

</td></tr></table></div>

#### `/w/api/account/namespace/owned` {: #wapiaccountnamespaceowned }

req:w&#47;api&#47;account&#47;namespace&#47;owned
:   アカウントが所有するネームスペースを含む通知を <ws:account&#47;namespace&#47;owned&#47;{address}> に送信するようノードに要求します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/account/namespace/owned

{ "account": "{address}" }
```

</td></tr></table></div>

#### `/w/api/block/last` {: #wapiblocklast }

req:w&#47;api&#47;block&#47;last
:   最新ブロックを含む通知を <ws:blocks> に送信するようノードに要求します。

    ノードはブロックが今追加されたかのように処理するため、そのブロックに関係するアカウントの [アカウントチャネル](#account-channels) にも再び通知します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/block/last
```

</td></tr></table></div>

#### `/w/api/node/info` {: #wapinodeinfo }

req:w&#47;api&#47;node&#47;info
:   ノード自身の情報を含む通知を <ws:node&#47;info> に送信するようノードに要求します。

<div class="frame-table" markdown>
<table markdown>
<tr markdown><th markdown>:material-arrow-up-bold: リクエストフレーム</th></tr>
<tr markdown><td markdown>

```stomp
SEND
destination:/w/api/node/info
```

</td></tr></table></div>
