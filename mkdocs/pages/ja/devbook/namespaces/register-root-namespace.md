---
title: ルートネームスペースを登録する
tutorial_level: intermediate
---

# ルートネームスペースを登録する

[ネームスペース](default:ネームスペース) は、ネイティブの `nem:xem` モザイクにおける `nem` プレフィックスのように、関連する [モザイク](default:モザイク) を意味のある名前でまとめるラベルを提供します。

ネームスペースは他のネームスペースの下にネストできます。このチュートリアルでは、1 年間の [ルートネームスペース](default:ルートネームスペース) を登録する方法を説明します。

ルートネームスペースではなく [サブネームスペース](default:サブネームスペース) を登録する方法については、[サブネームスペースを登録する](./register-subnamespace.md) ガイドを参照してください。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* 開発環境をセットアップする。
  [開発環境のセットアップ](../start/setup.md) を参照してください。
* ネームスペースを登録する [アカウント](default:アカウント) を、[コードから](../accounts/create-from-private-key.md) または [ウォレットを使って](../../userbook/wallet/create-account.md) 作成する。
* トランザクションとリース手数料を支払うための [XEM](default:XEM) を用意する。
  [フォーセットからテストネットの資金を取得する](../accounts/testnet-faucet.md) を参照してください。

さらに、トランザクションのアナウンスと承認の方法を理解するため、[XEM を送信する](../transactions/transfer-xem.md) チュートリアルを確認してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/namespaces/register_root_namespace', ['py', 'js']) }}

## コードの説明 {: #code-explanation }

### アカウントをセットアップする {: #setting-up-the-account }

{{ tutorial.code_snippet_tagged('step-1') }}

スニペットは、署名者の秘密鍵を `SIGNER_PRIVATE_KEY` 環境変数から読み込みます。
設定されていない場合は、テスト用のデフォルトキーを使用します。
署名者のアドレスは公開鍵から導出されます。
このアカウントが登録したネームスペースを所有します。

### ネットワーク時刻を取得する {: #fetching-network-time }

{{ tutorial.code_snippet_tagged('step-2') }}

ネットワーク時刻は <get:/time-sync/network-time> から取得し、[XEM を送信する](../transactions/transfer-xem.md) チュートリアルで説明されている手順に従って、トランザクションの `timestamp` と `deadline` フィールドを導出します。

### ネームスペース名を選択する {: #choosing-the-namespace-name }

{{ tutorial.code_snippet_tagged('step-3') }}

