package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.model.ProductCategory
import com.example.ui.theme.AccentWarm
import com.example.ui.theme.BgSurfaceCard
import com.example.ui.theme.BgSurfaceSubtle
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandPrimaryGradient
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.CardSheenGradient
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmDealGradient
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

fun formatTzs(amount: Double): String {
  val nf = NumberFormat.getNumberInstance(Locale.US)
  return "TSh ${nf.format(amount.roundToInt())}"
}

@Composable
fun rememberShimmerBrush(): Brush {
  val transition = rememberInfiniteTransition(label = "trady_shimmer")
  val translateAnim by transition.animateFloat(
    initialValue = -300f,
    targetValue = 1100f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1150, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "shimmer_translate",
  )
  val baseColor = BgSurfaceSubtle
  val highlightColor = BorderHairline.copy(alpha = 0.85f)
  val sheenColors = listOf(
    baseColor,
    highlightColor,
    BgSurfaceCard.copy(alpha = 0.9f),
    highlightColor,
    baseColor,
  )
  return Brush.linearGradient(
    colors = sheenColors,
    start = Offset(x = translateAnim - 260f, y = 0f),
    end = Offset(x = translateAnim + 260f, y = 260f),
  )
}

@Composable
fun TradeLinkLogoBadge(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.size(40.dp),
    shape = RoundedCornerShape(11.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.5.dp, BrandPrimaryGradient),
    shadowElevation = 3.dp,
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_trady_foreground_1794558910635),
      contentDescription = "Tradee Professional Logo",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .padding(3.dp)
        .clip(RoundedCornerShape(8.dp)),
    )
  }
}

@Composable
fun SkewedDealsBanner(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val parallelogramShape = remember {
    GenericShape { size, _ ->
      val skewPx = size.height * 0.34f
      moveTo(skewPx, 0f)
      lineTo(size.width, 0f)
      lineTo(size.width - skewPx, size.height)
      lineTo(0f, size.height)
      close()
    }
  }
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .clip(parallelogramShape)
      .clickable(onClick = onClick)
      .testTag("daily_deals_banner"),
    shape = parallelogramShape,
    color = Color.Transparent,
    border = BorderStroke(1.5.dp, AccentWarm),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(WarmDealGradient),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = buildAnnotatedString {
          append("Deals of today : ")
          withStyle(
            SpanStyle(
              fontWeight = FontWeight.ExtraBold,
              textDecoration = TextDecoration.Underline,
              color = BrandPrimaryDark,
            )
          ) {
            append("Click here")
          }
        },
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
      )
    }
  }
}

