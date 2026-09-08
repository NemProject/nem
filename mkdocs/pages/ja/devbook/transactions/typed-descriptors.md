---
title: 型付きディスクリプタ
tutorial_level: beginner
---

# JavaScript で型付きディスクリプタを使ってトランザクションを作成する

{% import 'tutorial.jinja2' as tutorial with context %}
{{ tutorial.code_full_tagged('devbook/transactions/transfer_xem.typed', ['js'], show=false) }}

トランザクションは NEM ブロックチェーンの基本的な要素です。ネットワークとのやり取りの多くがトランザクションを通じて行われるためです。

チュートリアル全体の JavaScript 例では、簡潔な構文でトランザクションを作成できる <js:TransactionFactory.create> メソッドを使用しています。
しかし、このメソッドは型安全ではありません。汎用オブジェクトを受け取り、正しいフィールドが含まれていることを前提とします。

このページでは、代わりに <js:NemFacade.createTransactionFromTypedDescriptor> を使う方法を説明します。
この方法は定義済みのパラメーターを受け取るため、型安全性が高く、IDE のサポートも向上します。

ここで示すコードは、[転送トランザクションを作成する](./transfer-xem.md) チュートリアルと同じですが、トランザクションの作成手順だけが異なります。
簡潔にするため、そのセクションだけを示します。署名やアナウンスを含む残りの手順は変わりません。

{{ tutorial.code_snippet_tagged('step-1') }}

[完全なチュートリアルコードをダウンロードする。]({{ config.repo_url }}/raw/refs/heads/{{config.extra.nem.branch}}/mkdocs/snippets/devbook/transactions/transfer_xem.typed.mjs){ .source-link }

## 作成手順 {: #creation-process }

トランザクションは、型安全な方法で次の 2 段階により作成します。トランザクションディスクリプタを作成し、そのディスクリプタからトランザクション自体を作成します。

### ディスクリプタを作成する {: #creating-the-descriptor }

{{ tutorial.code_snippet_tagged('step-2') }}

JavaScript でトランザクションを構築するとき、構造化されたパラメーターを持つコンストラクターによって型安全性を提供するのが型付きディスクリプタです。

例えば、コードで使われている <js:TransferTransactionV2Descriptor> を参照してください。

### トランザクションを作成する {: #creating-the-transaction }

{{ tutorial.code_snippet_tagged('step-3') }}

ディスクリプタの準備ができたら、トランザクションの作成は簡単です。ディスクリプタを <js:NemFacade.createTransactionFromTypedDescriptor> メソッドに渡し、必要な手数料と期限を指定します。

[転送トランザクションを作成する](./transfer-xem.md#calculating-the-transaction-fee) チュートリアルと同じく、トランザクションの内容によって手数料が決まるため、構築後に手数料を計算する必要があります。

!!! warning "型付き版と型なし版では期限の指定方法が異なります"

    <js:TransactionFactory.create> に渡す期限は秒数で指定し、_ネットワーク時刻_ を基準にします。
    一方、<js:NemFacade.createTransactionFromTypedDescriptor> に渡す期限も秒数で指定しますが、_システム時刻_、つまりコードを実行するマシンのローカル時計を基準にします。

    この方法は、現在のネットワーク時刻を取得する必要がないため便利です。例えばトランザクションを 2 時間後に期限切れにするには、上のコードのように `#!js 2 * 60 * 60` 秒の期限を指定するだけです。

    ただし、システム時計がネットワーク時刻と正しく同期していない場合、想定より早くトランザクションが期限切れになったり、指定した期限がネットワークの許可する最大オフセットである 24 時間を超えて完全に拒否されたりする可能性があります。

    **したがって、型安全なメソッドを使うアプリケーションでは、システム時計が正しく同期していることを確認するため、定期的にネットワーク時刻を確認する必要があります。**

トランザクションを作成したら、通常どおり使用できます。
型付きメソッドで作成したトランザクションと型なしメソッドで作成したトランザクションに違いはありません。
