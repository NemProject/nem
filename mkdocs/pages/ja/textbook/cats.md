# CATS DSL

CATS
:   **CATS DSL**（**CATS** は **Compact Affinitized Transfer Schema** というユーモラスな逆頭字語で、**DSL** は **Domain-Specific Language** の略です）は、構造化データのバイナリレイアウトを定義するためのコンパクトで記述的な言語です。

もともと Symbol と NEM のために開発され、両プロトコルのすべてのブロックとトランザクションの仕様に使われていますが、設計は十分に汎用的で、任意のバイナリ形式を記述できます。

CATS はサイズ効率、性能、厳密な型付けを優先し、可能な場合はゼロコピーのデシリアライズを目指します。
固定サイズバッファー、厳密な型エイリアス、インライン構造、条件付きフィールドなどの機能があります。

CATS 定義は _ジェネレーター_ で処理されます。ジェネレーターは、CATS で定義したバイナリ構造をネイティブ言語の構造へシリアライズ（書き込み）またはデシリアライズ（読み取り）できるように、特定のプログラミング言語のコードを生成するツールです。

現在は Python と JavaScript/TypeScript 用のジェネレーターがあり、Java 用は開発中です（2025年6月時点）。
これらは NEM SDK で使われ、プラットフォーム間で一貫した効率的なバイナリエンコードを保証します。