@Composable
fun LayeredCategoryCarousel(
  selectedIndex: Int,
  onIndexChanged: (Int) -> Unit,
  onCategoryTap: (ProductCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val categories = ProductCategory.entries
  val total = categories.size
  val scope = rememberCoroutineScope()
  val dragOffset = remember { Animatable(0f) }
  var widthPx by remember { mutableFloatStateOf(900f) }

  LaunchedEffect(selectedIndex) {
    dragOffset.animateTo(
      targetValue = 0f,
      animationSpec = spring(
        dampingRatio = 0.82f,
        stiffness = 240f,
      ),
    )
  }

  val leftIndex = (selectedIndex - 1 + total) % total
  val rightIndex = (selectedIndex + 1) % total
  val maxDrag = (0.65f * widthPx).coerceAtLeast(180f)
  val progress = (dragOffset.value / maxDrag).coerceIn(-1f, 1f)
  val absProgress = abs(progress)

  val sideOffsetPx = with(LocalDensity.current) { 54.dp.toPx() }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(350.dp)
        .pointerInput(selectedIndex) {
          widthPx = size.width.toFloat()
          val tracker = VelocityTracker()
          detectHorizontalDragGestures(
            onDragEnd = {
              val vx = tracker.calculateVelocity().x
              tracker.resetTracking()
              scope.launch {
                val thresholdPx = size.width * 0.20f
                when {
                  dragOffset.value < -thresholdPx || vx < -550f -> {
                    dragOffset.snapTo(0f)
                    onIndexChanged(rightIndex)
                  }
                  dragOffset.value > thresholdPx || vx > 550f -> {
                    dragOffset.snapTo(0f)
                    onIndexChanged(leftIndex)
                  }
                  else -> {
                    dragOffset.animateTo(
                      0f,
                      spring(dampingRatio = 0.82f, stiffness = 240f),
                    )
                  }
                }
              }
            },
            onDragCancel = {
              tracker.resetTracking()
              scope.launch {
                dragOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 240f))
              }
            },
            onHorizontalDrag = { change, dragAmount ->
              change.consume()
              tracker.addPosition(change.uptimeMillis, change.position)
              scope.launch {
                dragOffset.snapTo((dragOffset.value + dragAmount).coerceIn(-maxDrag, maxDrag))
              }
            },
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      // Left Peek Background Card
      CategoryPeekCard(
        category = categories[leftIndex],
        modifier = Modifier
          .zIndex(if (progress > 0.45f) 8f else 5f)
          .offset { IntOffset((-sideOffsetPx + dragOffset.value * 0.35f).roundToInt(), 0) }
          .scale(0.88f + 0.12f * progress.coerceAtLeast(0f))
          .alpha(0.68f + 0.32f * progress.coerceAtLeast(0f))
          .clickable { onIndexChanged(leftIndex) },
      )

      // Right Peek Background Card
      CategoryPeekCard(
        category = categories[rightIndex],
        modifier = Modifier
          .zIndex(if (progress < -0.45f) 8f else 5f)
          .offset { IntOffset((sideOffsetPx + dragOffset.value * 0.35f).roundToInt(), 0) }
          .scale(0.88f + 0.12f * (-progress).coerceAtLeast(0f))
          .alpha(0.68f + 0.32f * (-progress).coerceAtLeast(0f))
          .clickable { onIndexChanged(rightIndex) },
      )

      // Center Active Perspective Card
      val activeCategory = categories[selectedIndex]
      val activeScale = 1.0f - 0.12f * absProgress
      val activeElevation = (16f * (1f - absProgress) + 4f).dp

      Surface(
        modifier = Modifier
          .zIndex(10f)
          .width(236.dp)
          .height(328.dp)
          .graphicsLayer {
            translationX = dragOffset.value
            scaleX = activeScale
            scaleY = activeScale
            rotationY = -8f * progress
            cameraDistance = 12f * density
          }
          .clip(RoundedCornerShape(24.dp))
          .clickable { onCategoryTap(activeCategory) }
          .testTag("active_category_card_${activeCategory.key}"),
        shape = RoundedCornerShape(24.dp),
        color = BgSurfaceCard,
        border = BorderStroke(1.6.dp, TextPrimary),
        shadowElevation = activeElevation,
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(CardSheenGradient)
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // Subtle top accent tag
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = BgSurfaceSubtle,
          ) {
            Text(
              text = activeCategory.tagline.uppercase(),
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }

          // Floating 1.35x Parallax Illustration Pair
          CategoryIllustrationPair(
            category = activeCategory,
            parallaxShiftPx = dragOffset.value * 0.35f,
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = activeCategory.carouselLabel,
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp,
              ),
              color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tap to browse · Swipe deck",
              style = MaterialTheme.typography.labelMedium,
              color = BrandPrimary,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 8-Category Dot Indicator
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      categories.forEachIndexed { idx, cat ->
        val isSelected = idx == selectedIndex
        Box(
          modifier = Modifier
            .height(8.dp)
            .width(if (isSelected) 22.dp else 8.dp)
            .clip(CircleShape)
            .background(if (isSelected) BrandPrimary else BorderHairline)
            .clickable { onIndexChanged(idx) }
            .testTag("carousel_dot_${cat.key}"),
        )
      }
    }
  }
}

