# CATS DSL

CATS
:   The **CATS DSL** (humorously backronymed as **Compact Affinitized Transfer Schema**,
    and short for **Domain-Specific Language**) is a compact, descriptive language for defining the binary layout of
    structured data.

Originally developed for Symbol and NEM, it is used to specify all blocks and transactions in both protocols,
but its design is general enough to describe any binary format.

CATS prioritizes size efficiency, performance, and strict typing, aiming at zero-copy deserialization where possible.
Features include fixed-size buffers, strict type aliases, inline structures, and conditionally present fields.

CATS definitions are processed by _generators_: tools that produce code in a specific programming language to enable
applications to serialize (write) and deserialize (read) CATS-defined binary structures into native language constructs.

Generators currently exist for Python and JavaScript/TypeScript, with one for Java under development (as of June 2025).
These are used by the NEM SDKs to ensure consistent and efficient binary encoding across platforms.

This page describes the syntax and features of the CATS DSL.
For full precision, the Symbol source repository contains
[the exact grammar](https://github.com/symbol/symbol/blob/dev/catbuffer/parser/catparser/grammar/catbuffer.lark)
written using the [Lark parsing language](https://lark-parser.readthedocs.io).

!!! note "Whitespace"

    All CATS statements end with a line feed (semicolons are not used), but whitespace is otherwise not significant.

    Indentation is not required by the parsers, but is conventionally used to add clarity.

A CATS file is composed of four top-level keywords: `import`, `using`, `enum`, and `struct`.
Each of these is described in the sections below.

## `import`

CATS files can include other CATS files using the `import` statement.
This allows schema definitions to be modular and reusable.

To import another CATS file, specify its filename in quotes:

```cats
import "other.cats"
```

Imported filenames are resolved relative to the include path passed to the parser.

## `using`

The `using` statement defines a **type alias** for a built-in primitive type.
These aliases are treated as distinct types by the parser and generators,
enabling strict typing even when two types share the same underlying representation.

```cats
using <TypeAlias> = <Built-in type>
```

CATS supports aliases for two categories of built-in types:

* **Integer types**:
    * Unsigned: `uint8`, `uint16`, `uint32`, `uint64`
    * Signed: `int8`, `int16`, `int32`, `int64`
* **Fixed-size binary buffers**: `binary_fixed(N)` defines an N-bytes long buffer.

For example, to define a `Height` type as an 8-byte unsigned integer:

```cats
using Height = uint64
```

To define a `PublicKey` type as a 32-byte binary buffer:

```cats
using PublicKey = binary_fixed(32)
```

Although in the following example both `Height` and `Weight` are based on `uint64`,
they are treated as **distinct types** and cannot be used interchangeably:

```cats
using Height = uint64
using Weight = uint64
```

## `enum`

The `enum` statement defines an **enumeration**, a type consisting of named constants backed by an integer type.

Each enumeration must specify its backing type explicitly, and any of the built-in integer types can be used.

```cats
enum <TypeName> : <Backing type>
    <ConstantName> = <Value>
    ...
```

Enumeration members are defined on the lines below the `enum` declaration.
Each member must be assigned a constant integer value.

For example, to define a `TransportMode` enum backed by a 32-bit unsigned integer:

```cats
enum TransportMode : uint32
    ROAD = 0x0001
    SEA = 0x0002
    SKY = 0x0004
```

### Enum Attributes

Enumerations support attributes that modify their behavior.
Each attributes starts with `@` and must appear on the line above the enum declaration.
Currently, the only supported attribute is:

* `@is_bitwise`: indicates that the enumeration represents a bit field (i.e. a set of flags)
    and should support bitwise operations in the generated code.

    For example:

    ```cats
    @is_bitwise
    enum TransportMode : uint32
        ROAD = 0x0001
        SEA = 0x0002
        SKY = 0x0004
    ```

    This tells the generator that enum values can be combined using bitwise OR,
    and that individual flags may be checked using bitwise AND.

## `struct`

The `struct` statement defines a **structured binary layout** composed of named fields.

Structures are the most important building block in CATS: they are used to describe transactions, blocks,
and all other composite objects.

Each structure declaration starts with the `struct` keyword, optionally preceded by a _modifier_.
Fields are then defined on the lines following the declaration, giving them a name and a type:

```cats
[Optional modifier] struct <StructName>
    <FieldName> = <FieldType>
    ...
```

For example:

```cats
struct Vehicle
    weight = uint32
    wheel_count = uint8
```

### Modifiers

CATS supports the following modifiers:

* `abstract`: defines a base struct for inheritance.
    Generators produce a factory to instantiate the appropriate derived type.

* `inline`: indicates that the struct is used only for composition and should not be emitted as a standalone type.

If no modifier is specified, the struct is included in the generated output as-is.

### Special Field Constructors

Fields may also be declared using special constructors instead of a type:

* `make_const(type, value)`: defines a constant.
    This field does not appear in the layout. Instead, it becomes a constant accessible as
    `<StructName>.<FieldName>` in generated code.

    In this example, `TRANSPORT_MODE` is not serialized, but results in a constant `Car.TRANSPORT_MODE`
    of type `TransportMode` with value `ROAD`.

    ```cats
    struct Car
        TRANSPORT_MODE = make_const(TransportMode, ROAD)
    ```

* `make_reserved(type, value)`: defines a reserved field with a fixed value.
    This field is stored in the layout, and always has the provided value.

    In the example below, the field `wheel_count` is stored as a `uint8` with the fixed value `4`.

    ```cats
    struct Car
        wheel_count = make_reserved(uint8, 4)
    ```

* `sizeof(type, reference)`: defines a field automatically filled with the size (in bytes) of another field.
    This makes structures easier to maintain, since changing a referenced type does not require manually updating
    size fields.

    Here, `car_size` is an `uint16` that always contains the size, in bytes, of the field `car`,
    which has type `Car`.

    ```cats
    struct SingleCarGarage
        car_size = sizeof(uint16, car)
        car = Car
    ```

### Conditional Fields

Fields can be made **conditionally present** based on the value of another field.
This can be used to represent mutually exclusive layouts, similar to unions in other languages.

Conditional fields use the following syntax:

```cats
    <FieldName> = <FieldType> if <ConstantValue> <Operator> <SelectorField>
```

CATS supports the following conditional operators:

* `equals`: include the field if the selector field exactly matches the constant value.
* `not equals`: include the field if the selector field does not match the constant value.
* `has`: include the field if all bits in the constant value are set in the selector field (for bit flags).
* `not has`: include the field if any bits in the constant value are **not** set in the selector field.

For example, the field `buoyancy` is only included when `transport_mode` is equal to `SEA`:

```cats
struct Vehicle
    transport_mode = TransportMode

    buoyancy = uint32 if SEA equals transport_mode
```

### Array Fields

CATS supports both static and dynamically sized arrays, where all elements have the same type.

The syntax is:

```cats
    <FieldName> = array(<ElementType>, <NumberOfElements>)
```

Where `NumberOfElements` can be:

* A constant, producing a statically-sized array.

    ```cats
    struct SmallGarage
        vehicles = array(Vehicle, 4)
    ```

* A reference to another field, producing a dynamically-sized array.

    For example, the following struct defines a field `vehicles` containing `vehicles_count` elements of type `Vehicle`:

    ```cats
    struct Garage
        vehicles_count = uint32
        vehicles = array(Vehicle, vehicles_count)
    ```

* The special keyword `__FILL__` can be used to indicate that the array should extend until the end of the structure.

    In that case, the struct must be annotated with the `@size` attribute ([see below](#struct-attributes)),
    referencing a field that holds the total size in bytes.

    ```cats
    @size(garage_byte_size) struct Garage
        garage_byte_size = uint32
        vehicles = array(Vehicle, __FILL__)
    ```

!!! note

    `ElementType` must either be:

    * A fixed-size struct, or
    * A variable-size struct annotated with its own `@size` attribute

    Otherwise, the parser cannot determine how many elements to read from the byte stream.

#### Array Field Attributes

Array fields can be annotated with attributes to control how they are sized, aligned, or sorted.

Supported attributes include:

* `@is_byte_constrained`: interprets the array size as a number of bytes instead of element count.
* `@alignment(x [, [not] pad_last])`: aligns elements to `x`-byte boundaries; optionally pads the last element.

    By default, when alignment is used, the final element is padded.
    This can be disabled using the `not pad_last` qualifier.

* `@sort_key(x)`: ensures the array is sorted by the given property.

    For example, this array of `Vehicle` structs is sorted by weight:

    ```cats
    struct Garage
        @sort_key(weight)
        @alignment(8, not pad_last)
        vehicles = array(Vehicle, __FILL__)
    ```

### Inlines

A structure can be **inlined** within another using the `inline` modifier.
This allows the fields of one struct to be inserted directly into another without nesting.

For example, the following definition inlines the contents of `Vehicle` into `Car`:

```cats
struct Vehicle
    weight = uint32

struct Car
    inline Vehicle
    max_clearance = Height
    has_left_steering_wheel = uint8
```

Since the inlined fields are expanded in place the final layout of `Car` is equivalent to:

```cats
struct Car
    weight = uint32
    max_clearance = Height
    has_left_steering_wheel = uint8
```

!!! note "Named inlines"

    A struct can also be inlined with a **name**, which causes its fields to be renamed with that prefix:

    ```cats
    <FieldName> = inline <StructName>
    ```

    In this example, `SizePrefixedString` is inlined into `Vehicle` as `friendly_name`:

    ```cats
    struct SizePrefixedString
        size = uint32
        __value__ = array(int8, size)

    struct Vehicle
        weight = uint32
        friendly_name = inline SizePrefixedString
        year = uint16
    ```

    This expands to:

    ```cats
    struct Vehicle
        weight = uint32
        friendly_name_size = uint32
        friendly_name = array(int8, friendly_name_size)
        year = uint16
    ```

    The special field `__value__` is renamed to match the name given to the inline (`friendly_name`).
    All other fields are renamed with a prefix and underscore, such as `size` becoming `friendly_name_size`.

### Struct Attributes

Structures can include attributes that provide hints to code generators or affect layout behavior.
Attributes appear above the `struct` declaration, starting with `@`.

CATS supports the following struct-level attributes:

* `@is_aligned`: forces all fields to be aligned to their natural boundaries.
* `@is_size_implicit`: allows the struct to be referenced in a `sizeof(x)` expression.
* `@size(x)`: declares that the field `x` contains the full size of the struct in bytes.
* `@initializes(x, Y)`: initializes field `x` with the constant `Y` defined elsewhere.
* `@discriminator(x [, y...])`: used with `abstract` structs to select the appropriate derived type when decoding,
    based on the indicated properties.
* `@comparer(x [!transform] [, y...])`: defines which properties to use to sort or compare instances.
    The optional transforms are applied prior to property comparison.
    Currently, the only transform supported is `ripemd_keccak_256` for backwards compatibility with NEM.

For example, this links the field `transport_mode` in `Vehicle` to a constant defined in a derived struct:

```cats
@initializes(transport_mode, TRANSPORT_MODE)
abstract struct Vehicle
    transport_mode = TransportMode

struct Car
    TRANSPORT_MODE = make_const(TransportMode, ROAD)
    inline Vehicle
```

The constant `TRANSPORT_MODE` can be defined in any struct that extends `Vehicle`.

* * *

### Integer Field Attributes

Integer fields support one attribute:

* `@sizeref(x [, y])`: sets the value of the field to the size of `x`, optionally adjusted by an offset `y`.

    For example, to store the combined size of `vehicle_size` and `vehicle`:

    ```cats
    struct Garage
        @sizeref(vehicle, 2)
        vehicle_size = uint16
        vehicle = Vehicle
    ```

## Comments

Any line that begins with `#` is treated as a comment.

Comments not directly above a declaration are ignored by the parser.
However, if a comment is placed immediately before a declaration or field, it is treated as **documentation**
and may be preserved in the generated output.

For example:

```cats
# This comment is ignored

# This comment is included as documentation
# and will be associated with the `Height` alias.
using Height = uint64
```

This convention allows adding inline documentation to schemas without affecting the binary layout.
