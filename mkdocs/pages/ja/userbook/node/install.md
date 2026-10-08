---
title: ノードのインストール
---

# ノードクライアントのインストール

このガイドでは、[手動インストール](#manual-installation)または[Docker を使用](#using-docker)して NEM ノードをデプロイする方法を説明します。

## ハードウェア要件 {: #hardware-requirements }

* インターネットに接続されたマシン。データベースとログファイル用に約 30 GB のディスク容量と、16 GB の RAM が必要です（2026 年 7 月時点）。

* [コンセンサス](default:コンセンサス) と [ハーベスティング](default:ハーベスティング) に円滑に参加するには、ノードにパブリックに到達可能な IP アドレスが必要です。また、TCP ポート **7890** を受信と送信の双方に対して開く必要があります。

    パブリック IP アドレスがなくても、ノードは新しいブロックを生成してネットワークに送信できます。
    ただし、ピアノードは、検出した新しいブロックやトランザクションをノードに通知できません。
    この場合、ノードは定期的にピアへポーリングするため、新しいブロックやトランザクションの把握が遅くなります。

    また、[スーパーノードプログラム](default:スーパーノードプログラム) に参加するには、パブリック IP アドレスが必要です。

* ウォレットなどのアプリケーションがノードを介してネットワークと通信できるようにするには、次の TCP ポートを開く必要があります。

    * **7890**（[REST](../../devbook/reference/rest/nem.md) リクエスト用）。
    * **7778**（[WebSockets](../../devbook/reference/websockets/index.md) リクエスト用）。

## 手動インストール {: #manual-installation }

### 前提条件 {: #prerequisites }

* [Java JRE 11](https://docs.oracle.com/en/java/javase/11/) または [OpenJDK 11](https://openjdk.org/projects/jdk/11/) をインストールします。

NIS1 クライアントは、Linux、Windows、macOS など、Java をサポートするすべてのオペレーティングシステムで動作します。

### インストール {: #installation }

* [最新のバイナリをダウンロード](https://github.com/NemProject/nem/releases)します。

* NIS1 の _インストール_ フォルダーとなるフォルダーにファイルを解凍します。

### 設定 {: #manual-configuration }

新しい `nis/config-user.properties` ファイルを作成し、ケースに合わせて次の内容を変更します。

```ini
nem.folder = %h/nem
nis.bootName = my-server
nis.bootKey = 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
```

* `nem.folder` は NIS1 の _ホーム_ フォルダーを設定します。
    このフォルダーはインストールフォルダーと異なっていてもよく、プログラムのログとデータベースファイルを保存します。

    ユーザーのホームフォルダーを示すために `~` を使わず、`%h` を使います。
    デフォルトの場所は `%h/nem` です。

    Windows ではバックスラッシュを二重にする必要があります: `\\`。
    たとえば `D:\\NEM\\nis1-home` です。

* `nis.bootName` をサーバーに付ける名前に設定します。
    これは情報を示すだけの値です。
    先頭と末尾の空白は削除されますが、名前の途中には空白を含められます。
    UTF8 はエスケープによってサポートされていますが、ASCII 範囲外の文字は使わないでください
    （例: `\u3053\u3093\u306B\u3061\u306F`）。

* `nis.bootKey` をこのノードを管理するアカウントの[秘密鍵](default:秘密鍵)に設定します。
    まだアカウントを持っていない場合は、NEM NanoWallet などの[ウォレット](default:ウォレット)を使って作成します。

    * [委任ハーベスティング](default:委任ハーベスティング)を行う場合、これは[リモートアカウント](default:リモートキー)の秘密鍵です。
        この設定を**推奨**します。

    * [ローカルハーベスティング](default:ローカルハーベスティング)を行う場合、これはアカウント自体の秘密鍵です。
        この設定は**推奨しません**。

    !!! warning "このキーは常に秘密にしてください"

* ノードでハーベストする場合は、`nis.shouldAutoHarvestOnBoot` を `true` に設定します。

!!! note "ノードの初回起動を高速化する"

    ノードの初回起動を高速化するため、データベーススナップショットを任意でダウンロードできます。

    * [https://bob.nem.ninja/](https://bob.nem.ninja/) に移動し、最新の `nis5_mainnet-*.mv.db` ファイルをダウンロードします。
        例: [nis5_mainnet-5-565-850.mv.db.gz](https://bob.nem.ninja/nis5_mainnet-5-565-850.mv.db.gz)。
    * NIS1 のホームフォルダー内に `nis/data` という名前のフォルダーを作り、その中にファイルを解凍します。
        解凍したファイルの名前を `nis5_mainnet.mv.db` に変更します。

!!! example "テストネットノードを実行する"

    [テストネット](default:テストネット)ノードを実行する場合は、`config-user.properties` ファイルに次のプロパティを追加します。

    ```ini
    nem.network = testnet

    nis.treasuryReissuanceForkHeight = 1
    nis.treasuryReissuanceForkTransactionHashes =
    nis.treasuryReissuanceForkFallbackTransactionHashes =
    nis.multisigMOfNForkHeight = 1
    nis.mosaicsForkHeight = 1
    nis.firstFeeForkHeight = 1
    nis.secondFeeForkHeight = 1
    nis.remoteAccountForkHeight = 1
    nis.mosaicRedefinitionForkHeight = 1
    ```

### 起動 {: #launch }

ターミナルを開き、オペレーティングシステムに対応する起動スクリプトを確認します。

=== "Windows"

    ```bash
    runNis.bat
    ```

=== "Linux"

    ```bash
    nix.runNis.sh
    ```

!!! note "メモリ不足の問題"

    メモリ不足が発生した場合は、起動スクリプトを編集して[`-Xmx` パラメーターを増やします](https://docs.oracle.com/en/java/javase/11/tools/java.html#GUID-3B1CE181-CD30-4178-9602-230B800D4FAE__GUID-98AC4535-A539-406D-9AC5-390C1AF143F0)。

スクリプトを起動します。
コンソール出力にノードが実行中であることが示されます。

## Docker を使用する {: #using-docker }

これらの手順は、Windows Subsystem for Linux を含む Linux システムでのみ動作します。

### 前提条件 {: #prerequisites_1 }

* [Docker](https://docs.docker.com/get-docker/)。

* [Git](https://git-scm.com)。

### インストール {: #docker-installation }

次のコマンドで [nem-docker](https://github.com/NemProject/nem-docker) リポジトリをクローンします。

```bash
git clone https://github.com/NemProject/nem-docker.git
cd nem-docker
```

### 設定 {: #configuration }

初回実行時に、クライアントは（上の手動[設定](#manual-configuration)セクションで説明した）**boot name** と **boot key** のプロパティを尋ね、それらを保存します。

設定を手動で編集する場合は、クライアントを起動する前に、上記で説明した内容を使って `custom-configs/nis.config-user.properties` という新しいファイルを作成します。

### ノードを制御する {: #controlling-the-node }

* ノードを起動するには、次を実行します。

    ```bash
    ./boot.sh
    ```

* ノードを停止するには、次を実行します。

    ```bash
    ./stop.sh
    ```

他のコマンドについては、[nem-docker GitHub プロジェクト](https://github.com/NemProject/nem-docker)を参照してください。

## 同期 {: #synchronization }

ノードは初回起動時に、ピアからブロックチェーン全体をダウンロードします。

**この処理には最大 48 時間かかることがあります。**

任意のデータベーススナップショットをダウンロードした場合、ノードはまずデータベースを読み込み、その後、残りのブロックをダウンロードします。
これにより、同期時間が大幅に短縮されます。

同期中も、次のことを確認できます。

* パブリック IP がある場合、ノードを起動して数分後には、[nodewatch.symbol.tools](https://nodewatch.symbol.tools/nem/nodes) の公開ノード一覧に表示されるはずです。
    報告されるチェーン高は、ネットワークの他のノードに追いつくにつれて増加します。

* ブラウザで[localhost:7890/chain/height](http://localhost:7890/chain/height)を開いて、ノードに現在のチェーン高を尋ねることもできます。

## ノードを監視する {: #monitoring-the-node }

NIS はポート 7890 でクエリを待ち受けるため、ノードを監視する最初の方法は、ブラウザで[localhost:7890/node/info](http://localhost:7890/node/info)を開くことです。

`NIS_ILLEGAL_STATE_LOADING_CHAIN` のようなエラーを含め、何らかの応答があれば、ノードは実行中です。

クエリ可能な URL の一覧については、[REST API 仕様](../../devbook/reference/rest/nem.md)を参照してください。

## ノードを更新する {: #updating-a-node }

NIS1 クライアントを最新のプロトコルバージョンに更新するのは簡単です。

### 手動で行う {: #manually }

* `Ctrl+C` を押すかプロセスを終了して、サーバーを停止します。

* 古いパッケージを削除します。
    これは、`config-user.properties` を除く [NIS1 インストールフォルダー](#installation)内のすべてのファイルを削除することを意味します。
    NIS1 のホームフォルダー内のすべてのファイルは残します。

* [最新のバイナリ](https://github.com/NemProject/nem/releases)をダウンロードし、同じフォルダーに解凍します。

* [起動時と同じコマンド](#launch)で、サーバーを再び起動します。

### Docker を使用する {: #using-docker_1 }

* `./stop.sh` でサーバーを停止します。

* [インストール](#docker-installation)手順でクローンしたリポジトリを `git pull` で更新します。

* `./boot.sh` でサーバーを再起動します。
