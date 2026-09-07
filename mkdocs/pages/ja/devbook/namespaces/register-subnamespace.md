---
title: サブネームスペースを登録する
tutorial_level: intermediate
---

# サブネームスペースを登録する

[サブネームスペース](default:サブネームスペース)（「子」ネームスペースとも呼ばれます）は、[ネームスペース](default:ネームスペース) の階層構造を拡張します。

このチュートリアルでは、既存の [ルートネームスペース](default:ルートネームスペース) の下にサブネームスペースを登録する方法を説明します。

ルートネームスペースを登録する方法については、[ルートネームスペースを登録する](./register-root-namespace.md) ガイドを参照してください。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* 開発環境をセットアップする。
  [開発環境のセットアップ](../start/setup.md) を参照してください。
* 既存のルートネームスペースを持つ [アカウント](default:アカウント) を用意する。
  [ルートネームスペースを登録する](./register-root-namespace.md) を参照してください。

    !!! note
        このチュートリアルの例では、`ns_root` という名前のルートネームスペースを使用します。
        コードを自分のルートネームスペース名に更新してください。

* トランザクションとリース手数料を支払うための [XEM](default:XEM) を用意する。
  [フォーセットからテストネットの資金を取得する](../accounts/testnet-faucet.md) を参照してください。

さらに、トランザクションのアナウンスと承認の方法を理解するため、[XEM を送信する](../transactions/transfer-xem.md) チュートリアルを確認してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/namespaces/register_subnamespace', ['py', 'js']) }}

## コードの説明 {: #code-explanation }

コードは [ルートネームスペースを登録する](./register-root-namespace.md) チュートリアルと同じパターンに従います。
このセクションでは、主な違いだけに焦点を当てます。

共通手順（アカウントのセットアップ、ネットワーク時刻の取得、アナウンス）とルートネームスペースで共有されるトランザクションディスクリプタフィールドの詳細な説明については、[ルートネームスペースを登録する](./register-root-namespace.md) を参照してください。

### サブネームスペース名を設定する {: #choosing-the-subnamespace-name }

{{ tutorial.code_snippet_tagged('step-1') }}

サブネームスペースは、親ネームスペース名と子名をドットでつないだ完全な名前で識別します（`ns_root.product` など）。
命名規則については、テキストブックの [名前](../../textbook/namespaces.md#name) を参照してください。

チュートリアルを複数回実行したときの衝突を避けるため、子名にタイムスタンプを追加します。
ただし、実際のプログラムでは、サブネームスペースに固定名を使用します。
`ROOT_NAMESPACE` と `SUBNAMESPACE` 環境変数を使うと、チュートリアルで固定名を使用できます。

!!! warning "署名者が所有する親ネームスペースを使用してください"

    デフォルトでは、コードは `SIGNER_PRIVATE_KEY` が参照するテストアカウントと、`ns_root` という親ネームスペースを使用します。

    [ルートネームスペースを登録する](./register-root-namespace.md) チュートリアルから続けている場合は、`SIGNER_PRIVATE_KEY` と `ROOT_NAMESPACE` 環境変数を、そこで作成したアカウントとネームスペース、または署名者が所有する別のネームスペースに合わせて設定してください。

### トランザクションを構築する {: #building-the-transaction }

{{ tutorial.code_snippet_tagged('step-2') }}

サブネームスペースを登録する場合の主な違いは、トランザクションディスクリプタにあります。

* {{ tutorial.var('parent_name') }}: 前の手順で定義した親ネームスペースの名前。
    ルートネームスペースでも、別のサブネームスペースでも構いません。

* {{ tutorial.var('name') }}: 前の手順で設定したサブネームスペースの名前。

    これはサブネームスペースの名前だけで、完全なパスではないことに注意してください。
    例えば、ルートが `company` の `company.product` を作成する場合は、{{ tutorial.var("`name: 'product'`") }} と {{ tutorial.var("`parent_name: 'company'`") }} を設定します。

* {{ tutorial.var('rental_fee') }}: サブネームスペースのリース手数料である 10 XEM。ルートネームスペースと同じ [シンクアカウント](./register-root-namespace.md#building-the-transaction) に支払います。

    SDK の <dy:FeeCalculator.calculateNamespaceRentalFee> ヘルパーは、必要な金額を返します。
    {{ tutorial.lit('False') }} 引数は、サブネームスペースの手数料を要求します。

その後、[ルートネームスペースを登録する](./register-root-namespace.md#submitting-the-transaction) チュートリアルと同じ手順で、トランザクションに署名し、アナウンスして、承認を待ちます。

### サブネームスペースを取得する {: #retrieving-the-subnamespace }

{{ tutorial.code_snippet_tagged('step-3') }}

サブネームスペースが登録されたことを確認するため、コードは <get:/namespace> エンドポイントを使ってネットワークから取得し、そのプロパティを表示します。

サブネームスペースは、親名と子名をドットでつないだ完全な名前で照会します（例: `ns_root.sub_1783411728`）。

成功したレスポンスは、サブネームスペースが登録されてアクティブになったことを確認します。

レスポンスには登録時の高さも表示されます。サブネームスペースはルートネームスペースの [リース](../../textbook/namespaces.md#duration) を継承するため、これはルートネームスペースが登録されたブロックの高さです。

!!! note "サブネームスペースの期間"

    サブネームスペースはルートネームスペースの期限切れと同時に期限切れになり、単独で更新することはできません。
    [ルートネームスペースを更新する](./extend-root-namespace.md) と、サブネームスペースも更新されます。

## 出力 {: #output }

以下は、プログラムの実行時の出力例です。

```text linenums="1" hl_lines="5 6 7 32-34"
--8<-- 'devbook/namespaces/register_subnamespace.log'
```

出力の要点は次のとおりです。

* **完全なネームスペースパス**（5 行目）: `ns_root.sub_1783411728` は、親ネームスペース `ns_root` とトランザクションで設定したサブネームスペース名を組み合わせたものです。

* **リース手数料とトランザクション手数料**（6～7 行目）: サブネームスペースなのでリース手数料は 10 XEM です（ルートネームスペースは代わりに 100 XEM を支払います）。トランザクション手数料は 0.15 XEM です。

* **ネームスペース情報**（32～34 行目）: 登録されたサブネームスペース、その所有者（署名者のアドレス）、登録時の高さ。ルートネームスペースのリースが開始したブロックの高さをサブネームスペースが継承しています。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [サブネームスペース登録トランザクションを構築する](#building-the-transaction) | <dy:TransactionFactory.create>、<ser:NamespaceRegistrationTransactionV1> |
| [リース手数料を計算する](#building-the-transaction) | <dy:FeeCalculator.calculateNamespaceRentalFee> |
| [サブネームスペースを取得する](#retrieving-the-subnamespace) | <get:/namespace> |

## 次のステップ {: #next-steps }

サブネームスペースを取得できたので、次のことを行えます。

* 追加のサブネームスペースを登録して、階層構造を拡張する
* [モザイクを定義する](../mosaics/create-mosaic.md) ことで、サブネームスペースの下にカスタム資産を作成する
