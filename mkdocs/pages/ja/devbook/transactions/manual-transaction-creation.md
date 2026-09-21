---
title: 手動トランザクション作成
tutorial_level: intermediate
---

# トランザクションを手動で作成する

ほとんどのチュートリアルでは、ファサードを通じてディスクリプタからトランザクションを作成します。
これは推奨される方法です。型付きディスクリプタをサポートする言語では、コンパイル時または型チェックのサポートと優れたエディタ支援が得られ、ファサードは署名者、タイムスタンプ、デッドラインを期間から設定します。

完全性のため、このチュートリアルでは下位レベルの代替手段として、トランザクションファクトリを使ってトランザクションを手動で作成する方法を示します。

この例は [XEM を送信する](./transfer-xem.md) チュートリアルと同じ流れに沿っていますが、ディスクリプタベースの作成を手動のフィールド設定に置き換えます。
ネットワーク時刻を明示的に取得し、タイムスタンプと絶対デッドラインを設定して、NEM のトランザクション手数料を計算します。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* [開発環境をセットアップ](../start/setup.md) する。
* 転送トランザクションを送信する [アカウント](default:アカウント) を、[コード](../accounts/create-from-private-key.md) または [ウォレット](../../userbook/wallet/create-account.md) を使って作成する。
* トランザクション手数料と転送額を支払うための [XEM](default:XEM) を用意する。
    [フォーセットからテストネットの資金を取得する](../accounts/testnet-faucet.md) を参照してください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/transactions/manual_transaction_creation', ['py', 'js']) }}

## コードの説明 {: #code-explanation }

### アカウントをセットアップする {: #setting-up-the-accounts }

{{ tutorial.code_snippet_tagged('step-1') }}

署名者と受取人は、[XEM を送信する](./transfer-xem.md#setting-up-the-accounts) チュートリアルと同じ方法で設定します。

### 送金額を定義する {: #defining-the-transfer-amount }

{{ tutorial.code_snippet_tagged('step-2') }}

金額は原子単位で表します。XEM の [可分性](default:可分性) は 6 なので、デフォルトの 1 XEM の送金は `1_000_000` 原子単位です。

### ネットワーク時刻を取得する {: #fetching-network-time }

{{ tutorial.code_snippet_tagged('step-3') }}

手動でトランザクションを作成するには、NEM のネメシスブロックからの経過秒数で表す `timestamp` と絶対 `deadline` を明示的に指定する必要があります。

スニペットは <get:/time-sync/network-time> から現在のネットワーク時刻を取得します。
エンドポイントはミリ秒を返すため、<dy:NetworkTimestamp> でラップする前に 1000 で割ります。
デッドラインはタイムスタンプの 2 時間後に設定します。

一方、ディスクリプタベースのファサード作成では、秒単位のデッドライン期間を受け取り、ローカル時計から両方の値を導出するため、このリクエストは不要です。

### トランザクションを構築する {: #building-the-transaction }

{{ tutorial.code_snippet_tagged('step-4') }}

トランザクションは、プレーンなディスクリプタ辞書またはオブジェクトを受け取る <dy:TransactionFactory.create> で作成します。
ファサードの作成メソッドとは異なり、この下位レベルのファクトリは共通のトランザクションフィールドを設定しません。

ディスクリプタには次の値を指定します。

* {{ tutorial.var('type') }}: 現在の転送トランザクションバージョンである <ser:TransferTransactionV2>。
* {{ tutorial.var('signer_public_key') }}: トランザクションに署名し、手数料を支払い、XEM を送るアカウント。
* {{ tutorial.var('timestamp') }}: 前の手順で取得した現在のネットワーク時刻のタイムスタンプ。
* {{ tutorial.var('deadline') }}: タイムスタンプの 2 時間後に設定した、ネットワーク時刻の絶対デッドライン。
* {{ tutorial.var('recipient_address') }}: XEM を受け取るアドレス。
* {{ tutorial.var('amount') }}: 原子単位で表した送金額。

### トランザクション手数料を計算する {: #calculating-the-transaction-fee }

{{ tutorial.code_snippet_tagged('step-5') }}

下位レベルのファクトリは、NEM の固定手数料表に基づく手数料を計算しません。
スニペットは構築後に <dy:FeeCalculator.calculateTransactionFee> を呼び出し、署名前に結果を `transaction.fee` へ割り当てます。

### 署名してシリアライズする {: #signing-and-serializing }

{{ tutorial.code_snippet_tagged('step-6') }}

<dy:NemFacade.signTransaction> でトランザクションに署名します。
<dy:TransactionFactory.attachSignature> で署名を付加し、アナウンスできる JSON ペイロードを生成します。

### トランザクションをアナウンスする {: #announcing-the-transaction }

{{ tutorial.code_snippet_tagged('step-7') }}

署名済みペイロードを <post:/transaction/announce> に送信します。`SUCCESS` レスポンスは、トランザクションが未承認プールに入ったことを意味します。

### 承認を待つ {: #waiting-for-confirmation }

{{ tutorial.code_snippet_tagged('step-8') }}

トランザクションがブロックに含まれるか、2 分間の再試行上限に達するまで <get:/transaction/get> でポーリングします。

## 出力 {: #output }

```text linenums="1" hl_lines="2 3 4 11 14 20 21"
--8<-- 'devbook/transactions/manual_transaction_creation.log'
```

最初の 2 つのハイライト行は、明示的なネットワーク時刻のリクエストを示します。
さらに、計算した手数料、手動で設定した署名者と絶対デッドライン、アナウンス結果、承認確認に使うトランザクションハッシュも表示します。

## まとめ {: #conclusion }

このチュートリアルでは、トランザクションを手動で作成する方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [ネットワーク時刻を取得する](#fetching-network-time) | <get:/time-sync/network-time>、<dy:NetworkTimestamp> |
| [トランザクションを構築する](#building-the-transaction) | <dy:TransactionFactory.create>、<ser:TransferTransactionV2> |
| [手数料を計算する](#calculating-the-transaction-fee) | <dy:FeeCalculator.calculateTransactionFee> |
| [署名してシリアライズする](#signing-and-serializing) | <dy:NemFacade.signTransaction><br/><dy:TransactionFactory.attachSignature> |
| [アナウンスして承認を待つ](#announcing-the-transaction) | <post:/transaction/announce><br/><get:/transaction/get> |
