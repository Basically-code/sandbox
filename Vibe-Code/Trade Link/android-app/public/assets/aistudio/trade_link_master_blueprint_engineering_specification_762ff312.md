# TRADE LINK (TRADY) — MASTER TECHNICAL SPECIFICATION, UI/UX DESIGN SYSTEM & ENGINEERING BLUEPRINT

**Document Version:** 1.0.0 (Production-Ready Specification)  
**Parent Organization:** Urban Production  
**Application Names:** Trade Link / Trady / Trady Link  
**Target Platforms:** Android (Google Play Store) & iOS (Apple App Store)  
**Primary Market & Currency:** Tanzania (`TZS` / `TSh`)  
**Architectural Paradigm:** Multi-Tenant E-Commerce Marketplace + Dual-Mode Logistics & Fleet Lease-to-Own Network

---

## 1. EXECUTIVE OVERVIEW & PRODUCT GENRE CLASSIFICATION

To eliminate any ambiguity during software development, **Trade Link** is formally classified across four technical genres:
1. **B2C / P2P Multi-Category E-Commerce Marketplace:** Allowing verified sellers ("Providers") to list inventory across 8 core categories with real-time stock, sizing, and dynamic delivery pricing.
2. **Last-Mile On-Demand Courier & Telemetry Network:** Real-time dispatch of motorcycle couriers equipped with detachable rear cargo boxes (Phase 1) and light/heavy commercial trucks (Phase 2).
3. **Dual-Mode Fleet Utilization & Station Management ERP:** A fleet scheduling system enforcing morning station check-outs (`05:00–07:00 hrs`), active delivery/ride-hailing switching (`07:00–18:00 hrs`), and mandatory evening station check-ins (`18:00–21:00 hrs`) with automated curfew enforcement.
4. **FinTech Lease-to-Own Ledger & Escrow Engine:** Managing Phase 1 Cash-on-Delivery (with digital receipts), Phase 2 Firewall-Protected Card Processing, automated buyer disturbance compensation (`+ TSh 1,000`), and a 2-year (104-week) driver lease-to-own remittance tracker (`TSh 70,000/week`).

---

## 2. VISUAL DESIGN SYSTEM & LIGHT-THEME COLOR PALETTE

The visual identity strictly adheres to a **Clean, Daylight-First, High-Contrast Light Theme** ("Normal Light Colors") optimized for outdoor visibility in bright sunlight (crucial for buyers waiting at gates and motorcycle drivers on the road). Dark mode is disabled by default to maintain crisp, approachable, trustworthy daylight aesthetics.

### 2.1 Color Tokens (CSS / Flutter / React Native Hex Specs)

| Token Name | Hex Code | RGB Value | Usage & Component Assignment |
| :--- | :--- | :--- | :--- |
| `--bg-primary-canvas` | `#F8FAFC` | `rgb(248, 250, 252)` | Main app background (Soft Alabaster Daylight) |
| `--bg-surface-card` | `#FFFFFF` | `rgb(255, 255, 255)` | Elevated product cards, 3D carousel cards, modals |
| `--bg-surface-subtle` | `#F1F5F9` | `rgb(241, 245, 249)` | Search bar fill, input fields, inactive category pills |
| `--text-primary` | `#0F172A` | `rgb(15, 23, 42)` | Primary headings, prices, product titles, secret codes |
| `--text-secondary` | `#475569` | `rgb(71, 85, 105)` | Subtitles, seller names, size/quantity labels |
| `--text-muted` | `#94A3B8` | `rgb(148, 163, 184)` | Input placeholders, disabled states, timestamps |
| `--brand-primary` | `#0284C7` | `rgb(2, 132, 199)` | Primary buttons, active icons, "Trady Link" header accent |
| `--brand-secondary` | `#E0F2FE` | `rgb(224, 242, 254)` | "Deals of today" banner fill, active selection highlights |
| `--accent-warm` | `#F59E0B` | `rgb(245, 158, 11)` | Pickaxe/T-Logo stripe accents, ratings, deal badges |
| `--status-safe-green` | `#10B981` | `rgb(16, 185, 129)` | Verified driver badge, secret code match, station check-in |
| `--status-warning` | `#F97316` | `rgb(249, 115, 22)` | Curfew approaching (`18:00–20:45 hrs`), low stock alert |
| `--status-danger` | `#EF4444` | `rgb(239, 68, 68)` | Curfew breach (`>21:00 hrs`), SOS/Robbery trigger, refund claim |
| `--border-hairline` | `#E2E8F0` | `rgb(226, 232, 240)` | `1px` card borders, list dividers, input outlines |

