/**
 * Tradee (TradeLink) — Interactive Web Application Controller
 * Connects Buyers, Sellers, Price Comparison & Bolt-Style Transport Dispatch
 */
(function () {
  'use strict';

  const {
    CATEGORIES,
    MARKET_HUBS,
    TRANSPORT_TIERS,
    INITIAL_PRODUCTS,
    DEFAULT_DRIVER_CONTRACT,
  } = window.TradeeData;

  const Engine = window.TradeeEngine;

  const state = {
    activePortal: 'MARKETPLACE', // MARKETPLACE | COMPARE | BOLT_TRANSPORT | SELLER | DRIVER | TELEMETRY
    userRole: 'BUYER',
    userName: 'Amina Juma',
    userPhone: '+255 712 000 482',
    termsAccepted: true,
    searchQuery: '',
    selectedCategoryKey: null,
    dealsOnly: false,
    favoritesOnly: false,
    sortBy: 'NEWEST_FIRST',
    carouselIndex: 1, // Default: ELECTRONICS
    showCarouselDeck: true,
    buyerDistanceKm: 4.0,
    focusedCompareGroupKey: null,

    // Bolt Transport Booking State (Buyer & Seller)
    boltRoleMode: 'BUYER', // BUYER | SELLER
    boltPickupHub: MARKET_HUBS[0].name,
    boltDropoffLabel: 'Mikocheni B Gate #14, Dar es Salaam',
    boltDistanceKm: 4.5,
    boltWeightKg: 8.0,
    boltSelectedTierKey: 'TRADEE_MOTO_BOX',
    boltPaymentMethod: 'CASH_BY_HAND_WITH_RECEIPT',
    boltCargoNote: 'Fragile electronics & household parcels',

    // Data Stores
    products: INITIAL_PRODUCTS.slice(),
    favoriteIds: new Set(['prod_elec_gamepad_2', 'prod_elec_phone_2']),
    cartItems: [],
    orders: [
      {
        id: 'ord_demo_101',
        orderType: 'MARKETPLACE_ORDER',
        productId: 'prod_elec_gamepad_2',
        productTitle: 'Pro Wireless Gamepad DualShock',
        category: 'ELECTRONICS',
        providerName: 'Samora Digital Hub',
        pickupLabel: 'Samora Avenue Tech Hub',
        dropoffLabel: 'Mikocheni B Gate #14',
        selectedSize: '16 × 11 cm + Clip',
        quantity: 1,
        unitPriceTzs: 81000,
        itemSubtotalTzs: 81000,
        transportFeeTzs: 4100,
        totalAmountTzs: 85100,
        paymentMethod: 'CASH_BY_HAND_WITH_RECEIPT',
        secretSafetyCode: '4829 — "SIMBA-7"',
        receiptNumber: 'TRD-20261113-48291',
        driverName: 'Juma Hassan',
        driverPlate: 'MC 482 DTR',
        requiresTruck: false,
        transportTierName: 'Tradee Moto Cargo Box',
        cargoBoxAttached: true,
        status: 'IN_TRANSIT',
        refundAmountTzs: 0,
        disturbanceCompTzs: 0,
      },
    ],
    driverContract: Object.assign({}, DEFAULT_DRIVER_CONTRACT),
    showSecretHardwareGps: false,
    toastMessage: null,
    toastTimer: null,
  };

  function showToast(msg) {
    state.toastMessage = msg;
    const el = document.getElementById('tradee-toast');
    if (el) {
      el.textContent = msg;
      el.style.display = 'block';
    }
    if (state.toastTimer) clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => {
      if (el) el.style.display = 'none';
    }, 3600);
  }

  function escapeHtml(str) {
    return String(str == null ? '' : str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function getCategoryInfo(key) {
    return CATEGORIES.find((c) => c.key === key) || CATEGORIES[1];
  }

  function switchPortal(portalName, focusGroupKey) {
    state.activePortal = portalName;
    if (focusGroupKey !== undefined) {
      state.focusedCompareGroupKey = focusGroupKey;
    }
    renderAll();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function addToCart(productId) {
    const prod = state.products.find((p) => p.id === productId);
    if (!prod) return;
    const existing = state.cartItems.find((c) => c.productId === prod.id);
    if (existing) {
      existing.quantity = Math.min(prod.quantityAvailable, existing.quantity + 1);
    } else {
      state.cartItems.push({
        productId: prod.id,
        title: prod.title,
        category: prod.category,
        providerName: prod.providerName,
        pickupLabel: prod.pickupLabel,
        priceTzs: prod.priceTzs,
        quantity: 1,
        sizeSpec: prod.sizeSpec,
        weightKg: prod.weightKg,
        requiresTruck: prod.requiresTruck,
      });
    }
    showToast(`Added "${prod.title}" (${prod.providerName}) to cart`);
    renderAll();
  }

  function buyProductNow(productId) {
    const prod = state.products.find((p) => p.id === productId);
    if (!prod) return;
    const distanceKm = Engine.estimateHubDistanceKm(prod.pickupLabel, state.buyerDistanceKm, prod.hubDistanceKm);
    const transportFee = Engine.computeTransportFeeTzs(prod.requiresTruck, distanceKm);
    const total = prod.priceTzs + transportFee;
    const code = Engine.generateSecretSafetyCode();
    const receipt = Engine.generateReceiptNumber();

    const newOrder = {
      id: 'ord_' + Date.now(),
      orderType: 'MARKETPLACE_ORDER',
      productId: prod.id,
      productTitle: prod.title,
      category: prod.category,
      providerName: prod.providerName,
      pickupLabel: prod.pickupLabel,
      dropoffLabel: 'Buyer Verified Gate Pin',
      selectedSize: prod.sizeSpec,
      quantity: 1,
      unitPriceTzs: prod.priceTzs,
      itemSubtotalTzs: prod.priceTzs,
      transportFeeTzs: transportFee,
      totalAmountTzs: total,
      paymentMethod: 'CASH_BY_HAND_WITH_RECEIPT',
      secretSafetyCode: code,
      receiptNumber: receipt,
      driverName: 'Juma Hassan',
      driverPlate: prod.requiresTruck ? 'T 894 DTR' : 'MC 482 DTR',
      requiresTruck: prod.requiresTruck,
      transportTierName: prod.requiresTruck ? 'Tradee Canter Commercial Truck' : 'Tradee Moto Cargo Box',
      cargoBoxAttached: true,
      status: 'IN_TRANSIT',
      refundAmountTzs: 0,
      disturbanceCompTzs: 0,
    };

    state.orders.unshift(newOrder);
    showToast(`Order placed! Safety Code: ${code}`);
    switchPortal('TELEMETRY');
  }

  function checkoutCart() {
    if (!state.cartItems.length) {
      showToast('Your cart is empty. Add items from the Marketplace first.');
      return;
    }
    const totals = Engine.calculateCartTotals(state.cartItems, state.buyerDistanceKm);
    const anyTruck = state.cartItems.some((c) => Boolean(c.requiresTruck));
    const code = Engine.generateSecretSafetyCode();
    const receipt = Engine.generateReceiptNumber();
    const summaryTitle =
      state.cartItems.length === 1
        ? `${state.cartItems[0].title} (×${state.cartItems[0].quantity})`
        : `${state.cartItems[0].title} + ${state.cartItems.length - 1} more item(s)`;

    const cartOrder = {
      id: 'ord_cart_' + Date.now(),
      orderType: 'MARKETPLACE_ORDER',
      productId: state.cartItems[0].productId,
      productTitle: summaryTitle,
      category: state.cartItems[0].category,
      providerName: state.cartItems[0].providerName,
      pickupLabel: state.cartItems[0].pickupLabel,
      dropoffLabel: 'Buyer Verified Gate Pin',
      selectedSize: `${totals.totalQuantity} item(s) bundle`,
      quantity: totals.totalQuantity,
      unitPriceTzs: totals.itemsSubtotalTzs,
      itemSubtotalTzs: totals.itemsSubtotalTzs,
      transportFeeTzs: totals.transportFeeTzs,
      totalAmountTzs: totals.grandTotalTzs,
      paymentMethod: 'CASH_BY_HAND_WITH_RECEIPT',
      secretSafetyCode: code,
      receiptNumber: receipt,
      driverName: 'Juma Hassan',
      driverPlate: anyTruck ? 'T 894 DTR' : 'MC 482 DTR',
      requiresTruck: anyTruck,
      transportTierName: anyTruck ? 'Tradee Canter Commercial Truck' : 'Tradee Moto Cargo Box',
      cargoBoxAttached: true,
      status: 'IN_TRANSIT',
      refundAmountTzs: 0,
      disturbanceCompTzs: 0,
    };

    state.orders.unshift(cartOrder);
    state.cartItems = [];
    showToast(`Cart checked out! Safety Code: ${code}`);
    switchPortal('TELEMETRY');
  }

  function bookBoltTransport() {
    const quotes = Engine.compareAllBoltTiers(
      TRANSPORT_TIERS,
      state.boltDistanceKm,
      state.boltWeightKg
    );
    let chosen = quotes.find((q) => q.tierKey === state.boltSelectedTierKey && q.isEligible);
    if (!chosen) {
      chosen = quotes.find((q) => q.isRecommended) || quotes[quotes.length - 1];
      state.boltSelectedTierKey = chosen.tierKey;
    }

    const code = Engine.generateSecretSafetyCode();
    const receipt = Engine.generateReceiptNumber();
    const roleLabel = state.boltRoleMode === 'SELLER' ? 'Seller Cargo Dispatch' : 'Buyer Bolt Transport';

    const transportOrder = {
      id: 'trp_' + Date.now(),
      orderType: 'BOLT_TRANSPORT',
      productId: 'bolt_direct_' + chosen.tierKey.toLowerCase(),
      productTitle: `${chosen.name} (${roleLabel})`,
      category: 'ELECTRONICS',
      providerName: state.boltRoleMode === 'SELLER' ? state.userName + ' (Seller)' : 'Tradee On-Demand Fleet',
      pickupLabel: state.boltPickupHub,
      dropoffLabel: state.boltDropoffLabel,
      selectedSize: `${state.boltWeightKg} kg · ${state.boltDistanceKm} km`,
      quantity: 1,
      unitPriceTzs: 0,
      itemSubtotalTzs: 0,
      transportFeeTzs: chosen.fareTzs,
      totalAmountTzs: chosen.fareTzs,
      paymentMethod: state.boltPaymentMethod,
      secretSafetyCode: code,
      receiptNumber: receipt,
      driverName: 'Juma Hassan',
      driverPlate: chosen.requiresTruck ? 'T 894 DTR' : 'MC 482 DTR',
      requiresTruck: chosen.requiresTruck,
      transportTierName: chosen.name,
      cargoBoxAttached: true,
      status: 'IN_TRANSIT',
      refundAmountTzs: 0,
      disturbanceCompTzs: 0,
    };

    state.orders.unshift(transportOrder);
    showToast(`${chosen.name} dispatched! Safety Code: ${code}`);
    switchPortal('TELEMETRY');
  }

  function toggleFavorite(productId) {
    if (state.favoriteIds.has(productId)) {
      state.favoriteIds.delete(productId);
      showToast('Removed from saved favorites');
    } else {
      state.favoriteIds.add(productId);
      showToast('Saved to favorites');
    }
    renderAll();
  }

  function renderHeader() {
    const activeOrdersCount = state.orders.filter(
      (o) => !['DELIVERED_COMPLETED', 'DAMAGED_OR_LOST_REFUNDED', 'ROBBERY_OR_HARM_REFUNDED'].includes(o.status)
    ).length;
    const compareGroups = Engine.buildPriceComparisonMatrix(state.products, state.buyerDistanceKm);
    const multiSellerCount = compareGroups.filter((g) => g.sellerCount > 1).length;

    const tabsContainer = document.getElementById('portal-tabs-nav');
    if (tabsContainer) {
      const tabs = [
        { key: 'MARKETPLACE', label: '🛒 Marketplace (eBay/Amazon)', badge: state.products.length, badgeClass: '' },
        { key: 'COMPARE', label: '⚖️ Compare Prices', badge: `${multiSellerCount} Multi-Seller`, badgeClass: 'warm' },
        { key: 'BOLT_TRANSPORT', label: '🛵 Bolt Transport (Buyers & Sellers)', badge: 'Live Fare', badgeClass: 'bolt' },
        { key: 'SELLER', label: '🏪 Seller Portal (Post Items)', badge: null, badgeClass: '' },
        { key: 'DRIVER', label: '🚚 Fleet & 2-Yr Lease', badge: `${state.driverContract.weeksPaidCount}/104w`, badgeClass: '' },
        { key: 'TELEMETRY', label: '📍 Live Map & Safety Code', badge: activeOrdersCount, badgeClass: '' },
      ];
      tabsContainer.innerHTML = tabs
        .map(
          (t) => `
          <button class="portal-tab-btn ${state.activePortal === t.key ? 'active' : ''}"
                  data-portal="${t.key}"
                  id="nav-tab-${t.key.toLowerCase()}">
            <span>${t.label}</span>
            ${t.badge !== null ? `<span class="tab-badge ${t.badgeClass}">${t.badge}</span>` : ''}
          </button>
        `
        )
        .join('');

      tabsContainer.querySelectorAll('.portal-tab-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
          switchPortal(btn.getAttribute('data-portal'));
        });
      });
    }

    const floatBtn = document.getElementById('floating-map-btn');
    if (floatBtn) {
      if (activeOrdersCount > 0) {
        floatBtn.classList.add('pulsing');
      } else {
        floatBtn.classList.remove('pulsing');
      }
      const badgeEl = document.getElementById('floating-map-count');
      if (badgeEl) {
        badgeEl.textContent = activeOrdersCount > 0 ? `(${activeOrdersCount})` : '';
      }
    }

    const cartCountEl = document.getElementById('header-cart-count');
    if (cartCountEl) {
      const cartSummary = Engine.calculateCartTotals(state.cartItems, state.buyerDistanceKm);
      cartCountEl.textContent = `(${cartSummary.totalQuantity})`;
    }
  }

  function renderMarketplacePortal(container) {
    const totalCats = CATEGORIES.length;
    const leftIdx = (state.carouselIndex - 1 + totalCats) % totalCats;
    const rightIdx = (state.carouselIndex + 1) % totalCats;
    const activeCat = CATEGORIES[state.carouselIndex];
    const leftCat = CATEGORIES[leftIdx];
    const rightCat = CATEGORIES[rightIdx];

    const filteredProducts = Engine.filterAndSortProducts(state.products, {
      searchQuery: state.searchQuery,
      categoryKey: state.selectedCategoryKey,
      dealsOnly: state.dealsOnly,
      favoritesOnly: state.favoritesOnly,
      favoriteIds: state.favoriteIds,
      sortBy: state.sortBy,
    });

    const cartTotals = Engine.calculateCartTotals(state.cartItems, state.buyerDistanceKm);

    container.innerHTML = `
      <!-- Hero Vision Banner -->
      <section class="hero-vision-banner">
        <div class="hero-vision-text">
          <h2>Tradee — Where eBay, Amazon &amp; Bolt Meet in Tanzania</h2>
          <p>Browse 8 categories, compare live multi-seller prices including delivery fees, or order instant Bolt-style motorcycle &amp; truck transport for your purchases and seller inventory.</p>
        </div>
        <div class="hero-pillars">
          <button class="hero-pillar-chip" id="hero-btn-compare">⚖️ Compare Seller Prices</button>
          <button class="hero-pillar-chip" id="hero-btn-bolt">🛵 Order Bolt Transport</button>
          <button class="hero-pillar-chip" id="hero-btn-sell">🏪 Post Items to Sell</button>
        </div>
      </section>

      ${
        cartTotals.totalQuantity > 0
          ? `
        <div class="compare-group-card" id="active-cart-banner" style="background:var(--bg-surface); border:2px solid var(--brand-primary); margin-bottom:18px;">
          <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
            <div>
              <div style="font-size:15px; font-weight:900; color:var(--text-primary);">
                🛒 Shopping Cart (${cartTotals.totalQuantity} item${cartTotals.totalQuantity === 1 ? '' : 's'})
              </div>
              <div style="font-size:13px; color:var(--text-secondary);">
                Items: <strong>${Engine.formatTzs(cartTotals.itemsSubtotalTzs)}</strong> + Bolt Delivery: <strong>${Engine.formatTzs(cartTotals.transportFeeTzs)}</strong> = Grand Total: <strong style="color:var(--brand-primary-dark);">${Engine.formatTzs(cartTotals.grandTotalTzs)}</strong>
              </div>
            </div>
            <div style="display:flex; gap:8px;">
              <button class="btn btn-outline" id="cart-clear-btn">Clear Cart</button>
              <button class="btn btn-primary" id="cart-checkout-btn">✓ Checkout &amp; Generate Safety Code</button>
            </div>
          </div>
        </div>
      `
          : ''
      }

      <!-- Skewed Parallelogram Deals Banner -->
      <div class="skewed-deals-banner" id="daily-deals-banner">
        <div class="skewed-deals-inner">
          <div class="skewed-deals-title">
            🔥 Deals of today : <span>${state.dealsOnly ? 'Showing Daily Deals (Click to Show All)' : 'Click here to filter TSh Flash Offers'}</span>
          </div>
          <span class="badge-pill badge-deal">100% Refund + TSh 1,000 Buyer Protection</span>
        </div>
      </div>

      <!-- 3D Layered 8-Category Perspective Carousel -->
      <section class="carousel-showcase-section">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; flex-wrap:wrap; gap:8px;">
          <div>
            <h3 style="font-size:16px; font-weight:800;">3D Category Perspective Deck (8 Channels)</h3>
            <p style="font-size:12.5px; color:var(--text-secondary);">Click center card to filter category · Click side cards or dots to rotate deck</p>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-outline" id="carousel-prev-btn">◀ Prev</button>
            <button class="btn btn-outline" id="carousel-next-btn">Next ▶</button>
          </div>
        </div>

        <div class="carousel-stage">
          <!-- Left Peek Card -->
          <div class="carousel-card-3d peek-left" id="carousel-peek-left">
            <span class="carousel-tagline-pill">${escapeHtml(leftCat.tagline)}</span>
            <div class="parallax-icon-pair">
              <div class="parallax-box-top" style="background:${leftCat.gradient}">${leftCat.topIcon}</div>
              <div class="parallax-box-bottom">${leftCat.bottomIcon}</div>
            </div>
            <div class="carousel-footer-label">
              <h3>${escapeHtml(leftCat.carouselLabel)}</h3>
            </div>
          </div>

          <!-- Center Active Card -->
          <div class="carousel-card-3d active-card" id="carousel-active-card">
            <span class="carousel-tagline-pill">${escapeHtml(activeCat.tagline)}</span>
            <div class="parallax-icon-pair">
              <div class="parallax-box-top" style="background:${activeCat.gradient}">${activeCat.topIcon}</div>
              <div class="parallax-box-bottom">${activeCat.bottomIcon}</div>
            </div>
            <div class="carousel-footer-label">
              <h3>${escapeHtml(activeCat.carouselLabel)}</h3>
              <span>Tap to filter ${escapeHtml(activeCat.displayName)}</span>
            </div>
          </div>

          <!-- Right Peek Card -->
          <div class="carousel-card-3d peek-right" id="carousel-peek-right">
            <span class="carousel-tagline-pill">${escapeHtml(rightCat.tagline)}</span>
            <div class="parallax-icon-pair">
              <div class="parallax-box-top" style="background:${rightCat.gradient}">${rightCat.topIcon}</div>
              <div class="parallax-box-bottom">${rightCat.bottomIcon}</div>
            </div>
            <div class="carousel-footer-label">
              <h3>${escapeHtml(rightCat.carouselLabel)}</h3>
            </div>
          </div>
        </div>

        <div class="carousel-dots">
          ${CATEGORIES.map(
            (c, idx) => `
            <button class="carousel-dot ${idx === state.carouselIndex ? 'active' : ''}"
                    data-cat-idx="${idx}"
                    title="${escapeHtml(c.displayName)}"></button>
          `
          ).join('')}
        </div>
      </section>

      <!-- Category Filter Chips & Sort Controls -->
      <div class="filter-chips-bar">
        <div class="chips-scroll">
          <button class="filter-chip ${!state.selectedCategoryKey && !state.dealsOnly && !state.favoritesOnly ? 'active' : ''}"
                  data-filter-cat="">
            All (${state.products.length})
          </button>
          ${CATEGORIES.map(
            (cat) => `
            <button class="filter-chip ${state.selectedCategoryKey === cat.key ? 'active' : ''}"
                    data-filter-cat="${cat.key}">
              ${cat.topIcon} ${escapeHtml(cat.displayName)}
            </button>
          `
          ).join('')}
          <button class="filter-chip ${state.favoritesOnly ? 'active' : ''}" id="filter-fav-chip">
            ❤️ Favorites (${state.favoriteIds.size})
          </button>
        </div>

        <div style="display:flex; align-items:center; gap:8px;">
          <label for="marketplace-sort-select" style="font-size:12.5px; font-weight:700; color:var(--text-secondary);">Sort:</label>
          <select id="marketplace-sort-select" class="form-control" style="width:auto; height:38px;">
            <option value="NEWEST_FIRST" ${state.sortBy === 'NEWEST_FIRST' ? 'selected' : ''}>Newest First</option>
            <option value="PRICE_LOW_TO_HIGH" ${state.sortBy === 'PRICE_LOW_TO_HIGH' ? 'selected' : ''}>Price: Low to High</option>
            <option value="PRICE_HIGH_TO_LOW" ${state.sortBy === 'PRICE_HIGH_TO_LOW' ? 'selected' : ''}>Price: High to Low</option>
          </select>
        </div>
      </div>

      <!-- Product Listings Grid -->
      <div class="product-grid" id="marketplace-product-grid">
        ${filteredProducts
          .map((prod) => {
            const cat = getCategoryInfo(prod.category);
            const offers = Engine.compareProductOffers(state.products, prod, state.buyerDistanceKm);
            const isBestOffer = offers.length > 1 && offers[0].productId === prod.id;
            const effKm = Engine.estimateHubDistanceKm(prod.pickupLabel, state.buyerDistanceKm, prod.hubDistanceKm);
            const estTransport = Engine.computeTransportFeeTzs(prod.requiresTruck, effKm);
            const landedTotal = prod.priceTzs + estTransport;
            const isFav = state.favoriteIds.has(prod.id);
            const groupKey = Engine.normalizeComparisonKey(prod);

            return `
              <article class="product-card" data-product-id="${prod.id}">
                <div>
                  <div class="card-badges-row" style="margin-bottom:10px;">
                    <span class="badge-pill badge-category">${cat.topIcon} ${escapeHtml(cat.displayName)}</span>
                    <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap;">
                      ${prod.isDailyDeal ? '<span class="badge-pill badge-deal">🔥 Deal</span>' : ''}
                      <span class="badge-pill ${prod.requiresTruck ? 'badge-truck' : 'badge-moto'}">
                        ${prod.requiresTruck ? `🚚 Truck (${prod.weightKg}kg)` : '🛵 Moto Box'}
                      </span>
                      <button class="btn-fav" data-fav-id="${prod.id}" style="border:none; background:transparent; cursor:pointer; font-size:16px;" title="Toggle Favorite">
                        ${isFav ? '❤️' : '🤍'}
                      </button>
                    </div>
                  </div>

                  <h3>${escapeHtml(prod.title)}</h3>
                  <p class="product-desc" style="margin:6px 0 10px;">${escapeHtml(prod.description)}</p>

                  ${
                    offers.length > 1
                      ? `<div style="margin-bottom:10px;">
                          <button class="badge-pill badge-compare" data-compare-group="${escapeHtml(groupKey)}">
                            ⚖️ Compare ${offers.length} Sellers · From ${Engine.formatTzs(offers[0].unitPriceTzs)}
                            ${isBestOffer ? ' · ★ BEST LANDED VALUE' : ''}
                          </button>
                        </div>`
                      : ''
                  }

                  <div class="product-meta-grid">
                    <div><strong>Seller:</strong> ${escapeHtml(prod.providerName)} (★${prod.sellerRating || '4.8'})</div>
                    <div><strong>Hub:</strong> ${escapeHtml(prod.pickupLabel)} (${prod.hubDistanceKm || 3} km)</div>
                    <div><strong>Spec:</strong> ${escapeHtml(prod.sizeSpec)}</div>
                    <div><strong>Stock:</strong> ${prod.quantityAvailable} units</div>
                  </div>
                </div>

                <div class="product-price-footer">
                  <div class="price-block">
                    <div class="price-tzs">${Engine.formatTzs(prod.priceTzs)}</div>
                    <div class="landed-hint">+ ${Engine.formatTzs(estTransport)} Bolt Delivery = ${Engine.formatTzs(landedTotal)}</div>
                  </div>
                  <div class="card-btn-group">
                    <button class="btn btn-outline" data-compare-group="${escapeHtml(groupKey)}">Compare</button>
                    <button class="btn btn-outline" data-cart-id="${prod.id}">+ Cart</button>
                    <button class="btn btn-primary" data-buy-id="${prod.id}">Buy Now</button>
                  </div>
                </div>
              </article>
            `;
          })
          .join('')}
      </div>
    `;

    // Wire events
    document.getElementById('hero-btn-compare')?.addEventListener('click', () => switchPortal('COMPARE'));
    document.getElementById('hero-btn-bolt')?.addEventListener('click', () => switchPortal('BOLT_TRANSPORT'));
    document.getElementById('hero-btn-sell')?.addEventListener('click', () => switchPortal('SELLER'));

    document.getElementById('cart-clear-btn')?.addEventListener('click', () => {
      state.cartItems = [];
      showToast('Cart cleared');
      renderAll();
    });
    document.getElementById('cart-checkout-btn')?.addEventListener('click', checkoutCart);

    document.getElementById('daily-deals-banner')?.addEventListener('click', () => {
      state.dealsOnly = !state.dealsOnly;
      renderAll();
    });

    document.getElementById('carousel-prev-btn')?.addEventListener('click', () => {
      state.carouselIndex = (state.carouselIndex - 1 + totalCats) % totalCats;
      renderAll();
    });
    document.getElementById('carousel-next-btn')?.addEventListener('click', () => {
      state.carouselIndex = (state.carouselIndex + 1) % totalCats;
      renderAll();
    });
    document.getElementById('carousel-peek-left')?.addEventListener('click', () => {
      state.carouselIndex = leftIdx;
      renderAll();
    });
    document.getElementById('carousel-peek-right')?.addEventListener('click', () => {
      state.carouselIndex = rightIdx;
      renderAll();
    });
    document.getElementById('carousel-active-card')?.addEventListener('click', () => {
      state.selectedCategoryKey = state.selectedCategoryKey === activeCat.key ? null : activeCat.key;
      renderAll();
    });

    container.querySelectorAll('.carousel-dot').forEach((dot) => {
      dot.addEventListener('click', () => {
        state.carouselIndex = Number(dot.getAttribute('data-cat-idx'));
        renderAll();
      });
    });

    container.querySelectorAll('[data-filter-cat]').forEach((chip) => {
      chip.addEventListener('click', () => {
        const key = chip.getAttribute('data-filter-cat') || null;
        state.selectedCategoryKey = state.selectedCategoryKey === key ? null : key;
        if (!key) {
          state.dealsOnly = false;
          state.favoritesOnly = false;
        }
        renderAll();
      });
    });

    document.getElementById('filter-fav-chip')?.addEventListener('click', () => {
      state.favoritesOnly = !state.favoritesOnly;
      renderAll();
    });

    document.getElementById('marketplace-sort-select')?.addEventListener('change', (e) => {
      state.sortBy = e.target.value;
      renderAll();
    });

    container.querySelectorAll('[data-fav-id]').forEach((btn) => {
      btn.addEventListener('click', () => toggleFavorite(btn.getAttribute('data-fav-id')));
    });
    container.querySelectorAll('[data-cart-id]').forEach((btn) => {
      btn.addEventListener('click', () => addToCart(btn.getAttribute('data-cart-id')));
    });
    container.querySelectorAll('[data-buy-id]').forEach((btn) => {
      btn.addEventListener('click', () => buyProductNow(btn.getAttribute('data-buy-id')));
    });
    container.querySelectorAll('[data-compare-group]').forEach((btn) => {
      btn.addEventListener('click', () => {
        switchPortal('COMPARE', btn.getAttribute('data-compare-group'));
      });
    });
  }

  function renderComparePortal(container) {
    const groups = Engine.buildPriceComparisonMatrix(state.products, state.buyerDistanceKm);
    const orderedGroups = state.focusedCompareGroupKey
      ? groups.slice().sort((a, b) => (a.groupKey === state.focusedCompareGroupKey ? -1 : b.groupKey === state.focusedCompareGroupKey ? 1 : 0))
      : groups;

    container.innerHTML = `
      <div class="compare-group-card" style="background:var(--gradient-hero-banner); color:#FFFFFF;">
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:16px;">
          <div>
            <h2 style="font-size:22px; font-weight:800;">⚖️ Multi-Seller Price &amp; Landed Cost Comparison Engine</h2>
            <p style="color:#E0F2FE; font-size:14px;">Compare competing sellers side-by-side: Sticker Price + Bolt Delivery Fee = True Total Landed Cost.</p>
          </div>
          <div style="background:rgba(255,255,255,0.14); padding:12px 18px; border-radius:14px; min-width:260px;">
            <label style="font-size:12px; font-weight:800; display:block; margin-bottom:4px;">
              Your Gate Distance Override: <strong>${state.buyerDistanceKm} km</strong>
            </label>
            <input type="range" id="compare-distance-slider" min="1" max="20" step="0.5" value="${state.buyerDistanceKm}" style="width:100%;" />
          </div>
        </div>
      </div>

      ${orderedGroups
        .map(
          (group) => `
        <div class="compare-group-card">
          <div class="compare-group-header">
            <div>
              <span class="badge-pill badge-category">${escapeHtml(getCategoryInfo(group.category).displayName)}</span>
              <h3 style="font-size:19px; font-weight:800; margin-top:4px;">${escapeHtml(group.title)}</h3>
            </div>
            <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
              <span class="badge-pill badge-deal">${group.sellerCount} Competing Seller(s)</span>
              ${
                group.priceSpreadTzs > 0
                  ? `<span class="badge-pill badge-moto">Spread: Save up to ${Engine.formatTzs(group.priceSpreadTzs)} on Sticker</span>`
                  : ''
              }
            </div>
          </div>

          <div style="overflow-x:auto;">
            <table class="compare-offers-table">
              <thead>
                <tr>
                  <th>Rank &amp; Seller</th>
                  <th>Pickup Hub &amp; Distance</th>
                  <th>Condition &amp; Spec</th>
                  <th>Item Sticker Price</th>
                  <th>Bolt Transport Fee</th>
                  <th>Total Landed Cost</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                ${group.offers
                  .map(
                    (offer) => `
                  <tr class="${offer.isBestLandedValue ? 'best-value-row' : ''}">
                    <td>
                      <strong>#${offer.rank} ${escapeHtml(offer.providerName)}</strong>
                      <div style="font-size:12px; color:var(--text-secondary);">★ ${offer.sellerRating} Verified Seller</div>
                      ${offer.isBestLandedValue ? '<span class="badge-pill badge-moto">★ BEST LANDED VALUE</span>' : ''}
                      ${offer.isLowestStickerPrice && !offer.isBestLandedValue ? '<span class="badge-pill badge-deal">Lowest Sticker</span>' : ''}
                    </td>
                    <td>
                      <div>${escapeHtml(offer.pickupLabel)}</div>
                      <div style="font-size:12px; color:var(--text-secondary);">${offer.effectiveDistanceKm} km · ${offer.requiresTruck ? '🚚 Truck' : '🛵 Moto Box'}</div>
                    </td>
                    <td>
                      <div>${escapeHtml(offer.condition)}</div>
                      <div style="font-size:12px; color:var(--text-secondary);">${escapeHtml(offer.sizeSpec)} (${offer.quantityAvailable} in stock)</div>
                    </td>
                    <td style="font-weight:800;">${Engine.formatTzs(offer.unitPriceTzs)}</td>
                    <td style="color:var(--brand-primary-dark); font-weight:700;">+ ${Engine.formatTzs(offer.transportFeeTzs)}</td>
                    <td>
                      <div style="font-size:16px; font-weight:900; color:var(--text-primary);">${Engine.formatTzs(offer.landedTotalTzs)}</div>
                      ${
                        offer.savingsVsHighestTzs > 0
                          ? `<div style="font-size:11.5px; font-weight:800; color:#059669;">Save ${Engine.formatTzs(offer.savingsVsHighestTzs)}</div>`
                          : ''
                      }
                    </td>
                    <td>
                      <button class="btn btn-primary" data-buy-id="${offer.productId}">Buy Best Deal</button>
                    </td>
                  </tr>
                `
                  )
                  .join('')}
              </tbody>
            </table>
          </div>
        </div>
      `
        )
        .join('')}
    `;

    document.getElementById('compare-distance-slider')?.addEventListener('input', (e) => {
      state.buyerDistanceKm = Number(e.target.value);
      renderComparePortal(container);
    });

    container.querySelectorAll('[data-buy-id]').forEach((btn) => {
      btn.addEventListener('click', () => buyProductNow(btn.getAttribute('data-buy-id')));
    });
  }

  function renderBoltTransportPortal(container) {
    const quotes = Engine.compareAllBoltTiers(
      TRANSPORT_TIERS,
      state.boltDistanceKm,
      state.boltWeightKg
    );
    const activeQuote =
      quotes.find((q) => q.tierKey === state.boltSelectedTierKey && q.isEligible) ||
      quotes.find((q) => q.isRecommended) ||
      quotes[quotes.length - 1];

    container.innerHTML = `
      <div class="hero-vision-banner" style="background:var(--gradient-bolt-transport);">
        <div class="hero-vision-text">
          <h2>🛵 Tradee Bolt Transport — On-Demand Rides &amp; Cargo Dispatch</h2>
          <p>Both Buyers and Sellers can order instant transportation across Dar es Salaam — from express Boda rides and detachable Moto Cargo Boxes to Bajaji Tri-Cargo and Canter Commercial Trucks.</p>
        </div>
        <div style="display:flex; gap:8px;">
          <button class="btn ${state.boltRoleMode === 'BUYER' ? 'btn-primary' : 'btn-outline'}" id="bolt-mode-buyer">
            🛍️ Buyer Transport Mode
          </button>
          <button class="btn ${state.boltRoleMode === 'SELLER' ? 'btn-primary' : 'btn-outline'}" id="bolt-mode-seller">
            🏪 Seller Logistics Mode
          </button>
        </div>
      </div>

      <div class="bolt-grid">
        <!-- Left Column: Route & Load Configuration -->
        <div class="bolt-panel">
          <h3 style="font-size:18px; font-weight:800; margin-bottom:14px;">
            1. Configure ${state.boltRoleMode === 'SELLER' ? 'Seller Inventory / Customer Dispatch' : 'Buyer Ride / Parcel Pickup'} Route
          </h3>

          <div class="form-group">
            <label>Pickup Station / Market Hub</label>
            <select id="bolt-pickup-select" class="form-control">
              ${MARKET_HUBS.map(
                (h) => `<option value="${escapeHtml(h.name)}" ${state.boltPickupHub === h.name ? 'selected' : ''}>${escapeHtml(h.name)} (${escapeHtml(h.zone)})</option>`
              ).join('')}
            </select>
          </div>

          <div class="form-group">
            <label>Dropoff Destination / Buyer Gate</label>
            <input type="text" id="bolt-dropoff-input" class="form-control" value="${escapeHtml(state.boltDropoffLabel)}" />
          </div>

          <div style="display:grid; grid-template-columns:1fr 1fr; gap:14px;">
            <div class="form-group">
              <label>Trip Distance: <strong>${state.boltDistanceKm} km</strong></label>
              <input type="range" id="bolt-distance-slider" min="1" max="25" step="0.5" value="${state.boltDistanceKm}" style="width:100%;" />
            </div>
            <div class="form-group">
              <label>Cargo / Load Weight (kg)</label>
              <input type="number" id="bolt-weight-input" class="form-control" min="0.5" max="4500" step="0.5" value="${state.boltWeightKg}" />
            </div>
          </div>

          <div class="form-group">
            <label>Cargo / Ride Instructions</label>
            <input type="text" id="bolt-note-input" class="form-control" value="${escapeHtml(state.boltCargoNote)}" />
          </div>

          <div class="form-group">
            <label>Payment Mode</label>
            <select id="bolt-payment-select" class="form-control">
              <option value="CASH_BY_HAND_WITH_RECEIPT" ${state.boltPaymentMethod === 'CASH_BY_HAND_WITH_RECEIPT' ? 'selected' : ''}>Phase 1: Cash by Hand on Delivery (with Digital Receipt)</option>
              <option value="FIREWALL_CARD_PAYMENT" ${state.boltPaymentMethod === 'FIREWALL_CARD_PAYMENT' ? 'selected' : ''}>Phase 2: Firewall-Protected Tokenized Card</option>
            </select>
          </div>
        </div>

        <!-- Right Column: Vehicle Tier Selection & Instant Dispatch -->
        <div class="bolt-panel">
          <h3 style="font-size:18px; font-weight:800; margin-bottom:14px;">2. Select Tradee Vehicle Tier &amp; Compare Fares</h3>

          <div>
            ${quotes
              .map(
                (q) => `
              <div class="tier-option-card ${activeQuote.tierKey === q.tierKey ? 'selected' : ''} ${!q.isEligible ? 'disabled' : ''}"
                   data-tier-key="${q.tierKey}">
                <div style="display:flex; align-items:center; gap:12px;">
                  <div style="font-size:30px;">${q.icon}</div>
                  <div>
                    <div style="font-weight:800; font-size:15px;">
                      ${escapeHtml(q.name)}
                      ${q.isRecommended ? '<span class="badge-pill badge-moto" style="margin-left:6px;">★ Recommended</span>' : ''}
                    </div>
                    <div style="font-size:12.5px; color:var(--text-secondary);">${escapeHtml(q.subtitle)} · Max ${q.maxWeightKg} kg</div>
                    ${
                      q.ineligibleReason
                        ? `<div style="font-size:11.5px; color:var(--status-danger); font-weight:700;">⚠️ ${escapeHtml(q.ineligibleReason)}</div>`
                        : `<div style="font-size:11.5px; color:var(--brand-primary-dark); font-weight:700;">ETA ~${q.estimatedMinutes} mins · ${escapeHtml(q.badge)}</div>`
                    }
                  </div>
                </div>
                <div style="text-align:right;">
                  <div style="font-size:18px; font-weight:900; color:var(--text-primary);">${Engine.formatTzs(q.fareTzs)}</div>
                  <div style="font-size:11px; color:var(--text-secondary);">${q.distanceKm} km total</div>
                </div>
              </div>
            `
              )
              .join('')}
          </div>

          <div style="background:var(--bg-surface-subtle); border-radius:14px; padding:14px; margin:16px 0;">
            <div style="display:flex; justify-content:space-between; font-size:14px; margin-bottom:4px;">
              <span>Selected Tier:</span>
              <strong>${escapeHtml(activeQuote.name)}</strong>
            </div>
            <div style="display:flex; justify-content:space-between; font-size:14px; margin-bottom:4px;">
              <span>Route:</span>
              <strong>${escapeHtml(state.boltPickupHub)} → ${escapeHtml(state.boltDropoffLabel)}</strong>
            </div>
            <div style="display:flex; justify-content:space-between; font-size:16px; font-weight:900; color:var(--brand-primary-dark); padding-top:6px; border-top:1px solid var(--border-hairline);">
              <span>Total Guaranteed Transport Fare:</span>
              <span>${Engine.formatTzs(activeQuote.fareTzs)}</span>
            </div>
          </div>

          <button class="btn btn-bolt" id="dispatch-bolt-btn" style="width:100%; height:52px; font-size:15px;">
            🛵 DISPATCH ${escapeHtml(activeQuote.name.toUpperCase())} &amp; GENERATE SAFETY CODE
          </button>
        </div>
      </div>
    `;

    document.getElementById('bolt-mode-buyer')?.addEventListener('click', () => {
      state.boltRoleMode = 'BUYER';
      renderBoltTransportPortal(container);
    });
    document.getElementById('bolt-mode-seller')?.addEventListener('click', () => {
      state.boltRoleMode = 'SELLER';
      renderBoltTransportPortal(container);
    });
    document.getElementById('bolt-pickup-select')?.addEventListener('change', (e) => {
      state.boltPickupHub = e.target.value;
      renderBoltTransportPortal(container);
    });
    document.getElementById('bolt-dropoff-input')?.addEventListener('input', (e) => {
      state.boltDropoffLabel = e.target.value;
    });
    document.getElementById('bolt-distance-slider')?.addEventListener('input', (e) => {
      state.boltDistanceKm = Number(e.target.value);
      renderBoltTransportPortal(container);
    });
    document.getElementById('bolt-weight-input')?.addEventListener('input', (e) => {
      state.boltWeightKg = Math.max(0.1, Number(e.target.value) || 1);
      renderBoltTransportPortal(container);
    });
    document.getElementById('bolt-payment-select')?.addEventListener('change', (e) => {
      state.boltPaymentMethod = e.target.value;
    });

    container.querySelectorAll('.tier-option-card').forEach((card) => {
      card.addEventListener('click', () => {
        if (card.classList.contains('disabled')) return;
        state.boltSelectedTierKey = card.getAttribute('data-tier-key');
        renderBoltTransportPortal(container);
      });
    });

    document.getElementById('dispatch-bolt-btn')?.addEventListener('click', bookBoltTransport);
  }

  function renderSellerPortal(container) {
    container.innerHTML = `
      <div class="bolt-grid">
        <div class="bolt-panel">
          <h3 style="font-size:19px; font-weight:800; margin-bottom:12px;">🏪 Post New Item to Tradee Marketplace</h3>
          <p style="font-size:13px; color:var(--text-secondary); margin-bottom:14px;">
            Items published here immediately enter the 8-category catalog and the Multi-Seller Price Comparison Engine.
          </p>

          <div class="form-group">
            <label>Category (8 Core Channels)</label>
            <select id="seller-cat-select" class="form-control">
              ${CATEGORIES.map((c) => `<option value="${c.key}">${escapeHtml(c.displayName)}</option>`).join('')}
            </select>
          </div>

          <div class="form-group">
            <label>Product Title *</label>
            <input type="text" id="seller-title-input" class="form-control" placeholder="e.g. Pro Wireless Gamepad DualShock" />
          </div>

          <div class="form-group">
            <label>Description</label>
            <input type="text" id="seller-desc-input" class="form-control" placeholder="Condition, warranty, accessories included..." />
          </div>

          <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
            <div class="form-group">
              <label>Unit Price (TSh) *</label>
              <input type="number" id="seller-price-input" class="form-control" value="79000" />
            </div>
            <div class="form-group">
              <label>Quantity in Stock *</label>
              <input type="number" id="seller-qty-input" class="form-control" value="12" />
            </div>
          </div>

          <div id="seller-price-benchmark-box" style="background:var(--bg-surface-subtle); border:1px solid var(--border-hairline); border-radius:12px; padding:10px 14px; margin-bottom:12px; font-size:12.5px;"></div>

          <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
            <div class="form-group">
              <label>Size / Dimensions *</label>
              <input type="text" id="seller-size-input" class="form-control" value="Standard Retail Box" />
            </div>
            <div class="form-group">
              <label>Item Weight (kg) — Auto-Classifies >35 kg as Truck</label>
              <input type="number" id="seller-weight-input" class="form-control" value="0.8" step="0.5" />
            </div>
          </div>

          <div class="form-group">
            <label>Pickup Hub / Market Stall</label>
            <select id="seller-hub-select" class="form-control">
              ${MARKET_HUBS.map((h) => `<option value="${escapeHtml(h.name)}">${escapeHtml(h.name)}</option>`).join('')}
            </select>
          </div>

          <div style="display:flex; gap:10px; margin-top:16px;">
            <button class="btn btn-primary" id="seller-publish-btn" style="flex:1; height:48px;">
              + PUBLISH ITEM TO TRADEE
            </button>
            <button class="btn btn-bolt" id="seller-order-transport-btn" style="height:48px;">
              🛵 Order Seller Transport
            </button>
          </div>
        </div>

        <div class="bolt-panel">
          <h3 style="font-size:19px; font-weight:800; margin-bottom:12px;">📦 Active Marketplace Catalog (${state.products.length} Listings)</h3>
          <div style="max-height:540px; overflow-y:auto; display:flex; flex-direction:column; gap:10px;">
            ${state.products
              .map(
                (p) => `
              <div style="border:1px solid var(--border-hairline); border-radius:12px; padding:12px; display:flex; justify-content:space-between; align-items:center;">
                <div>
                  <div style="font-size:11px; font-weight:800; color:var(--brand-primary);">${escapeHtml(p.category)} · ${escapeHtml(p.providerName)}</div>
                  <div style="font-weight:800; font-size:14.5px;">${escapeHtml(p.title)}</div>
                  <div style="font-size:12px; color:var(--text-secondary);">Qty: ${p.quantityAvailable} · ${escapeHtml(p.sizeSpec)} · ${p.requiresTruck ? '🚚 Truck' : '🛵 Moto Box'}</div>
                </div>
                <div style="font-weight:900; font-size:15px;">${Engine.formatTzs(p.priceTzs)}</div>
              </div>
            `
              )
              .join('')}
          </div>
        </div>
      </div>
    `;

    function updateSellerBenchmark() {
      const catEl = document.getElementById('seller-cat-select');
      const priceEl = document.getElementById('seller-price-input');
      const boxEl = document.getElementById('seller-price-benchmark-box');
      if (!catEl || !boxEl) return;
      const catKey = catEl.value;
      const enteredPrice = Number(priceEl && priceEl.value) || 0;
      const bench = Engine.computeCategoryPriceBenchmark(state.products, catKey);
      const statusHint =
        enteredPrice > 0 && bench.lowestTzs > 0
          ? enteredPrice <= bench.lowestTzs
            ? '<span style="color:#059669; font-weight:800;">★ Beats or matches lowest sticker in category!</span>'
            : enteredPrice <= bench.averageTzs
            ? '<span style="color:var(--brand-primary-dark); font-weight:800;">✓ Competitive (below category average)</span>'
            : '<span style="color:#B45309; font-weight:800;">Above category average — check distance advantage</span>'
          : '';
      boxEl.innerHTML = `
        <div style="font-weight:800; color:var(--text-primary); margin-bottom:2px;">
          📊 Live Market Price Benchmark (${escapeHtml(catKey)} · ${bench.count} active listings)
        </div>
        <div style="color:var(--text-secondary);">
          Lowest: <strong>${Engine.formatTzs(bench.lowestTzs)}</strong> ·
          Average: <strong>${Engine.formatTzs(bench.averageTzs)}</strong> ·
          Highest: <strong>${Engine.formatTzs(bench.highestTzs)}</strong>
          ${statusHint ? ` · ${statusHint}` : ''}
        </div>
      `;
    }

    updateSellerBenchmark();
    document.getElementById('seller-cat-select')?.addEventListener('change', updateSellerBenchmark);
    document.getElementById('seller-price-input')?.addEventListener('input', updateSellerBenchmark);

    document.getElementById('seller-publish-btn')?.addEventListener('click', () => {
      const cat = document.getElementById('seller-cat-select').value;
      const title = document.getElementById('seller-title-input').value.trim();
      const desc = document.getElementById('seller-desc-input').value.trim();
      const price = Number(document.getElementById('seller-price-input').value) || 0;
      const qty = Math.max(1, Number(document.getElementById('seller-qty-input').value) || 1);
      const size = document.getElementById('seller-size-input').value.trim() || 'Standard';
      const weight = Math.max(0.1, Number(document.getElementById('seller-weight-input').value) || 0.5);
      const hubName = document.getElementById('seller-hub-select').value;
      const hubObj = MARKET_HUBS.find((h) => h.name === hubName) || MARKET_HUBS[0];

      if (!title || price <= 0) {
        showToast('Please enter a valid product title and price in TSh.');
        return;
      }

      const requiresTruck = weight > Engine.MOTORCYCLE_MAX_WEIGHT_KG;
      const newProd = {
        id: 'prod_custom_' + Date.now(),
        comparisonGroup: Engine.normalizeComparisonKey({ title }),
        category: cat,
        title,
        description: desc || `Listed by ${state.userName} from ${hubName}.`,
        providerId: 'seller_current_user',
        providerName: state.userName + ' Store',
        sellerRating: 4.9,
        sellerVerified: true,
        priceTzs: price,
        quantityAvailable: qty,
        sizeSpec: size,
        weightKg: weight,
        requiresTruck,
        isDailyDeal: true,
        isActive: true,
        pickupLabel: hubName,
        hubDistanceKm: hubObj.baseDistanceKm,
        condition: 'Brand New',
        createdAtSec: Math.floor(Date.now() / 1000),
      };

      state.products.unshift(newProd);
      showToast(
        requiresTruck
          ? 'Published! Auto-classified (>35kg): Phase 2 Commercial Truck'
          : 'Published! Ready for Motorcycle Cargo Box & Price Comparison'
      );
      renderSellerPortal(container);
    });

    document.getElementById('seller-order-transport-btn')?.addEventListener('click', () => {
      state.boltRoleMode = 'SELLER';
      switchPortal('BOLT_TRANSPORT');
    });
  }

  function renderDriverPortal(container) {
    const c = state.driverContract;
    const lease = Engine.calculateLeaseRemittance(c, 0);

    container.innerHTML = `
      <div class="bolt-grid">
        <div class="bolt-panel">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
            <div>
              <h3 style="font-size:19px; font-weight:800;">🛵 Driver Shift &amp; Station Curfew Terminal</h3>
              <p style="font-size:12.5px; color:var(--text-secondary);">Driver: ${escapeHtml(c.driverName)} · Plate: <strong>${escapeHtml(c.vehiclePlate)}</strong></p>
            </div>
            <span class="badge-pill ${c.curfewViolated ? 'badge-truck' : 'badge-moto'}">${escapeHtml(c.vehicleStatus)}</span>
          </div>

          <div style="background:var(--bg-surface-subtle); border-radius:14px; padding:14px; margin-bottom:14px;">
            <div><strong>Allocated Station:</strong> ${escapeHtml(c.stationName)}</div>
            <div><strong>Covert Anti-Theft GPS:</strong> <code>${escapeHtml(c.secretGpsImei)}</code></div>
            <div><strong>Morning Check-Out Window (05:00–07:00 EAT):</strong> ${escapeHtml(c.lastCheckedOutLabel || 'Pending')}</div>
            <div><strong>Evening Check-In Window (18:00–21:00 EAT):</strong> ${escapeHtml(c.lastCheckedInLabel || 'Pending')}</div>
          </div>

          <div style="display:flex; gap:10px; margin-bottom:14px; flex-wrap:wrap;">
            <button class="btn btn-primary" id="drv-checkout-btn">QR Morning Check-Out</button>
            <button class="btn btn-safe" id="drv-checkin-btn">QR Evening Check-In</button>
            <button class="btn btn-danger" id="drv-curfew-btn">Simulate 21:00 Curfew Breach</button>
          </div>

          <h4 style="font-size:15px; font-weight:800; margin:16px 0 8px;">Dual-Mode Utilization (Cargo Box vs Passenger Ride-Hail)</h4>
          <div style="display:flex; gap:8px; flex-wrap:wrap;">
            <button class="btn ${c.cargoBoxAttached ? 'btn-primary' : 'btn-outline'}" id="drv-toggle-box-btn">
              📦 ${c.cargoBoxAttached ? 'Cargo Box Mounted (Mode A)' : 'Mount Cargo Box'}
            </button>
            ${['STREET_HAIL', 'BOLT', 'UBER', 'LINKER']
              .map(
                (p) => `
              <button class="btn ${c.rideHailPartner === p ? 'btn-bolt' : 'btn-outline'}" data-partner="${p}">
                Mode B: ${p}
              </button>
            `
              )
              .join('')}
          </div>
        </div>

        <div class="bolt-panel">
          <h3 style="font-size:19px; font-weight:800; margin-bottom:10px;">🏆 2-Year (104-Week) Lease-to-Own Ledger</h3>
          <p style="font-size:13px; color:var(--text-secondary); margin-bottom:14px;">
            Weekly Remittance: <strong>TSh 70,000 / week</strong> · Total 104-Week Contract: <strong>TSh 7,280,000</strong>
          </p>

          <div style="background:var(--bg-surface-subtle); border-radius:14px; padding:16px; margin-bottom:16px;">
            <div style="display:flex; justify-content:space-between; font-size:16px; font-weight:900; margin-bottom:8px;">
              <span>${lease.weeksPaidCount} / ${lease.totalWeeksRequired} Weeks Paid (${lease.progressPercent}%)</span>
              <span style="color:var(--brand-primary);">${Engine.formatTzs(lease.totalPaidTzs)}</span>
            </div>
            <div style="height:12px; background:var(--border-hairline); border-radius:999px; overflow:hidden;">
              <div style="width:${lease.progressPercent}%; height:100%; background:var(--gradient-bolt-transport);"></div>
            </div>
            <div style="font-size:12px; color:var(--text-secondary); margin-top:6px;">
              Remaining Balance: ${Engine.formatTzs(lease.remainingTzs)}
            </div>
          </div>

          ${
            lease.ownershipTransferred
              ? `<div style="background:var(--status-safe-soft); border:1.5px solid var(--status-safe-green); border-radius:14px; padding:14px; margin-bottom:14px;">
                  <strong style="color:#065F46;">🎉 CERTIFICATE OF PERMANENT OWNERSHIP TRANSFERRED!</strong>
                  <p style="font-size:13px; color:var(--text-primary); margin-top:4px;">
                    Urban Production hereby transfers permanent title of vehicle ${escapeHtml(c.vehiclePlate)} to ${escapeHtml(c.driverName)}.
                  </p>
                </div>`
              : `<div style="display:flex; gap:10px;">
                  <button class="btn btn-primary" id="drv-pay-1w-btn" style="flex:1;">Pay +1 Week (+TSh 70,000)</button>
                  <button class="btn btn-outline" id="drv-pay-26w-btn" style="flex:1;">Pay +26 Weeks (6 Months)</button>
                </div>`
          }
        </div>
      </div>
    `;

    document.getElementById('drv-checkout-btn')?.addEventListener('click', () => {
      c.vehicleStatus = 'ACTIVE_PACKAGE_DELIVERY';
      c.cargoBoxAttached = true;
      c.rideHailPartner = 'NONE';
      c.curfewViolated = false;
      c.lastCheckedOutLabel = '06:15 EAT (Verified)';
      showToast('Checked Out at Station · Mode A Package Delivery');
      renderDriverPortal(container);
    });

    document.getElementById('drv-checkin-btn')?.addEventListener('click', () => {
      c.vehicleStatus = 'STATION_CHECKED_IN';
      c.cargoBoxAttached = false;
      c.rideHailPartner = 'NONE';
      c.curfewViolated = false;
      c.lastCheckedInLabel = '19:30 EAT (Curfew Cleared)';
      showToast('Vehicle Checked In at Station · Curfew Cleared');
      renderDriverPortal(container);
    });

    document.getElementById('drv-curfew-btn')?.addEventListener('click', () => {
      c.vehicleStatus = 'CURFEW_BREACH_STOLEN_ALERT';
      c.curfewViolated = true;
      showToast('21:00 CURFEW BREACH: Covert GPS polling at 1Hz!');
      renderDriverPortal(container);
    });

    document.getElementById('drv-toggle-box-btn')?.addEventListener('click', () => {
      c.cargoBoxAttached = !c.cargoBoxAttached;
      c.vehicleStatus = c.cargoBoxAttached ? 'ACTIVE_PACKAGE_DELIVERY' : 'ACTIVE_PASSENGER_IDLE_MODE';
      renderDriverPortal(container);
    });

    container.querySelectorAll('[data-partner]').forEach((btn) => {
      btn.addEventListener('click', () => {
        c.rideHailPartner = btn.getAttribute('data-partner');
        c.cargoBoxAttached = false;
        c.vehicleStatus = 'ACTIVE_PASSENGER_IDLE_MODE';
        showToast(`Switched to Idle Passenger Mode (${c.rideHailPartner}) · Background GPS ON`);
        renderDriverPortal(container);
      });
    });

    document.getElementById('drv-pay-1w-btn')?.addEventListener('click', () => {
      const updated = Engine.calculateLeaseRemittance(c, 1);
      c.weeksPaidCount = updated.weeksPaidCount;
      c.totalPaidTzs = updated.totalPaidTzs;
      c.ownershipTransferred = updated.ownershipTransferred;
      showToast(`Recorded +1 week payment (${c.weeksPaidCount}/104 weeks)`);
      renderDriverPortal(container);
    });

    document.getElementById('drv-pay-26w-btn')?.addEventListener('click', () => {
      const updated = Engine.calculateLeaseRemittance(c, 26);
      c.weeksPaidCount = updated.weeksPaidCount;
      c.totalPaidTzs = updated.totalPaidTzs;
      c.ownershipTransferred = updated.ownershipTransferred;
      showToast(`Recorded +26 weeks payment (${c.weeksPaidCount}/104 weeks)`);
      renderDriverPortal(container);
    });
  }

  function renderTelemetryPortal(container) {
    const activeOrder = state.orders[0];

    container.innerHTML = `
      <div class="bolt-grid">
        <!-- Left Column: Live Kalman-Smoothed Courier Map -->
        <div class="bolt-panel">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
            <div>
              <h3 style="font-size:18px; font-weight:800;">📍 Dar es Salaam Live Courier &amp; Bolt Telemetry Grid</h3>
              <p style="font-size:12.5px; color:var(--text-secondary);">
                ${activeOrder ? `${escapeHtml(activeOrder.pickupLabel)} → ${escapeHtml(activeOrder.dropoffLabel)}` : 'Standby Fleet Mode'}
              </p>
            </div>
            <span class="badge-pill badge-moto">60 FPS Kalman Spline</span>
          </div>

          <canvas id="telemetry-map-canvas" width="560" height="260"
                  style="width:100%; height:250px; background:#F1F5F9; border-radius:14px; border:1px solid var(--border-hairline);"></canvas>

          <div style="display:flex; justify-content:space-between; align-items:center; margin-top:12px;">
            <div>
              <strong style="font-size:13.5px;">Covert Anti-Theft Hardware GPS Overlay</strong>
              <div style="font-size:12px; color:var(--text-secondary);">IMEI-8642091100482 Dual-Tracker Parity</div>
            </div>
            <label style="display:flex; align-items:center; gap:6px; cursor:pointer; font-weight:700; font-size:13px;">
              <input type="checkbox" id="toggle-covert-gps" ${state.showSecretHardwareGps ? 'checked' : ''} />
              Show Covert GPS
            </label>
          </div>
        </div>

        <!-- Right Column: Verbal Secret Safety Code & Buyer Protection Claims -->
        <div class="bolt-panel">
          ${
            activeOrder
              ? `
            <div class="safety-code-box" style="margin-bottom:14px;">
              <div style="font-size:12px; font-weight:800; color:var(--brand-primary-dark); letter-spacing:0.06em;">
                🔒 BUYER VERBAL SECRET SAFETY CODE
              </div>
              <div class="safety-code-mono" id="active-safety-code">${escapeHtml(activeOrder.secretSafetyCode)}</div>
              <p style="font-size:12.5px; color:var(--text-secondary);">
                Keep your gate closed until driver <strong>${escapeHtml(activeOrder.driverName)} (${escapeHtml(activeOrder.driverPlate)})</strong> pronounces this exact code aloud.
              </p>
            </div>

            <div style="background:var(--bg-surface-subtle); border-radius:12px; padding:12px; margin-bottom:14px; font-size:13.5px;">
              <div><strong>Order / Dispatch:</strong> ${escapeHtml(activeOrder.productTitle)}</div>
              <div><strong>Receipt:</strong> <code>${escapeHtml(activeOrder.receiptNumber)}</code></div>
              <div><strong>Status:</strong> <span class="badge-pill badge-category">${escapeHtml(activeOrder.status)}</span></div>
              <div><strong>Total Payable:</strong> <strong>${Engine.formatTzs(activeOrder.totalAmountTzs)}</strong></div>
              ${
                activeOrder.refundAmountTzs > 0
                  ? `<div style="color:#059669; font-weight:800; margin-top:4px;">
                      Refund Issued: ${Engine.formatTzs(activeOrder.refundAmountTzs)}
                      ${activeOrder.disturbanceCompTzs > 0 ? ` + ${Engine.formatTzs(activeOrder.disturbanceCompTzs)} Disturbance Compensation` : ''}
                    </div>`
                  : ''
              }
            </div>

            <div style="display:flex; flex-direction:column; gap:8px;">
              <button class="btn btn-safe" id="confirm-receipt-btn" style="width:100%;">
                ✓ Confirm Secret Code Match &amp; Safe Receipt
              </button>
              <button class="btn btn-outline" id="claim-rule-a-btn" style="width:100%; border-color:var(--status-warning); color:#9A3412;">
                ⚠️ Rule A (Damaged / Lost): 100% Refund + TSh 1,000 Disturbance Comp
              </button>
              <button class="btn btn-danger" id="claim-rule-b-btn" style="width:100%;">
                🛡️ Rule B (Robbery / Harm): 100% Refund + Freeze Route
              </button>
            </div>
          `
              : '<p>No active orders. Place an order or dispatch a Bolt transport vehicle to view live telemetry.</p>'
          }
        </div>
      </div>
    `;

    drawTelemetryMapCanvas();

    document.getElementById('toggle-covert-gps')?.addEventListener('change', (e) => {
      state.showSecretHardwareGps = e.target.checked;
      drawTelemetryMapCanvas();
    });

    document.getElementById('confirm-receipt-btn')?.addEventListener('click', () => {
      if (!activeOrder) return;
      activeOrder.status = 'DELIVERED_COMPLETED';
      showToast(`Safe Receipt Confirmed for ${activeOrder.receiptNumber}!`);
      renderTelemetryPortal(container);
    });

    document.getElementById('claim-rule-a-btn')?.addEventListener('click', () => {
      if (!activeOrder) return;
      const claim = Engine.calculateRefundPayout(activeOrder, 'DAMAGED_OR_LOST');
      activeOrder.status = claim.status;
      activeOrder.refundAmountTzs = claim.refundAmountTzs;
      activeOrder.disturbanceCompTzs = claim.disturbanceCompTzs;
      showToast(`Rule A Approved: ${Engine.formatTzs(claim.totalBuyerPayoutTzs)} (100% + TSh 1,000)`);
      renderTelemetryPortal(container);
    });

    document.getElementById('claim-rule-b-btn')?.addEventListener('click', () => {
      if (!activeOrder) return;
      const claim = Engine.calculateRefundPayout(activeOrder, 'ROBBERY_OR_HARM');
      activeOrder.status = claim.status;
      activeOrder.refundAmountTzs = claim.refundAmountTzs;
      activeOrder.disturbanceCompTzs = claim.disturbanceCompTzs;
      showToast(`Rule B Approved: 100% Refund (${Engine.formatTzs(claim.totalBuyerPayoutTzs)}) & Route Frozen`);
      renderTelemetryPortal(container);
    });
  }

  function drawTelemetryMapCanvas() {
    const canvas = document.getElementById('telemetry-map-canvas');
    if (!canvas || !canvas.getContext) return;
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;
    ctx.clearRect(0, 0, w, h);

    // Subtle daylight grid
    ctx.strokeStyle = '#E2E8F0';
    ctx.lineWidth = 1.5;
    for (let i = 1; i <= 6; i++) {
      ctx.beginPath();
      ctx.moveTo(0, (h * i) / 7);
      ctx.lineTo(w, (h * i) / 7);
      ctx.stroke();

      ctx.beginPath();
      ctx.moveTo((w * i) / 7, 0);
      ctx.lineTo((w * i) / 7, h);
      ctx.stroke();
    }

    const pts = [
      { x: w * 0.14, y: h * 0.8 },
      { x: w * 0.38, y: h * 0.58 },
      { x: w * 0.64, y: h * 0.42 },
      { x: w * 0.85, y: h * 0.22 },
    ];

    // Route halo
    ctx.strokeStyle = '#E0F2FE';
    ctx.lineWidth = 14;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    pts.slice(1).forEach((p) => ctx.lineTo(p.x, p.y));
    ctx.stroke();

    // Dashed centerline
    ctx.strokeStyle = '#0284C7';
    ctx.lineWidth = 4;
    ctx.setLineDash([12, 8]);
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    pts.slice(1).forEach((p) => ctx.lineTo(p.x, p.y));
    ctx.stroke();
    ctx.setLineDash([]);

    // Origin Hub Pin
    ctx.fillStyle = '#F59E0B';
    ctx.beginPath();
    ctx.arc(pts[0].x, pts[0].y, 11, 0, Math.PI * 2);
    ctx.fill();

    // Buyer Gate Pin
    ctx.fillStyle = '#10B981';
    ctx.beginPath();
    ctx.arc(pts[3].x, pts[3].y, 12, 0, Math.PI * 2);
    ctx.fill();

    // Interpolated Courier Position
    const pos = Engine.interpolateRoutePosition(pts, 0.68);
    if (state.showSecretHardwareGps) {
      ctx.fillStyle = 'rgba(239, 68, 68, 0.25)';
      ctx.beginPath();
      ctx.arc(pos.x, pos.y, 26, 0, Math.PI * 2);
      ctx.fill();
    }

    ctx.fillStyle = '#0F172A';
    ctx.beginPath();
    ctx.arc(pos.x, pos.y, 14, 0, Math.PI * 2);
    ctx.fill();
  }

  function renderAll() {
    renderHeader();
    const workspace = document.getElementById('portal-workspace');
    if (!workspace) return;

    switch (state.activePortal) {
      case 'COMPARE':
        renderComparePortal(workspace);
        break;
      case 'BOLT_TRANSPORT':
        renderBoltTransportPortal(workspace);
        break;
      case 'SELLER':
        renderSellerPortal(workspace);
        break;
      case 'DRIVER':
        renderDriverPortal(workspace);
        break;
      case 'TELEMETRY':
        renderTelemetryPortal(workspace);
        break;
      case 'MARKETPLACE':
      default:
        renderMarketplacePortal(workspace);
        break;
    }
  }

  function initApp() {
    const searchInput = document.getElementById('global-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        state.searchQuery = e.target.value;
        if (state.activePortal !== 'MARKETPLACE') {
          state.activePortal = 'MARKETPLACE';
        }
        renderAll();
      });
    }

    document.getElementById('brand-home-link')?.addEventListener('click', () => {
      switchPortal('MARKETPLACE');
    });

    document.getElementById('header-bolt-btn')?.addEventListener('click', () => {
      switchPortal('BOLT_TRANSPORT');
    });

    document.getElementById('header-compare-btn')?.addEventListener('click', () => {
      switchPortal('COMPARE');
    });

    document.getElementById('header-cart-btn')?.addEventListener('click', () => {
      if (state.cartItems.length === 0) {
        showToast('Cart is empty — click "+ Cart" on any product to add items');
      } else {
        showToast(`${state.cartItems.length} item(s) in cart — ready for checkout below`);
      }
      switchPortal('MARKETPLACE');
    });

    document.getElementById('floating-map-btn')?.addEventListener('click', () => {
      switchPortal('TELEMETRY');
    });

    renderAll();
  }

  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', initApp);
    } else {
      initApp();
    }
  }
})();
