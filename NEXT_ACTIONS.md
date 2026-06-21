# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 11/25 (44.0%)
- **Function parity:** 76/289 matched (target 115) — 26.3%
- **Class/type parity:** 21/50 matched (target 38) — 42.0%
- **Combined symbol parity:** 97/339 matched (target 153) — 28.6%
- **Average inline-code cosine:** 0.73 (function body across 9 matched files)
- **Average documentation cosine:** 0.59 (doc text across 9 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 4 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. base64.chunked_encoder

- **Target:** `base64.ChunkedEncoder [PROVENANCE-FALLBACK]`
- **Similarity:** 0.73
- **Dependents:** 2
- **Priority Score:** 2041702.8
- **Functions:** 8/11 matched (target 12)
- **Missing functions:** `chunked_encode_matches_normal_encode_random`, `chunked_encode_str`, `encode_to_string`
- **Types:** 5/6 matched
- **Missing types:** `Error`
- **Tests:** 5/8 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `chunked_encoder.rs` vs expected `chunked_encoder.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:chunked_encoder.rs` vs expected `chunked_encoder.rs`
- **Proposed provenance header:** `// port-lint: source chunked_encoder.rs` (current: `// port-lint: source chunked_encoder.rs`)
- **Proposed provenance header:** `// port-lint: tests chunked_encoder.rs` (current: `// port-lint: tests chunked_encoder.rs`)
- **Lint issues:** 2

### 2. general_purpose.mod

- **Target:** `generalpurpose.GeneralPurpose [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 71710.0
- **Functions:** 8/13 matched (target 14)
- **Missing functions:** `new`, `encode_table`, `decode_table`, `read_u64`, `default`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Config`, `DecodeEstimate`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/general_purpose/mod.rs` vs expected `engine/general_purpose/mod.rs`
- **Proposed provenance header:** `// port-lint: source engine/general_purpose/mod.rs` (current: `// port-lint: source engine/general_purpose/mod.rs`)
- **Lint issues:** 1

### 3. base64.decode

- **Target:** `base64.Decode [PROVENANCE-FALLBACK]`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 42004.4
- **Functions:** 14/18 matched (target 24)
- **Missing functions:** `fmt`, `source`, `from`, `do_decode_slice_doesnt_clobber_existing_prefix_or_suffix`
- **Types:** 2/2 matched (target 9)
- **Missing types:** _none_
- **Tests:** 9/10 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `decode.rs` vs expected `decode.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:decode.rs` vs expected `decode.rs`
- **Proposed provenance header:** `// port-lint: source decode.rs` (current: `// port-lint: source decode.rs`)
- **Proposed provenance header:** `// port-lint: tests decode.rs` (current: `// port-lint: tests decode.rs`)
- **Lint issues:** 2

### 4. general_purpose.decode

- **Target:** `generalpurpose.Decode [PROVENANCE-FALLBACK]`
- **Similarity:** 0.42
- **Dependents:** 0
- **Priority Score:** 41105.8
- **Functions:** 6/10 matched (target 9)
- **Missing functions:** `new`, `decode_chunk_8_writes_only_6_bytes`, `decode_chunk_4_writes_only_3_bytes`, `estimate_via_u128_inflation`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 1/4 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/general_purpose/decode.rs` vs expected `engine/general_purpose/decode.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/general_purpose/decode.rs` vs expected `engine/general_purpose/decode.rs`
- **Proposed provenance header:** `// port-lint: source engine/general_purpose/decode.rs` (current: `// port-lint: source engine/general_purpose/decode.rs`)
- **Proposed provenance header:** `// port-lint: source engine/general_purpose/decode.rs` (current: `// port-lint: source engine/general_purpose/decode.rs`)
- **Lint issues:** 2

### 5. base64.encode

- **Target:** `base64.Encode [PROVENANCE-FALLBACK]`
- **Similarity:** 0.70
- **Dependents:** 0
- **Priority Score:** 31903.0
- **Functions:** 15/18 matched (target 21)
- **Missing functions:** `fmt`, `encoded_size_overflow`, `assert_encoded_length`
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_
- **Tests:** 8/10 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `encode.rs` vs expected `encode.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:encode.rs` vs expected `encode.rs`
- **Proposed provenance header:** `// port-lint: source encode.rs` (current: `// port-lint: source encode.rs`)
- **Proposed provenance header:** `// port-lint: tests encode.rs` (current: `// port-lint: tests encode.rs`)
- **Lint issues:** 2

### 6. base64.alphabet

- **Target:** `alphabet.Alphabet [PROVENANCE-FALLBACK]`
- **Similarity:** 0.66
- **Dependents:** 0
- **Priority Score:** 31603.4
- **Functions:** 11/13 matched (target 16)
- **Missing functions:** `as_str`, `fmt`
- **Types:** 2/3 matched (target 7)
- **Missing types:** `Error`
- **Tests:** 8/8 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `alphabet.rs` vs expected `alphabet.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `alphabet.rs` vs expected `alphabet.rs`
- **Proposed provenance header:** `// port-lint: source alphabet.rs` (current: `// port-lint: source alphabet.rs`)
- **Proposed provenance header:** `// port-lint: source alphabet.rs` (current: `// port-lint: source alphabet.rs`)
- **Lint issues:** 2

### 7. base64.display

- **Target:** `base64.Display [PROVENANCE-FALLBACK]`
- **Similarity:** 0.68
- **Dependents:** 0
- **Priority Score:** 21003.2
- **Functions:** 5/6 matched (target 7)
- **Missing functions:** `encode_to_string`
- **Types:** 3/4 matched
- **Missing types:** `Error`
- **Tests:** 2/3 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `display.rs` vs expected `display.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:display.rs` vs expected `display.rs`
- **Proposed provenance header:** `// port-lint: source display.rs` (current: `// port-lint: source display.rs`)
- **Proposed provenance header:** `// port-lint: tests display.rs` (current: `// port-lint: tests display.rs`)
- **Lint issues:** 2

### 8. engine.mod

- **Target:** `engine.Mod [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 11410.0
- **Functions:** 8/9 matched (target 11)
- **Missing functions:** `inner`
- **Types:** 5/5 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/mod.rs` vs expected `engine/mod.rs`
- **Proposed provenance header:** `// port-lint: source engine/mod.rs` (current: `// port-lint: source engine/mod.rs`)
- **Lint issues:** 1

### 9. general_purpose.decode_suffix

- **Target:** `generalpurpose.DecodeSuffix [PROVENANCE-FALLBACK]`
- **Similarity:** 0.81
- **Dependents:** 0
- **Priority Score:** 101.9
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/general_purpose/decode_suffix.rs` vs expected `engine/general_purpose/decode_suffix.rs`
- **Proposed provenance header:** `// port-lint: source engine/general_purpose/decode_suffix.rs` (current: `// port-lint: source engine/general_purpose/decode_suffix.rs`)
- **Lint issues:** 1

### 10. base64.lib

- **Target:** `base64.Lib [PROVENANCE-FALLBACK]`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `lib.rs` vs expected `lib.rs`
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source lib.rs`)
- **Lint issues:** 1

### 11. base64.prelude

- **Target:** `prelude.Prelude [PROVENANCE-FALLBACK]`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `prelude.rs` vs expected `prelude.rs`
- **Proposed provenance header:** `// port-lint: source prelude.rs` (current: `// port-lint: source prelude.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `read.mod` | `base64.src.read.Mod` | 0 | `base64/src/read/mod.rs` | `base64/src/read/Mod.kt` |
| `write.mod` | `base64.src.write.Mod` | 0 | `base64/src/write/mod.rs` | `base64/src/write/Mod.kt` |

