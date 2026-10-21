# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 12/21 (57.1%)
- **Function parity:** 103/231 matched (target 152) — 44.6%
- **Class/type parity:** 22/39 matched (target 42) — 56.4%
- **Combined symbol parity:** 125/270 matched (target 194) — 46.3%
- **Average inline-code cosine:** 0.71 (function body across 12 matched files)
- **Average documentation cosine:** 0.55 (doc text across 12 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 1 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. chunked_encoder

- **Target:** `base64.ChunkedEncoder`
- **Similarity:** 0.73
- **Dependents:** 2
- **Priority Score:** 2041702.8
- **Functions:** 8/11 matched (target 12)
- **Missing functions:** `chunked_encode_matches_normal_encode_random`, `chunked_encode_str`, `encode_to_string`
- **Types:** 5/6 matched
- **Missing types:** `Error`
- **Tests:** 5/8 matched

### 2. engine.naive

- **Target:** `engine.Naive`
- **Similarity:** 0.67
- **Dependents:** 0
- **Priority Score:** 21303.3
- **Functions:** 8/8 matched (target 11)
- **Missing functions:** _none_
- **Types:** 3/5 matched (target 3)
- **Missing types:** `Config`, `DecodeEstimate`

### 3. display

- **Target:** `base64.Display`
- **Similarity:** 0.68
- **Dependents:** 0
- **Priority Score:** 21003.2
- **Functions:** 5/6 matched (target 7)
- **Missing functions:** `encode_to_string`
- **Types:** 3/4 matched
- **Missing types:** `Error`
- **Tests:** 2/3 matched

### 4. decode

- **Target:** `base64.Decode`
- **Similarity:** 0.70
- **Dependents:** 0
- **Priority Score:** 12003.0
- **Functions:** 17/18 matched (target 29)
- **Missing functions:** `do_decode_slice_doesnt_clobber_existing_prefix_or_suffix`
- **Types:** 2/2 matched (target 9)
- **Missing types:** _none_
- **Tests:** 9/10 matched

### 5. encode

- **Target:** `base64.Encode`
- **Similarity:** 0.76
- **Dependents:** 0
- **Priority Score:** 11902.4
- **Functions:** 17/18 matched (target 31)
- **Missing functions:** `assert_encoded_length`
- **Types:** 1/1 matched (target 4)
- **Missing types:** _none_
- **Tests:** 9/10 matched

### 6. alphabet

- **Target:** `alphabet.Alphabet`
- **Similarity:** 0.69
- **Dependents:** 0
- **Priority Score:** 11603.1
- **Functions:** 13/13 matched (target 18)
- **Missing functions:** _none_
- **Types:** 2/3 matched (target 7)
- **Missing types:** `Error`
- **Tests:** 8/8 matched

### 7. write.encoder

- **Target:** `write.EncoderWriter`
- **Similarity:** 0.60
- **Dependents:** 0
- **Priority Score:** 11104.0
- **Functions:** 9/10 matched (target 11)
- **Missing functions:** `drop`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 8. write.encoder_string_writer

- **Target:** `write.EncoderStringWriter`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 1104.4
- **Functions:** 8/8 matched (target 10)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 5)
- **Missing types:** _none_
- **Tests:** 2/2 matched

### 9. general_purpose.decode

- **Target:** `generalpurpose.Decode`
- **Similarity:** 0.66
- **Dependents:** 0
- **Priority Score:** 1103.4
- **Functions:** 10/10 matched (target 13)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 4/4 matched

### 10. read.decoder

- **Target:** `read.DecoderReader`
- **Similarity:** 0.64
- **Dependents:** 0
- **Priority Score:** 803.6
- **Functions:** 7/7 matched (target 9)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 11. general_purpose.decode_suffix

- **Target:** `generalpurpose.DecodeSuffix`
- **Similarity:** 0.81
- **Dependents:** 0
- **Priority Score:** 101.9
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 12. prelude

- **Target:** `prelude.Prelude`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

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
| `general_purpose.mod` | `engine.generalpurpose.Mod` | 0 | `engine/general_purpose/mod.rs` | `engine/generalpurpose/Mod.kt` |
| `engine.mod` | `engine.Mod` | 0 | `engine/mod.rs` | `engine/Mod.kt` |
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |
| `read.mod` | `read.Mod` | 0 | `read/mod.rs` | `read/Mod.kt` |
| `write.mod` | `write.Mod` | 0 | `write/mod.rs` | `write/Mod.kt` |