### 2.2 Typography & Scannability Scale
* **Primary UI Font Family:** `Inter` or `Plus Jakarta Sans` (Clean geometric sans-serif).
* **Monospace Font Family (For Secret Codes, Plates & Receipts):** `JetBrains Mono` or `Roboto Mono`.
* **Type Scale:**
  * `Display-Code`: `32px` Bold Monospace (`letter-spacing: 0.15em`) — Used for the Buyer Verbal Safety Code and Vehicle Plate verification.
  * `H1-Header`: `22px` Bold (`line-height: 28px`) — App Bar ("Trady Link") and Screen Titles.
  * `H2-Category`: `18px` ExtraBold (`uppercase`, `letter-spacing: 0.08em`) — Used at the bottom of the 3D Carousel Card (e.g., `ELECTRONIC`).
  * `Body-Regular`: `15px` Medium (`line-height: 22px`) — Product descriptions, terms & conditions.
  * `Caption-Tag`: `12px` SemiBold (`line-height: 16px`) — GPS coordinates, TSh transport fee breakdown.

### 2.3 Brand Logo & App Icon Vector Construction
* **Outer Container:** Rounded square squircle (`border-radius: 22%` of width), background `#FFFFFF` with a subtle `#E2E8F0` inner border and soft drop shadow.
* **The "T" Pickaxe/Crossbar Emblem:**
  * A vertical central pillar tapering slightly toward the bottom (`#0F172A` outline with `#F1F5F9` interior).
  * A curved, asymmetric top crossbar sweeping from a sharp left point (pickaxe tip) to a blunt right end, segmented with bold diagonal stripes (`#0F172A` and `#F59E0B`).

---

## 3. UI/UX PHYSICS, GESTURE MECHANICS & DYNAMIC MOTION ENGINE

Every visual transition in **Trade Link** is governed by deterministic spring physics rather than linear easing, creating a tactile, responsive feel on mobile hardware.

### 3.1 The 3D Layered Category Carousel (Home Screen Core)
As drawn in the technical schematic, the center of the Home Screen houses a **3-Card Layered Perspective Deck** representing the 8 product categories:
1. **Sports & Outdoors**
2. **Electronics** (Default Focused Card on Launch)
3. **Food**
4. **Books**
5. **Clothing**
6. **Shoes**
7. **Jewellery**
8. **Beauty & Personal Care**

#### Mathematical Transform & Spring Physics
Let $x$ be the horizontal drag offset in pixels, and $W$ be the screen width. The normalized progress of a swipe is $p = \text{clamp}\left(\frac{x}{0.65W}, -1, 1\right)$.

* **Center (Active) Card Transform:**
  $$\text{Scale}_{\text{active}} = 1.0 - 0.12|p|$$
  $$\text{TranslateX}_{\text{active}} = x$$
  $$\text{RotateY}_{\text{active}} = -8^\circ \cdot p$$
  $$\text{Elevation (Shadow)} = 16\text{dp} \cdot (1 - |p|) + 4\text{dp}$$

* **Left & Right Peek (Background) Cards Transform:**
  * Positioned symmetrically behind the active card at horizontal offset $\pm 44\text{px}$.
  $$\text{Scale}_{\text{side}} = 0.88 + 0.12|p|$$
  $$\text{Opacity}_{\text{side}} = 0.65 + 0.35|p|$$
  $$\text{Z-Index}_{\text{active}} = 10, \quad \text{Z-Index}_{\text{side}} = 5$$

* **Spring Solver Parameters (React Native Reanimated / Flutter `SpringSimulation`):**
  * `mass`: $1.0$
  * `stiffness`: $240\text{ N/m}$
  * `damping`: $22\text{ Ns/m}$ (Critically damped with slight, crisp settle; zero wobble)
  * `velocityThreshold`: $550\text{ px/s}$ (Flicking faster than $550\text{ px/s}$ advances to the next category regardless of distance dragged).

* **Parallax Icon Floating Inside Active Card:**
  * Each category card displays two floating vector illustrations (e.g., a **Gamepad Icon** on top and a **Smartphone Icon** in the center for `ELECTRONIC`).
  * As the user drags horizontally or tilts the phone (via device gyroscope $\theta_{\text{roll}}$), the inner icons shift at $1.35\times$ the card velocity, creating a 3D depth-box optical illusion.

### 3.2 Floating Live Map Button (`MAP` Widget) Expansion Physics
* **Resting State:** Positioned at the bottom-right corner (`bottom: 24px`, `right: 20px`), shaped as a tactile pill/card featuring a folded-map vector icon and the bold label `MAP`.
* **Active Delivery Pulse:** When an order is in `IN_TRANSIT` state, a soft sky-blue radar ring (`#0284C7` at $25\%$ opacity) expands outward from the `MAP` button every $1.8\text{ seconds}$ using cubic-bezier easing `(0.16, 1, 0.3, 1)`.
* **Shared-Element Morph Transition:** Tapping `MAP` triggers an expanding circular reveal originating from the exact center coordinates $(x_{\text{btn}}, y_{\text{btn}})$ of the button, expanding to full screen in $380\text{ms}$ (`stiffness: 280`, `damping: 26`).

