import SwiftUI

public struct LiveTelemetryMapView: View {
    @ObservedObject var store: TradeeStore

    public init(store: TradeeStore) {
        self.store = store
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Simulated Dar es Salaam GPS Corridor Canvas
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "location.north.line.fill")
                            .foregroundStyle(.white)
                        Text("Dar es Salaam Live GPS Telemetry Corridor")
                            .font(.headline.weight(.heavy))
                            .foregroundStyle(.white)
                        Spacer()
                        Text("Kalman 1Hz")
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(TradeeTheme.accentWarmSoft)
                    }

                    Text("Kariakoo Market Block A (-6.8180, 39.2750) → Buyer Gate Pin (-6.7920, 39.2680)")
                        .font(.caption)
                        .foregroundStyle(TradeeTheme.brandSecondary)

                    HStack(spacing: 12) {
                        Label("Pickup Hub", systemImage: "building.2.fill")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.white)
                        Image(systemName: "arrow.right")
                            .foregroundStyle(TradeeTheme.accentWarmSoft)
                        Label("Live Courier (\(store.driverContract.vehiclePlate))", systemImage: "bicycle")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(TradeeTheme.accentWarmSoft)
                        Image(systemName: "arrow.right")
                            .foregroundStyle(TradeeTheme.accentWarmSoft)
                        Label("Buyer Gate", systemImage: "mappin.and.ellipse")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.white)
                    }
                    .padding(.vertical, 8)
                }
                .padding(16)
                .background(TradeeTheme.heroOceanGradient)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                if store.orders.isEmpty {
                    VStack(spacing: 8) {
                        Image(systemName: "shippingbox.and.arrow.backward.fill")
                            .font(.largeTitle)
                            .foregroundStyle(TradeeTheme.brandPrimary)
                        Text("No Active Orders or Bolt Dispatches Yet")
                            .font(.headline.weight(.bold))
                        Text("Buy a product from the Marketplace or dispatch a Bolt vehicle to receive your Verbal Secret Safety Code.")
                            .font(.caption)
                            .foregroundStyle(TradeeTheme.textSecondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity)
                    .background(TradeeTheme.bgSurfaceCard)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                } else {
                    ForEach(store.orders) { order in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Text(order.productTitle)
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(TradeeTheme.textPrimary)
                                Spacer()
                                Text(TradeeTheme.formatTzs(order.totalAmountTzs))
                                    .font(.subheadline.weight(.heavy))
                                    .foregroundStyle(TradeeTheme.brandPrimaryDark)
                            }

                            Text("Status: \(order.status.label) · Driver: \(order.driverName) (\(order.driverPlate))")
                                .font(.caption)
                                .foregroundStyle(TradeeTheme.textSecondary)

                            // Verbal Secret Safety Code Gate Card
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("VERBAL SECRET SAFETY CODE (SPEAK AT GATE)")
                                        .font(.caption2.weight(.bold))
                                        .foregroundStyle(TradeeTheme.brandPrimaryDark)
                                    Text(order.secretSafetyCode)
                                        .font(.system(.title3, design: .monospaced).weight(.black))
                                        .foregroundStyle(TradeeTheme.textPrimary)
                                    Text("Receipt: \(order.receiptNumber) · \(order.paymentMethod.title)")
                                        .font(.caption2)
                                        .foregroundStyle(TradeeTheme.textSecondary)
                                }
                                Spacer()
                                Image(systemName: "lock.shield.fill")
                                    .font(.title)
                                    .foregroundStyle(TradeeTheme.brandPrimary)
                            }
                            .padding(12)
                            .background(TradeeTheme.brandSecondary)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                            if order.refundAmountTzs > 0 {
                                Text("Refund Issued: \(TradeeTheme.formatTzs(order.refundAmountTzs))")
                                    .font(.caption.weight(.heavy))
                                    .foregroundStyle(TradeeTheme.statusDanger)
                            }

                            if !order.status.isTerminal {
                                HStack(spacing: 8) {
                                    Button {
                                        store.confirmSafeReceipt(orderId: order.id)
                                    } label: {
                                        Text("Confirm Safe Receipt")
                                            .font(.caption.weight(.bold))
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 10)
                                            .foregroundStyle(.white)
                                            .background(TradeeTheme.statusSafeGreen)
                                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    }

                                    Button {
                                        store.claimRuleADamagedOrLostRefund(orderId: order.id)
                                    } label: {
                                        Text("Rule A: Damaged (100% + 1k)")
                                            .font(.caption2.weight(.bold))
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 10)
                                            .foregroundStyle(TradeeTheme.textPrimary)
                                            .background(TradeeTheme.accentWarmSoft)
                                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    }

                                    Button {
                                        store.claimRuleBRobberyOrHarmRefund(orderId: order.id)
                                    } label: {
                                        Text("Rule B: Robbery (100%)")
                                            .font(.caption2.weight(.bold))
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 10)
                                            .foregroundStyle(.white)
                                            .background(TradeeTheme.statusDanger)
                                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    }
                                }
                            }
                        }
                        .padding(16)
                        .background(TradeeTheme.bgSurfaceCard)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .stroke(TradeeTheme.borderHairline, lineWidth: 1)
                        )
                    }
                }
            }
            .padding(16)
        }
        .background(TradeeTheme.bgPrimaryCanvas)
    }
}
