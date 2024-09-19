// swift-tools-version: 5.9
// This is a Skip (https://skip.tools) package,
// containing a Swift Package Manager project
// that will use the Skip build plugin to transpile the
// Swift Package, Sources, and Tests into an
// Android Gradle Project with Kotlin sources and JUnit tests.
import PackageDescription
import Foundation

// Set SKIP_ZERO=1 to build without Skip libraries
let zero = ProcessInfo.processInfo.environment["SKIP_ZERO"] != nil

let package = Package(
    name: "sharedqprotocol",
    defaultLocalization: "en",
    platforms: [.iOS(.v16), .macOS(.v13), .tvOS(.v16), .watchOS(.v9), .macCatalyst(.v16)],
    products: [
        .library(name: "SharedQProtocol", targets: ["SharedQProtocol"]),
    ],
    dependencies: [
        .package(url: "https://github.com/apple/swift-docc", branch: "main")
    ],
    targets: [
        .target(name: "SharedQProtocol", dependencies: ([]), resources: [.process("Resources")]),
    ]
)