### 3.3 Real-Time Map Vehicle Marker Kinematics (Kalman-Smoothed Dead Reckoning)
Raw GPS updates arrive every $3.0\text{ seconds}$. Jumping the motorcycle icon every 3 seconds looks robotic. Trade Link implements **Linear/Hermite Spline Dead-Reckoning Interpolation**:
* Given previous coordinate $P_0(\text{lat}_0, \text{lng}_0)$ at $t_0$ and new coordinate $P_1(\text{lat}_1, \text{lng}_1)$ at $t_1$, the marker position $P(t)$ at 60 FPS ($16.67\text{ms}$ frame step) is interpolated along the road polyline vector:
  $$P(t) = P_0 + (P_1 - P_0) \cdot \left(\frac{t - t_0}{\Delta t}\right)$$
* **Bearing Rotation Smoothing:** The motorcycle sprite rotates smoothly toward the heading angle $\psi = \text{atan2}(\Delta \text{lng}, \Delta \text{lat})$ using shortest-arc angular interpolation so the bike leans naturally around street corners.

---

## 4. COMPLETE COMPONENT MARKUP & SCREEN-BY-SCREEN HIERARCHY

Below is the exact structural markup blueprint for every screen and user portal.

### 4.1 Screen 1: Mandatory Onboarding & Terms Gate
Per core business rules: *"All users have to agree to the terms and conditions before using the app."*

```
+-------------------------------------------------------------------+
| [Logo: Striped Pickaxe "T" inside Light Squircle]                 |
|                                                                   |
| Welcome to Trade Link (Trady)                                     |
| By Urban Production                                               |
|                                                                   |
| Select Your Role Portal:                                          |
| (o) Buyer (Shop & Track Deliveries)                               |
| ( ) Provider / Seller (Post Products & Manage Sales)              |
| ( ) Fleet Driver (Deliveries, Ride-Hail & 2-Yr Lease Contract)    |
|                                                                   |
| +---------------------------------------------------------------+ |
| | SCROLLABLE TERMS & CONDITIONS BOX (#F1F5F9 Fill)              | |
| | 1. Buyers pay product price + transport fee via app/cash.     | |
| | 2. Damaged/lost goods guarantee: 100% refund + TSh 1,000.     | |
| | 3. Drivers must pronounce Buyer's Secret Code before entry.   | |
| | 4. Fleet curfew: Vehicles returned to stations 18:00-21:00.   | |
| +---------------------------------------------------------------+ |
|                                                                   |
| [x] I have read and agree to all Trade Link Terms & Conditions    |
|                                                                   |
| [        CONTINUE TO TRADY LINK (Disabled until checked)        ] |
+-------------------------------------------------------------------+
```

