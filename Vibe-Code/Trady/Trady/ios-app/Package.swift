// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "TradeeIOS",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "TradeeIOS",
            targets: ["TradeeIOS"]
        )
    ],
    targets: [
        .target(
            name: "TradeeIOS",
            path: "Sources/TradeeIOS",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "TradeeIOSTests",
            dependencies: ["TradeeIOS"],
            path: "Tests/TradeeIOSTests"
        )
    ]
)
