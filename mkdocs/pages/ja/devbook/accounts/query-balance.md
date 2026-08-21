---
title: アカウント残高の照会
tutorial_level: beginner
---

# アカウント残高を照会する

NEM の [アカウント](default:アカウント) は、ネイティブ通貨である [XEM](default:XEM) を含む [モザイク](default:モザイク)（代替可能トークン）を保有できます。

このチュートリアルでは、アカウントのモザイク残高を照会し、NEM の整数で表される [原子単位の数量](../../textbook/mosaics.md#divisibility) を小数形式で表示する方法を説明します。

## 前提条件 {: #prerequisites }

このチュートリアルでは、[NEM REST API](../reference/rest/nem.md) を使用し、[SDK](default:SDK) は必要ありません。
HTTP リクエストを送信する方法だけが必要です。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/accounts/query_balance', ['py', 'js']) }}

スニペットでは、`NODE_URL` 環境変数を使って NEM API ノードを指定します。
値が指定されていない場合は、デフォルト値を使用します。

チュートリアルでは、次の関数を定義します。

* {{ tutorial.var('get_mosaic_balances()') }}: アカウントが所有するすべての [モザイク](default:モザイク) を取得します。
* {{ tutorial.var('get_mosaic_definitions()') }}: [可分性](default:可分性) を含むモザイク定義を取得します。
* {{ tutorial.var('format_amount()') }}: モザイクの [可分性](default:可分性) に応じた小数桁数で金額を整形します。

## コードの説明 {: #code-explanation }

### モザイク残高を取得する {: #fetching-mosaic-balances }

{{ tutorial.code_snippet_tagged('step-2') }}

<get:/account/mosaic/owned> エンドポイントは、アカウントが保有するすべてのモザイクと、その数量を _原子単位_ で返します。

### モザイク定義を取得する {: #fetching-mosaic-definitions }

{{ tutorial.code_snippet_tagged('step-3') }}

モザイク残高を正しく整形するため、スニペットはネットワークからモザイク定義を取得します。
必要となる主なプロパティは [可分性](default:可分性) で、モザイクがサポートする小数桁数を定義します。

<get:/account/mosaic/owned/definition> エンドポイントは、アカウントが所有するすべてのモザイクの定義を、可分性やその他のプロパティとともに 1 回のリクエストで返します。

### 金額を整形する {: #formatting-amounts }

{{ tutorial.code_snippet_tagged('step-4') }}

このユーティリティ関数は、_原子単位_ の数量を人が読みやすい形式に変換します。

* **原子単位の数量:** ブロックチェーンに保存される整数の生の値。
* **整形済みの金額:** モザイクの可分性によって決まる小数桁数を使った表示形式。

整形では、\(10^{\text{divisibility}}\) に対する除算と剰余によって、原子単位の数量を整数部分と小数部分に分けます。
その後、小数部分をゼロで埋め、常に正しい小数桁数で表示されるようにします。

### すべてを組み合わせる {: #putting-it-all-together }

{{ tutorial.code_snippet_tagged('step-5') }}

メインコードは `ADDRESS` 環境変数を読み込み、照会するアカウントを決定します。
値が指定されていない場合は、デフォルトのサンプルアドレスを使用します。

ヘルパー関数を組み合わせて、次の処理を行います。

1. アカウントのモザイク残高を取得する。
2. モザイク定義を取得して、それぞれの可分性を確認する。
3. 各モザイクを反復処理し、適切な小数桁数で残高を整形する。

## 出力 {: #output }

以下の出力は、プログラムを通常実行した場合の例です。

```text
--8<-- 'devbook/accounts/query_balance.log'
```

出力には、アカウントが保有するすべてのモザイクが表示されます。モザイクによって可分性の値が異なることに注目してください。

* 1 つ目のモザイクは `nem:xem` で、ネットワークのネイティブ通貨です。可分性は 6 なので、小数点以下 6 桁（`9883.200000`）で表示されます。
* 2 つ目のモザイクは `company:token` で、ユーザー定義モザイクです。可分性は 0 なので、整数（`1000000`）で表示されます。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [モザイク残高を取得する](#fetching-mosaic-balances) | <get:/account/mosaic/owned> |
| [モザイク定義を取得する](#fetching-mosaic-definitions) | <get:/account/mosaic/owned/definition> |