このページでは CATS DSL の構文と機能を説明します。
完全な精度が必要な場合は、Symbol のソースリポジトリに [Lark 構文解析言語](https://lark-parser.readthedocs.io) で記述された [正確な文法](https://github.com/symbol/symbol/blob/dev/catbuffer/parser/catparser/grammar/catbuffer.lark) があります。

!!! note "空白"

    すべての CATS 文は改行で終わります（セミコロンは使いません）が、それ以外では空白は意味を持ちません。

    構文解析器にインデントは必要ありませんが、通常は明確さを加えるために使用します。

CATS ファイルは、トップレベルの4つのキーワード `#!cats import`、`#!cats using`、`#!cats enum`、`#!cats struct` で構成されます。
それぞれについて、以下のセクションで説明します。

## `#!cats import` {: #cats-import }

`#!cats import` 文を使うと、CATS ファイルに他の CATS ファイルを含められます。
これにより、スキーマ定義をモジュール化して再利用できます。

別の CATS ファイルをインポートするには、ファイル名を引用符で指定します。

```cats
import "other.cats"
```

インポートしたファイル名は、構文解析器に渡されたインクルードパスを基準に解決されます。

## `#!cats using` {: #cats-using }

`using` 文は、組み込みプリミティブ型の **型エイリアス** を定義します。
これらのエイリアスは構文解析器とジェネレーターでは別の型として扱われるため、2つの型が同じ基礎表現を共有していても厳密な型付けが可能です。

```cats
using <TypeAlias> = <Built-in type>
```

CATS は組み込み型を次の2カテゴリでエイリアス化できます。

* **整数型**：
    * 符号なし：`#!cats uint8`、`#!cats uint16`、`#!cats uint32`、`#!cats uint64`
    * 符号付き：`#!cats int8`、`#!cats int16`、`#!cats int32`、`#!cats int64`
* **固定サイズバイナリバッファー**：`#!cats binary_fixed(N)` は N バイト長のバッファーを定義します。

たとえば、8バイトの符号なし整数として `#!cats Height` 型を定義します。

```cats
using Height = uint64
```

32バイトのバイナリバッファーとして `#!cats PublicKey` 型を定義します。

```cats
using PublicKey = binary_fixed(32)
```

次の例では `#!cats Height` と `#!cats Weight` はどちらも `#!cats uint64` に基づきますが、**別の型** として扱われ、相互に入れ替えて使用できません。

```cats
using Height = uint64
using Weight = uint64
```

## `#!cats enum` {: #cats-enum }

`#!cats enum` 文は、整数型を基礎とする名前付き定数で構成される型、つまり **列挙型** を定義します。

各列挙型では基礎型を明示する必要があり、組み込み整数型のいずれかを使用できます。

```cats
enum <TypeName> : <Backing type>
    <ConstantName> = <Value>
    ...
```

列挙型のメンバーは `#!cats enum` 宣言の下の行に定義します。
各メンバーには定数の整数値を割り当てる必要があります。

たとえば、32ビット符号なし整数を基礎型とする `#!cats TransportMode` 列挙型を定義します。

```cats
enum TransportMode : uint32
    ROAD = 0x0001
    SEA = 0x0002
    SKY = 0x0004
```

### 列挙型属性 {: #enum-attributes }

列挙型は動作を変更する属性をサポートします。
各属性は `@` で始まり、列挙型宣言の上の行に記述する必要があります。
現在サポートされている属性は次の1つだけです。

* `#!cats @is_bitwise`：列挙型がビットフィールド（フラグの集合）を表し、生成コードでビット演算をサポートすることを示します。

    例：

    ```cats
    @is_bitwise
    enum TransportMode : uint32
        ROAD = 0x0001
        SEA = 0x0002
        SKY = 0x0004
    ```

    これは、ジェネレーターに列挙値をビット単位の OR で結合でき、個々のフラグをビット単位の AND で確認できることを伝えます。

## `#!cats struct` {: #cats-struct }

`#!cats struct` 文は、名前付きフィールドで構成される **構造化バイナリレイアウト** を定義します。

構造体は CATS の最も重要な構成要素です。トランザクション、ブロック、その他すべての複合オブジェクトを記述するために使われます。

各構造体宣言は、任意で _修飾子_ が前に付く `#!cats struct` キーワードで始まります。
宣言の後の行で、フィールド名と型を指定してフィールドを定義します。

```cats
[Optional modifier] struct <StructName>
    <FieldName> = <FieldType>
    ...
```

例：

```cats
struct Vehicle
    weight = uint32
    wheel_count = uint8
```

### 修飾子 {: #modifiers }

CATS は次の修飾子をサポートします。

* `#!cats abstract`：継承用の基底構造体を定義します。
    ジェネレーターは、適切な派生型をインスタンス化するファクトリーを生成します。

* `#!cats inline`：構造体が合成にだけ使われ、独立した型として出力されないことを示します。

修飾子を指定しなければ、構造体はそのまま生成出力に含まれます。

### 特別なフィールドコンストラクター {: #special-field-constructors }

型の代わりに、特別なコンストラクターを使ってフィールドを宣言することもできます。

* `#!cats make_const(type, value)`：定数を定義します。
    このフィールドはレイアウトに現れません。代わりに、生成コードで `#!cats <StructName>.<FieldName>` としてアクセスできる定数になります。

    次の例では `#!cats TRANSPORT_MODE` はシリアライズされませんが、`#!cats ROAD` 値を持つ `#!cats TransportMode` 型の `#!cats Car.TRANSPORT_MODE` 定数になります。

    ```cats
    struct Car
        TRANSPORT_MODE = make_const(TransportMode, ROAD)
    ```

* `#!cats make_reserved(type, value)`：固定値を持つ予約フィールドを定義します。
    このフィールドはレイアウトに保存され、常に指定された値になります。

    次の例では、フィールド `#!cats wheel_count` が固定値 `#!cats 4` の `#!cats uint8` として保存されます。

    ```cats
    struct Car
        wheel_count = make_reserved(uint8, 4)
    ```

* `#!cats sizeof(type, reference)`：別のフィールドのサイズ（バイト）で自動的に埋められるフィールドを定義します。
    参照する型を変更してもサイズフィールドを手動で更新する必要がないため、構造体の保守が簡単になります。

    ここで `#!cats car_size` は、`#!cats Car` 型のフィールド `#!cats car` のサイズ（バイト）を常に保持する `#!cats uint16` です。

    ```cats
    struct SingleCarGarage
        car_size = sizeof(uint16, car)
        car = Car
    ```

### 条件付きフィールド {: #conditional-fields }

別のフィールドの値に基づいて、条件付きで存在するフィールドを作成できます。
他の言語の共用体に似た、相互排他的なレイアウトを表せます。

条件付きフィールドの構文は次のとおりです。

```cats
    <FieldName> = <FieldType> if <ConstantValue> <Operator> <SelectorField>
```

CATS は次の条件演算子をサポートします。

* `#!cats equals`：セレクターフィールドが定数値と完全に一致する場合にフィールドを含めます。
* `#!cats not equals`：セレクターフィールドが定数値と一致しない場合にフィールドを含めます。
* `#!cats in`：セレクターフィールドに定数が含まれる場合にフィールドを含めます（ビットフラグ用）。
* `#!cats not in`：セレクターフィールドに定数が含まれない場合にフィールドを含めます。

たとえば、`#!cats transport_mode` が `#!cats SEA` と等しい場合だけ `#!cats buoyancy` フィールドが含まれます。

```cats
struct Vehicle
    transport_mode = TransportMode

    buoyancy = uint32 if SEA equals transport_mode
```

### 配列フィールド {: #array-fields }

CATS は、すべての要素が同じ型を持つ、静的サイズと動的サイズの両方の配列をサポートします。

構文は次のとおりです。

```cats
    <FieldName> = array(<ElementType>, <NumberOfElements>)
```

`#!cats <NumberOfElements>` には次を指定できます。

* 要素数を固定する定数。

    ```cats
    struct SmallGarage
        vehicles = array(Vehicle, 4)
    ```

* 別のフィールドへの参照。動的サイズの配列になります。

    たとえば次の構造体は、`#!cats vehicles_count` 個の `#!cats Vehicle` 型要素を含む `#!cats vehicles` フィールドを定義します。

    ```cats
    struct Garage
        vehicles_count = uint32
        vehicles = array(Vehicle, vehicles_count)
    ```

* 特別なキーワード `#!cats __FILL__`。構造体の末尾まで配列を拡張することを示します。

    この場合、構造体に [下記](#struct-attributes) の `#!cats @size` 属性を付け、合計サイズ（バイト）を保持するフィールドを参照する必要があります。

    ```cats
    @size(garage_byte_size)
    struct Garage
        garage_byte_size = uint32
        vehicles = array(Vehicle, __FILL__)
    ```

!!! note

    `#!cats <ElementType>` には次のいずれかを指定する必要があります。

    * 固定サイズ構造体。
    * 独自の `#!cats @size` 属性が付いた可変サイズ構造体。

    それ以外の場合、構文解析器はバイトストリームから読み取る要素数を判断できません。

#### 配列フィールド属性 {: #array-field-attributes }

配列フィールドには、サイズ、アラインメント、ソート方法を制御する属性を付けられます。

サポートされる属性には次があります。

* `#!cats @is_byte_constrained`：配列サイズを要素数ではなくバイト数として解釈します。
* `#!cats @alignment(x [, [not] pad_last])`：要素を x バイト境界に揃え、任意で最後の要素にパディングを付けます。

    デフォルトでは、アラインメントを使うと最後の要素にパディングが付きます。
    `#!cats not pad_last` 修飾子で無効にできます。

* `#!cats @sort_key(x)`：指定したプロパティで配列がソートされるようにします。

    たとえば、次の `#!cats Vehicle` 構造体の配列は weight でソートされます。

    ```cats
    struct Garage
        @sort_key(weight)
        @alignment(8, not pad_last)
        vehicles = array(Vehicle, __FILL__)
    ```

### インライン {: #inlines }

`#!cats inline` 修飾子を使うと、ある構造体を別の構造体の中に **インライン化** できます。
これにより、入れ子にせずに1つの構造体のフィールドを別の構造体へ直接挿入できます。

たとえば、次の定義は `#!cats Vehicle` の内容を `#!cats Car` にインライン化します。

```cats
struct Vehicle
    weight = uint32

struct Car
    inline Vehicle
    max_clearance = Height
    has_left_steering_wheel = uint8
```

インライン化されたフィールドはその場所で展開されるため、`#!cats Car` の最終レイアウトは次と同じです。

```cats
struct Car
    weight = uint32
    max_clearance = Height
    has_left_steering_wheel = uint8
```

!!! note "名前付きインライン"

    構造体は **名前** を付けてインライン化することもでき、その接頭辞でフィールド名が変更されます。

    ```cats
    <FieldName> = inline <StructName>
    ```

    次の例では `#!cats SizePrefixedString` を `#!cats friendly_name` として `#!cats Vehicle` にインライン化します。

    ```cats
    struct SizePrefixedString
        size = uint32
        __value__ = array(int8, size)

    struct Vehicle
        weight = uint32
        friendly_name = inline SizePrefixedString
        year = uint16
    ```

    次のように展開されます。

    ```cats
    struct Vehicle
        weight = uint32
        friendly_name_size = uint32
        friendly_name = array(int8, friendly_name_size)
        year = uint16
    ```

    特別なフィールド `#!cats __value__` は、インラインに指定された名前（`#!cats friendly_name`）に変更されます。
    それ以外のフィールドは接頭辞とアンダースコアで変更されます。たとえば `#!cats size` は `#!cats friendly_name_size` になります。

### 構造体属性 {: #struct-attributes }

構造体には、コードジェネレーターへのヒントやレイアウト動作への影響を与える属性を含められます。
属性は `@` で始まり、`#!cats struct` 宣言の上に記述します。

CATS は次の構造体レベル属性をサポートします。

* `#!cats @is_aligned`：すべてのフィールドを自然な境界に揃えます。
* `#!cats @is_size_implicit`：構造体を `#!cats sizeof(type, field)` 式で参照できるようにします。
* `#!cats @size(x)`：フィールド `x` が構造体全体のサイズ（バイト）を保持することを宣言します。
* `#!cats @initializes(x, Y)`：別の場所で定義された定数 `Y` でフィールド `x` を初期化します。
* `#!cats @discriminator(x [, y...])`：`#!cats abstract` 構造体で使い、指定したプロパティに基づいてデコード時に適切な派生型を選択します。
* `#!cats @comparer(x [!transform] [, y...])`：インスタンスのソートまたは比較に使うプロパティを定義します。
    任意の変換はプロパティ比較の前に適用されます。
    現在サポートされている変換は、NEM との後方互換性のための `#!cats ripemd_keccak_256` だけです。

たとえば、次は `#!cats Vehicle` のフィールド `#!cats transport_mode` を派生構造体に定義された定数へリンクします。

```cats
@initializes(transport_mode, TRANSPORT_MODE)
abstract struct Vehicle
    transport_mode = TransportMode

struct Car
    TRANSPORT_MODE = make_const(TransportMode, ROAD)
    inline Vehicle
```

定数 `#!cats TRANSPORT_MODE` は `#!cats Vehicle` を拡張する任意の構造体で定義できます。

### 整数フィールド属性 {: #integer-field-attributes }

整数フィールドは1つの属性をサポートします。

* `#!cats @sizeref(x [, y])`：フィールドの値を `x` のサイズに設定し、任意でオフセット `y` を加えます。

    たとえば、`#!cats vehicle_size` と `#!cats vehicle` の合計サイズを保存します。

    ```cats
    struct Garage
        @sizeref(vehicle, 2)
        vehicle_size = uint16
        vehicle = Vehicle
    ```

## コメント {: #comments }

`#` で始まる行はコメントとして扱われます。

宣言の直上にないコメントは構文解析器に無視されます。
ただし、宣言またはフィールドの直前にコメントを置くと **ドキュメント** として扱われ、生成出力に保持されることがあります。

例：

```cats
# This comment is ignored

# This comment is included as documentation
# and will be associated with the `#!cats Height` alias.
using Height = uint64
```

この規約により、バイナリレイアウトに影響を与えずにスキーマへインラインドキュメントを追加できます。
