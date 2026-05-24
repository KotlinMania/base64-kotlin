# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 10/21 (47.6%)
- **Function parity:** 39/257 matched (target 60) — 15.2%
- **Class/type parity:** 16/48 matched (target 29) — 33.3%
- **Combined symbol parity:** 55/305 matched (target 89) — 18.0%
- **Average inline-code cosine:** 0.52 (function body across 8 matched files)
- **Average documentation cosine:** 0.58 (doc text across 8 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 6 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. chunked_encoder

- **Target:** `base64.ChunkedEncoder`
- **Similarity:** 0.10
- **Dependents:** 2
- **Priority Score:** 2121709.0
- **Functions:** 2/11 matched (target 2)
- **Missing functions:** `new`, `chunked_encode_empty`, `chunked_encode_intermediate_fast_loop`, `chunked_encode_fast_loop`, `chunked_encode_slow_loop_only`, `chunked_encode_matches_normal_encode_random_string_sink`, `chunked_encode_matches_normal_encode_random`, `chunked_encode_str`, `encode_to_string`
- **Types:** 3/6 matched (target 3)
- **Missing types:** `Error`, `SinkTestHelper`, `StringSinkTestHelper`
- **Tests:** 0/8 matched

### 2. decode

- **Target:** `base64.Decode`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 182010.0
- **Functions:** 0/18 matched (target 3)
- **Missing functions:** `fmt`, `source`, `from`, `decode`, `decode_engine`, `decode_engine_vec`, `decode_engine_slice`, `decoded_len_estimate`, `decode_into_nonempty_vec_doesnt_clobber_existing_prefix`, `decode_slice_doesnt_clobber_existing_prefix_or_suffix`, `decode_slice_unchecked_doesnt_clobber_existing_prefix_or_suffix`, `decode_engine_estimation_works_for_various_lengths`, `decode_slice_output_length_errors`, `do_decode_slice_doesnt_clobber_existing_prefix_or_suffix`, `decode_error`, `decode_slice_error`, `deprecated_fns`, `decoded_len_est`
- **Types:** 2/2 matched (target 8)
- **Missing types:** _none_
- **Tests:** 0/10 matched

### 3. encode

- **Target:** `base64.Encode`
- **Similarity:** 0.15
- **Dependents:** 0
- **Priority Score:** 151908.5
- **Functions:** 3/18 matched (target 4)
- **Missing functions:** `encode`, `encode_engine`, `encode_engine_string`, `encode_engine_slice`, `fmt`, `encoded_size_correct_standard`, `encoded_size_correct_no_pad`, `encoded_size_overflow`, `encode_engine_string_into_nonempty_buffer_doesnt_clobber_prefix`, `encode_engine_slice_into_nonempty_buffer_doesnt_clobber_suffix`, `encode_to_slice_random_valid_utf8`, `encode_with_padding_random_valid_utf8`, `add_padding_random_valid_utf8`, `assert_encoded_length`, `encode_imap`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 0/10 matched

### 4. general_purpose.mod

- **Target:** `generalpurpose.GeneralPurpose [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 71710.0
- **Functions:** 8/13 matched (target 14)
- **Missing functions:** `new`, `encode_table`, `decode_table`, `read_u64`, `default`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Config`, `DecodeEstimate`

### 5. general_purpose.decode

- **Target:** `generalpurpose.Decode`
- **Similarity:** 0.42
- **Dependents:** 0
- **Priority Score:** 41105.8
- **Functions:** 6/10 matched (target 9)
- **Missing functions:** `new`, `decode_chunk_8_writes_only_6_bytes`, `decode_chunk_4_writes_only_3_bytes`, `estimate_via_u128_inflation`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 1/4 matched

### 6. alphabet

- **Target:** `alphabet.Alphabet`
- **Similarity:** 0.66
- **Dependents:** 0
- **Priority Score:** 31603.4
- **Functions:** 11/13 matched (target 16)
- **Missing functions:** `as_str`, `fmt`
- **Types:** 2/3 matched (target 7)
- **Missing types:** `Error`
- **Tests:** 8/8 matched

### 7. engine.mod

- **Target:** `engine.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 11410.0
- **Functions:** 8/9 matched (target 11)
- **Missing functions:** `inner`
- **Types:** 5/5 matched
- **Missing types:** _none_

### 8. general_purpose.decode_suffix

- **Target:** `generalpurpose.DecodeSuffix`
- **Similarity:** 0.81
- **Dependents:** 0
- **Priority Score:** 101.9
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 9. lib

- **Target:** `base64.Lib`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 10. prelude

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
| `read.mod` | `read.Mod` | 0 | `read/mod.rs` | `read/Mod.kt` |
| `write.mod` | `write.Mod` | 0 | `write/mod.rs` | `write/Mod.kt` |

