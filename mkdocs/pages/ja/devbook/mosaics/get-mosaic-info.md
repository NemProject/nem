---
title: モザイク情報を取得する
tutorial_level: beginner
---

# モザイク情報を取得する

NEM のすべての [モザイク](default:モザイク) には、供給量、可分性、転送ルールなど、オンチェーンのプロパティがあります。

このチュートリアルでは、モザイクのプロパティと現在の供給量を取得する方法を説明します。

## 前提条件 {: #prerequisites }

このチュートリアルではネットワークからデータを読み取るだけです。[アカウント](default:アカウント) は必要ありません。

始める前に、[開発環境をセットアップ](../start/setup.md) してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/mosaics/get_mosaic_info', ['py', 'js']) }}

スニペットでは、`NODE_URL` 環境変数を使って NEM API ノードを指定します。
値が指定されていない場合は、デフォルトの [テストネット](default:テストネット) ノードを使用します。

`MOSAIC_ID` 環境変数では、[完全修飾名](../../textbook/mosaics.md#fully-qualified-name) で照会するモザイクを指定します。
設定されていない場合は、[XEM](default:XEM) モザイク（`nem:xem`）をデフォルト値として使用します。

## コードの説明 {: #code-explanation }

### モザイク情報を取得する {: #fetching-mosaic-information }

{{ tutorial.code_snippet_tagged('step-1') }}

<get:/mosaic/definition> エンドポイントは、次の内容を含むモザイク定義を取得します。

* **説明:** モザイクを [説明する](../../textbook/mosaics.md#description) テキスト。
* **作成者:** モザイクを作成したアカウントの [公開鍵](default:公開鍵)。
* **プロパティ:** モザイクの [動作プロパティ](../../textbook/mosaics.md#properties)。
    * **[可分性](default:可分性):** モザイクがサポートする小数桁数。
        例えば、XEM の可分性は `6` なので、1 XEM は 1'000'000 原子単位に相当します。
    * **[初期供給量](../../textbook/mosaics.md#initial-supply):** 作成時の供給量で、全単位で表されます。
    * **[供給量の可変性](../../textbook/mosaics.md#supply-mutability):** 作成後に作成者が供給量を変更できるかどうか。
    * **[転送可能性](../../textbook/mosaics.md#transferability):** モザイクをアカウント間で自由に送信できるか、作成者との間でのみ送信できるか。
* **徴収手数料（levy）:** モザイクを転送するたびに 3 つ目のアカウントへ支払われる、オプションの [徴収手数料](default:徴収手数料（levy）)。

### 現在の供給量を取得する {: #fetching-the-current-supply }

{{ tutorial.code_snippet_tagged('step-2') }}

定義に記録されるのは **初期** 供給量だけです。
供給量が可変なモザイクでは現在値が異なる可能性があるため、<get:/mosaic/supply> エンドポイントは、現在流通している供給量を [全単位](../../textbook/mosaics.md#divisibility) で返します。

### 原子単位に変換する {: #converting-to-atomic-units }

{{ tutorial.code_snippet_tagged('step-3') }}

エンドポイントは供給量を全単位で報告しますが、トランザクションの数量は [原子単位](../../textbook/mosaics.md#divisibility) で表されます。
全単位から原子単位に変換するには、コードで供給量に 10 のモザイクの可分性乗を掛けます。

XEM（可分性 `6`）では、`8'999'999'999` 全単位の供給量は `8'999'999'999'000'000` 原子単位に相当します。

## 出力 {: #output }

以下の出力は、テストネットの XEM モザイクを照会した場合の実行例です。

```text linenums="1" hl_lines="5 6 7 8 9 10 11 12 15 17"
--8<-- 'devbook/mosaics/get_mosaic_info.log'
```

出力の要点は次のとおりです。

* **モザイク ID**（5 行目）: XEM モザイクの識別子である完全修飾名 `nem:xem`。

* **説明**（6 行目）: 作成者が設定した、モザイクを説明するテキスト。

* **作成者**（7 行目）: モザイクを作成したアカウントの公開鍵。

* **可分性**（8 行目）: `6` は、1 XEM が 1'000'000（10^6^）原子単位であることを意味します。

* **初期供給量**（9 行目）: 作成時の供給量で、全単位で表されます。

* **供給量の可変性**（10 行目）: `false` は、XEM の供給量が決して変更できないことを意味します。

* **転送可能性**（11 行目）: `true` は、XEM をアカウント間で自由に送信できることを意味します。

* **徴収手数料（levy）**（12 行目）: XEM の転送に追加のモザイク手数料はかかりません。

* **現在の供給量**（15 行目）: 現在流通している供給量。XEM は可変ではないため初期供給量と同じです。

* **原子単位の供給量**（17 行目）: モザイクの可分性を使って、全単位から変換した供給量。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [モザイク定義を取得する](#fetching-mosaic-information) | <get:/mosaic/definition> |
| [現在の供給量を取得する](#fetching-the-current-supply) | <get:/mosaic/supply> |

## 次のステップ {: #next-steps }

* [モザイクを転送する](../transactions/transfer-mosaics.md) ことで、アカウント間でモザイクを送信する
* [アカウント残高を照会する](../accounts/query-balance.md) ことで、アカウントが保有するモザイクの量を確認する