### 4.2 Screen 2: Buyer Home Screen (Exact Technical Sketch Implementation)
```html
<!-- Conceptual Declarative Markup Structure (React Native / Flutter / Web View) -->
<SafeAreaView theme="light" backgroundColor="#F8FAFC">
  
  <!-- 1. Top Navigation Header -->
  <HeaderBar height="56px" paddingHorizontal="16px" backgroundColor="#FFFFFF" borderBottom="#E2E8F0">
    <LeftGroup>
      <IconButton icon="hamburger-menu" color="#0F172A" onPress="openDrawer()" />
      <BrandTitle font="PlusJakartaSans-Bold" size="20px" color="#0F172A">
        Trady Link
      </BrandTitle>
    </LeftGroup>
    <RightGroup>
      <AuthButton label="Sign In" icon="user-avatar-outline" onPress="navigate('AuthModal')" />
      <CartInboxButton icon="envelope-parcel-box" badgeCount="2" onPress="navigate('OrdersAndCart')" />
    </RightGroup>
  </HeaderBar>

  <!-- 2. Global Search Bar -->
  <SearchContainer padding="12px 16px">
    <SearchPill backgroundColor="#F1F5F9" borderColor="#CBD5E1" borderRadius="999px" height="46px">
      <TextInput placeholder="Search Trade Link" placeholderColor="#64748B" />
      <SearchDivider verticalLine="#CBD5E1" />
      <SearchIcon icon="magnifying-glass" color="#0284C7" />
    </SearchPill>
  </SearchContainer>

  <!-- 3. Promotional Daily Deals Banner -->
  <PromoBannerContainer paddingHorizontal="16px" marginBottom="16px">
    <SkewedParallelogramBanner 
      backgroundColor="#E0F2FE" 
      borderColor="#0284C7" 
      skewAngle="-12deg"
      onPress="navigate('DailyDealsScreen')">
      <BannerText color="#0369A1" weight="SemiBold">
        Deals of today : <Underline>Click here</Underline>
      </BannerText>
    </SkewedParallelogramBanner>
  </PromoBannerContainer>

  <!-- 4. Central 3D Layered Category Carousel (8 Categories) -->
  <CarouselViewport flex="1" alignItems="center" justifyContent="center">
    <BackgroundPeekCard position="left" offset="-44px" scale="0.88" category="SPORTS & OUTDOORS" />
    
    <ActiveCategoryCard 
      width="240px" 
      height="360px" 
      backgroundColor="#FFFFFF" 
      borderRadius="24px" 
      border="1.5px solid #0F172A"
      shadow="0 16px 32px rgba(15, 23, 42, 0.08)"
      onTap="openCategoryCatalog('ELECTRONIC')">
      
      <ParallaxIllustrationLayer depth="1.35">
        <VectorIcon name="gamepad-controller" size="72px" color="#0F172A" />
        <Spacer height="28px" />
        <VectorIcon name="smartphone-rear-camera" size="84px" color="#0F172A" />
      </ParallaxIllustrationLayer>

      <CategoryFooterLabel position="bottom" paddingBottom="20px">
        <Title uppercase="true" size="22px" weight="ExtraBold" letterSpacing="1.5px" color="#0F172A">
          ELECTRONIC
        </Title>
      </CategoryFooterLabel>
    </ActiveCategoryCard>

    <BackgroundPeekCard position="right" offset="+44px" scale="0.88" category="FOOD" />
  </CarouselViewport>

  <!-- 5. Bottom-Right Floating Live Map Button -->
  <FloatingActionZone position="bottom-right" bottom="24px" right="20px">
    <MapQuickButton 
      backgroundColor="#FFFFFF" 
      border="1.5px solid #0F172A" 
      borderRadius="14px" 
      padding="10px 16px"
      shadow="0 8px 20px rgba(2, 132, 199, 0.15)"
      onPress="expandLiveTelemetryMap()">
      <FoldedMapPinVector size="28px" color="#0284C7" />
      <Label weight="Bold" size="15px" color="#0F172A">MAP</Label>
    </MapQuickButton>
  </FloatingActionZone>

</SafeAreaView>
```

---

## 5. USER ACCESS, AUTHENTICATION & ROLE-BASED ENTRY WORKFLOWS

This section defines how every participant enters the ecosystem, authenticates, and adds data.

### 5.1 How Buyers Enter, Browse, Order & Verify Deliveries
1. **Registration & Entry:**
   * Buyers tap `Sign In` on the top header.
   * Authenticate via Phone Number + SMS OTP (`+255 ...`) or Email/Password.
   * Grant Location Permission (`ACCESS_FINE_LOCATION`) so the app can compute accurate motorcycle delivery fees and render the buyer's gate/door pin on the Map.
2. **Purchasing & Fee Calculation:**
   * Buyer taps one of the 8 Category Cards (e.g., `ELECTRONIC`), selects a product, chooses **Quantity** and **Size/Variant**, and views the transparent checkout breakdown:
     $$\text{Total Payable (TSh)} = (\text{Unit Price} \times \text{Quantity}) + \text{Base Motorcycle Fee} + (\text{Distance in km} \times \text{Per-Km Rate})$$
   * **Phase 1 Payment Mode:** Buyer selects **Cash by Hand on Delivery (with Receipt)**.
   * **Phase 2 Payment Mode:** Buyer selects **Credit / Debit Card** (processed inside a PCI-DSS Level 1 / WAF Firewall-isolated tokenized iframe).
3. **Secret Safety Code Generation & Gate Handover:**
   * The moment an order is dispatched, the backend generates a cryptographically random **4-Digit + Phonetic Word Verbal Safety Code** (e.g., `4829 — "SIMBA-7"`).
   * The Buyer's Live Map screen displays:
     * Real-time approaching motorcycle/truck GPS pin.
     * Driver's verified **Full Legal Name**, **High-Resolution Face Photo**, and **Vehicle Registration Plate Number** (e.g., `MC 482 DTR`).
   * **Gate Protocol:** Before opening the gate or door, the buyer asks the driver through the gate/intercom to speak the Safety Code aloud. Once the driver pronounces the exact code and the buyer visually matches the driver's face and license plate, the buyer opens the door, pays cash (if Phase 1), receives the receipt, and taps `[ Confirm Safe Receipt ]`.

### 5.2 How Providers (Sellers) Enter & Add Their Own Products
1. **Provider Onboarding:**
   * Select **Provider / Seller Portal** or switch via the Hamburger Menu (`=`) $\rightarrow$ `"Sell on Trade Link"`.
   * Provide Business/Seller Name, Pickup Location GPS Pin (or Market Stall Number), and Payout Mobile Money / Bank Account.
