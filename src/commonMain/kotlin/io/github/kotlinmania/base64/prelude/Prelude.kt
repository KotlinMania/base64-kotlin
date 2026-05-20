// port-lint: source prelude.rs
package io.github.kotlinmania.base64.prelude

import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD
import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD_NO_PAD
import io.github.kotlinmania.base64.engine.generalpurpose.URL_SAFE
import io.github.kotlinmania.base64.engine.generalpurpose.URL_SAFE_NO_PAD
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose

/** A standard base64 engine. */
public val BASE64_STANDARD: GeneralPurpose = STANDARD

/** A standard base64 engine that omits padding. */
public val BASE64_STANDARD_NO_PAD: GeneralPurpose = STANDARD_NO_PAD

/** A URL-safe base64 engine. */
public val BASE64_URL_SAFE: GeneralPurpose = URL_SAFE

/** A URL-safe base64 engine that omits padding. */
public val BASE64_URL_SAFE_NO_PAD: GeneralPurpose = URL_SAFE_NO_PAD
