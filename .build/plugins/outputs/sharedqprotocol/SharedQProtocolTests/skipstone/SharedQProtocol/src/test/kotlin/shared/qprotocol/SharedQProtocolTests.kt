package shared.qprotocol

import skip.lib.*

import skip.unit.*
import skip.foundation.*
import shared.qprotocol.*

internal val logger: SkipLogger = SkipLogger(subsystem = "SharedQProtocol", category = "Tests")

internal class SharedQProtocolTests: XCTestCase {
    @Test
    internal fun testSharedQProtocol() {
        logger.log("running testSharedQProtocol")
        XCTAssertEqual(1 + 2, 3, "basic test")

        // load the TestData.json file from the Resources folder and decode it into a struct
        val resourceURL: URL = XCTUnwrap(Bundle.module.url(forResource = "TestData", withExtension = "json"))
        val testData = JSONDecoder().decode(TestData::class, from = Data(contentsOf = resourceURL))
        XCTAssertEqual("SharedQProtocol", testData.testModuleName)
    }
}

internal class TestData: Codable, MutableStruct {
    internal var testModuleName: String
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(testModuleName: String) {
        this.testModuleName = testModuleName
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = TestData(testModuleName)

    override fun equals(other: Any?): Boolean {
        if (other !is TestData) return false
        return testModuleName == other.testModuleName
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, testModuleName)
        return result
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        testModuleName("testModuleName");
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(testModuleName, forKey = CodingKeys.testModuleName)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.testModuleName = container.decode(String::class, forKey = CodingKeys.testModuleName)
    }

    companion object: DecodableCompanion<TestData> {
        override fun init(from: Decoder): TestData = TestData(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? {
            return when (rawValue) {
                "testModuleName" -> CodingKeys.testModuleName
                else -> null
            }
        }
    }
}
