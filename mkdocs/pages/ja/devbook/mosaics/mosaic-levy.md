---
title: 徴収手数料付きモザイクを作成する
tutorial_level: advanced
---

# 徴収手数料付きモザイクを作成する

[モザイク](default:モザイク) にはオプションの [徴収手数料](default:徴収手数料（levy）) を含められます。
徴収手数料は転送ごとに送信者へ課され、指定されたアカウントへ付与される手数料です。

徴収手数料の一般的な用途は、資産の背後にあるアカウントへ資金を提供することです。例えば、すべての転送にコミッションやロイヤリティを課します。

このチュートリアルでは、徴収手数料付きモザイクを作成する方法を説明します。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* 開発環境をセットアップする。
    [開発環境のセットアップ](../start/setup.md) を参照してください。
* モザイクを所有する [アカウント](default:アカウント) を、[コード](../accounts/create-from-private-key.md) または [ウォレット](../../userbook/wallet/create-account.md) を使って作成する。
* モザイクが属する [ネームスペース](default:ネームスペース) を登録する。
    [ルートネームスペースを登録する](../namespaces/register-root-namespace.md) を参照してください。
* トランザクションと作成手数料を支払うための [XEM](default:XEM) を用意する。
    [フォーセットからテストネットの資金を取得する](../accounts/testnet-faucet.md) を参照してください。

さらに、モザイク定義の構築、アナウンス、承認の方法を理解するため、[モザイクを作成する](./create-mosaic.md) チュートリアルを確認してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/mosaics/mosaic_levy', ['py', 'js']) }}

## コードの説明 {: #code-explanation }

### アカウントとモザイクをセットアップする {: #setting-up-the-account-and-the-mosaic }

{{ tutorial.code_snippet_tagged('step-1') }}