ネームスペースは名前で識別され、その名前をトランザクションによってネットワーク上で 1 年間予約します。
命名規則については、テキストブックの [名前](../../textbook/namespaces.md#name) を参照してください。

チュートリアルを複数回実行したときの衝突を避けるため、名前にタイムスタンプを追加します。
ただし、実際のプログラムでは、ネームスペースに固定名を使用します。
`ROOT_NAMESPACE` 環境変数を使うと、チュートリアルで固定名を使用できます。

### トランザクションを構築する {: #building-the-transaction }

{{ tutorial.code_snippet_tagged('step-4') }}

ネームスペース登録トランザクションでは、次の項目を指定してネットワークにネームスペースを登録します。

* {{ tutorial.var('type') }}: ネームスペース登録トランザクションでは、タイプ <ser:NamespaceRegistrationTransactionV1> を使用します。

* {{ tutorial.var('signer_public_key') }}: トランザクションに署名して手数料を支払うアカウント。
    登録したネームスペースの所有者になります。

* {{ tutorial.var('timestamp') }} と {{ tutorial.var('deadline') }}: ネットワーク時刻の手順で計算した値。

* {{ tutorial.var('rental_fee_sink') }}: ネームスペースの [リース手数料](../../textbook/namespaces.md#lease-fee) を集める特別なアカウント。
    各ネットワークには固定されたシンクアドレスがあります。

    * [メインネット](default:メインネット): `NAMESPACEWH4MKFMBCVFERDPOOP4FK7MTBXDPZZA`
    * [テストネット](default:テストネット): `TAMESPACEWH4MKFMBCVFERDPOOP4FK7MTDJEYP35`

    ネットワークは、リース手数料を他のアドレスへ送るトランザクションを拒否します。

* {{ tutorial.var('rental_fee') }}: ルートネームスペースのリース手数料である 100 XEM。
    SDK の <dy:FeeCalculator.calculateNamespaceRentalFee> ヘルパーは、必要な金額を返します。
    {{ tutorial.lit('True') }} 引数は、ルートネームスペースの手数料を要求します。

    ネットワークは、この手数料を下回る金額を支払うトランザクションを拒否します。
    より大きい金額は受け付けられますが、全額がシンクアカウントへ送られます。

* {{ tutorial.var('name') }}: ルートネームスペースの名前。

{{ tutorial.code_snippet_tagged('step-5') }}

最後に、<dy:FeeCalculator.calculateTransactionFee> でトランザクション手数料を計算し、トランザクションに付加します。
リース手数料とは異なり、トランザクション手数料は [ハーベスターアカウント](default:ハーベスターアカウント) に支払われます。
ネームスペース登録トランザクションの固定手数料は 0.15 XEM で、[手数料表](../../textbook/transactions.md#fee-schedule) に示されています。

### トランザクションを送信する {: #submitting-the-transaction }

{{ tutorial.code_snippet_tagged('step-6') }}

[XEM を送信する](../transactions/transfer-xem.md#announcing-the-transaction) チュートリアルと同じ手順で、トランザクションに署名してアナウンスします。

{{ tutorial.code_snippet_tagged('step-7') }}

次にコードは、トランザクションがブロックに含まれるまで <get:/transaction/get> エンドポイントをポーリングし、承認を待ちます。

### ネームスペースを取得する {: #retrieving-the-namespace }

{{ tutorial.code_snippet_tagged('step-8') }}

ネームスペースが登録されたことを確認するため、コードは <get:/namespace> エンドポイントを使ってネットワークから取得し、そのプロパティを表示します。

成功したレスポンスは、ネームスペースが登録されてアクティブになったことを確認します。

レスポンスには登録時の高さも表示されます。これはネームスペースが登録されたブロックで、1 年間のリースの開始を示します。

## 出力 {: #output }

以下の出力は、プログラムを通常実行した場合の例です。

```text linenums="1" hl_lines="5 6 7 31-33"
--8<-- 'devbook/namespaces/register_root_namespace.log'
```

出力の要点は次のとおりです。

* **ネームスペース名**（5 行目）: 選択した名前 `ns_1783091378` には、一意性を確保するためのタイムスタンプが含まれています。
    この名前を [NEM テストネットエクスプローラー](https://testnet.nem.fyi/) で検索すると、ネームスペースの詳細を確認できます。

* **リース手数料とトランザクション手数料**（6～7 行目）: ルートネームスペースなのでリース手数料は 100 XEM です（[サブネームスペース](default:サブネームスペース) は代わりに 10 XEM を支払います）。トランザクション手数料は 0.15 XEM です。

* **ネームスペース情報**（31～33 行目）: 登録されたネームスペース、その所有者（署名者のアドレス）、登録時の高さ。リースが開始したブロックの高さです。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [ネームスペース登録トランザクションを構築する](#building-the-transaction) | <dy:TransactionFactory.create>、<ser:NamespaceRegistrationTransactionV1> |
| [リース手数料を計算する](#building-the-transaction) | <dy:FeeCalculator.calculateNamespaceRentalFee> |
| [ネームスペースを取得する](#retrieving-the-namespace) | <get:/namespace> |

## 次のステップ {: #next-steps }

ルートネームスペースを取得できたので、次のことを行えます。

* [モザイクを定義する](../mosaics/create-mosaic.md) ことで、ネームスペースの下にカスタム資産を作成する
* [サブネームスペースを登録する](./register-subnamespace.md) ことで、階層構造を作成する
* 有効期限前に [ネームスペースを延長する](./extend-root-namespace.md) ことで、アクティブな状態を維持する
