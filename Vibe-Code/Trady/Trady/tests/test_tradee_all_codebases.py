import functools
import http.server
import json
import os
import re
import socketserver
import subprocess
import tempfile
import threading
import unittest
import urllib.request
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
WEBSITE_DIR = ROOT_DIR / "website"
TRADY_DIR = ROOT_DIR / "Trady"
ANDROID_DIR = ROOT_DIR / "android-app"
IOS_DIR = ROOT_DIR / "ios-app"

KOTLINC_BAT = Path(r"C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\bin\kotlinc.bat")
KOTLIN_COMPILER_JAR = Path(r"C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\lib\kotlin-compiler.jar")
JUNIT_JAR = Path(r"C:\Program Files\Android\Android Studio\plugins\junit\lib\junit.jar")
KT_TEST_JUNIT_JAR = Path(r"C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\lib\kotlin-test-junit.jar")
KT_TEST_JAR = Path(r"C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\lib\kotlin-test.jar")
KT_STDLIB_JAR = Path(r"C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\lib\kotlin-stdlib.jar")
MSEDGE_EXE = Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe")


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, format, *args):
        pass


class TestTradeeAllCodebases(unittest.TestCase):

    def test_01_plan_and_two_revisits_documented(self):
        plan_file = ROOT_DIR / "TRADEE_REDESIGN_PLAN_AND_FEASIBILITY.md"
        blueprint_file = ROOT_DIR / "trade_link_master_blueprint_engineering_specification.md"
        self.assertTrue(plan_file.exists(), "Redesign plan file must exist")
        self.assertTrue(blueprint_file.exists(), "Master blueprint file must exist")

        plan_upper = plan_file.read_text(encoding="utf-8").upper()
        self.assertIn("PASS 0", plan_upper)
        self.assertIn("REVISIT #1", plan_upper)
        self.assertIn("REVISIT #2", plan_upper)
        self.assertIn("LANDED COST", plan_upper)
        self.assertIn("BOLT", plan_upper)

    def test_02_professional_logo_assets_across_all_three_codebases(self):
        jpg_paths = [
            WEBSITE_DIR / "assets" / "tradee-logo.jpg",
            TRADY_DIR / "app" / "src" / "main" / "res" / "drawable" / "ic_trady_foreground_1794558910635.jpg",
            ANDROID_DIR / "app" / "src" / "main" / "res" / "drawable" / "ic_trady_foreground_1794558910635.jpg",
            IOS_DIR / "Sources" / "TradeeIOS" / "Resources" / "tradee-logo.jpg",
        ]
        for path in jpg_paths:
            self.assertTrue(path.exists(), f"Missing logo JPG at {path}")
            raw = path.read_bytes()
            self.assertGreater(len(raw), 10_000, f"Logo file too small at {path}")
            self.assertEqual(raw[:3], b"\xff\xd8\xff", f"Invalid JPEG magic bytes at {path}")

        svg_path = WEBSITE_DIR / "assets" / "tradee-logo.svg"
        self.assertTrue(svg_path.exists(), "Website SVG logo must exist")
        svg_text = svg_path.read_text(encoding="utf-8")
        self.assertIn("<svg", svg_text)
        self.assertIn("linearGradient", svg_text)

    def test_03_website_live_http_server_and_assets(self):
        handler = functools.partial(QuietHandler, directory=str(WEBSITE_DIR))
        server = socketserver.TCPServer(("127.0.0.1", 0), handler)
        port = server.server_address[1]
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            endpoints = [
                "/index.html",
                "/styles/tradee.css",
                "/js/data.js",
                "/js/engine.js",
                "/js/app.js",
                "/assets/tradee-logo.svg",
                "/assets/tradee-logo.jpg",
            ]
            for ep in endpoints:
                url = f"http://127.0.0.1:{port}{ep}"
                with urllib.request.urlopen(url, timeout=5) as resp:
                    self.assertEqual(resp.status, 200, f"HTTP GET {ep} failed")
                    body = resp.read()
                    self.assertGreater(len(body), 100, f"Empty response for {ep}")
        finally:
            server.shutdown()
            server.server_close()

    def test_04_website_gradients_and_engine_completeness(self):
        css_text = (WEBSITE_DIR / "styles" / "tradee.css").read_text(encoding="utf-8")
        for token in [
            "--gradient-brand-primary",
            "--gradient-deal-warm",
            "--gradient-bolt-transport",
            "--gradient-safe-emerald",
            "--gradient-card-sheen",
            "--gradient-hero-banner",
        ]:
            self.assertIn(token, css_text, f"Missing CSS gradient token {token}")

        engine_text = (WEBSITE_DIR / "js" / "engine.js").read_text(encoding="utf-8")
        for fn in [
            "computeTransportFeeTzs",
            "computeBoltTierQuote",
            "compareAllBoltTiers",
            "compareProductOffers",
            "buildPriceComparisonGroups",
            "calculateCartTotals",
            "calculateRefund",
            "calculateLeaseProgress",
            "interpolateKalmanPosition",
        ]:
            self.assertIn(fn, engine_text, f"Missing JS engine function {fn}")

        data_text = (WEBSITE_DIR / "js" / "data.js").read_text(encoding="utf-8")
        for cat in [
            "SPORTS_AND_OUTDOORS",
            "ELECTRONICS",
            "FOOD",
            "BOOKS",
            "CLOTHING",
            "SHOES",
            "JEWELLERY",
            "BEAUTY_AND_PERSONAL_CARE",
        ]:
            self.assertIn(cat, data_text, f"Missing category {cat} in website data.js")

        for tier in [
            "TRADEE_BODA",
            "TRADEE_MOTO_BOX",
            "TRADEE_BAJAJI",
            "TRADEE_TRUCK",
        ]:
            self.assertIn(tier, data_text, f"Missing transport tier {tier} in website data.js")

    def test_05_android_codebases_structure_and_syntax_balance(self):
        for base in [TRADY_DIR, ANDROID_DIR]:
            self.assertTrue(base.exists(), f"Missing Android directory {base}")
            kt_files = [
                base / "app/src/main/java/com/example/ui/theme/Color.kt",
                base / "app/src/main/java/com/example/ui/components/TradeLinkComponents.kt",
                base / "app/src/main/java/com/example/data/model/TradeLinkModels.kt",
                base / "app/src/main/java/com/example/data/repository/TradeLinkRepository.kt",
                base / "app/src/main/java/com/example/ui/screens/PriceCompareAndBoltScreen.kt",
                base / "app/src/main/java/com/example/ui/screens/MarketplaceScreen.kt",
                base / "app/src/main/java/com/example/ui/screens/ProductDetailScreen.kt",
                base / "app/src/main/java/com/example/ui/screens/ProviderPortalScreen.kt",
                base / "app/src/main/java/com/example/ui/viewmodel/TradeLinkViewModel.kt",
                base / "app/src/main/java/com/example/MainActivity.kt",
                base / "app/src/test/java/com/example/TradeLinkDomainUnitTest.kt",
            ]
            for kt in kt_files:
                self.assertTrue(kt.exists(), f"Missing Kotlin file {kt}")
                content = kt.read_text(encoding="utf-8")
                self.assertEqual(
                    content.count("{"),
                    content.count("}"),
                    f"Unbalanced curly braces in {kt}",
                )

            repo_text = (base / "app/src/main/java/com/example/data/repository/TradeLinkRepository.kt").read_text(encoding="utf-8")
            for symbol in [
                "compareProductOffers",
                "buildPriceComparisonGroups",
                "computeBoltTierQuote",
                "compareAllBoltTiers",
                "dispatchBoltTransport",
            ]:
                self.assertIn(symbol, repo_text, f"Missing {symbol} in {base.name} TradeLinkRepository.kt")

    def test_06_ios_codebase_structure_and_syntax_balance(self):
        self.assertTrue((IOS_DIR / "Package.swift").exists(), "Missing iOS Package.swift")
        swift_files = [
            IOS_DIR / "Sources/TradeeIOS/Theme/TradeeTheme.swift",
            IOS_DIR / "Sources/TradeeIOS/Models/TradeeModels.swift",
            IOS_DIR / "Sources/TradeeIOS/Engine/TradeeEngine.swift",
            IOS_DIR / "Sources/TradeeIOS/Views/MarketplaceView.swift",
            IOS_DIR / "Sources/TradeeIOS/Views/PriceCompareView.swift",
            IOS_DIR / "Sources/TradeeIOS/Views/BoltTransportView.swift",
            IOS_DIR / "Sources/TradeeIOS/Views/SellerPortalView.swift",
            IOS_DIR / "Sources/TradeeIOS/Views/DriverFleetView.swift",
            IOS_DIR / "Sources/TradeeIOS/Views/LiveTelemetryMapView.swift",
            IOS_DIR / "Sources/TradeeIOS/TradeeApp.swift",
            IOS_DIR / "Tests/TradeeIOSTests/TradeeEngineTests.swift",
        ]
        for sw in swift_files:
            self.assertTrue(sw.exists(), f"Missing Swift file {sw}")
            content = sw.read_text(encoding="utf-8")
            self.assertEqual(
                content.count("{"),
                content.count("}"),
                f"Unbalanced curly braces in {sw}",
            )

        engine_text = (IOS_DIR / "Sources/TradeeIOS/Engine/TradeeEngine.swift").read_text(encoding="utf-8")
        for fn in [
            "computeTransportFeeTzs",
            "compareProductOffers",
            "buildPriceComparisonGroups",
            "computeBoltTierQuote",
            "compareAllBoltTiers",
            "dispatchBoltTransport",
        ]:
            self.assertIn(fn, engine_text, f"Missing {fn} in iOS TradeeEngine.swift")

    def test_07_cross_platform_formula_parity_and_edge_cases(self):
        # Verify Website, Android, and iOS share identical numeric constants
        web_engine = (WEBSITE_DIR / "js/engine.js").read_text(encoding="utf-8")
        android_repo = (TRADY_DIR / "app/src/main/java/com/example/data/repository/TradeLinkRepository.kt").read_text(encoding="utf-8")
        ios_engine = (IOS_DIR / "Sources/TradeeIOS/Engine/TradeeEngine.swift").read_text(encoding="utf-8")

        # 35.0 kg threshold
        self.assertIn("35.0", web_engine)
        self.assertIn("35.0", android_repo)
        self.assertIn("35.0", ios_engine)

        # 70,000 TSh * 104 weeks = 7,280,000 TSh
        self.assertIn("70000", web_engine)
        self.assertIn("70_000", android_repo)
        self.assertIn("70_000", ios_engine)
        self.assertIn("104", web_engine)
        self.assertIn("104", android_repo)
        self.assertIn("104", ios_engine)

        # Verify truck fee formula parity (8,000 base + 500/km) and tier caps (15, 35, 250, 5000 kg)
        ios_models = (IOS_DIR / "Sources/TradeeIOS/Models/TradeeModels.swift").read_text(encoding="utf-8")
        android_models = (TRADY_DIR / "app/src/main/java/com/example/data/model/TradeLinkModels.kt").read_text(encoding="utf-8")
        self.assertIn("8_000.0 + km * 500.0", ios_engine)
        self.assertIn("case .TRADEE_BAJAJI: return 5_000.0", ios_models)
        self.assertIn("case .TRADEE_TRUCK: return 8_000.0", ios_models)
        self.assertIn("case .TRADEE_BAJAJI: return 250.0", ios_models)
        self.assertIn("case .TRADEE_TRUCK: return 5_000.0", ios_models)
        self.assertIn("computeCategoryPriceBenchmark", web_engine)
        self.assertIn("computeCategoryPriceBenchmark", android_repo)
        self.assertIn("computeCategoryPriceBenchmark", ios_engine)
        self.assertNotIn("tier.baseFareTzs", android_repo)
        self.assertIn("val baseFareTzs: Double", android_models)

        # Verify ProductDetailScreen forwards chosen competing offer instead of original product
        prod_detail = (TRADY_DIR / "app/src/main/java/com/example/ui/screens/ProductDetailScreen.kt").read_text(encoding="utf-8")
        self.assertIn("onPlaceOfferOrder(", prod_detail)
        self.assertIn("onAddOfferToCart(chosenProduct)", prod_detail)

    def test_08_kotlin_compiler_and_junit_execution_of_android_domain_tests(self):
        self.assertTrue(KOTLINC_BAT.exists(), f"kotlinc not found at {KOTLINC_BAT}")
        repo_text = (TRADY_DIR / "app/src/main/java/com/example/data/repository/TradeLinkRepository.kt").read_text(encoding="utf-8")
        comp_start = repo_text.index("  data class CartPriceSummary(")
        comp_end = repo_text.index("  private data class SampleSeed(")
        companion_body = repo_text[comp_start:comp_end]

        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            (tmp_path / "Timestamp.kt").write_text(
                "package com.google.firebase\ndata class Timestamp(val seconds: Long = 0L, val nanoseconds: Int = 0)\n",
                encoding="utf-8",
            )
            (tmp_path / "FirestoreAnnotations.kt").write_text(
                "package com.google.firebase.firestore\nannotation class IgnoreExtraProperties\nannotation class DocumentId\nannotation class ServerTimestamp\n",
                encoding="utf-8",
            )
            repo_stub = tmp_path / "TradeLinkRepositoryCompanion.kt"
            repo_stub.write_text(
                "package com.example.data.repository\n"
                "import com.example.data.model.*\n"
                "import java.security.SecureRandom\n"
                "import java.text.SimpleDateFormat\n"
                "import java.util.Date\n"
                "import java.util.Locale\n"
                "import java.util.TimeZone\n"
                f"class TradeLinkRepository {{\n{companion_body}\n}}\n",
                encoding="utf-8",
            )
            (tmp_path / "JUnitShim.kt").write_text(
                "package org.junit\n"
                "import kotlin.math.abs\n"
                "@Target(AnnotationTarget.FUNCTION)\n"
                "@Retention(AnnotationRetention.RUNTIME)\n"
                "annotation class Test\n"
                "object Assert {\n"
                "  fun assertEquals(expected: Any?, actual: Any?) {\n"
                "    if (expected != actual) throw AssertionError(\"Expected <$expected> but was <$actual>\")\n"
                "  }\n"
                "  fun assertEquals(expected: Double, actual: Double, delta: Double) {\n"
                "    if (abs(expected - actual) > delta) throw AssertionError(\"Expected <$expected> +/- <$delta> but was <$actual>\")\n"
                "  }\n"
                "  fun assertTrue(condition: Boolean) {\n"
                "    if (!condition) throw AssertionError(\"Expected true but was false\")\n"
                "  }\n"
                "  fun assertFalse(condition: Boolean) {\n"
                "    if (condition) throw AssertionError(\"Expected false but was true\")\n"
                "  }\n"
                "}\n",
                encoding="utf-8",
            )
            (tmp_path / "TestRunnerMain.kt").write_text(
                "package com.example\n"
                "import org.junit.Test\n"
                "fun main() {\n"
                "  val instance = TradeLinkDomainUnitTest()\n"
                "  val methods = TradeLinkDomainUnitTest::class.java.declaredMethods\n"
                "    .filter { it.isAnnotationPresent(Test::class.java) }\n"
                "    .sortedBy { it.name }\n"
                "  for (m in methods) {\n"
                "    m.invoke(instance)\n"
                "    println(\"PASS: ${m.name}\")\n"
                "  }\n"
                "  println(\"OK (${methods.size} tests)\")\n"
                "}\n",
                encoding="utf-8",
            )
            out_jar = tmp_path / "tradee_domain_tests.jar"
            cp_compile = f"{KT_STDLIB_JAR}"
            compile_cmd = [
                "java",
                "-jar",
                str(KOTLIN_COMPILER_JAR),
                str(tmp_path / "JUnitShim.kt"),
                str(tmp_path / "Timestamp.kt"),
                str(tmp_path / "FirestoreAnnotations.kt"),
                str(TRADY_DIR / "app/src/main/java/com/example/data/model/TradeLinkModels.kt"),
                str(TRADY_DIR / "app/src/main/java/com/example/ui/screens/MarketplaceDomainFilter.kt"),
                str(repo_stub),
                str(TRADY_DIR / "app/src/test/java/com/example/TradeLinkDomainUnitTest.kt"),
                str(tmp_path / "TestRunnerMain.kt"),
                "-cp",
                cp_compile,
                "-d",
                str(out_jar),
            ]
            compile_res = subprocess.run(compile_cmd, capture_output=True, text=True, timeout=60)
            self.assertEqual(
                compile_res.returncode,
                0,
                f"kotlinc compilation failed:\nSTDOUT:\n{compile_res.stdout}\nSTDERR:\n{compile_res.stderr}",
            )

            cp_run = f"{out_jar};{KT_STDLIB_JAR}"
            run_cmd = [
                "java",
                "-cp",
                cp_run,
                "com.example.TestRunnerMainKt",
            ]
            run_res = subprocess.run(run_cmd, capture_output=True, text=True, timeout=30)
            self.assertEqual(
                run_res.returncode,
                0,
                f"JUnit execution failed:\nSTDOUT:\n{run_res.stdout}\nSTDERR:\n{run_res.stderr}",
            )
            self.assertIn("OK (9 tests)", run_res.stdout)

    def test_09_headless_edge_browser_renders_website_and_executes_js_engine(self):
        self.assertTrue(MSEDGE_EXE.exists(), f"Microsoft Edge not found at {MSEDGE_EXE}")
        verify_html = WEBSITE_DIR / "_verify_engine_temp.html"
        verify_html.write_text(
            """<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>Tradee JS Engine Verification</title></head>
<body>
<pre id="engine-results">RUNNING</pre>
<script src="js/data.js"></script>
<script src="js/engine.js"></script>
<script>
(function() {
  try {
    const D = window.TradeeData;
    const E = window.TradeeEngine;
    const target = D.INITIAL_PRODUCTS.find(p => p.id === 'prod_elec_gamepad_1');
    const at4km = E.compareProductOffers(D.INITIAL_PRODUCTS, target, 4.0);
    const at1km = E.compareProductOffers(D.INITIAL_PRODUCTS, target, 1.0);
    const at12km = E.compareProductOffers(D.INITIAL_PRODUCTS, target, 12.0);
    const tiers1kg = E.compareAllBoltTiers(D.TRANSPORT_TIERS, 4.0, 1.5);
    const tiers5kg = E.compareAllBoltTiers(D.TRANSPORT_TIERS, 0.0, 5.0);
    const tiers50kg = E.compareAllBoltTiers(D.TRANSPORT_TIERS, 4.0, 50.0);
    const tiers400kg = E.compareAllBoltTiers(D.TRANSPORT_TIERS, 4.0, 400.0);
    const bench = E.computeCategoryPriceBenchmark(D.INITIAL_PRODUCTS, 'ELECTRONICS');
    const cart = E.calculateCartTotals([{ priceTzs: 81000, quantity: 2, requiresTruck: false }], 4.0);
    const result = {
      ok: true,
      at4kmWinner: at4km[0].productId,
      at4kmWinnerLanded: at4km[0].landedTotalTzs,
      at4kmLowestStickerId: at4km.find(o => o.isLowestStickerPrice).productId,
      at1kmWinner: at1km[0].productId,
      at1kmTransport: at1km[0].transportFeeTzs,
      at12kmTransport: at12km[0].transportFeeTzs,
      rec1kg: tiers1kg.find(q => q.isRecommended).tierKey,
      rec5kg: tiers5kg.find(q => q.isRecommended).tierKey,
      rec5kgFare: tiers5kg.find(q => q.tierKey === 'TRADEE_MOTO_BOX').fareTzs,
      rec50kg: tiers50kg.find(q => q.isRecommended).tierKey,
      rec400kg: tiers400kg.find(q => q.isRecommended).tierKey,
      benchCount: bench.count,
      benchLowest: bench.lowestTzs,
      cartGrandTotal: cart.grandTotalTzs
    };
    document.getElementById('engine-results').textContent = JSON.stringify(result);
  } catch (err) {
    document.getElementById('engine-results').textContent = JSON.stringify({ ok: false, error: String(err) });
  }
})();
</script>
</body></html>""",
            encoding="utf-8",
        )
        handler = functools.partial(QuietHandler, directory=str(WEBSITE_DIR))
        server = socketserver.TCPServer(("127.0.0.1", 0), handler)
        port = server.server_address[1]
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            # 1. Verify full live DOM rendering of index.html
            dom_res = subprocess.run(
                [
                    str(MSEDGE_EXE),
                    "--headless",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--dump-dom",
                    f"http://127.0.0.1:{port}/index.html",
                ],
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                timeout=30,
            )
            self.assertEqual(dom_res.returncode, 0, f"Headless Edge failed on index.html: {dom_res.stderr}")
            dom_html = dom_res.stdout
            self.assertIn("nav-tab-marketplace", dom_html)
            self.assertIn("nav-tab-compare", dom_html)
            self.assertIn("nav-tab-bolt_transport", dom_html)
            self.assertIn("nav-tab-seller", dom_html)
            self.assertIn("carousel-active-card", dom_html)
            self.assertIn("Pro Wireless Gamepad DualShock", dom_html)
            self.assertIn("BEST LANDED VALUE", dom_html)

            # 2. Verify live browser JS execution of TradeeEngine
            eng_res = subprocess.run(
                [
                    str(MSEDGE_EXE),
                    "--headless",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--dump-dom",
                    f"http://127.0.0.1:{port}/_verify_engine_temp.html",
                ],
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                timeout=30,
            )
            self.assertEqual(eng_res.returncode, 0)
            match = re.search(r'<pre id="engine-results">(.*?)</pre>', eng_res.stdout, re.S)
            self.assertIsNotNone(match, f"Could not find #engine-results in:\n{eng_res.stdout}")
            payload = json.loads(match.group(1))
            self.assertTrue(payload.get("ok"), f"Browser JS Engine error: {payload}")
            # At 4.0 km: Samora Digital Hub (81,000 + 4,100 = 85,100) wins Best Landed Value while Mlimani (79,000) is Lowest Sticker
            self.assertEqual(payload["at4kmWinner"], "prod_elec_gamepad_2")
            self.assertEqual(payload["at4kmWinnerLanded"], 85100)
            self.assertEqual(payload["at4kmLowestStickerId"], "prod_elec_gamepad_3")
            # At 1.0 km: Mlimani (79,000 + 3,650 = 82,650) overtakes Samora (81,000 + 3,000 = 84,000)
            self.assertEqual(payload["at1kmWinner"], "prod_elec_gamepad_3")
            # Distance slider dynamically updates transport fee
            self.assertNotEqual(payload["at1kmTransport"], payload["at12kmTransport"])
            # Bolt tier recommendations match Android and iOS
            self.assertEqual(payload["rec1kg"], "TRADEE_BODA")
            self.assertEqual(payload["rec5kg"], "TRADEE_MOTO_BOX")
            self.assertEqual(payload["rec5kgFare"], 3000)
            self.assertEqual(payload["rec50kg"], "TRADEE_BAJAJI")
            self.assertEqual(payload["rec400kg"], "TRADEE_TRUCK")
            self.assertGreater(payload["benchCount"], 0)
            self.assertEqual(payload["cartGrandTotal"], 166500)
        finally:
            server.shutdown()
            server.server_close()
            if verify_html.exists():
                verify_html.unlink()


if __name__ == "__main__":
    unittest.main()
