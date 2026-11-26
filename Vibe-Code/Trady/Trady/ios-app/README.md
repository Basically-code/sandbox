# Tradee iOS App (`ios-app`)

Native **SwiftUI (iOS 17+)** codebase for the **Tradee (TradeLink)** Unified Marketplace, Multi-Seller Price Comparison, and On-Demand Bolt-Style Transport & Cargo Dispatch platform.

## Core Capabilities
1. **Marketplace & 8-Category 3D Deck (`MarketplaceView.swift`)**:
   - Browse competing seller listings across all 8 mandatory channels (`SPORTS_AND_OUTDOORS`, `ELECTRONICS`, `FOOD`, `BOOKS`, `CLOTHING`, `SHOES`, `JEWELLERY`, `BEAUTY_AND_PERSONAL_CARE`).
   - High-contrast Daylight-First UI with multi-stop ocean-to-sky and warm amber gradients.
2. **Multi-Seller Price Comparison Engine (`PriceCompareView.swift`)**:
   - Compares identical/equivalent items across Dar es Salaam market hubs using **Sticker Price + Bolt Transport Fee = Landed Total (TSh)**.
   - Automatically highlights **★ Best Landed Value** and **Lowest Sticker Price** with exact savings in TSh.
3. **Bolt-Style On-Demand Transport & Cargo Dispatch (`BoltTransportView.swift`)**:
   - Available to **both Buyers and Sellers** (Buyer Ride/Parcel Pickup & Seller Inventory Restock/Customer Dispatch).
   - Real-time fare & ETA comparison across 4 vehicle tiers:
     - **Tradee Boda Ride** (`<= 15 kg`, `TSh 1,500 + 400/km`)
     - **Tradee Moto Cargo Box** (`<= 35 kg`, `TSh 2,500 + 500/km`)
     - **Tradee Bajaji 3-Wheeler** (`<= 150 kg`, `TSh 4,500 + 800/km`)
     - **Tradee Commercial Truck** (`<= 2,500 kg`, `TSh 6,000 + 1,000/km`)
4. **Seller Portal (`SellerPortalView.swift`)**:
   - Publish items with automatic `> 35 kg` commercial truck escalation and 1-tap Seller Bolt Transport dispatch.
5. **Driver Fleet & 104-Week Lease-to-Own (`DriverFleetView.swift`)**:
   - 06:00 EAT check-out, 21:00 EAT curfew check-in, dual-mode Cargo Box vs Idle Passenger Ride-Hail (`BOLT`, `UBER`, `LINKER`, `STREET_HAIL`), and `TSh 70,000/week × 104 weeks = TSh 7,280,000` ownership transfer ledger.
6. **Live Telemetry Map, Verbal Secret Safety Code & Refund Engine (`LiveTelemetryMapView.swift`)**:
   - Kalman-smoothed Dar es Salaam GPS corridor tracking, verbal secret code (`TRD-XXXX-XXXX`) release gate, Rule A (`100% + TSh 1,000` disturbance) refund, and Rule B (`100%` robbery/harm + route freeze) refund.
