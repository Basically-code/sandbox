package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.OrderStatus
import com.example.data.model.ProductCategory
import com.example.data.model.UserRole
import com.example.data.repository.TradeLinkRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.components.LayeredCategoryCarousel
import com.example.ui.components.TradeLinkLogoBadge
import com.example.ui.screens.BuyerHomeScreen
import com.example.ui.screens.CategoryCatalogScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.DriverPortalScreen
import com.example.ui.screens.LiveMapAndOrdersScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.OnboardingTermsScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProviderPortalScreen
import com.example.ui.theme.BgPrimaryCanvas
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeLinkTheme
import com.example.ui.viewmodel.TradeLinkViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private object Routes {
  const val MARKETPLACE = "marketplace"
  const val SHOWCASE = "showcase"
  const val CATALOG = "catalog/{categoryKey}/{dealsOnly}"
  const val PRODUCT_DETAIL = "product_detail/{productId}"
  const val CHECKOUT = "checkout"

  fun catalog(categoryKey: String, dealsOnly: Boolean) = "catalog/$categoryKey/$dealsOnly"
  fun productDetail(productId: String) = "product_detail/$productId"
}

private enum class AppTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  MARKETPLACE("Marketplace", Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_tab_marketplace"),
  FLEET("Fleet", Icons.Filled.TwoWheeler, Icons.Outlined.TwoWheeler, "nav_tab_fleet"),
  TELEMETRY("Telemetry", Icons.Filled.LocationOn, Icons.Outlined.LocationOn, "nav_tab_telemetry"),
  PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile"),
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      TradeLinkTheme {
        TradeLinkAppRoot()
      }
    }
  }
}

