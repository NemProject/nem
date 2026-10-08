---
title: Hello World
tutorial_level: beginner
---

# Hello World

このチュートリアルでは、最小限のプログラムを作成して Symbol SDK が正しく動作することを確認します。
プログラムは次の処理を行います。

* SDK を使ってネットワーク名と起動日を取得する。
* [ノード](default:ノード) に接続し、現在のチェーン高を表示する。

必要なのは基本的な SDK 呼び出しと REST リクエストだけで、アカウント、キー、トランザクションは必要ありません。

## 前提条件 {: #prerequisites }

まだ準備できていない場合は、まず [開発環境のセットアップ](../start/setup.md) を行ってください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/start/hello_world') }}

### SDK の呼び出し {: #making-sdk-calls }

{{ tutorial.code_snippet_tagged('step-1') }}

<dy:NemFacade> クラスは、NEM ブロックチェーンで Symbol SDK を使用する際の主なエントリーポイントです。
トランザクションの構築と署名からネットワーク関連情報の取得まで、必要になるほとんどのメソッドを提供します。

ファサードを作成するには、`mainnet` または `testnet` のいずれかの、使用するネットワーク名を指定します。

この例では、ネットワークの起動日も取得します。
<dy:NetworkTimestampDatetimeConverter.toDatetime> メソッドは、ネットワークタイムスタンプを UTC の日時に変換します。
ジェネシスタイムスタンプである `0` を渡すと、ジェネシスブロックが生成された時点、つまりネットワークの起動日を取得できます。

### ノードから情報を取得する {: #retrieving-information-from-a-node }

{{ tutorial.code_snippet_tagged('step-2') }}

NEM ブロックチェーンとのやりとりは、[ノード](default:ノード) を通じて行われます。このノードは、ネットワークの状態を照会したり、トランザクションを送信するための REST インターフェースを提供しています。

この例では、テストネットのノードに接続し、<get:/chain/height> エンドポイントから現在のチェーン高を取得します。

このリクエストに秘密鍵や認証の必要はなく、環境が正しくセットアップされ、ネットワークに接続できることを確認するための簡単で効果的なテストになります。

## 出力 {: #output }

以下は、プログラムの実行時の出力例です。

```text
--8<-- 'devbook/start/hello_world.log'
```

## まとめ {: #conclusion }

上記の出力が表示されたら、準備は完了です。
Symbol SDK にアクセスでき、NEM ノードへの接続にも成功しています。

これで NEM の冒険を始めるための準備は万端です。

次は [アカウントを作成](../accounts/create-from-private-key.md) してみてはいかがでしょうか。
