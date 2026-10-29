package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DistributorWithDistance
import com.example.data.model.GasBrand
import com.example.data.model.SriLankaDistricts
import com.example.data.model.StockStatus
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.theme.SpaceGroteskFamily
import kotlin.math.hypot
import kotlin.math.min

/**
 * Interactive zoomable & pannable Sri Lanka Spatial Radar Map Canvas.
 * Projects real Sri Lankan geographic coordinates (Lat 5.85..9.90, Lon 79.50..81.95)
 * onto the DrawScope viewport without requiring external Maps API keys.
 */
@Composable
fun SriLankaSpatialMapCanvas(
    stations: List<DistributorWithDistance>,
    referenceLat: Double,
    referenceLon: Double,
    selectedStationId: String?,
    onSelectStation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    var zoom by remember { mutableFloatStateOf(1.15f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val pulseTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    // Stylized Sri Lanka coastline geographic vertices (Lat, Lon)
    val islandOutlineGeo = remember {
        listOf(
            9.82 to 80.22, // Point Pedro (North)
            9.66 to 79.95, // Jaffna Peninsula West
            9.40 to 80.08,
            8.98 to 79.88, // Mannar
            8.50 to 79.82,
            8.03 to 79.73, // Kalpitiya
            7.55 to 79.79, // Chilaw
            7.20 to 79.83, // Negombo
            6.93 to 79.84, // Colombo
            6.58 to 79.95, // Kalutara
            6.13 to 80.10, // Hikkaduwa
            6.03 to 80.22, // Galle
            5.94 to 80.54, // Matara / Dondra Head (South)
            6.02 to 80.85, // Tangalle
            6.13 to 81.12, // Hambantota
            6.38 to 81.52, // Yala
            6.84 to 81.83, // Arugam Bay
            7.42 to 81.82, // Kalmunai
            7.72 to 81.70, // Batticaloa
            8.58 to 81.23, // Trincomalee
            9.25 to 80.81, // Mullaitivu
            9.82 to 80.22
        )
    }

    fun projectGeoToScreen(lat: Double, lon: Double, size: Size): Offset {
        if (size.width <= 0f || size.height <= 0f) return Offset.Zero
        val minLat = 5.80
        val maxLat = 9.95
        val minLon = 79.45
        val maxLon = 82.05

        // Aspect-ratio preserving fit inside DrawScope
        val mapAspect = (maxLon - minLon).toFloat() / (maxLat - minLat).toFloat()
        val baseHeight = min(size.height * 0.88f, (size.width * 0.88f) / mapAspect)
        val baseWidth = baseHeight * mapAspect

        val scaledW = baseWidth * zoom
        val scaledH = baseHeight * zoom

        val originX = (size.width - scaledW) / 2f + panOffset.x
        val originY = (size.height - scaledH) / 2f + panOffset.y

        val normX = ((lon - minLon) / (maxLon - minLon)).toFloat()
        val normY = (1.0 - (lat - minLat) / (maxLat - minLat)).toFloat()

        return Offset(
            x = originX + normX * scaledW,
            y = originY + normY * scaledH
        )
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .testTag("spatial_map_canvas")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ext.mapOceanSurface)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(0.85f, 4.5f)
                            zoom = newZoom
                            val maxPanX = size.width * 0.85f * newZoom
                            val maxPanY = size.height * 0.85f * newZoom
                            panOffset = Offset(
                                x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                            )
                        }
                    }
                    .pointerInput(stations, zoom, panOffset) {
                        detectTapGestures { tapOffset ->
                            val currentCanvas = Size(size.width.toFloat(), size.height.toFloat())
                            val hitTarget = stations
                                .map { item ->
                                    val pos = projectGeoToScreen(
                                        item.distributor.latitude,
                                        item.distributor.longitude,
                                        currentCanvas
                                    )
                                    val distPx = hypot(tapOffset.x - pos.x, tapOffset.y - pos.y)
                                    item.distributor.id to distPx
                                }
                                .filter { it.second <= 42.dp.toPx() }
                                .minByOrNull { it.second }

                            if (hitTarget != null) {
                                onSelectStation(hitTarget.first)
                            }
                        }
                    }
            ) {
                canvasSize = size

                // 1. Subtle nautical/radar coordinate grid
                val gridStep = 48.dp.toPx()
                var gx = 0f
                while (gx < size.width) {
                    drawLine(
                        color = ext.mapGridLine,
                        start = Offset(gx, 0f),
                        end = Offset(gx, size.height),
                        strokeWidth = 1f
                    )
                    gx += gridStep
                }
                var gy = 0f
                while (gy < size.height) {
                    drawLine(
                        color = ext.mapGridLine,
                        start = Offset(0f, gy),
                        end = Offset(size.width, gy),
                        strokeWidth = 1f
                    )
                    gy += gridStep
                }

                // 2. Sri Lanka Island Polygon Silhouette
                val islandPath = Path()
                islandOutlineGeo.forEachIndexed { idx, (lat, lon) ->
                    val pt = projectGeoToScreen(lat, lon, size)
                    if (idx == 0) islandPath.moveTo(pt.x, pt.y) else islandPath.lineTo(pt.x, pt.y)
                }
                islandPath.close()

                drawPath(path = islandPath, color = ext.mapLandFill)
                drawPath(
                    path = islandPath,
                    color = ext.mapLandStroke,
                    style = Stroke(width = 2.2.dp.toPx())
                )

                // 3. Major District Hub Labels
                val majorHubs = listOf("Colombo", "Kandy", "Galle", "Jaffna", "Kurunegala", "Trincomalee")
                SriLankaDistricts.all.filter { it.name in majorHubs }.forEach { hub ->
                    val hubPos = projectGeoToScreen(hub.centerLat, hub.centerLon, size)
                    drawCircle(
                        color = ext.mapLandStroke.copy(alpha = 0.55f),
                        radius = 3.dp.toPx(),
                        center = hubPos
                    )
                    val measured = textMeasurer.measure(
                        text = hub.name,
                        style = TextStyle(
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceColor.copy(alpha = 0.55f)
                        )
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(hubPos.x + 5.dp.toPx(), hubPos.y - measured.size.height / 2f)
                    )
                }

                // 4. User / Selected District Radar Beacon
                val userPos = projectGeoToScreen(referenceLat, referenceLon, size)
                drawCircle(
                    color = secondaryColor.copy(alpha = (1f - pulseProgress) * 0.45f),
                    radius = (10.dp.toPx() + 26.dp.toPx() * pulseProgress),
                    center = userPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = userPos
                )
                drawCircle(
                    color = secondaryColor,
                    radius = 5.dp.toPx(),
                    center = userPos
                )

                // 5. Station Pins & Stock Badges
                stations.forEach { item ->
                    val st = item.distributor
                    val pos = projectGeoToScreen(st.latitude, st.longitude, size)
                    val isSelected = st.id == selectedStationId

                    val statusFill = when (st.statusEnum) {
                        StockStatus.AVAILABLE -> ext.statusAvailable
                        StockStatus.IN_TRANSIT -> ext.statusTransit
                        StockStatus.SCHEDULED -> ext.statusScheduled
                        StockStatus.SOLD_OUT -> ext.statusSoldOut
                    }
                    val brandRing = if (st.brandEnum == GasBrand.LITRO) ext.litroBlue else ext.laugfsYellow

                    if (st.statusEnum == StockStatus.IN_TRANSIT || isSelected) {
                        drawCircle(
                            color = statusFill.copy(alpha = (1f - pulseProgress) * 0.5f),
                            radius = 12.dp.toPx() + 18.dp.toPx() * pulseProgress,
                            center = pos
                        )
                    }

                    if (isSelected) {
                        drawCircle(
                            color = primaryColor,
                            radius = 15.dp.toPx(),
                            center = pos,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Outer brand ring + inner availability status core
                    drawCircle(
                        color = brandRing,
                        radius = 11.dp.toPx(),
                        center = pos
                    )
                    drawCircle(
                        color = statusFill,
                        radius = 7.dp.toPx(),
                        center = pos
                    )

                    // Floating Cylinder Count Pill next to pin
                    val badgeLabel = "${st.totalCylinders}"
                    val labelLayout = textMeasurer.measure(
                        text = badgeLabel,
                        style = TextStyle(
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    val pillW = labelLayout.size.width + 10.dp.toPx()
                    val pillH = labelLayout.size.height + 4.dp.toPx()
                    val pillTopLeft = Offset(pos.x - pillW / 2f, pos.y - 11.dp.toPx() - pillH - 3.dp.toPx())

                    drawRoundRect(
                        color = statusFill,
                        topLeft = pillTopLeft,
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(
                            x = pillTopLeft.x + 5.dp.toPx(),
                            y = pillTopLeft.y + 2.dp.toPx()
                        )
                    )
                }
            }

            // Legend Overlay (Top-Start)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    LegendDot(color = ext.litroBlue, label = "Litro")
                    LegendDot(color = ext.laugfsYellow, label = "LAUGFS")
                    LegendDot(color = ext.statusAvailable, label = "Avail")
                    LegendDot(color = ext.statusSoldOut, label = "0")
                }
            }

            // Zoom & Recenter Controls (Bottom-End, >= 48dp accessible touch targets)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { zoom = (zoom * 1.3f).coerceAtMost(4.5f) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_zoom_in_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom in map")
                }
                FilledTonalIconButton(
                    onClick = { zoom = (zoom / 1.3f).coerceAtLeast(0.85f) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_zoom_out_button")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom out map")
                }
                FilledTonalIconButton(
                    onClick = {
                        zoom = 1.15f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_reset_button")
                ) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Recenter Sri Lanka map")
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
