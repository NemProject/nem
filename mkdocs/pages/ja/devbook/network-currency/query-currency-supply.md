---
title: 通貨供給量を照会する
tutorial_level: beginner
---

# 通貨供給量を照会する

取引所や市場データアグリゲーターは、時価総額やトークン指標を表示するために、正確な供給量を必要とします。

NEM は、ネイティブ通貨である [XEM](default:XEM) の供給量を REST API で公開しています。
このチュートリアルでは、総供給量を照会し、そこから流通供給量を導出する方法を説明します。

## 前提条件 {: #prerequisites }

このチュートリアルでは、SDK を必要とせずに [NEM REST API](../reference/rest/nem.md) を使用します。
HTTP リクエストを送信する方法だけが必要です。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/network-currency/query_currency_supply') }}

スニペットでは、`NODE_URL` 環境変数を使って NEM [メインネット](default:メインネット) ノードを指定します。

!!! note "なぜメインネットなのか"
    他のチュートリアルは、通常、実際の資金を使わないように [テストネット](default:テストネット) に対して実行します。
    このチュートリアルは、流通供給量を計算するために固定されたメインネットアカウントのアドレスを照会する点で異なります。そのため、`NODE_URL` はメインネットノードを指す必要があります。

## コードの説明 {: #code-explanation }

### 総供給量を取得する {: #fetching-the-total-supply }

{{ tutorial.code_snippet_tagged('step-1') }}

[XEM](default:XEM) の総供給量は固定されています。
すべての 8'999'999'999 XEM は [ネメシスブロック](default:ネメシスブロック) で作成され、新しい XEM が発行されることはありません。

このチュートリアルでは、供給量や可分性などの値をハードコードせず API から読み取ります。
そのため、同じ方法を、供給量を変更できるものを含む他の [モザイク](default:モザイク) にも使用できます。

コードは、XEM モザイク識別子 `nem:xem` を `mosaicId` クエリパラメーターとして渡し、<get:/mosaic/supply> エンドポイントへ `GET` リクエストを送信します。

レスポンスは、モザイク識別子と現在の `supply` を含む JSON オブジェクトです。
`supply` は [全体単位](../../textbook/mosaics.md#divisibility) で表されます。

### モザイクの可分性を読み取る {: #reading-the-mosaics-divisibility }

{{ tutorial.code_snippet_tagged('step-2') }}

前の手順で取得した供給量はすでに全体単位ですが、次の手順で読み取るアカウント残高は [原子単位](../../textbook/mosaics.md#divisibility) で報告されます。

値を同じ単位に変換するため、この手順ではまずモザイクの [可分性](default:可分性) を取得します。
その値を使って、残高を原子単位から全体単位へ変換します。

<get:/mosaic/definition> エンドポイントは、可分性を含むモザイク定義を返します。
`nem:xem` の可分性は 6 です。

### 非流通供給量を取得する {: #fetching-the-non-circulating-supply }

{{ tutorial.code_snippet_tagged('step-3') }}

総供給量の一部は、公開市場に含まれないアカウントによって保有されています。

* **トレジャリー**: チームが管理する XEM を保有する準備金アカウント。
* **ネメシス**: ネメシスブロックに署名したアカウント。
    ネメシスブロックの後はトランザクションを送信できないため、このアカウントが保有する XEM は実質的に流通していません。
* **ネームスペースレンタルシンク**: [ネームスペース](default:ネームスペース) の登録に支払われた手数料を集めます。
* **モザイクレンタルシンク**: [モザイク](default:モザイク) の作成に支払われた手数料を集めます。

コードは <get:/account/get> エンドポイントで各アカウントを照会し、残高を合計します。

残高は原子単位で合計します。
表示するときだけ全 XEM に変換します。`scale`（`nem:xem` では 1'000'000）で割った商が整数部分で、余りが小数点以下 6 桁になります。

これを通常の除算で行うと浮動小数点数になり、残高が非常に大きいため、最後の桁を誤る可能性があります。

### 流通供給量を導出する {: #deriving-the-circulating-supply }

{{ tutorial.code_snippet_tagged('step-4') }}

流通供給量は、総供給量から非流通残高を引いた値です。

これは、公開市場で自由に利用できる XEM の量です。

<get:/mosaic/supply> の総供給量は全体単位なので、コードは差し引く前に `scale` を掛けて原子単位に変換し、その結果を表示します。

## 出力 {: #output }

以下の出力は、通貨供給量を照会した場合の実行例です。

```text linenums="1" hl_lines="2 7 8"
--8<-- 'devbook/network-currency/query_currency_supply.log'
```

出力には、XEM 供給量の内訳が表示されます。

* **総供給量**（2 行目）: 存在するすべての XEM。
* **非流通供給量**（7 行目）: トレジャリー、ネメシス、レンタルシンクの残高の合計。
* **流通供給量**（8 行目）: 実際に流通して利用できる XEM。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [総供給量を取得する](#fetching-the-total-supply) | <get:/mosaic/supply> |
| [モザイクの可分性を読み取る](#reading-the-mosaics-divisibility) | <get:/mosaic/definition> |
| [非流通供給量を取得する](#fetching-the-non-circulating-supply) | <get:/account/get> |

## 次のステップ {: #next-steps }

特定のアカウントの XEM 残高を確認するには、[アカウント残高を照会する](../accounts/query-balance.md) チュートリアルを参照してください。
