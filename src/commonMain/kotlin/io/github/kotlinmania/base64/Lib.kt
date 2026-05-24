// port-lint: source lib.rs
package io.github.kotlinmania.base64

/**
 * Correct, fast, and configurable [base64](https://developer.mozilla.org/en-US/docs/Glossary/Base64)
 * decoding and encoding. Base64 transports binary data efficiently in contexts where only plain
 * text is allowed.
 *
 * # Usage
 *
 * Use an [io.github.kotlinmania.base64.engine.Engine] to decode or encode base64, configured with
 * the base64 alphabet and padding behavior best suited to your application.
 *
 * ## Engine setup
 *
 * There is more than one way to encode a stream of bytes as "base64". Different applications use
 * different encoding [alphabets][io.github.kotlinmania.base64.alphabet.Alphabet] and
 * [padding behaviors][io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurposeConfig].
 *
 * ### Encoding alphabet
 *
 * Almost all base64 [alphabets][io.github.kotlinmania.base64.alphabet.Alphabet] use `A-Z`, `a-z`,
 * and `0-9`, which gives nearly 64 characters (26 + 26 + 10 = 62), but they differ in their choice
 * of their final 2.
 *
 * Most applications use the [standard][io.github.kotlinmania.base64.alphabet.STANDARD] alphabet
 * specified in [RFC 4648](https://datatracker.ietf.org/doc/html/rfc4648#section-4). If that's all
 * you need, you can get started quickly by using the pre-configured
 * [STANDARD][io.github.kotlinmania.base64.engine.generalpurpose.STANDARD] engine, which is also
 * available in the [io.github.kotlinmania.base64.prelude] module if you prefer a minimal `import`
 * footprint.
 *
 * Other common alphabets are available in the [io.github.kotlinmania.base64.alphabet] module.
 *
 * #### URL-safe alphabet
 *
 * The standard alphabet uses `+` and `/` as its two non-alphanumeric tokens, which cannot be safely
 * used in URLs without encoding them as `%2B` and `%2F`.
 *
 * To avoid that, some applications use a
 * ["URL-safe" alphabet][io.github.kotlinmania.base64.alphabet.URL_SAFE], which uses `-` and `_`
 * instead. To use that alternative alphabet, use the
 * [URL_SAFE][io.github.kotlinmania.base64.engine.generalpurpose.URL_SAFE] engine.
 *
 * ### Padding characters
 *
 * Each base64 character represents 6 bits (2⁶ = 64) of the original binary data, and every 3 bytes
 * of input binary data will encode to 4 base64 characters (8 bits × 3 = 6 bits × 4 = 24 bits).
 *
 * When the input is not an even multiple of 3 bytes in length,
 * [canonical](https://datatracker.ietf.org/doc/html/rfc4648#section-3.5) base64 encoders insert
 * padding characters at the end, so that the output length is always a multiple of 4.
 *
 * Canonical encoding ensures that base64 encodings will be exactly the same, byte-for-byte,
 * regardless of input length. But the `=` padding characters aren't necessary for decoding, and
 * they may be omitted by using a
 * [NO_PAD][io.github.kotlinmania.base64.engine.generalpurpose.NO_PAD] configuration.
 *
 * The pre-configured `NO_PAD` engines will reject inputs containing padding `=` characters. To
 * encode without padding and still accept padding while decoding, create an
 * [engine][io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose] with that
 * [padding mode][io.github.kotlinmania.base64.engine.DecodePaddingMode].
 *
 * ### Further customization
 *
 * Decoding and encoding behavior can be customized by creating an
 * [engine][io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose] with an
 * [alphabet][io.github.kotlinmania.base64.alphabet.Alphabet] and
 * [padding configuration][io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurposeConfig].
 *
 * ## Memory allocation
 *
 * The [decode][io.github.kotlinmania.base64.engine.Engine.decode] and
 * [encode][io.github.kotlinmania.base64.engine.Engine.encode] engine methods allocate memory for
 * their results — `decode` returns a `ByteArray` and `encode` returns a `String`. To instead
 * decode or encode into a buffer that you allocated, use one of the alternative methods:
 *
 * #### Decoding
 *
 * | Method                  | Output                              | Allocates memory             |
 * | ----------------------- | ----------------------------------- | ---------------------------- |
 * | `Engine.decode`         | returns a new `ByteArray`           | always                       |
 * | `Engine.decodeVec`      | appends to provided `MutableList<Byte>` | if the list lacks capacity |
 * | `Engine.decodeSlice`    | writes to provided `ByteArray`      | never                        |
 *
 * #### Encoding
 *
 * | Method                  | Output                              | Allocates memory             |
 * | ----------------------- | ----------------------------------- | ---------------------------- |
 * | `Engine.encode`         | returns a new `String`              | always                       |
 * | `Engine.encodeString`   | appends to provided `StringBuilder` | if the builder lacks capacity |
 * | `Engine.encodeSlice`    | writes to provided `ByteArray`      | never                        |
 *
 * ## Input and output
 *
 * The base64 module can [decode][io.github.kotlinmania.base64.engine.Engine.decode] and
 * [encode][io.github.kotlinmania.base64.engine.Engine.encode] values in memory, or
 * [DecoderReader][io.github.kotlinmania.base64.read.DecoderReader] and
 * [EncoderWriter][io.github.kotlinmania.base64.write.EncoderWriter] provide streaming decoding and
 * encoding for any readable or writable byte stream.
 *
 * #### Display
 *
 * If you only need a base64 representation for use in a `toString` value, use
 * [Base64Display][io.github.kotlinmania.base64.display.Base64Display].
 *
 * # Panics
 *
 * If length calculations result in overflowing `Int`, an exception will be thrown.
 */

internal const val PAD_BYTE: Byte = '='.code.toByte()
