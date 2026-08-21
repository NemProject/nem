---
title: モザイク定義を変更する
tutorial_level: intermediate
---

# モザイク定義を変更する

[モザイク](default:モザイク) の作成後、作成者は同じモザイク識別子を使う別のモザイク定義トランザクションを送信することで、一部のプロパティを変更できます。
新しいモザイクを作成するのではなく、ネットワークが既存のモザイクを更新します。

このチュートリアルでは、既存のモザイク定義を変更する方法を説明します。
モザイクの供給量を変更する方法については、[モザイクの供給量を変更する](./change-mosaic-supply.md) を参照してください。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* 変更に署名する [アカウント](default:アカウント) で [モザイク](default:モザイク) を作成する。
    モザイクを変更できるのは作成者だけです。
    [モザイクを作成する](./create-mosaic.md) チュートリアルを参照してください。
* モザイクを保持する [ネームスペース](default:ネームスペース) をアクティブな状態に保つ。
    テキストブックの [有効期間](../../textbook/mosaics.md#lifetime) を参照してください。
* トランザクションと作成手数料を支払うための [XEM](default:XEM) を用意する。
    [フォーセットからテストネットの資金を取得する](../accounts/testnet-faucet.md) を参照してください。

## 変更できる内容 {: #what-can-be-changed }

[説明](../../textbook/mosaics.md#description) はいつでも変更できます。

[転送可能性](../../textbook/mosaics.md#transferability) と [名前](../../textbook/mosaics.md#name) は変更できません。

[可分性](default:可分性)、[初期供給量](../../textbook/mosaics.md#initial-supply)、[供給量の可変性](../../textbook/mosaics.md#supply-mutability)、[徴収手数料](default:徴収手数料（levy）) を変更するには、作成者がモザイクの供給量全体をまだ所有している必要があります。
実際には、ほとんどのモザイク定義は、モザイクを配布する前にしか変更できません。

完全なルールについては、テキストブックの [モザイクを変更する](../../textbook/mosaics.md#modifying-a-mosaic) を参照してください。

## 手順 {: #procedure }

モザイク定義を変更するには、[モザイクを作成する](./create-mosaic.md) チュートリアルの手順を再利用します。

1. [モザイクを取得する](./create-mosaic.md#retrieving-the-mosaic) で説明されているように、<get:/mosaic/definition> から現在の定義を取得します。

2. [モザイク定義トランザクションを構築する](./create-mosaic.md#building-the-mosaic-definition-transaction) で説明されているように、同じモザイク識別子（ネームスペース名とモザイク名）を使って新しい <ser:MosaicDefinitionTransactionV1> を構築し、**モザイク作成者**のアカウントで署名します。

    トランザクションには、[説明](../../textbook/mosaics.md#description)、[モザイクプロパティ](../../textbook/mosaics.md#properties)、[徴収手数料](default:徴収手数料（levy）) を含む完全なモザイク定義を含める必要があります。
    前の手順で取得した定義を再送し、更新する値だけを変更してください。

    !!! warning "完全な定義を再送してください"

        説明はトランザクションに含まれる値で置き換えられるため、空のままにできません。

        トランザクションから省略されたモザイクプロパティは、デフォルト値にリセットされます。

        * `divisibility`: `0`
        * `initialSupply`: `1000`
        * `supplyMutable`: `false`
        * `transferable`: `true`

        省略された徴収手数料はモザイクから削除されます。

3. [モザイク定義を送信する](./create-mosaic.md#submitting-the-mosaic-definition) で説明されているように、トランザクションを送信します。

4. もう一度定義を取得して、モザイクが更新した値を保持していることを確認します。

トランザクションには、固定トランザクション手数料 0.15 XEM に加えて、作成トランザクションと同じ 10 XEM の全額の [作成手数料](../../textbook/mosaics.md#creation-fee) がかかります。固定手数料は [手数料表](../../textbook/transactions.md#fee-schedule) に示されています。
例えば説明だけを変更する場合でも、新しいモザイクを作成する場合と同じ費用がかかります。

## 結果 {: #outcome }

説明だけを変更する場合、ネットワークは既存の供給量と、モザイクを保有するすべてのアカウントの残高を維持します。

作成者が供給量全体を所有している間だけ許可される、その他のプロパティを変更した場合、ネットワークは新しい定義からモザイクを再構築します。
再構築によって総供給量は `initialSupply` にリセットされ、その全量が作成者アカウントに割り当てられます。

その時点では作成者だけが保有者なので、他のアカウントの残高には影響しません。
モザイクが配布された後は、ネットワークはこれらの他のプロパティを変更するトランザクションを拒否します。
