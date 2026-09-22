---
title: ネームスペース情報を取得する
tutorial_level: beginner
---

# ネームスペース情報を取得する

このチュートリアルでは、[ネームスペース](default:ネームスペース) のプロパティ、その [サブネームスペース](default:サブネームスペース)、およびその下に定義された [モザイク](default:モザイク) を取得する方法を説明します。

## 前提条件 {: #prerequisites }

このチュートリアルではネットワークからデータを読み取るだけです。アカウントは必要ありません。

始める前に、[開発環境をセットアップ](../start/setup.md) してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/namespaces/get_namespace_info') }}

スニペットでは、`NODE_URL` 環境変数を使って NEM API ノードを指定します。
値が指定されていない場合は、デフォルトの [テストネット](default:テストネット) ノードを使用します。

`NAMESPACE_NAME` 環境変数では、`foo` や `foo.bar` のような、ドットで区切られた完全な [名前](../../textbook/namespaces.md#name) で照会するネームスペースを指定します。
設定されていない場合は、テストネットに登録された [ルートネームスペース](default:ルートネームスペース) である `company` を使用します。

## コードの説明 {: #code-explanation }

### ネームスペース情報を取得する {: #fetching-namespace-information }

{{ tutorial.code_snippet_tagged('step-1') }}

<get:/namespace> エンドポイントは、次の内容を含むネームスペースの現在のプロパティを取得します。

* **名前:** ネームスペースの完全なドット区切りの [識別子](../../textbook/namespaces.md#name)。ルートから照会したレベルまでを表します。
    例えば、`foo` は [ルートネームスペース](default:ルートネームスペース) で、`foo.bar` は `foo` の [サブネームスペース](default:サブネームスペース) です。

    返されるデータ構造の `fqn` は _Fully-Qualified Name_（完全修飾名）を表します。

* **所有者:** ネームスペースを [登録した](../../textbook/namespaces.md#ownership) アカウントの [アドレス](default:アドレス)。

* **高さ:** 現在の所有権が開始した [ブロック](default:ブロック) の高さ。

### リースの有効期限を計算する {: #computing-the-lease-expiration }

{{ tutorial.code_snippet_tagged('step-2') }}

ネームスペースが永久に所有されることはありません。
ルートネームスペースは 525600 ブロック（およそ 1 年）[リース](../../textbook/namespaces.md#duration) を受け、期限切れになる前に更新する必要があります。
サブネームスペースは個別にリースされず、ルートネームスペースと同時に期限切れになります。

有効期限の高さは API レスポンスに含まれませんが、ネームスペースの高さにリース期間を加えて導出できます。
これを <get:/chain/height> が返す現在のチェーン高と比較すると、ネームスペースの期限切れまでに残っているブロック数がわかります。

### サブネームスペースを一覧表示する {: #listing-subnamespaces }

{{ tutorial.code_snippet_tagged('step-3') }}

ネームスペースの子を直接返すエンドポイントはありません。
ただし、[サブネームスペースは常にルートネームスペースの所有者を共有する](../../textbook/namespaces.md#ownership) ため、そのアカウントが所有するネームスペースを照会すれば見つけられます。

<get:/account/namespace/page> エンドポイントは、アカウントが所有するネームスペースを返します。
オプションの `parent` パラメーターを使うと、指定したネームスペースのサブネームスペースだけに結果を制限できます。
前の手順で取得したネームスペース所有者と照会したネームスペースを `parent` の値として使うと、そのサブネームスペースが返されます。

### ネームスペースのモザイクを一覧表示する {: #listing-the-namespaces-mosaics }

{{ tutorial.code_snippet_tagged('step-4') }}

モザイクは常に [ネームスペースの下に定義](../../textbook/mosaics.md#fully-qualified-name) され、関連するモザイクをまとめるプレフィックスとして機能します。

<get:/namespace/mosaic/definition/page> エンドポイントは、照会した名前とネームスペースが完全に一致するモザイクごとに 1 つの定義を返します。
より深いサブネームスペース（`foo` を照会したときの `foo.bar:baz` など）で定義されたモザイクは含まれません。
それらも一覧に含めるには、前の手順で見つかった各サブネームスペースについて、この照会を繰り返してください。

## 出力 {: #output }

以下の出力は、テストネットの `company` ネームスペースを照会した場合の実行例です。

```text linenums="1" hl_lines="5 6 7 9 10 11 14 15 18 19"
--8<-- 'devbook/namespaces/get_namespace_info.log'
```

出力の要点は次のとおりです。

* **ネームスペース名**（5 行目）: 照会したネームスペース `company`。
    ドットを含まないため、ルートネームスペースです。

* **所有者**（6 行目）: 現在ネームスペースを所有するアカウント。

* **高さ**（7 行目）: 現在の所有権期間が開始したブロックの高さ。

* **リースの有効期限**（9～11 行目）: 有効期限の高さは、所有権の高さにリース期間 525600 ブロックを加えた値です。
    現在のチェーン高を引くと、有効期限までに残っているブロック数がわかります。

* **サブネームスペース**（14～15 行目）: `company` の下に `company.division` というサブネームスペースが 1 つ存在します。

* **モザイク**（18～19 行目）: ネームスペースの直下に `company:token` というモザイクが 1 つ定義されています。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [ネームスペースのプロパティを取得する](#fetching-namespace-information) | <get:/namespace> |
| [リースの有効期限を計算する](#computing-the-lease-expiration) | <get:/chain/height> |
| [サブネームスペースを一覧表示する](#listing-subnamespaces) | <get:/account/namespace/page> |
| [ネームスペースのモザイクを一覧表示する](#listing-the-namespaces-mosaics) | <get:/namespace/mosaic/definition/page> |
