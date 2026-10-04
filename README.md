<div align="center">

# D A E D A L U S

### Your models. Any format. Built for speed.

Text and binary serialization for Java.<br>
Powered by compile-time metaprogramming.

[Data formats](#data-formats) · [Release status](#release-status) · [Contribute](#how-to-contribute)

</div>

---

## What is Daedalus

Daedalus is a general-purpose library for working with text and binary data formats.
Its ambition is to bring exceptional performance and a natural Java API to every format it supports.
You write the models. Daedalus generates the code that reads, writes, and measures them.


> **Daedalus will not have a stable release until all three are finalized:**
>
> - [Project Valhalla](https://openjdk.org/projects/valhalla/)
> - [Vector API](https://openjdk.org/jeps/529)
> - [Lazy Constants](https://openjdk.org/jeps/526)

---

## Data formats

Protobuf is the first completed datatype. More text and binary formats are planned.

- [Protocol Buffers](daedalus-datatype/daedalus-protobuf)
- [JSON](daedalus-datatype/daedalus-json)

---

## How to contribute

Help shape a library that makes data easier to work with. Contributions can start with:

- **Formats:** implement a new datatype on the shared foundation.
- **Performance:** bring a representative workload and reproducible measurements.
- **Correctness:** improve compatibility and cover difficult edge cases.
- **Developer experience:** refine diagnostics, APIs, and examples.

Correctness and a coherent Java API matter just as much as speed. The Protobuf implementation is the starting reference for new datatypes.

<details>
<summary><strong>A guide to the source</strong></summary>

- [Type system](daedalus-typesystem): conversion, defaults, and sizing contracts.
- [Annotation processor](daedalus-processor): model analysis and source generation.
- [JDK support](daedalus-typesystem/daedalus-typesystem-jdk-support): mixins for standard Java types.
- [Datatypes](daedalus-datatype): format implementations and schema tools.
- [CLI](daedalus-cli): the shared command framework.

</details>

---

## Origins & support

Daedalus started as **ModernProtobuf**, born from building [Cobalt](https://github.com/Auties00/Cobalt) and wanting Protobuf models that felt at home in a public Java API. It is growing into a shared foundation for text and binary serialization.

Developed by [Alessandro Autiero](https://github.com/Auties00). If you find the project useful, you can help it grow through [GitHub Sponsors](https://github.com/sponsors/Auties00).
