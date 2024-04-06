package shared.qprotocol

import skip.lib.*


internal val skip.foundation.Bundle.Companion.module: skip.foundation.Bundle
    get() = _moduleBundle
private val _moduleBundle : skip.foundation.Bundle by lazy { skip.foundation.Bundle(_ModuleBundleLocator::class) }
internal class _ModuleBundleLocator {}
