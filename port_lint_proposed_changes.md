# port-lint Proposed Changes

**Generated:** 2026-08-25
**Source:** tmp/base64/src
**Target:** src/commonMain/kotlin

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonTest/kotlin/io/github/kotlinmania/base64/TopLevelEncodeTest.kt` | `// port-lint: tests tests/encode.rs` | `// port-lint: tests encode.rs` | `encode.rs` | `port-lint provenance header matched only by basename: 'tests:tests/encode.rs' vs expected 'encode.rs'` |
