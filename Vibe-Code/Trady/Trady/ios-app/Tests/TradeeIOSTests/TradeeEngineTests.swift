import XCTest
@testable import TradeeIOS

final class TradeeEngineTests: XCTestCase {

    func testCategoriesCoverAllEightBlueprintChannels() {
        XCTAssertEqual(ProductCategory.allCases.count, 8)
        XCTAssertEqual(ProductCategory.allCases[1], .ELECTRONICS)
    }

    func testTransportFeeAppliesMotorcycleVsTruckRates() {
        let motoFee = TradeeCalculator.computeTransportFeeTzs(requiresTruck: false, distanceKm: 4.0)
        let truckFee = TradeeCalculator.computeTransportFeeTzs(requiresTruck: true, distanceKm: 4.0)
        let truckFee10Km = TradeeCalculator.computeTransportFeeTzs(requiresTruck: true, distanceKm: 10.0)
        XCTAssertEqual(motoFee, 4_500.0, accuracy: 0.01)
        XCTAssertEqual(truckFee, 10_000.0, accuracy: 0.01)
        XCTAssertEqual(truckFee10Km, 13_000.0, accuracy: 0.01)
    }

    func testLeaseToOwnTwoYearTotalMatches104WeeksAt70k() {
        let total = TradeeCalculator.weeklyLeaseFeeTzs * Int64(TradeeCalculator.totalLeaseWeeks)
        XCTAssertEqual(total, 7_280_000)
    }

    func testPriceComparisonRanksOffersByLandedTotal() {
        XCTAssertTrue(TradeeCalculator.compareProductOffers(offers: []).isEmpty)

        let offers = [
            Product(id: "s1", providerName: "Mlimani Gaming Store", category: .ELECTRONICS, title: "Pro Wireless Gamepad DualShock", priceTzs: 89_000, pickupLabel: "Mlimani City Retail Plaza"),
            Product(id: "s2", providerName: "Samora Digital Hub", category: .ELECTRONICS, title: "Pro Wireless Gamepad DualShock", priceTzs: 81_000, pickupLabel: "Samora Tech Hub"),
            Product(id: "s3", providerName: "Kariakoo Official Merchant", category: .ELECTRONICS, title: "Pro Wireless Gamepad DualShock", priceTzs: 85_000, pickupLabel: "Kariakoo Market Block A")
        ]

        let ranked = TradeeCalculator.compareProductOffers(offers: offers, baseDistanceKm: 4.0)
        XCTAssertEqual(ranked.count, 3)
        XCTAssertEqual(ranked.first?.product.id, "s2")
        XCTAssertTrue(ranked.first?.isBestLandedValue ?? false)
        XCTAssertTrue(ranked.first?.isLowestStickerPrice ?? false)
        XCTAssertGreaterThan(ranked.first?.savingsVsHighestTzs ?? 0.0, 0.0)

        let groups = TradeeCalculator.buildPriceComparisonGroups(products: offers, baseDistanceKm: 4.0)
        XCTAssertEqual(groups.count, 1)
        XCTAssertEqual(groups.first?.sellerCount, 3)
        XCTAssertEqual(groups.first?.priceSpreadTzs ?? 0.0, 8_000.0, accuracy: 0.01)

        let benchmark = TradeeCalculator.computeCategoryPriceBenchmark(products: offers, category: .ELECTRONICS)
        XCTAssertEqual(benchmark.count, 3)
        XCTAssertEqual(benchmark.lowestTzs, 81_000.0, accuracy: 0.01)
        XCTAssertEqual(benchmark.averageTzs, 85_000.0, accuracy: 0.01)
        XCTAssertEqual(benchmark.highestTzs, 89_000.0, accuracy: 0.01)

        let code = TradeeCalculator.generateSecretSafetyCode()
        XCTAssertTrue(code.contains("—"))
    }

    func testBoltTransportTiersEnforceWeightLimitsAndClampDistance() {
        let lightQuotes = TradeeCalculator.compareAllBoltTiers(distanceKm: 0.0, cargoWeightKg: 5.0)
        XCTAssertEqual(lightQuotes.count, 4)
        let motoBox = lightQuotes.first(where: { $0.tier == .TRADEE_MOTO_BOX })!
        XCTAssertTrue(motoBox.isEligible)
        XCTAssertTrue(motoBox.isRecommended)
        XCTAssertEqual(motoBox.fareTzs, 3_000.0, accuracy: 0.01)

        let mediumQuotes = TradeeCalculator.compareAllBoltTiers(distanceKm: 4.0, cargoWeightKg: 50.0)
        XCTAssertFalse(mediumQuotes.first(where: { $0.tier == .TRADEE_BODA })!.isEligible)
        XCTAssertFalse(mediumQuotes.first(where: { $0.tier == .TRADEE_MOTO_BOX })!.isEligible)
        XCTAssertTrue(mediumQuotes.first(where: { $0.tier == .TRADEE_BAJAJI })!.isRecommended)

        let bulkQuotes = TradeeCalculator.compareAllBoltTiers(distanceKm: 4.0, cargoWeightKg: 400.0)
        let truck = bulkQuotes.first(where: { $0.tier == .TRADEE_TRUCK })!
        XCTAssertTrue(truck.isEligible)
        XCTAssertTrue(truck.isRecommended)
        XCTAssertEqual(truck.fareTzs, 10_000.0, accuracy: 0.01)
    }
}
