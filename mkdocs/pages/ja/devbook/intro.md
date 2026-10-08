---
title: 開発者マニュアルへようこそ
---

# 開発者マニュアルへようこそ

開発者マニュアルは、NEM 上でアプリケーションを構築する開発者向けのドキュメントです。
[SDK](default:SDK) または HTTP API を使って一般的なタスクを実行する方法を、複数のプログラミング言語によるコード例で説明します。

マニュアルは次のように構成されています。

<div class="icon-list" markdown>

* :material-laptop: **はじめに**

    開発マシンをセットアップし、簡単な `Hello World` サンプルを実行して準備が整っていることを確認します。

* :material-school: **チュートリアル**

    分野ごとにまとめられた、タスクに焦点を当てたチュートリアルに沿って進めます。
    各チュートリアルには、背景情報が必要な場合に参照できる [テキストブック](../textbook/intro.md) と関連するリファレンスガイドへのリンクがあります。

* :material-book-open-page-variant: **リファレンスガイド**

    SDK のメソッド、HTTP および WebSocket エンドポイント、バイナリ構造について網羅的な情報を参照できます。

</div>

ナビゲーションメニューを使うか、以下のチュートリアルのいずれかに直接移動してください。

チュートリアルは、NEM の概念に関する必要な知識に応じて、初級から上級までのレベルに分かれています。

{% import 'tutorials_table.jinja2' as tutorials_table with context %}

{{ tutorials_table.render() }}