@Composable
private fun CategoryPeekCard(
  category: ProductCategory,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .width(214.dp)
      .height(300.dp),
    shape = RoundedCornerShape(22.dp),
    color = BgSurfaceCard,
    border = BorderStroke(1.dp, BorderHairline),
    shadowElevation = 4.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      Text(
        text = category.carouselLabel,
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted,
      )
    }
  }
}

@Composable
fun CategoryIllustrationPair(
  category: ProductCategory,
  parallaxShiftPx: Float,
) {
  val (topIcon, bottomIcon, accent) = remember(category) {
    categoryVisualSpec(category)
  }
  Column(
    modifier = Modifier.graphicsLayer { translationX = parallaxShiftPx },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Surface(
      modifier = Modifier.size(72.dp),
      shape = RoundedCornerShape(20.dp),
      color = accent.copy(alpha = 0.14f),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = topIcon,
          contentDescription = null,
          tint = TextPrimary,
          modifier = Modifier.size(44.dp),
        )
      }
    }
    Surface(
      modifier = Modifier.size(84.dp),
      shape = RoundedCornerShape(22.dp),
      color = BgSurfaceSubtle,
      border = BorderStroke(1.dp, BorderHairline),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = bottomIcon,
          contentDescription = null,
          tint = TextPrimary,
          modifier = Modifier.size(50.dp),
        )
      }
    }
  }
}

private fun categoryVisualSpec(category: ProductCategory): Triple<ImageVector, ImageVector, Color> =
  when (category) {
    ProductCategory.ELECTRONICS -> Triple(Icons.Filled.SportsEsports, Icons.Filled.Smartphone, BrandPrimary)
    ProductCategory.SPORTS_AND_OUTDOORS -> Triple(Icons.Filled.SportsSoccer, Icons.Filled.Hiking, Color(0xFF10B981))
    ProductCategory.FOOD -> Triple(Icons.Filled.Restaurant, Icons.Filled.LocalDining, AccentWarm)
    ProductCategory.BOOKS -> Triple(Icons.AutoMirrored.Filled.MenuBook, Icons.Filled.AutoStories, BrandPrimary)
    ProductCategory.CLOTHING -> Triple(Icons.Filled.Checkroom, Icons.Filled.DryCleaning, Color(0xFF6366F1))
    ProductCategory.SHOES -> Triple(Icons.AutoMirrored.Filled.DirectionsRun, Icons.Filled.ShoppingBag, AccentWarm)
    ProductCategory.JEWELLERY -> Triple(Icons.Filled.Diamond, Icons.Filled.Watch, Color(0xFFF59E0B))
    ProductCategory.BEAUTY_AND_PERSONAL_CARE -> Triple(Icons.Filled.Spa, Icons.Filled.FaceRetouchingNatural, Color(0xFFEC4899))
  }

@Composable
fun FloatingLiveMapButton(
  isOrderInTransit: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pulseTransition = rememberInfiniteTransition(label = "map_radar_pulse")
  val pulseScale by pulseTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.45f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "pulse_scale",
  )
  val pulseAlpha by pulseTransition.animateFloat(
    initialValue = 0.28f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "pulse_alpha",
  )

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    if (isOrderInTransit) {
      Box(
        modifier = Modifier
          .size(width = 114.dp, height = 56.dp)
          .scale(pulseScale)
          .clip(RoundedCornerShape(16.dp))
          .background(BrandPrimary.copy(alpha = pulseAlpha)),
      )
    }

    Surface(
      modifier = Modifier
        .height(52.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag("floating_map_button"),
      shape = RoundedCornerShape(14.dp),
      color = BgSurfaceCard,
      border = BorderStroke(1.6.dp, TextPrimary),
      shadowElevation = 8.dp,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Map,
          contentDescription = "Open Live Delivery Map",
          tint = BrandPrimary,
          modifier = Modifier.size(24.dp),
        )
        Text(
          text = "MAP",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
          color = TextPrimary,
        )
      }
    }
  }
}
