import SwiftUI

public struct DriverFleetView: View {
    @ObservedObject var store: TradeeStore

    public init(store: TradeeStore) {
        self.store = store
    }

    private var contract: DriverContract { store.driverContract }
    private var totalTargetTzs: Double { Double(contract.weeklyFeeTzs) * Double(contract.totalWeeksRequired) }
    private var progressFraction: Double {
        min(1.0, Double(contract.weeksPaidCount) / Double(contract.totalWeeksRequired))
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Shift & Curfew Card
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(contract.driverName)
                                .font(.title3.weight(.heavy))
                                .foregroundStyle(TradeeTheme.textPrimary)
                            Text("Plate: \(contract.vehiclePlate) · \(contract.stationName)")
                                .font(.caption)
                                .foregroundStyle(TradeeTheme.textSecondary)
                        }
                        Spacer()
                        Text(contract.vehicleStatus.label)
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(contract.curfewViolated ? TradeeTheme.statusDanger : TradeeTheme.statusSafeGreen)
                            .clipShape(Capsule())
                    }

                    Text("Secret Anti-Theft GPS IMEI: \(contract.secretGpsImei) · 06:00 Check-Out / 21:00 Mandatory Check-In")
                        .font(.caption2)
                        .foregroundStyle(TradeeTheme.textSecondary)

                    HStack(spacing: 10) {
                        Button {
                            store.driverContract.vehicleStatus = .ACTIVE_PACKAGE_DELIVERY
                            store.driverContract.cargoBoxAttached = true
                            store.driverContract.curfewViolated = false
                            store.bannerMessage = "06:00 Morning Check-Out Confirmed · Cargo Box Locked"
                        } label: {
                            Text("06:00 Check-Out")
                                .font(.caption.weight(.bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .foregroundStyle(.white)
                                .background(TradeeTheme.brandPrimary)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }

                        Button {
                            store.driverContract.vehicleStatus = .STATION_CHECKED_IN
                            store.driverContract.cargoBoxAttached = false
                            store.driverContract.curfewViolated = false
                            store.bannerMessage = "21:00 Station Check-In Complete · Curfew Cleared"
                        } label: {
                            Text("21:00 Check-In")
                                .font(.caption.weight(.bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .foregroundStyle(.white)
                                .background(TradeeTheme.statusSafeGreenDark)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }

                        Button {
                            store.driverContract.vehicleStatus = .CURFEW_BREACH_STOLEN_ALERT
                            store.driverContract.curfewViolated = true
                            store.bannerMessage = "21:00 CURFEW BREACH: Secret GPS (\(contract.secretGpsImei)) polling at 1Hz!"
                        } label: {
                            Text("Test Curfew")
                                .font(.caption.weight(.bold))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 10)
                                .foregroundStyle(.white)
                                .background(TradeeTheme.statusDanger)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
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

                // Dual-Mode Cargo Box vs Idle Passenger Ride-Hail
                VStack(alignment: .leading, spacing: 10) {
                    Text("Dual-Mode Operation: Cargo Box vs Passenger Ride-Hail")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(TradeeTheme.textPrimary)

                    Text("When no Tradee package orders are active, detach the rear cargo box to carry passengers via Bolt, Uber, Linker, or street hail while keeping background GPS active.")
                        .font(.caption)
                        .foregroundStyle(TradeeTheme.textSecondary)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(RideHailPartner.allCases) { partner in
                                Button {
                                    store.driverContract.rideHailPartner = partner
                                    store.driverContract.cargoBoxAttached = (partner == .NONE)
                                    store.driverContract.vehicleStatus = (partner == .NONE)
                                        ? .ACTIVE_PACKAGE_DELIVERY
                                        : .ACTIVE_PASSENGER_IDLE_MODE
                                    store.bannerMessage = "Switched to \(partner.label)"
                                } label: {
                                    Text(partner.label)
                                        .font(.caption.weight(.bold))
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .foregroundStyle(contract.rideHailPartner == partner ? .white : TradeeTheme.textPrimary)
                                        .background(contract.rideHailPartner == partner ? TradeeTheme.brandPrimary : TradeeTheme.bgSurfaceSubtle)
                                        .clipShape(Capsule())
                                }
                                .buttonStyle(.plain)
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

                // 2-Year (104-Week) Lease-to-Own Ledger
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("104-Week Motorcycle Lease-to-Own")
                            .font(.headline.weight(.bold))
                            .foregroundStyle(TradeeTheme.textPrimary)
                        Spacer()
                        Text("\(contract.weeksPaidCount) / 104 Weeks")
                            .font(.subheadline.weight(.heavy))
                            .foregroundStyle(TradeeTheme.brandPrimaryDark)
                    }

                    ProgressView(value: progressFraction)
                        .tint(TradeeTheme.statusSafeGreen)

                    HStack {
                        Text("Paid: \(TradeeTheme.formatTzs(contract.totalPaidTzs))")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(TradeeTheme.statusSafeGreenDark)
                        Spacer()
                        Text("2-Year Total: \(TradeeTheme.formatTzs(totalTargetTzs)) (TSh 70,000/wk)")
                            .font(.caption)
                            .foregroundStyle(TradeeTheme.textSecondary)
                    }

                    HStack(spacing: 10) {
                        Button {
                            store.recordWeeklyLeasePayment(weeks: 1)
                        } label: {
                            Text("Remit +1 Week (TSh 70,000)")
                                .font(.caption.weight(.bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .foregroundStyle(.white)
                                .background(TradeeTheme.brandPrimary)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }

                        Button {
                            store.recordWeeklyLeasePayment(weeks: 4)
                        } label: {
                            Text("Remit +4 Weeks")
                                .font(.caption.weight(.bold))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .foregroundStyle(TradeeTheme.brandPrimaryDark)
                                .background(TradeeTheme.brandSecondary)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
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
            .padding(16)
        }
        .background(TradeeTheme.bgPrimaryCanvas)
    }
}