スニペットは、署名者の秘密鍵を `SIGNER_PRIVATE_KEY` 環境変数から読み込みます。
設定されていない場合は、テスト用のデフォルトキーを使用します。
このアカウントがトランザクションに署名してモザイクの所有者になるため、モザイクを置くネームスペースも所有していなければなりません。
モザイク識別子は、そのネームスペースとモザイク名から組み立てます。
命名規則については、テキストブックの [名前](../../textbook/mosaics.md#name) を参照してください。

チュートリアルを複数回実行したときの衝突を避けるため、モザイク名にタイムスタンプを追加します。
ただし、実際のプログラムでは、モザイクに固定名を使用します。
`NAMESPACE` と `MOSAIC` 環境変数を使うと、チュートリアルで固定名を使用できます。

!!! warning "署名者が所有するネームスペースを使用してください"

    デフォルトでは、コードは `SIGNER_PRIVATE_KEY` が参照するテストアカウントと、`my_namespace` という名前のネームスペースを使用します。

    [ルートネームスペースを登録する](../namespaces/register-root-namespace.md) チュートリアルから続けている場合は、`SIGNER_PRIVATE_KEY` と `NAMESPACE` 環境変数を、そこで作成したアカウントとネームスペース、または署名者が所有する別のネームスペースに合わせて設定してください。

### 徴収手数料を記述する {: #describing-the-levy }

{{ tutorial.code_snippet_tagged('step-2') }}

徴収手数料は、次の 4 つのフィールドを持つ <ser:MosaicLevy> 構造体です。

* {{ tutorial.var('transfer_fee_type') }}: 徴収手数料の金額を計算する方法。

    * `absolute`: 転送量に関係なく、すべての転送に課す固定量。
    * `percentile`: 転送量に比例する量。

    このチュートリアルでは `absolute` の徴収手数料を使うため、すべての転送に同じ金額が課されます。

* {{ tutorial.var('recipient_address') }}: 転送ごとに徴収手数料を受け取るアカウント。
    モザイクの作成者でも、別のアカウントでも構いません。

* {{ tutorial.var('mosaic_id') }}: 徴収手数料を支払うモザイク。
    このチュートリアルでは `nem:xem` で徴収手数料を課すため、送信者はネットワーク通貨で支払います。

    定義するモザイク自身で徴収手数料を支払うこともできます。
    その他の徴収手数料用モザイクはネットワーク上にすでに存在し、[転送可能](../../textbook/mosaics.md#transferability) でなければなりません。

* {{ tutorial.var('fee') }}: 徴収手数料の金額。
    `absolute` の徴収手数料では、徴収手数料用モザイクの [原子単位](../../textbook/mosaics.md#divisibility) で表します。
    `nem:xem` の [可分性](default:可分性) は 6 なので、1'000'000 という値は転送ごとに 1 XEM を課します。

    `percentile` の徴収手数料では、手数料は代わりにベーシスポイントで解釈されます。`fee` が `100` なら、転送量の 1% が課されます。
    完全なルールについては、テキストブックの [パーセンタイル徴収手数料の計算](../../textbook/mosaics.md#percentile-levy-calculation) を参照してください。

### モザイク定義に徴収手数料を付加する {: #attaching-the-levy-to-the-mosaic-definition }

{{ tutorial.code_snippet_tagged('step-3') }}

徴収手数料はモザイク定義の一部なので、[モザイクを作成する](./create-mosaic.md#building-the-mosaic-definition-transaction) で使う <ser:MosaicDefinitionTransactionV1> と同じものを使って設定します。
このチュートリアルでは同じトランザクションを再利用し、{{ tutorial.var('mosaic_definition') }} フィールドに徴収手数料を追加します。
ファサードは署名者を追加し、2 時間のデッドライン期間からタイムスタンプとデッドラインを導出します。

[作成手数料](../../textbook/mosaics.md#creation-fee) は 10 XEM、トランザクション手数料は固定の 0.15 XEM で、[手数料表](../../textbook/transactions.md#fee-schedule) に示されています。

### モザイク定義を送信する {: #submitting-the-mosaic-definition }

{{ tutorial.code_snippet_tagged('step-4') }}

[XEM を送信する](../transactions/transfer-xem.md#announcing-the-transaction) チュートリアルと同じ手順で、トランザクションに署名し、アナウンスして、承認を待ちます。

### 徴収手数料を検証する {: #verifying-the-levy }

{{ tutorial.code_snippet_tagged('step-5') }}

徴収手数料付きモザイクが作成されたことを確認するため、コードは <get:/mosaic/definition> エンドポイントからモザイク定義を取得します。レスポンスにはモザイクプロパティとともに徴収手数料が返されます。

レスポンスに徴収手数料が含まれていれば、今後のモザイク転送に徴収手数料が課されることを確認できます。

## 徴収手数料が課される仕組み {: #how-the-levy-is-charged }

モザイクが作成された後、徴収手数料はモザイクのすべての [転送](../transactions/transfer-mosaics.md) に適用され、転送トランザクションに追加のフィールドは必要ありません。

!!! warning "徴収手数料は保証された徴収ではありません"

    徴収手数料は再帰的ではないため、回避できます。
    テキストブックの [例](../../textbook/mosaics.md#limitations) を参照してください。

徴収手数料は転送量に上乗せして課されます。そのため、このチュートリアルで作成したモザイクを 50 単位転送する送信者からは、次の金額が引き落とされます。

* モザイク 50 単位。転送の受取人に付与されます。
* 1 XEM。徴収手数料の受取人に付与されます。
* トランザクション手数料。[ハーベスターアカウント](default:ハーベスターアカウント) に付与されます。

送信者が転送量と徴収手数料の両方を支払えない場合、ネットワークは転送を拒否します。
この徴収手数料は `nem:xem` で支払うため、送信者は徴収手数料とトランザクション手数料の両方をカバーする十分な XEM も保有しなければなりません。
徴収手数料を別のモザイクで支払う場合、送信者はそのモザイクも十分な残高を保有しなければなりません。

## 出力 {: #output }

以下は、プログラムの実行時の出力例です。

```text linenums="1" hl_lines="3 4-8 56-66 81-84"
--8<-- 'devbook/mosaics/mosaic_levy.log'
```

出力の要点は次のとおりです。

* **モザイク ID**（3 行目）: モザイクは、ネームスペース `my_namespace` とタイムスタンプ付きモザイク名を組み合わせた完全修飾名で識別されます。
    [NEM テストネットエクスプローラー](https://testnet.nem.fyi/) でこの名前を検索すると、モザイクの詳細を確認できます。

* **徴収手数料のフィールド**（4～8 行目）: 作成する徴収手数料。`nem:xem` の原子単位 1'000'000（1 XEM）の `absolute` 手数料で、転送ごとに徴収手数料の受取人へ支払われます。

* **トランザクション内の徴収手数料**（56～66 行目）: 徴収手数料はモザイク定義の内部で定義されます。受取人アドレス、徴収手数料のモザイク名、モザイク名はペイロード内で 16 進数にエンコードされ、この `absolute` 徴収手数料は原子単位で表されます。

* **検証された徴収手数料**（81～84 行目）: ネットワークからモザイクを取得し、徴収手数料のタイプ、受取人、支払いに使うモザイク、その金額を確認します。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [徴収手数料を記述する](#describing-the-levy) | <ser:MosaicLevy> |
| [モザイクに徴収手数料を付加する](#attaching-the-levy-to-the-mosaic-definition) | <dy:NemFacade.createTransactionFromTypedDescriptor>、<ser:MosaicDefinitionTransactionV1> |
| [徴収手数料を検証する](#verifying-the-levy) | <get:/mosaic/definition> |

## 次のステップ {: #next-steps }

徴収手数料付きモザイクを作成できたので、次のことを行えます。

* [転送トランザクションでモザイクを送信する](../transactions/transfer-mosaics.md) ことで、送信者に徴収手数料が課されることを確認する
* [モザイク情報を取得する](./get-mosaic-info.md) ことで、任意のモザイクの徴収手数料を確認する
