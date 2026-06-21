# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 15/25 (60.0%)
- **Function parity:** 123/285 matched (target 180) — 43.2%
- **Class/type parity:** 29/50 matched (target 49) — 58.0%
- **Combined symbol parity:** 152/335 matched (target 229) — 45.4%
- **Average inline-code cosine:** 0.73 (function body across 13 matched files)
- **Average documentation cosine:** 0.44 (doc text across 13 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 3 files with <0.60 function similarity

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

### 2. base64.encode

- **Target:** `base64.Encode [PROVENANCE-FALLBACK]`
- **Similarity:** 0.75
- **Dependents:** 0
- **Priority Score:** 21902.5
- **Functions:** 16/18 matched (target 30)
- **Missing functions:** `fmt`, `assert_encoded_length`
- **Types:** 1/1 matched (target 4)
- **Missing types:** _none_
- **Tests:** 9/10 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `encode.rs` vs expected `encode.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:encode.rs` vs expected `encode.rs`
- **Provenance warning:** port-lint provenance header matched only by basename: `tests:tests/encode.rs` vs expected `encode.rs`
- **Proposed provenance header:** `// port-lint: source encode.rs` (current: `// port-lint: source encode.rs`)
- **Proposed provenance header:** `// port-lint: tests encode.rs` (current: `// port-lint: tests encode.rs`)
- **Proposed provenance header:** `// port-lint: tests encode.rs` (current: `// port-lint: tests tests/encode.rs`)
- **Lint issues:** 3

### 3. general_purpose.mod

- **Target:** `generalpurpose.GeneralPurpose [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 21710.0
- **Functions:** 13/13 matched (target 18)
- **Missing functions:** _none_
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Config`, `DecodeEstimate`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/general_purpose/mod.rs` vs expected `engine/general_purpose/mod.rs`
- **Proposed provenance header:** `// port-lint: source engine/general_purpose/mod.rs` (current: `// port-lint: source engine/general_purpose/mod.rs`)
- **Lint issues:** 1

### 4. engine.naive

- **Target:** `engine.Naive [PROVENANCE-FALLBACK]`
- **Similarity:** 0.67
- **Dependents:** 0
- **Priority Score:** 21303.3
- **Functions:** 8/8 matched (target 11)
- **Missing functions:** _none_
- **Types:** 3/5 matched (target 3)
- **Missing types:** `Config`, `DecodeEstimate`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/naive.rs` vs expected `engine/naive.rs`
- **Proposed provenance header:** `// port-lint: source engine/naive.rs` (current: `// port-lint: source engine/naive.rs`)
- **Lint issues:** 1

### 5. base64.display

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

### 6. base64.decode

- **Target:** `base64.Decode [PROVENANCE-FALLBACK]`
- **Similarity:** 0.70
- **Dependents:** 0
- **Priority Score:** 12003.0
- **Functions:** 17/18 matched (target 29)
- **Missing functions:** `do_decode_slice_doesnt_clobber_existing_prefix_or_suffix`
- **Types:** 2/2 matched (target 9)
- **Missing types:** _none_
- **Tests:** 9/10 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `decode.rs` vs expected `decode.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:decode.rs` vs expected `decode.rs`
- **Proposed provenance header:** `// port-lint: source decode.rs` (current: `// port-lint: source decode.rs`)
- **Proposed provenance header:** `// port-lint: tests decode.rs` (current: `// port-lint: tests decode.rs`)
- **Lint issues:** 2

### 7. base64.alphabet

- **Target:** `alphabet.Alphabet [PROVENANCE-FALLBACK]`
- **Similarity:** 0.69
- **Dependents:** 0
- **Priority Score:** 11603.1
- **Functions:** 13/13 matched (target 18)
- **Missing functions:** _none_
- **Types:** 2/3 matched (target 7)
- **Missing types:** `Error`
- **Tests:** 8/8 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `alphabet.rs` vs expected `alphabet.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `alphabet.rs` vs expected `alphabet.rs`
- **Proposed provenance header:** `// port-lint: source alphabet.rs` (current: `// port-lint: source alphabet.rs`)
- **Proposed provenance header:** `// port-lint: source alphabet.rs` (current: `// port-lint: source alphabet.rs`)
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

### 9. write.encoder

- **Target:** `write.EncoderWriter [PROVENANCE-FALLBACK]`
- **Similarity:** 0.60
- **Dependents:** 0
- **Priority Score:** 11104.0
- **Functions:** 9/10 matched (target 11)
- **Missing functions:** `drop`
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `write/encoder.rs` vs expected `write/encoder.rs`
- **Proposed provenance header:** `// port-lint: source write/encoder.rs` (current: `// port-lint: source write/encoder.rs`)
- **Lint issues:** 1

### 10. write.encoder_string_writer

- **Target:** `write.EncoderStringWriter [PROVENANCE-FALLBACK]`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 1104.4
- **Functions:** 8/8 matched (target 10)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 5)
- **Missing types:** _none_
- **Tests:** 2/2 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `write/encoder_string_writer.rs` vs expected `write/encoder_string_writer.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:write/encoder_string_writer.rs` vs expected `write/encoder_string_writer.rs`
- **Proposed provenance header:** `// port-lint: source write/encoder_string_writer.rs` (current: `// port-lint: source write/encoder_string_writer.rs`)
- **Proposed provenance header:** `// port-lint: tests write/encoder_string_writer.rs` (current: `// port-lint: tests write/encoder_string_writer.rs`)
- **Lint issues:** 2

### 11. general_purpose.decode

- **Target:** `generalpurpose.Decode [PROVENANCE-FALLBACK]`
- **Similarity:** 0.66
- **Dependents:** 0
- **Priority Score:** 1103.4
- **Functions:** 10/10 matched (target 13)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 4/4 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/general_purpose/decode.rs` vs expected `engine/general_purpose/decode.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `engine/general_purpose/decode.rs` vs expected `engine/general_purpose/decode.rs`
- **Proposed provenance header:** `// port-lint: source engine/general_purpose/decode.rs` (current: `// port-lint: source engine/general_purpose/decode.rs`)
- **Proposed provenance header:** `// port-lint: source engine/general_purpose/decode.rs` (current: `// port-lint: source engine/general_purpose/decode.rs`)
- **Lint issues:** 2

### 12. read.decoder

- **Target:** `read.DecoderReader [PROVENANCE-FALLBACK]`
- **Similarity:** 0.64
- **Dependents:** 0
- **Priority Score:** 803.6
- **Functions:** 7/7 matched (target 9)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `read/decoder.rs` vs expected `read/decoder.rs`
- **Proposed provenance header:** `// port-lint: source read/decoder.rs` (current: `// port-lint: source read/decoder.rs`)
- **Lint issues:** 1

### 13. general_purpose.decode_suffix

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

### 14. base64.lib

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

### 15. base64.prelude

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

