---
title: フォーセットで資金を調達する
tutorial_level: beginner
---

# フォーセットからテストネットの資金を取得する

NEM の [テストネット](default:テストネット) には、テスト用に開発者の [アカウント](default:アカウント) へ無料の [XEM](default:XEM) を配布するフォーセットがあります。
このガイドでは、Web ベースのフォーセットを使ってテストネットの資金を請求する方法を説明します。

!!! note
    テストネットの XEM に現実世界での価値はありません。
    現実の通貨を使わずに NEM の機能を試せるようにするためだけに存在します。

    [メインネット](default:メインネット) の XEM が必要な場合は、[取引所](https://coinmarketcap.com/currencies/nem/#Markets) で購入する必要があります。

## 前提条件 {: #prerequisites }

始める前に、次の準備をしてください。

* 資金を受け取るテストネットの [アカウント](default:アカウント) を作成する。
  [コードから](../accounts/create-from-private-key.md) 作成することも、[ウォレットを使って](../../userbook/wallet/create-account.md) 作成することもできます。
* フォーセットで本人確認を行うための 𝕏 アカウントを用意する。

## テストネットの資金を請求する {: #how-to-claim-testnet-funds }

{% import 'tutorial.jinja2' as tutorial %}

{{ tutorial.list_begin() }}

{{ tutorial.step_begin("faucet-open.jpg") }}
Web ブラウザーを開き、NEM テストネットフォーセット [testnet.nem.tools](https://testnet.nem.tools) に移動します。
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-sign-in.jpg") }}
**Sign in with Twitter**（現在の 𝕏）をクリックし、認証手順に従います。

この手順では、フォーセットの悪用を防ぐため、テスト用資金を 1 アカウントあたり 10'000 XEM に制限します。
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-authorize.jpg") }}
サインインすると、𝕏 から、フォーセットアプリケーションによるアカウント情報へのアクセスを許可するよう求められます。

権限を確認し、**Authorize app** をクリックして続行します。
許可すると、再びフォーセットへリダイレクトされます。
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-address.jpg") }}
資金を受け取るアドレスを **Your Testnet Address** フィールドに入力します。

アドレスがテストネットのアカウントを意味する `T` で始まっていることを確認してください。
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-xem.jpg") }}
**XEM Amount** フィールドに、請求する XEM の量を指定します。
1 回のリクエストの最大量は 10'000 XEM です。
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-claim.jpg") }}
**Claim** をクリックしてリクエストを送信します。
リクエストが成功すると、フォーセットは指定した量の XEM をアドレスに送信します。
{{ tutorial.step_end() }}

{{ tutorial.step_begin("faucet-view-explorer.jpg") }}
右上に表示される通知の **View in Explorer** をクリックして、トランザクションが処理されたことを確認します。

エクスプローラーには、確認状態を含むトランザクションの詳細が表示されます。
通常のネットワーク状態では、トランザクションは 1 分ほどで承認されます。

ウォレットを設定している場合は、[ウォレット](default:ウォレット) から送金を監視することもできます。
{{ tutorial.step_end() }}

{{ tutorial.list_end() }}

## フォーセットに資金を返す {: #returning-funds-to-the-faucet }

テストが終わったら、使っていない XEM をフォーセットに返すことを検討してください。
フォーセットのアドレスは、資金を送ってきたアドレスと同じです。

[ブロックチェーンエクスプローラー](https://testnet.nem.fyi/) でトランザクションを確認するか、アカウントのトランザクション履歴を検索すると、送信者のアドレスを確認できます。

さらによい方法は、テストトランザクションの受取人としてフォーセットのアドレスを使うことです。
これにより、トランザクションの送信を練習しながら、他の開発者が使えるフォーセットの資金を維持できます。

## 次のステップ {: #next-steps }

次は [転送トランザクションを送信する](../transactions/transfer-xem.md) のはいかがでしょうか。