@Composable
fun TradeLinkAppRoot(auth: FirebaseAuth = Firebase.auth) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {},
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  var isGuestMode by rememberSaveable { mutableStateOf(false) }

  val user = currentUser
  if (user == null) {
    if (isGuestMode) {
      GuestTradeLinkFlow(
        onExitGuestToAuth = { isGuestMode = false },
      )
    } else {
      AuthScreen(
        onAuthSuccess = { isGuestMode = false },
        onEnterWithoutSignUp = { isGuestMode = true },
      )
    }
  } else {
    LaunchedEffect(user.uid) {
      isGuestMode = false
    }
    key(user.uid) {
      val viewModel: TradeLinkViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            TradeLinkViewModel(
              repository = TradeLinkRepository(db, auth),
              currentUserId = user.uid,
              defaultDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Trade Link Member",
            )
          }
        },
      )
      AuthenticatedTradeLinkFlow(
        viewModel = viewModel,
        userEmail = user.email,
        defaultName = user.displayName ?: "Trade Link Member",
        onSignOut = {
          signOut(credentialManager = credentialManager, scope = scope)
        },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedTradeLinkFlow(
  viewModel: TradeLinkViewModel,
  userEmail: String?,
  defaultName: String,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val activeProductsState by viewModel.activeProductsState.collectAsStateWithLifecycle()
  val providerProductsState by viewModel.providerProductsState.collectAsStateWithLifecycle()
  val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
  val driverContractState by viewModel.driverContractState.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val carouselIndex by viewModel.carouselIndex.collectAsStateWithLifecycle()
  val favoriteProductIds by viewModel.favoriteProductIdsState.collectAsStateWithLifecycle()
  val cartItems by viewModel.cartItemsState.collectAsStateWithLifecycle()
  val actionBanner by viewModel.actionBanner.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(actionBanner) {
    val msg = actionBanner
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBanner()
    }
  }

  when (val pState = profileState) {
    UiState.Loading -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(color = BrandPrimary)
      }
    }

    is UiState.Error -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Session Sync Issue", style = MaterialTheme.typography.titleLarge, color = StatusDanger)
          Text(pState.message, style = MaterialTheme.typography.bodyMedium)
          Button(onClick = onSignOut) { Text("Sign Out & Retry") }
        }
      }
    }

    is UiState.Success -> {
      val profile = pState.data
      if (profile == null || !profile.termsAccepted) {
        OnboardingTermsScreen(
          defaultName = defaultName,
          isBusy = isBusy,
          onContinue = { name, phone, role, station, plate ->
            viewModel.acceptTermsAndSaveProfile(name, phone, role, station, plate)
          },
        )
      } else {
        val activeRole = UserRole.fromKey(profile.activeRole)
        val products = (activeProductsState as? UiState.Success)?.data ?: emptyList()
        val sellerProducts = (providerProductsState as? UiState.Success)?.data ?: emptyList()
        val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
        val driverContract = (driverContractState as? UiState.Success)?.data
        val activeOrdersCount = orders.count { !OrderStatus.fromKey(it.status).isTerminal }

        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var selectedTab by rememberSaveable { mutableStateOf(AppTab.MARKETPLACE) }
        var telemetryStartOnMap by rememberSaveable { mutableStateOf(true) }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
        ) { _ ->
          telemetryStartOnMap = true
          selectedTab = AppTab.TELEMETRY
        }

        val openLiveMapWithPermissionCheck = {
          val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
          ) == PackageManager.PERMISSION_GRANTED
          if (hasFine) {
            telemetryStartOnMap = true
            selectedTab = AppTab.TELEMETRY
          } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
          }
        }

        BackHandler(enabled = selectedTab != AppTab.MARKETPLACE) {
          selectedTab = AppTab.MARKETPLACE
        }

        ModalNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            ModalDrawerSheet(drawerContainerColor = BgSurfaceCard) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  TradeLinkLogoBadge(modifier = Modifier.size(44.dp))
                  Column {
                    Text("Trady Link", style = MaterialTheme.typography.titleLarge)
                    Text(profile.fullName, style = MaterialTheme.typography.bodyMedium)
                  }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = BorderHairline)
                Spacer(modifier = Modifier.height(6.dp))
                Text("SWITCH PORTAL", style = MaterialTheme.typography.labelMedium, color = BrandPrimary)

                val drawerColors = NavigationDrawerItemDefaults.colors(
                  selectedContainerColor = BrandSecondary,
                  selectedTextColor = BrandPrimary,
                  selectedIconColor = BrandPrimary,
                )

                NavigationDrawerItem(
                  label = { Text("Buyer Marketplace") },
                  selected = activeRole == UserRole.BUYER && selectedTab == AppTab.MARKETPLACE,
                  onClick = {
                    scope.launch { drawerState.close() }
                    viewModel.switchPortalRole(UserRole.BUYER)
                    selectedTab = AppTab.MARKETPLACE
                  },
                  icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = null) },
                  colors = drawerColors,
                )
                NavigationDrawerItem(
                  label = { Text("Provider / Seller Portal") },
                  selected = activeRole == UserRole.PROVIDER && selectedTab == AppTab.MARKETPLACE,
                  onClick = {
                    scope.launch { drawerState.close() }
                    viewModel.switchPortalRole(UserRole.PROVIDER)
                    selectedTab = AppTab.MARKETPLACE
                  },
                  icon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
                  colors = drawerColors,
                )
                NavigationDrawerItem(
                  label = { Text("Fleet Driver & Lease") },
                  selected = selectedTab == AppTab.FLEET,
                  onClick = {
                    scope.launch { drawerState.close() }
                    viewModel.switchPortalRole(UserRole.DRIVER)
                    selectedTab = AppTab.FLEET
                  },
                  icon = { Icon(Icons.Filled.TwoWheeler, contentDescription = null) },
                  colors = drawerColors,
                )

                HorizontalDivider(color = BorderHairline, modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                  label = { Text("Live Courier Map & Safety Code") },
                  selected = selectedTab == AppTab.TELEMETRY,
                  onClick = {
                    scope.launch { drawerState.close() }
                    openLiveMapWithPermissionCheck()
                  },
                  icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                  colors = drawerColors,
                )

                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                  label = { Text("Sign Out", color = StatusDanger) },
                  selected = false,
                  onClick = {
                    scope.launch { drawerState.close() }
                    onSignOut()
                  },
                  icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = StatusDanger) },
                )
              }
            }
          },
        ) {
          Scaffold(
            containerColor = BgPrimaryCanvas,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
              Surface(
                color = BgSurfaceCard,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
              ) {
                Column {
                  HorizontalDivider(color = BorderHairline)
                  NavigationBar(
                    containerColor = BgSurfaceCard,
                    contentColor = TextPrimary,
                    windowInsets = NavigationBarDefaults.windowInsets,
                  ) {
                    AppTab.entries.forEach { tab ->
                      val isSelected = selectedTab == tab
                      NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                          if (tab == AppTab.TELEMETRY) {
                            openLiveMapWithPermissionCheck()
                          } else {
                            selectedTab = tab
                          }
                        },
                        icon = {
                          val iconVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon
                          if (tab == AppTab.TELEMETRY && activeOrdersCount > 0) {
                            BadgedBox(
                              badge = {
                                Badge(containerColor = BrandPrimary, contentColor = Color.White) {
                                  Text(activeOrdersCount.toString())
                                }
                              },
                            ) {
                              Icon(iconVector, contentDescription = tab.label)
                            }
                          } else {
                            Icon(iconVector, contentDescription = tab.label)
                          }
                        },
                        label = {
                          Text(
                            text = tab.label,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                          )
                        },
                        colors = NavigationBarItemDefaults.colors(
                          selectedIconColor = BrandPrimary,
                          selectedTextColor = BrandPrimary,
                          indicatorColor = BrandSecondary,
                          unselectedIconColor = TextSecondary,
                          unselectedTextColor = TextSecondary,
                        ),
                        modifier = Modifier.testTag(tab.testTag),
                      )
                    }
                  }
                }
              }
            },
          ) { innerPadding ->
            val contentModifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())

            when (selectedTab) {
              AppTab.MARKETPLACE -> {
                if (activeRole == UserRole.PROVIDER) {
                  ProviderPortalScreen(
                    sellerProducts = sellerProducts,
                    orders = orders,
                    isBusy = isBusy,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onCreateProduct = { cat, title, desc, price, qty, size, weight, deal, pickup, photo, onDone ->
                      viewModel.createProduct(cat, title, desc, price, qty, size, weight, deal, pickup, photo, onDone)
                    },
                    onAdvanceOrderStep = { viewModel.advanceOrderStep(it) },
                    onDispatchBoltTransport = { tier, isSellerMode, pickupHub, dropoff, distKm, weightKg, method ->
                      viewModel.dispatchBoltTransport(tier, isSellerMode, pickupHub, dropoff, distKm, weightKg, method) {
                        telemetryStartOnMap = true
                        selectedTab = AppTab.TELEMETRY
                      }
                    },
                    modifier = contentModifier,
                  )
                } else {
                  NavHost(
                    navController = navController,
                    startDestination = Routes.MARKETPLACE,
                    modifier = contentModifier,
                  ) {
                    composable(Routes.MARKETPLACE) {
                      MarketplaceScreen(
                        userName = profile.fullName,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        productsState = activeProductsState,
                        favoriteProductIds = favoriteProductIds,
                        cartItems = cartItems,
                        orders = orders,
                        isBusy = isBusy,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenProfile = { selectedTab = AppTab.PROFILE },
                        onOpenOrders = {
                          telemetryStartOnMap = false
                          selectedTab = AppTab.TELEMETRY
                        },
                        onOpenCategoryShowcase = {
                          navController.navigate(Routes.SHOWCASE)
                        },
                        onOpenLiveMap = openLiveMapWithPermissionCheck,
                        onSeedSampleCatalog = { viewModel.seedSampleCatalog() },
                        onSwitchToProvider = { viewModel.switchPortalRole(UserRole.PROVIDER) },
                        onProductClicked = { product ->
                          navController.navigate(Routes.productDetail(product.id))
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToCart = { viewModel.addToCart(it) },
                        onUpdateCartQuantity = { item, newQty ->
                          viewModel.updateCartQuantity(item.productId, newQty)
                        },
                        onRemoveCartItem = { item ->
                          viewModel.removeFromCart(item.productId)
                        },
                        onClearCart = { viewModel.clearCart() },
                        onOpenCheckout = { navController.navigate(Routes.CHECKOUT) },
                        onPlaceOrder = { product, qty, size, distKm, method ->
                          viewModel.placeOrder(product, qty, size, distKm, method) {
                            telemetryStartOnMap = true
                            selectedTab = AppTab.TELEMETRY
                          }
                        },
                        onDispatchBoltTransport = { tier, isSellerMode, pickupHub, dropoff, distKm, weightKg, method ->
                          viewModel.dispatchBoltTransport(tier, isSellerMode, pickupHub, dropoff, distKm, weightKg, method) {
                            telemetryStartOnMap = true
                            selectedTab = AppTab.TELEMETRY
                          }
                        },
                      )
                    }

                    composable(Routes.SHOWCASE) {
                      BuyerHomeScreen(
                        userName = profile.fullName,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        selectedCarouselIndex = carouselIndex,
                        onCarouselIndexChange = { viewModel.setCarouselIndex(it) },
                        products = products,
                        orders = orders,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenAccountSheet = { selectedTab = AppTab.PROFILE },
                        onOpenOrders = {
                          telemetryStartOnMap = false
                          selectedTab = AppTab.TELEMETRY
                        },
                        onOpenDailyDeals = {
                          navController.navigate(Routes.catalog(ProductCategory.ELECTRONICS.key, dealsOnly = true))
                        },
                        onCategorySelected = { cat ->
                          navController.navigate(Routes.catalog(cat.key, dealsOnly = false))
                        },
                        onProductClicked = { product ->
                          navController.navigate(Routes.productDetail(product.id))
                        },
                        onOpenLiveMap = openLiveMapWithPermissionCheck,
                      )
                    }

                    composable(
                      route = Routes.CATALOG,
                      arguments = listOf(
                        navArgument("categoryKey") { type = NavType.StringType },
                        navArgument("dealsOnly") { type = NavType.BoolType },
                      ),
                    ) { entry ->
                      val catKey = entry.arguments?.getString("categoryKey") ?: ProductCategory.ELECTRONICS.key
                      val dealsOnly = entry.arguments?.getBoolean("dealsOnly") ?: false
                      val category = ProductCategory.fromKey(catKey)
                      val scopedProducts = if (dealsOnly) {
                        products.filter { it.isDailyDeal }
                      } else {
                        products.filter { it.category == category.key }
                      }
                      CategoryCatalogScreen(
                        title = if (dealsOnly) "Deals of Today" else category.displayName,
                        subtitle = if (dealsOnly) "Featured Tanzania TZS Offers" else category.tagline,
                        products = scopedProducts,
                        isBusy = isBusy,
                        onBack = { navController.popBackStack() },
                        onSeedSampleCatalog = { viewModel.seedSampleCatalog() },
                        onSwitchToProvider = { viewModel.switchPortalRole(UserRole.PROVIDER) },
                        onProductClicked = { product ->
                          navController.navigate(Routes.productDetail(product.id))
                        },
                        onPlaceOrder = { product, qty, size, distKm, method ->
                          viewModel.placeOrder(product, qty, size, distKm, method) {
                            telemetryStartOnMap = true
                            selectedTab = AppTab.TELEMETRY
                          }
                        },
                      )
                    }

                    composable(
                      route = Routes.PRODUCT_DETAIL,
                      arguments = listOf(
                        navArgument("productId") { type = NavType.StringType },
                      ),
                    ) { entry ->
                      val productId = entry.arguments?.getString("productId").orEmpty()
                      val product = products.firstOrNull { it.id == productId }
                      val cartQuantity = cartItems.firstOrNull { it.productId == productId }?.quantity ?: 0L
                      ProductDetailScreen(
                        product = product,
                        isFavorite = favoriteProductIds.contains(productId),
                        cartQuantity = cartQuantity,
                        isBusy = isBusy,
                        onBack = { navController.popBackStack() },
                        onToggleFavorite = {
                          if (product != null) {
                            viewModel.toggleFavorite(product)
                          }
                        },
                        onAddToCart = {
                          if (product != null) {
                            viewModel.addToCart(product)
                          }
                        },
                        onPlaceOrder = { qty, size, distKm, method ->
                          if (product != null) {
                            viewModel.placeOrder(product, qty, size, distKm, method) {
                              telemetryStartOnMap = true
                              selectedTab = AppTab.TELEMETRY
                            }
                          }
                        },
                        allProducts = products,
                        onAddOfferToCart = { chosenProduct ->
                          viewModel.addToCart(chosenProduct)
                        },
                        onPlaceOfferOrder = { chosenProduct, qty, size, distKm, method ->
                          viewModel.placeOrder(chosenProduct, qty, size, distKm, method) {
                            telemetryStartOnMap = true
                            selectedTab = AppTab.TELEMETRY
                          }
                        },
                      )
                    }

                    composable(Routes.CHECKOUT) {
                      CheckoutScreen(
                        cartItems = cartItems,
                        isBusy = isBusy,
                        onBack = { navController.popBackStack() },
                        onConfirmPurchase = { distKm, method ->
                          viewModel.checkoutCart(distKm, method) {
                            navController.popBackStack(Routes.MARKETPLACE, inclusive = false)
                            telemetryStartOnMap = true
                            selectedTab = AppTab.TELEMETRY
                          }
                        },
                      )
                    }
                  }
                }
              }

              AppTab.FLEET -> {
                DriverPortalScreen(
                  contract = driverContract,
                  orders = orders,
                  onOpenDrawer = { scope.launch { drawerState.open() } },
                  onEnsureInitialized = { viewModel.ensureDriverInitialized() },
                  onMorningCheckOut = { viewModel.driverCheckOutMorning(it) },
                  onEveningCheckIn = { viewModel.driverCheckInEvening(it) },
                  onToggleCargoBox = { viewModel.toggleCargoBox(it) },
                  onSelectRideHailPartner = { c, p -> viewModel.selectRideHailPartner(c, p) },
                  onTriggerCurfewTest = { viewModel.triggerCurfewBreachProtocol(it) },
                  onRecordLeasePayment = { c, w -> viewModel.recordWeeklyLeasePayment(c, w) },
                  onAdvanceOrderStep = { viewModel.advanceOrderStep(it) },
                  modifier = contentModifier,
                )
              }

              AppTab.TELEMETRY -> {
                LiveMapAndOrdersScreen(
                  orders = orders,
                  startOnMap = telemetryStartOnMap,
                  isBusy = isBusy,
                  onBack = null,
                  onAdvanceOrderStep = { viewModel.advanceOrderStep(it) },
                  onConfirmSafeReceipt = { viewModel.confirmSafeReceipt(it) },
                  onClaimDamagedOrLost = { viewModel.claimDamagedOrLostRefund(it) },
                  onClaimRobberyOrHarm = { viewModel.claimRobberyOrHarmRefund(it) },
                  modifier = contentModifier,
                )
              }

              AppTab.PROFILE -> {
                ProfileScreen(
                  profile = profile,
                  email = userEmail,
                  activeOrdersCount = activeOrdersCount,
                  sellerListingsCount = sellerProducts.size,
                  onSwitchRole = { role ->
                    viewModel.switchPortalRole(role)
                    if (role == UserRole.DRIVER) {
                      selectedTab = AppTab.FLEET
                    } else if (role == UserRole.BUYER || role == UserRole.PROVIDER) {
                      selectedTab = AppTab.MARKETPLACE
                    }
                  },
                  onSignOut = onSignOut,
                  modifier = contentModifier,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GuestTradeLinkFlow(
  onExitGuestToAuth: () -> Unit,
) {
  var selectedTab by rememberSaveable { mutableStateOf(AppTab.MARKETPLACE) }
  var carouselIndex by rememberSaveable { mutableStateOf(1) }

  BackHandler {
    if (selectedTab != AppTab.MARKETPLACE) {
      selectedTab = AppTab.MARKETPLACE
    } else {
      onExitGuestToAuth()
    }
  }

  Scaffold(
    containerColor = BgPrimaryCanvas,
    contentWindowInsets = WindowInsets(0),
    bottomBar = {
      Surface(
        color = BgSurfaceCard,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
      ) {
        Column {
          HorizontalDivider(color = BorderHairline)
          NavigationBar(
            containerColor = BgSurfaceCard,
            contentColor = TextPrimary,
            windowInsets = NavigationBarDefaults.windowInsets,
          ) {
            AppTab.entries.forEach { tab ->
              val isSelected = selectedTab == tab
              NavigationBarItem(
                selected = isSelected,
                onClick = { selectedTab = tab },
                icon = {
                  Icon(
                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = {
                  Text(
                    text = tab.label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = BrandPrimary,
                  selectedTextColor = BrandPrimary,
                  indicatorColor = BrandSecondary,
                  unselectedIconColor = TextSecondary,
                  unselectedTextColor = TextSecondary,
                ),
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = innerPadding.calculateBottomPadding())
        .statusBarsPadding()
        .testTag("guest_mode_container"),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgSurfaceCard,
      ) {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(60.dp)
              .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              TradeLinkLogoBadge(modifier = Modifier.size(36.dp))
              Column {
                Text(
                  text = selectedTab.label,
                  style = MaterialTheme.typography.titleLarge,
                  color = TextPrimary,
                )
                Text(
                  text = "Guest Preview",
                  style = MaterialTheme.typography.labelMedium,
                  color = BrandPrimary,
                )
              }
            }

            Button(
              onClick = onExitGuestToAuth,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              modifier = Modifier
                .height(48.dp)
                .testTag("guest_sign_in_cta"),
            ) {
              Text("Sign In / Sign Up", style = MaterialTheme.typography.labelLarge)
            }
          }
          HorizontalDivider(color = BorderHairline)
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (selectedTab == AppTab.MARKETPLACE) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            LayeredCategoryCarousel(
              selectedIndex = carouselIndex,
              onIndexChanged = { idx ->
                val size = ProductCategory.entries.size
                carouselIndex = ((idx % size) + size) % size
              },
              onCategoryTap = { onExitGuestToAuth() },
            )

            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = BgSurfaceCard),
              border = BorderStroke(1.dp, BorderHairline),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "Sign in to load live Marketplace listings",
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                Text(
                  text = "Explore categories above, or sign in / sign up to browse live Firestore products, save favorites, use your shopping cart, and track orders.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
                Button(
                  onClick = onExitGuestToAuth,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("guest_marketplace_sign_in_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                ) {
                  Text("Sign In or Sign Up")
                }
              }
            }
          }
        } else {
          Card(
            modifier = Modifier.widthIn(max = 480.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BgSurfaceCard),
            border = BorderStroke(1.dp, BorderHairline),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = "${selectedTab.label} requires an account",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
              )
              Text(
                text = "Sign in or sign up to sync ${selectedTab.label.lowercase()} data and orders with Firestore.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
              Button(
                onClick = onExitGuestToAuth,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("guest_tab_sign_in_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
              ) {
                Text("Go to Sign In / Sign Up")
              }
            }
          }
        }
      }
    }
  }
}