2. **Step-by-Step "Add Product" Form Specification:**
   * **Category Selector (Mandatory Enum):** Dropdown strictly bound to the 8 categories (`Sports and Outdoors`, `Electronics`, `Food`, `Books`, `Clothing`, `Shoes`, `Jewellery`, `Beauty and Personal Care`).
   * **Product Title & Description:** Text inputs with character counters.
   * **Media Uploader:** Up to 5 product photos (auto-compressed to WebP on light backgrounds).
   * **Required Inventory Attributes (Per Notebook Spec):**
     * `Quantity in Stock` (Integer stepper, e.g., `1` to `999`).
     * `Size / Dimensions / Weight Class` (e.g., `S, M, L, XL`, `42 EU`, or `Weight in kg`).
     * *Auto-Vehicle Classifier:* If item weight $> 35\text{ kg}$ or volume exceeds the motorcycle rear cargo box dimensions ($55\text{cm} \times 45\text{cm} \times 45\text{cm}$), the system automatically flags the product as `TRUCK_REQUIRED` (Phase 2 fleet).
   * **Unit Price (`TSh`):** Seller enters net price; app previews final buyer price.
   * **Daily Deal Toggle:** Optional toggle to submit the item to the `"Deals of today: Click here"` promotional banner.

### 5.3 How Fleet Drivers Enter, Operate Dual-Mode & Manage Contracts
1. **Strict KYC & Biometric Registration (Station Supervisor Terminal):**
   * Drivers cannot self-register anonymously. They are onboarded at a supervised **Allocated Station** (e.g., rented market blocks).
   * **Mandatory Captures:**
     1. Full Legal Name & National ID (NIDA) / Driver License.
     2. Live Liveness-Verified Facial Photograph.
     3. Assigned Vehicle Plate Number & Hidden Hardware GPS IMEI binding.
     4. Digital + Physical signature on the **2-Year (104-Week) Lease-to-Own Contract**.
2. **Daily Shift State Machine (`05:00 hrs` to `21:00 hrs`):**
   * **`05:00–07:00 hrs` (Morning Check-Out):** Driver arrives at the Allocated Station. Supervisor scans Driver QR code. Vehicle state transitions from `STATION_LOCKED` to `ACTIVE_ON_SHIFT`.
   * **Active Mode A — Trade Link Package Delivery:** When a package order arrives, the app alerts the driver. The driver mounts the **detachable rear open cargo box** at the pickup point/station, collects the package from the Provider, navigates to the Buyer, pronounces the **Secret Safety Code**, collects cash (if Phase 1), issues a receipt, and marks delivered.
   * **Active Mode B — Idle-Time Passenger Transport:** When the driver's Trade Link package queue is empty during normal hours, the Driver App switches to `IDLE_PASSENGER_MODE`. The driver detaches the rear cargo box (if at station) and is authorized to transport passengers via street hailing or third-party ride-hailing apps (**Bolt, Uber, Linker**, etc.) while keeping the Trade Link background GPS active.
   * **`18:00–21:00 hrs` (Mandatory Evening Check-In):** Driver returns the motorcycle or truck to the supervised Allocated Station. Supervisor inspects the vehicle and scans it in.
   * **Post-`21:00 hrs` (Automated Curfew & Theft Protocol):** If vehicle status is not `CHECKED_IN_AT_STATION` at `21:00:01 hrs`, the backend automatically triggers `THEFT_ESCALATION_PROTOCOL`: locks the driver account, polls the **Secret Hidden Vehicle GPS Tracker** at 1-second intervals, alerts Station Supervisors, and generates a police recovery dossier.

---

## 6. FULL-STACK SYSTEM ARCHITECTURE & SECURITY FIREWALL

### 6.1 Recommended Technology Stack
* **Mobile Client (Single Codebase for Google Play & Apple App Store):**
  * **Framework:** Flutter (Dart) or React Native (TypeScript) with New Architecture (Fabric + TurboModules).
  * **Animation & Physics:** Flutter `AnimationController` / `SpringSimulation` or React Native `Reanimated 3` + `React Native Skia`.
  * **Maps & Telemetry:** Google Maps SDK + Socket.IO / MQTT for bidirectional sub-second GPS streaming.
* **Backend Core & API Gateway:**
  * **Runtime:** Node.js (NestJS with TypeScript) or Go (Golang) microservices.
  * **Primary Database:** PostgreSQL 16 with **PostGIS** extension (for geospatial radius queries, nearest-driver dispatch, and station geofencing).
  * **Real-Time Telemetry & Ephemeral State:** Redis Cluster (Geo-hashes for active drivers, WebSocket pub/sub, rate limiting).
