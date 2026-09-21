---
title: モザイクを作成する
tutorial_level: intermediate
---

# モザイクを作成する

[モザイク](default:モザイク) は、通貨、コレクティブル、アクセス権など、NEM ブロックチェーン上の資産を表します。
他のプラットフォームのトークンとは異なり、NEM のモザイクはプロトコルレベルで直接サポートされているため、利用に追加のコーディングは必要ありません。

モザイクのプロパティは設定可能で、単純な通貨から、供給量や転送ルールをカスタマイズしたトークンまで、さまざまな用途に対応できます。

すべてのモザイクは登録済みの [ネームスペース](default:ネームスペース) に属します。
ネームスペースは、`my_namespace:token` のような [完全修飾名](../../textbook/mosaics.md#fully-qualified-name) の前半部分を提供します。
そのため、モザイクを作成する前にネームスペースを登録する必要があります。

このチュートリアルでは、既存のネームスペースの下にモザイクを作成し、初期プロパティを設定する方法を説明します。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* 開発環境をセットアップする。
    [開発環境のセットアップ](../start/setup.md) を参照してください。
* モザイクを所有する [アカウント](default:アカウント) を、[コード](../accounts/create-from-private-key.md) または [ウォレット](../../userbook/wallet/create-account.md) を使って作成する。
* モザイクが属する [ネームスペース](default:ネームスペース) を登録する。
    [ルートネームスペースを登録する](../namespaces/register-root-namespace.md) を参照してください。
* トランザクションと作成手数料を支払うための [XEM](default:XEM) を用意する。
    [フォーセットからテストネットの資金を取得する](../accounts/testnet-faucet.md) を参照してください。

さらに、トランザクションのアナウンスと承認の方法を理解するため、[XEM を送信する](../transactions/transfer-xem.md) チュートリアルを確認してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/mosaics/create_mosaic', ['py', 'js']) }}

## コードの説明 {: #code-explanation }

### アカウントをセットアップする {: #setting-up-the-account }

{{ tutorial.code_snippet_tagged('step-1') }}

スニペットは、署名者の秘密鍵を `SIGNER_PRIVATE_KEY` 環境変数から読み込みます。
設定されていない場合は、テスト用のデフォルトキーを使用します。
署名者のアドレスは公開鍵から導出されます。
このアカウントが作成したモザイクを所有し、モザイクが属するネームスペースも所有していなければなりません。

### モザイク名を設定する {: #choosing-the-mosaic-name }

{{ tutorial.code_snippet_tagged('step-2') }}

モザイク ID は、既存のネームスペースとモザイク名から組み立てられます。
命名規則については、テキストブックの [名前](../../textbook/mosaics.md#name) を参照してください。

チュートリアルを複数回実行したときの衝突を避けるため、モザイク名にタイムスタンプを追加します。
ただし、実際のプログラムでは、モザイクに固定名を使用します。
`NAMESPACE` と `MOSAIC` 環境変数を使うと、チュートリアルで固定名を使用できます。

!!! warning "署名者が所有するネームスペースを使用してください"

    デフォルトでは、コードは `SIGNER_PRIVATE_KEY` が参照するテストアカウントと、`my_namespace` という名前のネームスペースを使用します。

    [ルートネームスペースを登録する](../namespaces/register-root-namespace.md) チュートリアルから続けている場合は、`SIGNER_PRIVATE_KEY` と `NAMESPACE` 環境変数を、そこで作成したアカウントとネームスペース、または署名者が所有する別のネームスペースに合わせて設定してください。

### モザイクを定義する {: #defining-the-mosaic }

{{ tutorial.code_snippet_tagged('step-3') }}

モザイクの定義では、資産そのものを、それを登録するトランザクションとは区別して記述しています：

* {{ tutorial.var('owner_public_key') }}: モザイクを作成するアカウントの [公開鍵](default:公開鍵)。{{ tutorial.var('signer_public_key') }} と一致しなければなりません。
    2 つが異なるトランザクションはネットワークに拒否されます。

* {{ tutorial.var('id') }}: ネームスペースとモザイク名から作られるモザイク識別子。

* {{ tutorial.var('description') }}: モザイクを [説明する](../../textbook/mosaics.md#description) テキスト。

* {{ tutorial.var('properties') }}: モザイクの動作を設定するキーと値の組。

    * {{ tutorial.var('divisibility') }}: モザイクがサポートする小数桁数（可分性）。
        例えば `2` は、1 全体単位を 100（10^2^）原子単位に分割できることを意味します。
        テキストブックの [可分性](default:可分性) を参照してください。
    * {{ tutorial.var('initialSupply') }}: モザイク定義時に作成者へ発行される全体単位の数。
        テキストブックの [初期供給量](../../textbook/mosaics.md#initial-supply) を参照してください。
    * {{ tutorial.var('supplyMutable') }}: 作成後に総供給量を変更できるかどうか。
        テキストブックの [供給量の可変性](../../textbook/mosaics.md#supply-mutability) を参照してください。
    * {{ tutorial.var('transferable') }}: 作成者以外の任意の 2 アカウント間でモザイクを送信できるかどうか。
        テキストブックの [転送可能性](../../textbook/mosaics.md#transferability) を参照してください。

    この例では、モザイクは小数点以下 2 桁まで分割可能で、`1000.00` 全体単位の供給量で始まります。
    作成後に供給量を変更でき、アカウント間で自由に転送できます。

!!! note "オプションの徴収手数料"

    モザイク定義には、オプションの [徴収手数料](default:徴収手数料（levy）) も含められます。
    詳細については、[徴収手数料付きモザイクを作成する](./mosaic-levy.md) チュートリアルを参照してください。

### モザイク定義トランザクションを構築する {: #building-the-mosaic-definition-transaction }

{{ tutorial.code_snippet_tagged('step-4') }}

モザイク定義トランザクションでは、次の項目を指定してネットワークにモザイクを登録します。

* **トランザクションタイプ:** モザイク定義トランザクションでは <ser:MosaicDefinitionTransactionV1> を使用します。

* {{ tutorial.var('rental_fee_sink') }}: モザイクの [作成手数料](../../textbook/mosaics.md#creation-fee) を集める特別なアカウント。
    各ネットワークには固定されたシンクアドレスがあります。

    * [メインネット](default:メインネット): `NBMOSAICOD4F54EE5CDMR23CCBGOAM2XSIUX6TRS`
    * [テストネット](default:テストネット): `TBMOSAICOD4F54EE5CDMR23CCBGOAM2XSJBR5OLC`

    ネットワークは、作成手数料を他のアドレスへ送るトランザクションを拒否します。

* {{ tutorial.var('rental_fee') }}: 10 XEM の作成手数料。
    SDK の <dy:FeeCalculator.calculateMosaicRentalFee> ヘルパーは、必要な金額を返します。

    ネットワークは、この手数料を下回る金額を支払うトランザクションを拒否します。
    より大きい金額は受け付けられますが、全額がシンクアカウントへ送られます。

* {{ tutorial.var('mosaic_definition') }}: 前の手順で構築したモザイク定義。

ファサードは署名者を追加し、2 時間のデッドライン期間からタイムスタンプとデッドラインを導出します。
署名者はネームスペースを所有している必要があり、作成したモザイクの所有者になります。

{{ tutorial.code_snippet_tagged('step-5') }}

最後に、<dy:FeeCalculator.calculateTransactionFee> でトランザクション手数料を計算し、トランザクションに付加します。
作成手数料とは異なり、トランザクション手数料は [ハーベスターアカウント](default:ハーベスターアカウント) に支払われます。
モザイク定義トランザクションの固定手数料は 0.15 XEM で、[手数料表](../../textbook/transactions.md#fee-schedule) に示されています。

### モザイク定義を送信する {: #submitting-the-mosaic-definition }

{{ tutorial.code_snippet_tagged('step-6') }}

[XEM を送信する](../transactions/transfer-xem.md#announcing-the-transaction) チュートリアルと同じ手順で、モザイク定義トランザクションに署名してアナウンスします。

{{ tutorial.code_snippet_tagged('step-7') }}

次にコードは、トランザクションがブロックに含まれるまで <get:/transaction/get> エンドポイントをポーリングし、承認を待ちます。

### モザイクを取得する {: #retrieving-the-mosaic }

{{ tutorial.code_snippet_tagged('step-8') }}

モザイクが正常に作成されたことを確認するため、コードは <get:/mosaic/definition> エンドポイントから定義を取得し、そのプロパティを表示します。

レスポンスが成功すると、そのモザイクがネットワーク上に存在し、期待したプロパティを持っていることを確認できます。

!!! note "モザイクの有効期間"

    モザイク自身に有効期間はなく、親ネームスペースの有効期限が切れると非アクティブになります。
    [ルートネームスペースを延長する](../namespaces/extend-root-namespace.md) と、モザイクが使用可能な状態を維持できます。
    詳細については、テキストブックの [有効期間](../../textbook/mosaics.md#lifetime) を参照してください。

## 出力 {: #output }

以下は、プログラムの実行時の出力例です。

```text linenums="1" hl_lines="3 4 5 65 66 67 68"
--8<-- 'devbook/mosaics/create_mosaic.log'
```

出力の要点は次のとおりです。

* **モザイク ID**（3 行目）: モザイクは、ネームスペース `my_namespace` とタイムスタンプ付きモザイク名を組み合わせた完全修飾名で識別されます。
    [NEM テストネットエクスプローラー](https://testnet.nem.fyi/) でこの名前を検索すると、モザイクの詳細を確認できます。

* **作成手数料とトランザクション手数料**（4～5 行目）: 作成手数料は 10 XEM、トランザクション手数料は 0.15 XEM です。

* **確認されたプロパティ**（65～68 行目）: ネットワークからモザイクを取得し、可分性、初期供給量 `1000`、供給量が可変で転送可能であることを確認します。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [モザイクを定義する](#defining-the-mosaic) | <dy:NemFacade.createTransactionFromTypedDescriptor>、<ser:MosaicDefinitionTransactionV1> |
| [作成手数料を計算する](#building-the-mosaic-definition-transaction) | <dy:FeeCalculator.calculateMosaicRentalFee> |
| [モザイクを取得する](#retrieving-the-mosaic) | <get:/mosaic/definition> |

## 次のステップ {: #next-steps }

モザイクを作成できたので、次のことを行えます。

* [供給量が可変](../../textbook/mosaics.md#supply-mutability) として作成したモザイクを発行または焼却するために、[モザイクの供給量を変更する](./change-mosaic-supply.md)
* [転送トランザクションでモザイクを送信する](../transactions/transfer-mosaics.md) ことで、他のアカウントに配布する
* [モザイク情報を取得する](./get-mosaic-info.md) ことで、任意のモザイクのプロパティと供給量を確認する
* 配布前にプロパティを変更するために、[モザイク定義を変更する](./modify-mosaic-definition.md)
