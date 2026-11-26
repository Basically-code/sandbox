import SwiftUI

public enum TradeeTab: String, CaseIterable, Identifiable {
    case marketplace = "Marketplace"
    case compare = "Compare"
    case boltTransport = "Bolt Transport"
    case seller = "Seller"
    case driver = "Fleet"
    case telemetry = "Live Map"

    public var id: String { rawValue }
}

public struct TradeeRootView: View {
    @StateObject private var store = TradeeStore()
    @State private var selectedTab: TradeeTab = .marketplace
    @State private var focusedCompareProduct: Product? = nil

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Top Daylight Header with Redesigned Tradee Logo
            HStack(spacing: 12) {
                TradeeLogoBadgeView(size: 40)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Tradee")
                        .font(.title3.weight(.heavy))
                        .foregroundStyle(TradeeTheme.textPrimary)
                    Text("Marketplace · Price Compare · Bolt Transport")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(TradeeTheme.brandPrimary)
                }
                Spacer()
                Button {
                    selectedTab = .telemetry
                } label: {
                    HStack(spacing: 5) {
                        Image(systemName: "location.fill")
                        Text("MAP (\(store.orders.filter { !$0.status.isTerminal }.count))")
                            .font(.caption.weight(.heavy))
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .foregroundStyle(.white)
                    .background(TradeeTheme.brandPrimaryGradient)
                    .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(TradeeTheme.bgSurfaceCard)

            if let banner = store.bannerMessage {
                HStack {
                    Text(banner)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.white)
                    Spacer()
                    Button {
                        store.bannerMessage = nil
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.white)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(TradeeTheme.brandPrimaryDark)
            }

            Divider()

            TabView(selection: $selectedTab) {
                MarketplaceView(
                    store: store,
                    onOpenPriceCompare: { product in
                        focusedCompareProduct = product
                        selectedTab = .compare
                    },
                    onOpenBoltTransport: {
                        selectedTab = .boltTransport
                    },
                    onOpenTelemetry: {
                        selectedTab = .telemetry
                    }
                )
                .tabItem {
                    Label("Market", systemImage: "bag.fill")
                }
                .tag(TradeeTab.marketplace)

                PriceCompareView(
                    store: store,
                    focusedProduct: focusedCompareProduct,
                    onOrderPlaced: {
                        selectedTab = .telemetry
                    }
                )
                .tabItem {
                    Label("Compare", systemImage: "arrow.left.arrow.right.circle.fill")
                }
                .tag(TradeeTab.compare)

                BoltTransportView(
                    store: store,
                    onDispatched: {
                        selectedTab = .telemetry
                    }
                )
                .tabItem {
                    Label("Bolt Ride", systemImage: "bicycle")
                }
                .tag(TradeeTab.boltTransport)

                SellerPortalView(
                    store: store,
                    onOpenBoltDispatch: {
                        selectedTab = .boltTransport
                    }
                )
                .tabItem {
                    Label("Seller", systemImage: "storefront.fill")
                }
                .tag(TradeeTab.seller)

                DriverFleetView(store: store)
                    .tabItem {
                        Label("Fleet", systemImage: "scooter")
                    }
                    .tag(TradeeTab.driver)

                LiveTelemetryMapView(store: store)
                    .tabItem {
                        Label("Live Map", systemImage: "map.fill")
                    }
                    .tag(TradeeTab.telemetry)
            }
            .tint(TradeeTheme.brandPrimary)
        }
    }
}