* **Enterprise Firewall & Privacy Architecture (Per Notebook Spec):**
  * *"There has to be maximum protection by enabling large firewalls in the app which will block all reveal of privacy."*
  * **Edge Layer:** Cloudflare Enterprise WAF (Web Application Firewall) + DDoS mitigation + TLS 1.3 strict certificate pinning inside the mobile app (prevents Man-in-the-Middle packet sniffing).
  * **Network Isolation:** AWS VPC / GCP Private Service Connect where the PostgreSQL database and Payment Vault sit in private subnets with zero public IP addresses.
  * **Card Tokenization:** Zero raw credit/debit card PANs ever touch Trade Link servers; cards are tokenized via PCI-DSS Level 1 vaulting with AES-256-GCM encryption at rest.
  * **Phone Number Masking:** Buyers and Drivers communicate or verify without exposing personal phone numbers after delivery completion.

---

## 7. RELATIONAL DATABASE SCHEMA (POSTGRESQL + POSTGIS DDL)

An engineer or AI can execute this SQL schema directly to initialize the production database:

```sql
-- Enable PostGIS for real-time GPS tracking and station geofencing
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- 1. ENUMERATIONS
CREATE TYPE user_role AS ENUM ('BUYER', 'PROVIDER', 'DRIVER', 'STATION_SUPERVISOR', 'ADMIN');
CREATE TYPE product_category AS ENUM (
    'SPORTS_AND_OUTDOORS',
    'ELECTRONICS',
    'FOOD',
    'BOOKS',
    'CLOTHING',
    'SHOES',
    'JEWELLERY',
    'BEAUTY_AND_PERSONAL_CARE'
);
CREATE TYPE vehicle_type AS ENUM ('MOTORCYCLE', 'SMALL_TRUCK', 'LARGE_TRUCK');
CREATE TYPE vehicle_status AS ENUM (
    'STATION_CHECKED_IN',
    'ACTIVE_PACKAGE_DELIVERY',
    'ACTIVE_PASSENGER_IDLE_MODE',
    'CURFEW_BREACH_STOLEN_ALERT',
    'MAINTENANCE'
);
CREATE TYPE order_status AS ENUM (
    'PENDING_DISPATCH',
    'DRIVER_ASSIGNED',
    'BOX_ATTACHED_PICKED_UP',
    'IN_TRANSIT',
    'ARRIVED_AWAITING_SECRET_CODE',
    'DELIVERED_COMPLETED',
    'DAMAGED_OR_LOST_REFUNDED',
    'ROBBERY_OR_HARM_REFUNDED'
);
CREATE TYPE payment_method AS ENUM ('CASH_BY_HAND_WITH_RECEIPT', 'FIREWALL_CARD_PAYMENT');

-- 2. USERS TABLE (All Portals)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name VARCHAR(120) NOT NULL,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE,
    password_hash TEXT NOT NULL,
    role user_role NOT NULL DEFAULT 'BUYER',
    face_photo_url TEXT, -- Mandatory for DRIVER
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    terms_accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. SUPERVISED ALLOCATED STATIONS (e.g., Rented Market Blocks)
CREATE TABLE allocated_stations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_name VARCHAR(120) NOT NULL, -- e.g., "Kariakoo Market Block A"
    supervisor_id UUID REFERENCES users(id),
    geofence_polygon GEOMETRY(Polygon, 4326) NOT NULL,
    center_point GEOMETRY(Point, 4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. FLEET VEHICLES & DUAL GPS TRACKERS
CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plate_number VARCHAR(25) UNIQUE NOT NULL,
    type vehicle_type NOT NULL DEFAULT 'MOTORCYCLE',
    has_detachable_open_box BOOLEAN NOT NULL DEFAULT TRUE,
    is_box_currently_attached BOOLEAN NOT NULL DEFAULT FALSE,
    secret_hardware_gps_imei VARCHAR(64) UNIQUE NOT NULL, -- Covert anti-theft tracker
    assigned_station_id UUID REFERENCES allocated_stations(id) NOT NULL,
    current_status vehicle_status NOT NULL DEFAULT 'STATION_CHECKED_IN',
    last_visible_phone_gps GEOMETRY(Point, 4326),
    last_secret_hardware_gps GEOMETRY(Point, 4326),
    last_telemetry_at TIMESTAMPTZ
);

-- 5. DRIVER PROFILES & 2-YEAR LEASE-TO-OWN CONTRACTS
CREATE TABLE driver_contracts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id UUID UNIQUE REFERENCES users(id) NOT NULL,
    vehicle_id UUID UNIQUE REFERENCES vehicles(id) NOT NULL,
    contract_start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    contract_end_date DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '2 years'),
    weekly_fee_tzs NUMERIC(12, 2) NOT NULL DEFAULT 70000.00, -- TSh 70,000 / week
    total_weeks_required INT NOT NULL DEFAULT 104,
    weeks_paid_count INT NOT NULL DEFAULT 0,
    total_paid_tzs NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    ownership_transferred BOOLEAN NOT NULL DEFAULT FALSE,
    contract_signed_pdf_url TEXT NOT NULL
);

-- 6. DAILY STATION CHECK-OUT (05:00-07:00) & CHECK-IN (18:00-21:00) LOGS
CREATE TABLE station_shift_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id UUID REFERENCES users(id) NOT NULL,
    vehicle_id UUID REFERENCES vehicles(id) NOT NULL,
    station_id UUID REFERENCES allocated_stations(id) NOT NULL,
    shift_date DATE NOT NULL DEFAULT CURRENT_DATE,
    checked_out_at TIMESTAMPTZ, -- Valid window: 05:00 - 07:00 hrs
    checked_in_at TIMESTAMPTZ,  -- Valid window: 18:00 - 21:00 hrs
    curfew_violated BOOLEAN NOT NULL DEFAULT FALSE,
    supervisor_notes TEXT
);

-- 7. MARKETPLACE PRODUCTS (8 Categories)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id UUID REFERENCES users(id) NOT NULL,
    category product_category NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    price_tzs NUMERIC(12, 2) NOT NULL,
    quantity_available INT NOT NULL CHECK (quantity_available >= 0),
    size_specification VARCHAR(80) NOT NULL, -- Required per spec
    requires_truck BOOLEAN NOT NULL DEFAULT FALSE,
    is_daily_deal BOOLEAN NOT NULL DEFAULT FALSE,
    image_urls TEXT[] NOT NULL,
    pickup_location GEOMETRY(Point, 4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. ORDERS, VERBAL SECRET CODES & REFUND ENGINE
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id UUID REFERENCES users(id) NOT NULL,
    provider_id UUID REFERENCES users(id) NOT NULL,
    driver_id UUID REFERENCES users(id),
    vehicle_id UUID REFERENCES vehicles(id),
    product_id UUID REFERENCES products(id) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    selected_size VARCHAR(50) NOT NULL,
    item_subtotal_tzs NUMERIC(12, 2) NOT NULL,
    transport_fee_tzs NUMERIC(12, 2) NOT NULL,
    total_amount_tzs NUMERIC(12, 2) NOT NULL,
    payment_type payment_method NOT NULL DEFAULT 'CASH_BY_HAND_WITH_RECEIPT',
    buyer_secret_safety_code VARCHAR(32) NOT NULL, -- Driver must pronounce at gate
    secret_code_verified_at TIMESTAMPTZ,
    receipt_number VARCHAR(40) UNIQUE NOT NULL,
    delivery_location GEOMETRY(Point, 4326) NOT NULL,
    status order_status NOT NULL DEFAULT 'PENDING_DISPATCH',
    refund_amount_tzs NUMERIC(12, 2) DEFAULT 0.00,
    disturbance_compensation_tzs NUMERIC(12, 2) DEFAULT 0.00, -- TSh 1,000 when damaged/lost
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 8. CORE BUSINESS LOGIC ALGORITHMS & AUTOMATED ENGINES

### 8.1 Automated Refund & Disturbance Compensation Engine
Per the handwritten specification:
* **Rule A (Damaged or Lost Goods):** If an order fails to reach the buyer intact, the buyer receives $100\%$ of `total_amount_tzs` back **plus** a fixed disturbance compensation of $\text{TSh } 1,000$:
  $$\text{Payout}_{\text{damaged/lost}} = \text{Price}_{\text{item}} + \text{Fee}_{\text{transport}} + 1000\text{ TSh}$$
* **Rule B (Customer Harmed, Product Stolen, or Purchase Cash Robbed):** If a customer reports robbery or harm during the transaction, the system flags the incident, freezes the driver route log for investigation, and issues a $100\%$ refund:
  $$\text{Payout}_{\text{robbery/harm}} = \text{Price}_{\text{item}} + \text{Fee}_{\text{transport}}$$

### 8.2 Curfew Enforcement Cron Job (`21:00:00 EAT` Daily Trigger)
```typescript
// Scheduled Worker running every minute between 18:00 and 21:05 EAT (Africa/Dar_es_Salaam)
async function enforceStationCurfew(): Promise<void> {
  const now = new Date();
  const currentHour = now.getHours();

  // 1. Send warning push notifications at 20:30 hrs
  if (currentHour === 20 && now.getMinutes() === 30) {
    await notifyUnreturnedDrivers("URGENT: Return vehicle to Allocated Station before 21:00 hrs.");
  }

  // 2. Hard Curfew Enforcement at 21:00 hrs
  if (currentHour >= 21) {
    const overdueVehicles = await db.query(`
      SELECT v.id, v.plate_number, v.secret_hardware_gps_imei, d.driver_id
      FROM vehicles v
      JOIN driver_contracts d ON d.vehicle_id = v.id
      WHERE v.current_status != 'STATION_CHECKED_IN'
    `);

    for (const vehicle of overdueVehicles) {
      await db.transaction(async (trx) => {
        // Flag vehicle as stolen / curfew breached
        await trx.query(
          `UPDATE vehicles SET current_status = 'CURFEW_BREACH_STOLEN_ALERT' WHERE id = $1`,
          [vehicle.id]
        );
        // Log violation against driver's 2-year contract
        await trx.query(
          `UPDATE station_shift_logs SET curfew_violated = TRUE WHERE vehicle_id = $1 AND shift_date = CURRENT_DATE`,
          [vehicle.id]
        );
        // Activate high-frequency 1Hz polling on the Secret Hardware GPS Tracker
        await gpsHardwareService.enableHighFrequencyRecoveryMode(vehicle.secret_hardware_gps_imei);
        // Alert Station Supervisor & Legal Dispatch
        await alertSecurityTeam(vehicle);
      });
    }
  }
}
```

### 8.3 2-Year (104-Week) Lease-to-Own Ownership Transfer Logic
Every Sunday at `23:59 EAT`, the Weekly Remittance Ledger verifies driver payments of **TSh 70,000**:
* Upon recording payment `#104` ($\text{Total Remitted} = 104 \times 70,000 = \text{TSh } 7,280,000$), the smart contract / database trigger sets `ownership_transferred = TRUE`, generates a **Certificate of Permanent Motorcycle Ownership Transfer** from Urban Production to the driver, and converts the driver from a Lease-Contract Driver to an Independent Owner-Partner on the Trade Link network.

