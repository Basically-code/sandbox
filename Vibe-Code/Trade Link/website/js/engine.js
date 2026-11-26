/**
 * Tradee (TradeLink) — Pure Deterministic Domain, Price Comparison & Bolt Logistics Engine
 */
(function (root) {
  'use strict';

  const MOTORCYCLE_MAX_WEIGHT_KG = 35.0;
  const BASE_MOTORCYCLE_FEE_TZS = 2500.0;
  const BASE_TRUCK_FEE_TZS = 8000.0;
  const PER_KM_FEE_TZS = 500.0;
  const WEEKLY_LEASE_FEE_TZS = 70000;
  const TOTAL_LEASE_WEEKS = 104;
  const RULE_A_DISTURBANCE_COMP_TZS = 1000.0;

  const PHONETIC_WORDS = ['SIMBA', 'KILIMA', 'ZANZIBAR', 'SERENGETI', 'DAR', 'NYOTA', 'AMANI'];

  function formatTzs(amount) {
    const safe = Number.isFinite(Number(amount)) ? Math.round(Number(amount)) : 0;
    return 'TSh ' + safe.toLocaleString('en-US');
  }

  function computeTransportFeeTzs(requiresTruck, distanceKm) {
    const clampedKm = Math.max(1.0, Number(distanceKm) || 1.0);
    const base = requiresTruck ? BASE_TRUCK_FEE_TZS : BASE_MOTORCYCLE_FEE_TZS;
    return base + clampedKm * PER_KM_FEE_TZS;
  }

  function computeBoltTierQuote(tier, distanceKm, weightKg) {
    const clampedKm = Math.max(1.0, Number(distanceKm) || 1.0);
    const safeWeight = Math.max(0.1, Number(weightKg) || 0.5);
    const isEligible = safeWeight <= tier.maxWeightKg;
    const fareTzs = tier.baseFeeTzs + clampedKm * tier.perKmRateTzs;
    const estimatedMinutes = Math.round(tier.etaMinutes + clampedKm * 1.6);
    return {
      tierKey: tier.key,
      name: tier.name,
      subtitle: tier.subtitle,
      icon: tier.icon,
      badge: tier.badge,
      isEligible,
      requiresTruck: Boolean(tier.requiresTruck),
      maxWeightKg: tier.maxWeightKg,
      distanceKm: clampedKm,
      weightKg: safeWeight,
      fareTzs,
      estimatedMinutes,
      ineligibleReason: isEligible ? null : `Exceeds ${tier.maxWeightKg} kg limit`,
    };
  }

  function compareAllBoltTiers(tiers, distanceKm, weightKg) {
    const safeWeight = Math.max(0.1, Number(weightKg) || 0.5);
    const quotes = (tiers || []).map((t) => computeBoltTierQuote(t, distanceKm, safeWeight));
    let idealKey = 'TRADEE_TRUCK';
    if (safeWeight <= 3.0) {
      idealKey = 'TRADEE_BODA';
    } else if (safeWeight <= 35.0) {
      idealKey = 'TRADEE_MOTO_BOX';
    } else if (safeWeight <= 250.0) {
      idealKey = 'TRADEE_BAJAJI';
    }
    const eligible = quotes.filter((q) => q.isEligible);
    const idealEligible = eligible.find((q) => q.tierKey === idealKey);
    const recommendedKey = idealEligible
      ? idealEligible.tierKey
      : eligible.length > 0
      ? eligible.reduce((best, cur) => (cur.fareTzs < best.fareTzs ? cur : best), eligible[0]).tierKey
      : 'TRADEE_TRUCK';
    return quotes.map((q) => ({
      ...q,
      isRecommended: q.tierKey === recommendedKey,
    }));
  }

  function estimateHubDistanceKm(pickupLabel, buyerBaseDistanceKm, offerHubDistanceKm) {
    const base = Math.max(1.0, Number(buyerBaseDistanceKm) || 4.0);
    const scale = base / 4.0;
    if (Number.isFinite(Number(offerHubDistanceKm)) && Number(offerHubDistanceKm) > 0) {
      return Math.max(1.0, Math.round(Number(offerHubDistanceKm) * scale * 10) / 10);
    }
    const lower = String(pickupLabel || '').toLowerCase();
    let hubDelta = 0.5;
    if (lower.includes('kariakoo')) hubDelta = 0.0;
    else if (lower.includes('samora') || lower.includes('posta')) hubDelta = 0.8;
    else if (lower.includes('uhuru') || lower.includes('kinondoni')) hubDelta = 1.4;
    else if (lower.includes('mwenge')) hubDelta = 2.2;
    else if (lower.includes('mlimani')) hubDelta = 3.2;
    else if (lower.includes('slipway') || lower.includes('masaki')) hubDelta = 4.5;
    else if (lower.includes('nyerere') || lower.includes('warehouse')) hubDelta = 5.0;
    return Math.max(1.0, Math.round((base + hubDelta) * 10) / 10);
  }

  function normalizeComparisonKey(product) {
    if (!product) return '';
    if (typeof product === 'string') {
      return product.trim().toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
    }
    if (product.comparisonGroup && String(product.comparisonGroup).trim()) {
      return String(product.comparisonGroup).trim().toLowerCase();
    }
    return String(product.title || '')
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  /**
   * Compares all active offers matching a product's comparison group or normalized title,
   * calculating both Unit Item Price and Total Landed Cost (Item + Transport Fee).
   */
  function compareProductOffers(allProducts, targetProduct, buyerDistanceKm) {
    if (!Array.isArray(allProducts) || allProducts.length === 0) return [];
    const resolvedTarget = targetProduct || allProducts[0];
    if (!resolvedTarget) return [];
    const key = normalizeComparisonKey(resolvedTarget);
    const userKm = Math.max(1.0, Number(buyerDistanceKm) || 4.0);

    const matching = allProducts.filter(
      (p) => p && p.isActive !== false && normalizeComparisonKey(p) === key
    );
    const pool = matching.length > 0 ? matching : [resolvedTarget];

    const enriched = pool.map((offer) => {
      // Combine seller hub distance factor with buyer's selected distance
      const effectiveKm = estimateHubDistanceKm(offer.pickupLabel, userKm, offer.hubDistanceKm);
      const transportFeeTzs = computeTransportFeeTzs(Boolean(offer.requiresTruck), effectiveKm);
      const landedTotalTzs = Number(offer.priceTzs) + transportFeeTzs;
      return {
        productId: offer.id,
        title: offer.title,
        category: offer.category,
        providerId: offer.providerId,
        providerName: offer.providerName || 'Verified Seller',
        sellerRating: Number(offer.sellerRating || 4.8),
        pickupLabel: offer.pickupLabel || 'Kariakoo Market Block A',
        effectiveDistanceKm: effectiveKm,
        sizeSpec: offer.sizeSpec || 'Standard',
        condition: offer.condition || 'Brand New',
        quantityAvailable: Number(offer.quantityAvailable || 0),
        requiresTruck: Boolean(offer.requiresTruck),
        unitPriceTzs: Number(offer.priceTzs),
        transportFeeTzs,
        landedTotalTzs,
        rawProduct: offer,
      };
    });

    enriched.sort((a, b) => {
      if (a.landedTotalTzs !== b.landedTotalTzs) return a.landedTotalTzs - b.landedTotalTzs;
      return a.unitPriceTzs - b.unitPriceTzs;
    });

    const minUnitPrice = Math.min(...enriched.map((e) => e.unitPriceTzs));
    const minLandedTotal = Math.min(...enriched.map((e) => e.landedTotalTzs));
    const maxLandedTotal = Math.max(...enriched.map((e) => e.landedTotalTzs));

    return enriched.map((item, idx) => ({
      ...item,
      rank: idx + 1,
      isBestLandedValue: item.landedTotalTzs === minLandedTotal,
      isLowestStickerPrice: item.unitPriceTzs === minUnitPrice,
      savingsVsHighestTzs: Math.max(0, maxLandedTotal - item.landedTotalTzs),
    }));
  }

  /**
   * Builds a summary of all multi-seller comparison groups across the marketplace.
   */
  function buildPriceComparisonMatrix(allProducts, buyerDistanceKm) {
    if (!Array.isArray(allProducts) || allProducts.length === 0) return [];
    const seenKeys = new Set();
    const groups = [];

    for (const prod of allProducts) {
      if (!prod || prod.isActive === false) continue;
      const key = normalizeComparisonKey(prod);
      if (seenKeys.has(key)) continue;
      seenKeys.add(key);

      const offers = compareProductOffers(allProducts, prod, buyerDistanceKm);
      const lowestSticker = Math.min(...offers.map((o) => o.unitPriceTzs));
      const highestSticker = Math.max(...offers.map((o) => o.unitPriceTzs));
      const bestLanded = offers[0];

      groups.push({
        groupKey: key,
        title: prod.title,
        category: prod.category,
        sellerCount: offers.length,
        lowestStickerTzs: lowestSticker,
        highestStickerTzs: highestSticker,
        priceSpreadTzs: Math.max(0, highestSticker - lowestSticker),
        bestLandedOffer: bestLanded,
        offers,
      });
    }

    // Prioritize items with multiple competing sellers first
    groups.sort((a, b) => {
      if (b.sellerCount !== a.sellerCount) return b.sellerCount - a.sellerCount;
      return b.priceSpreadTzs - a.priceSpreadTzs;
    });

    return groups;
  }

  function filterAndSortProducts(products, options) {
    if (!Array.isArray(products)) return [];
    const opts = options || {};
    const q = String(opts.searchQuery || '').trim().toLowerCase();
    const catKey = opts.categoryKey || null;
    const dealsOnly = Boolean(opts.dealsOnly);
    const favoritesOnly = Boolean(opts.favoritesOnly);
    const favoriteIds = opts.favoriteIds instanceof Set ? opts.favoriteIds : new Set(opts.favoriteIds || []);
    const sortBy = opts.sortBy || 'NEWEST_FIRST';

    const filtered = products.filter((p) => {
      if (!p || p.isActive === false) return false;
      const matchesCat = !catKey || p.category === catKey;
      const matchesDeal = !dealsOnly || Boolean(p.isDailyDeal);
      const matchesFav = !favoritesOnly || favoriteIds.has(p.id);
      const matchesQuery =
        !q ||
        String(p.title || '').toLowerCase().includes(q) ||
        String(p.description || '').toLowerCase().includes(q) ||
        String(p.category || '').toLowerCase().includes(q) ||
        String(p.providerName || '').toLowerCase().includes(q) ||
        String(p.pickupLabel || '').toLowerCase().includes(q);
      return matchesCat && matchesDeal && matchesFav && matchesQuery;
    });

    filtered.sort((a, b) => {
      if (sortBy === 'PRICE_LOW_TO_HIGH') {
        if (a.priceTzs !== b.priceTzs) return a.priceTzs - b.priceTzs;
      } else if (sortBy === 'PRICE_HIGH_TO_LOW') {
        if (b.priceTzs !== a.priceTzs) return b.priceTzs - a.priceTzs;
      }
      return (b.createdAtSec || 0) - (a.createdAtSec || 0);
    });

    return filtered;
  }

  function calculateCartTotals(items, distanceKm) {
    if (!Array.isArray(items) || items.length === 0) {
      return {
        totalQuantity: 0,
        itemsSubtotalTzs: 0.0,
        transportFeeTzs: 0.0,
        grandTotalTzs: 0.0,
      };
    }
    const clampedKm = Math.max(1.0, Number(distanceKm) || 4.0);
    let totalQuantity = 0;
    let itemsSubtotalTzs = 0.0;
    let transportFeeTzs = 0.0;

    items.forEach((item, index) => {
      const qty = Math.max(1, Number(item.quantity) || 1);
      totalQuantity += qty;
      itemsSubtotalTzs += Number(item.priceTzs || 0) * qty;
      transportFeeTzs += computeTransportFeeTzs(
        Boolean(item.requiresTruck),
        index === 0 ? clampedKm : 1.0
      );
    });

    return {
      totalQuantity,
      itemsSubtotalTzs,
      transportFeeTzs,
      grandTotalTzs: itemsSubtotalTzs + transportFeeTzs,
    };
  }

  function generateSecretSafetyCode() {
    const fourDigits = 1000 + Math.floor(Math.random() * 9000);
    const word = PHONETIC_WORDS[Math.floor(Math.random() * PHONETIC_WORDS.length)];
    const tailDigit = 1 + Math.floor(Math.random() * 9);
    return `${fourDigits} — "${word}-${tailDigit}"`;
  }

  function generateReceiptNumber() {
    const now = new Date();
    const y = now.getUTCFullYear();
    const m = String(now.getUTCMonth() + 1).padStart(2, '0');
    const d = String(now.getUTCDate()).padStart(2, '0');
    const suffix = 10000 + Math.floor(Math.random() * 90000);
    return `TRD-${y}${m}${d}-${suffix}`;
  }

  function calculateRefundPayout(order, claimType) {
    const baseTotal = Number(order && order.totalAmountTzs) || 0;
    if (claimType === 'DAMAGED_OR_LOST') {
      return {
        status: 'DAMAGED_OR_LOST_REFUNDED',
        refundAmountTzs: baseTotal,
        disturbanceCompTzs: RULE_A_DISTURBANCE_COMP_TZS,
        totalBuyerPayoutTzs: baseTotal + RULE_A_DISTURBANCE_COMP_TZS,
      };
    }
    return {
      status: 'ROBBERY_OR_HARM_REFUNDED',
      refundAmountTzs: baseTotal,
      disturbanceCompTzs: 0.0,
      totalBuyerPayoutTzs: baseTotal,
    };
  }

  function calculateLeaseRemittance(contract, weeksToAdd) {
    const currentWeeks = Number(contract && contract.weeksPaidCount) || 0;
    const add = Math.max(0, Number(weeksToAdd) || 0);
    const nextWeeks = Math.min(TOTAL_LEASE_WEEKS, currentWeeks + add);
    const nextTotalPaidTzs = nextWeeks * WEEKLY_LEASE_FEE_TZS;
    const ownershipTransferred = nextWeeks >= TOTAL_LEASE_WEEKS;
    return {
      weeksPaidCount: nextWeeks,
      totalWeeksRequired: TOTAL_LEASE_WEEKS,
      weeklyFeeTzs: WEEKLY_LEASE_FEE_TZS,
      totalPaidTzs: nextTotalPaidTzs,
      remainingTzs: Math.max(0, TOTAL_LEASE_WEEKS * WEEKLY_LEASE_FEE_TZS - nextTotalPaidTzs),
      progressPercent: Math.round((nextWeeks / TOTAL_LEASE_WEEKS) * 100),
      ownershipTransferred,
    };
  }

  function computeCategoryPriceBenchmark(allProducts, categoryKey) {
    if (!Array.isArray(allProducts) || allProducts.length === 0) {
      return { count: 0, lowestTzs: 0, averageTzs: 0, highestTzs: 0 };
    }
    const matching = allProducts.filter(
      (p) => p && p.isActive !== false && (!categoryKey || p.category === categoryKey) && Number(p.priceTzs) > 0
    );
    if (matching.length === 0) {
      return { count: 0, lowestTzs: 0, averageTzs: 0, highestTzs: 0 };
    }
    const prices = matching.map((p) => Number(p.priceTzs));
    const lowestTzs = Math.min(...prices);
    const highestTzs = Math.max(...prices);
    const averageTzs = Math.round(prices.reduce((sum, v) => sum + v, 0) / prices.length);
    return {
      count: matching.length,
      lowestTzs,
      averageTzs,
      highestTzs,
    };
  }

  function interpolateRoutePosition(points, normalizedProgress) {
    if (!Array.isArray(points) || points.length === 0) {
      return { x: 0, y: 0, bearingDeg: 0 };
    }
    if (points.length === 1) {
      return { x: points[0].x, y: points[0].y, bearingDeg: 0 };
    }
    const p = Math.max(0, Math.min(1, Number(normalizedProgress) || 0));
    const scaled = p * (points.length - 1);
    const seg = Math.min(points.length - 2, Math.max(0, Math.floor(scaled)));
    const localT = scaled - seg;
    const start = points[seg];
    const end = points[seg + 1];
    const x = start.x + (end.x - start.x) * localT;
    const y = start.y + (end.y - start.y) * localT;
    const bearingDeg = (Math.atan2(end.y - start.y, end.x - start.x) * 180) / Math.PI;
    return { x, y, bearingDeg };
  }

  const TradeeEngine = {
    MOTORCYCLE_MAX_WEIGHT_KG,
    BASE_MOTORCYCLE_FEE_TZS,
    BASE_TRUCK_FEE_TZS,
    PER_KM_FEE_TZS,
    WEEKLY_LEASE_FEE_TZS,
    TOTAL_LEASE_WEEKS,
    RULE_A_DISTURBANCE_COMP_TZS,
    formatTzs,
    computeTransportFeeTzs,
    computeBoltTierQuote,
    compareAllBoltTiers,
    estimateHubDistanceKm,
    normalizeComparisonKey,
    compareProductOffers,
    buildPriceComparisonMatrix,
    buildPriceComparisonGroups: buildPriceComparisonMatrix,
    computeCategoryPriceBenchmark,
    filterAndSortProducts,
    calculateCartTotals,
    generateSecretSafetyCode,
    generateReceiptNumber,
    calculateRefundPayout,
    calculateRefund: calculateRefundPayout,
    calculateLeaseRemittance,
    calculateLeaseProgress: calculateLeaseRemittance,
    interpolateRoutePosition,
    interpolateKalmanPosition: interpolateRoutePosition,
  };

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = TradeeEngine;
  }
  root.TradeeEngine = TradeeEngine;
})(typeof window !== 'undefined' ? window : globalThis);
