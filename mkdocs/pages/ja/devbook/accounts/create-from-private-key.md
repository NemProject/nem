---
title: 秘密鍵から作成
tutorial_level: beginner
---

# 秘密鍵からアカウントを作成する

このチュートリアルでは、既存の [秘密鍵](default:秘密鍵) を使うか、新しいランダムアカウントを生成して、NEM の [アカウント](default:アカウント) を作成する方法を説明します。

## 前提条件 {: #prerequisites }

まだ準備できていない場合は、まず [開発環境のセットアップ](../start/setup.md) を行ってください。

## 完全なコード {: #full-code }

{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full_tagged('devbook/accounts/create_from_private_key') }}

## コードの説明 {: #code-explanation }

### ファサードを初期化する {: #initializing-the-facade }

{{ tutorial.code_snippet_tagged('step-1') }}

<dy:NemFacade> は、NEM の暗号処理とネットワークユーティリティへのアクセスを提供します。
ネットワーク名（`testnet` または `mainnet`）で初期化することで、[アドレス](default:アドレス) などのネットワーク固有の値が正しく生成されるようにします。

### 秘密鍵を定義する {: #defining-a-private-key }

{{ tutorial.code_snippet_tagged('step-2') }}

この例では、まず環境変数 `PRIVATE_KEY` から秘密鍵を 16 進数文字列として取得します。
環境変数が設定されている場合は、その値を <dy:PrivateKey> オブジェクトに変換します。
設定されていない場合は、代わりに <dy:PrivateKey.random> を使って新しいランダムな秘密鍵を生成します。

!!! warning "秘密鍵を安全に保管してください"
    秘密鍵があれば、アカウントとそこに保有されている資産を完全に管理することができます。
    秘密鍵を失うと、アカウントに永久にアクセスできなくなります。
    他人が秘密鍵を入手すると、その人物がアカウントを管理できてしまいます。

    秘密鍵は決して誰とも共有せず、必ず安全な場所に保管してください。

### アカウントを作成する {: #creating-the-account }

{{ tutorial.code_snippet_tagged('step-3') }}

秘密鍵を定義した後、公開鍵とアドレスを導出してアカウントを作成します。

1. **キーペアの作成:** <dy:KeyPair> コンストラクターは秘密鍵を受け取り、対応する [公開鍵](default:公開鍵) を数学的に導出します。
   秘密鍵は秘密にしておく必要がありますが、公開鍵は安全に誰とでも共有できます。

2. **アドレスの導出:** <dy:network.publicKeyToAddress> メソッドは公開鍵を [アドレス](default:アドレス) に変換します。
   アドレスは、アカウントを識別する、より短く人間が読みやすいネットワーク固有の識別子です。

## 出力 {: #output }

以下は、プログラムの実行時の出力例です。

```text
--8<-- 'devbook/accounts/create_from_private_key.log'
```

環境変数なしでプログラムを実行するたびに、異なるランダムなアカウントが生成されます。
秘密鍵を指定した場合は、常に同じ公開鍵とアドレスが導出されます。

## まとめ {: #conclusion }

このチュートリアルでは、次の方法を説明しました。

| 手順 | 関連ドキュメント |
| --- | --- |
| [秘密鍵を読み込む](#defining-a-private-key) | <dy:PrivateKey> |
| [ランダムな秘密鍵を作成する](#defining-a-private-key) | <dy:PrivateKey.random> |
| [公開鍵を取得する](#creating-the-account) | <dy:KeyPair.publicKey> |
| [アドレスを取得する](#creating-the-account) | <dy:network.publicKeyToAddress> |

## 次のステップ {: #next-steps }

アカウントを作成できたので、次のことを行えます。

* [テストネットの資金をフォーセットから取得する](./testnet-faucet.md)
* [最初のトランザクションを送信する](../transactions/transfer-xem.md)