---

## 9. STEP-BY-STEP BUILD ROADMAP (FROM EMPTY REPO TO PRODUCTION)

An engineer or AI agent building **Trade Link** from scratch should execute the following 6 sequential phases:

### Phase 1: Repository & Monorepo Scaffolding
1. Initialize a monorepo (`tradelink-monorepo/`) containing:
   * `apps/mobile-app` (Flutter or React Native TypeScript for Buyer, Seller & Driver views).
   * `apps/station-supervisor-web` (React/Next.js dashboard for Allocated Station managers to scan vehicles in/out between `05:00–07:00` and `18:00–21:00`).
   * `services/core-api` (NestJS / Go backend with PostgreSQL + PostGIS and Redis).
2. Configure design tokens (`theme/lightColors.ts`) strictly enforcing the Light Color Palette (`#F8FAFC` background, `#FFFFFF` cards, `#0F172A` ink, `#0284C7` sky blue).

### Phase 2: Database, Auth & Terms Gate
1. Run the PostgreSQL + PostGIS migrations defined in **Section 7**.
2. Build the **Mandatory Terms & Conditions Gate** screen (`terms_accepted = TRUE`) and Role Switcher.
3. Build Seller KYC and Driver Biometric/Plate Registration endpoints.

### Phase 3: The 3D Category Carousel & Marketplace Engine
1. Implement the Home Screen exactly matching the schematic:
   * Top Bar (`=`, `Trady Link`, `Sign In`, Cart/Inbox).
   * Rounded `Search Trade Link` bar.
   * Skewed `"Deals of today : Click here"` banner.
   * 3D Layered 8-Category Carousel with spring physics (`mass: 1, stiffness: 240, damping: 22`) and parallax category icons.
