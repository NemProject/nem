---
title: モザイクの供給量を変更する
tutorial_level: intermediate
---

# モザイクの供給量を変更する

[供給量が可変](../../textbook/mosaics.md#supply-mutability) として作成された [モザイク](default:モザイク) は、作成後に総供給量を増減できます。

供給量を変更できるのはモザイクの作成者だけです。
供給量の変更が影響するのは作成者の残高だけです。発行した単位は追加され、焼却した単位は削除されます。
モザイクを保有する他のアカウントの残高は変わりません。

このチュートリアルでは、単位の発行と焼却によってモザイクの供給量を変更する方法を説明します。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* 開発環境をセットアップする。
    [開発環境のセットアップ](../start/setup.md) を参照してください。
* 供給量の変更に署名する [アカウント](default:アカウント) を使い、[供給量が可変](../../textbook/mosaics.md#supply-mutability) なモザイクを作成する。
    [モザイクを作成する](./create-mosaic.md) チュートリアルを参照してください。
* モザイクを保持する [ネームスペース](default:ネームスペース) をアクティブな状態に保つ。
    テキストブックの [有効期間](../../textbook/mosaics.md#lifetime) を参照してください。
* トランザクション手数料を支払うための [XEM](default:XEM) を用意する。
    [フォーセットからテストネットの資金を取得する](../accounts/testnet-faucet.md) を参照してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/mosaics/change_mosaic_supply', ['py', 'js']) }}

## コードの説明 {: #code-explanation }

モザイクの供給量を変更するには、<ser:MosaicSupplyChangeTransactionV1> トランザクションを使います。
このチュートリアルでは、単位を発行するものと焼却するものの 2 つをアナウンスします。

どちらのトランザクションも同じ方法で送信するため、スニペットでは {{ tutorial.var('announce_transaction') }} と {{ tutorial.var('wait_for_confirmation') }} の 2 つのヘルパーを定義します。これらはトランザクションをアナウンスし、ブロックに含まれるまでネットワークをポーリングします。

3 つ目のヘルパー {{ tutorial.var('fetch_supply') }} は、<get:/mosaic/supply> からモザイクの現在の供給量を読み取り、各トランザクションの効果を確認できるようにします。

### アカウントとモザイクをセットアップする {: #setting-up-the-account-and-the-mosaic }

{{ tutorial.code_snippet_tagged('step-1') }}

スニペットは、署名者の秘密鍵を `SIGNER_PRIVATE_KEY` 環境変数から読み込みます。
設定されていない場合は、テスト用のデフォルトキーを使用します。
署名者はモザイクの作成者でなければなりません。

更新するモザイクは、`NAMESPACE` と `MOSAIC` 環境変数から読み込みます。デフォルト値は `my_namespace:token` です。

!!! warning "署名者が作成したモザイクを使用してください"

    デフォルトでは、コードは `SIGNER_PRIVATE_KEY` が参照するテストアカウントと、`my_namespace:token` という名前のモザイクを使用します。

    [モザイクを作成する](./create-mosaic.md) チュートリアルから続けている場合は、`SIGNER_PRIVATE_KEY`、`NAMESPACE`、`MOSAIC` 環境変数を、そこで作成したアカウントとモザイク、または署名者が所有する別のモザイクに合わせて設定してください。

### ネットワーク時刻を取得する {: #fetching-network-time }

{{ tutorial.code_snippet_tagged('step-2') }}

ネットワーク時刻は <get:/time-sync/network-time> から取得し、[XEM を送信する](../transactions/transfer-xem.md) チュートリアルで説明されている手順に従って、トランザクションの `timestamp` と `deadline` フィールドを導出します。

### 供給量を増やす（発行） {: #increasing-supply-minting }

{{ tutorial.code_snippet_tagged('step-3') }}

スニペットはまずモザイクの供給量を読み取り、トランザクションが承認されたときに新しく発行された単位を確認できるようにします。

新しい単位を発行するには、トランザクションに次の値を設定します。

* {{ tutorial.var('type') }}: モザイク供給量変更トランザクションでは、タイプ <ser:MosaicSupplyChangeTransactionV1> を使用します。

* {{ tutorial.var('signer_public_key') }}: トランザクションに署名して手数料を支払うアカウント。モザイクの作成者でなければなりません。

* {{ tutorial.var('timestamp') }} と {{ tutorial.var('deadline') }}: ネットワーク時刻の手順で計算した値。

* {{ tutorial.var('mosaic_id') }}: 更新するモザイクの [完全修飾名](../../textbook/mosaics.md#fully-qualified-name)。

* {{ tutorial.var('action') }}: `increase` の値は、新しい単位を発行します。

* {{ tutorial.var('delta') }}: 追加する [全単位](../../textbook/mosaics.md#divisibility) の数。
    結果として得られる総供給量は [最大供給量](../../textbook/mosaics.md#initial-supply) を超えられません。

    !!! note "上限は原子単位で表されます"

        最大供給量は $9 \cdot 10^{15}$ **原子**単位で固定されていますが、{{ tutorial.var('delta') }} は **全単位**で表されます。

        したがって、{{ tutorial.var('delta') }} の最大値はモザイクの [可分性](default:可分性) によって異なります。

        \[
        \text{max\_whole\_units} = \frac{9 \cdot 10^{15}}{10^{\text{divisibility}}}
        \]

        このチュートリアルのモザイクの可分性は `2` なので、1 全単位は $100$ 原子単位に相当し、供給量は最大 $9 \cdot 10^{13}$ 全単位まで増やせます。

その後、[XEM を送信する](../transactions/transfer-xem.md) チュートリアルと同じ手順で、トランザクション手数料を計算し、トランザクションに署名して、アナウンスし、承認を待ちます。

モザイク供給量変更トランザクションの固定手数料は 0.15 XEM で、[手数料表](../../textbook/transactions.md#fee-schedule) に示されています。

承認されたら、供給量をもう一度読み取り、結果の供給量を表示します。
発行された単位は作成者のアカウントに加算されます。

### 供給量を減らす（焼却） {: #decreasing-supply-burning }

{{ tutorial.code_snippet_tagged('step-4') }}

既存の単位を焼却するには、同じトランザクションタイプを使い、{{ tutorial.var('action') }} を `decrease` に、{{ tutorial.var('delta') }} を削除する全単位数に設定します。

焼却する単位は作成者のアカウントから取得されるため、作成者がまだ保有している単位だけを焼却できます。
他のアカウントにすでに配布された単位はその残高に残り、作成者自身の残高が {{ tutorial.var('delta') }} をカバーできない場合はトランザクションが失敗します。

承認されたら、供給量をもう一度読み取り、焼却された単位を表示します。
このチュートリアルでは供給量を同じ量だけ増やしてから減らすため、最終的な供給量は変更前の値と一致します。

## 出力 {: #output }

以下の出力は、プログラムを通常実行した場合の例です。

```text linenums="1" hl_lines="8 25-26 35 54-55 64"
--8<-- 'devbook/mosaics/change_mosaic_supply.log'
```

出力の要点は次のとおりです。

* **発行前の供給量**（8 行目）: モザイクは 1000 全単位の供給量で始まります。

* **供給量の増加**（25～26 行目）: デルタが `500` の `increase` アクションにより、作成者の残高に新しい単位が発行されます。

* **発行後の供給量**（35 行目）: 供給量が 1500 全単位に増えます。

* **供給量の減少**（54～55 行目）: 同じデルタの `decrease` アクションによって、それらの単位が焼却されます。

* **焼却後の供給量**（64 行目）: 増加と減少が相殺されるため、供給量は 1000 に戻ります。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [モザイクの供給量を発行する](#increasing-supply-minting) | <dy:TransactionFactory.create>、<ser:MosaicSupplyChangeTransactionV1> |
| [モザイクの供給量を焼却する](#decreasing-supply-burning) | <dy:TransactionFactory.create>、<ser:MosaicSupplyChangeTransactionV1> |
| [トランザクション手数料を計算する](#increasing-supply-minting) | <dy:FeeCalculator.calculateTransactionFee> |
| [モザイクの供給量を読み取る](#increasing-supply-minting) | <get:/mosaic/supply> |

## 次のステップ {: #next-steps }

モザイクの供給量を変更できるようになったので、次のことを行えます。

* [転送トランザクションでモザイクを送信する](../transactions/transfer-mosaics.md) ことで、他のアカウントに配布する
* [モザイク情報を取得する](./get-mosaic-info.md) ことで、プロパティと供給量を確認する