2. Build the Seller **"Add Product"** flow supporting all 8 categories, quantity, size, and automatic motorcycle-vs-truck classification.

### Phase 4: Dual-GPS Telemetry & Live Map Overlay
1. Build the floating bottom-right `MAP` button with shared-element expansion transition.
2. Integrate WebSocket streaming for the driver's visible smartphone GPS and IoT webhook ingestion for the **Secret Hidden Vehicle GPS Tracker**.
3. Apply Kalman/Hermite spline interpolation so motorcycle markers glide smoothly at 60 FPS on the buyer's map.

### Phase 5: Handover Security, Secret Code & Payment/Refund Engine
1. Implement the **Buyer Secret Safety Code** generator and the Gate Verification UI displaying the Driver's Face, Name, and Plate Number.
2. Build Phase 1 Cash-by-Hand digital receipt generation and Phase 2 WAF-protected Card Checkout.
3. Implement the one-click **Claim & Refund Engine** (`100% + TSh 1,000` for damaged/lost goods; `100%` refund for theft/robbery/harm).

### Phase 6: Fleet Scheduling, Ride-Hail Idle Mode & Lease Ledger
1. Build the Driver App's **Detachable Cargo Box Reminder** and **Idle-Time Passenger Mode** switcher (allowing drivers without active packages to accept passenger trips via street hail, Bolt, Uber, or Linker).
2. Deploy the `05:00–07:00` Check-Out, `18:00–21:00` Check-In QR scanner, the `21:00 hrs` Curfew Theft Escalation Cron Job, and the **TSh 70,000/week** 2-Year Lease-to-Own tracker.