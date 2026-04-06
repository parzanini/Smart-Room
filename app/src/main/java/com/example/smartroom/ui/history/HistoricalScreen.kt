package com.example.smartroom.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartroom.data.model.CurrentDataResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalScreen(
    modifier: Modifier = Modifier,
    uiState: HistoricalUiState,
    onRangeSelected: (Long?, Long?) -> Unit,
    onLoadHistoryClicked: () -> Unit
) {
    // Control if the date picker is visible
    var showRangePicker by remember { mutableStateOf(false) }

    // Dialog for selecting start and end dates
    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = uiState.selectedRange.first,
            initialSelectedEndDateMillis = uiState.selectedRange.second
        )

        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onRangeSelected(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                    showRangePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(480.dp)
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Historical Data") }) }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clickable card to open the date picker
            Card(
                onClick = { showRangePicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Time Period", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.rangeLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }

            // Button to trigger data loading
            Button(
                onClick = onLoadHistoryClicked,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.selectedRange.first != null
            ) {
                Text("Refresh Data")
            }

            // Progress bar shown while loading
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Error text display
            if (uiState.errorMessage.isNotBlank()) {
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }

            // Main content shown when data is ready
            if (uiState.historicalData.isNotEmpty()) {
                // Quick stats cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Avg Temp", "${String.format("%.1f", uiState.avgTemp ?: 0f)}°C", Color.Red, Modifier.weight(1f))
                    StatCard("Avg Hum", "${String.format("%.1f", uiState.avgHum ?: 0f)}%", Color.Blue, Modifier.weight(1f))
                }

                Text("Visual Trend", style = MaterialTheme.typography.titleMedium)
                
                // Graphical line chart
                HistoryLineChart(
                    data = uiState.historicalData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else if (!uiState.isLoading) {
                // Message when no results are found
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("No data for this period", color = Color.Gray)
                    }
                }
            }
        }
    }
}

// Reusable card for a single statistic
@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Black)
        }
    }
}

// Custom chart component
@Composable
fun HistoryLineChart(data: List<CurrentDataResponse>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val orderedData = data.reversed()
    val temps = orderedData.map { it.temperature.toFloat() }
    val hums = orderedData.map { it.humidity.toFloat() }

    // Calculate Y-axis bounds
    val minTemp = (temps.minOrNull() ?: 0f) - 2f
    val maxTemp = (temps.maxOrNull() ?: 100f) + 2f
    val minHum = (hums.minOrNull() ?: 0f) - 5f
    val maxHum = (hums.maxOrNull() ?: 100f) + 5f

    // Track which point is being touched
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(orderedData) {
                    // Detect taps on the chart
                    detectTapGestures { offset ->
                        val paddingPx = 40.dp.toPx()
                        val chartWidth = size.width - paddingPx * 2
                        val stepX = if (orderedData.size > 1) chartWidth / (orderedData.size - 1) else chartWidth
                        val clickedIndex = ((offset.x - paddingPx + stepX / 2) / stepX).toInt()
                        selectedIndex = clickedIndex.coerceIn(orderedData.indices)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val paddingPx = 40.dp.toPx()
            val bottomPaddingPx = 30.dp.toPx()
            val chartWidth = width - paddingPx * 2
            val chartHeight = height - bottomPaddingPx - paddingPx

            // Draw background grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = paddingPx + chartHeight - (i * chartHeight / gridLines)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(paddingPx, y),
                    end = Offset(width - paddingPx, y),
                    strokeWidth = 1f
                )
                
                val tempVal = minTemp + (maxTemp - minTemp) * i / gridLines
                drawText(textMeasurer, String.format("%.0f", tempVal), Offset(5f, y - 10.sp.toPx()), TextStyle(Color.Red, 10.sp))
            }

            if (orderedData.isEmpty()) return@Canvas

            val stepX = if (orderedData.size > 1) chartWidth / (orderedData.size - 1) else chartWidth
            val tempPath = Path()
            val humPath = Path()

            // Build lines for temperature and humidity
            orderedData.forEachIndexed { index, entry ->
                val x = paddingPx + index * stepX
                val tY = paddingPx + chartHeight - ((entry.temperature.toFloat() - minTemp) / (maxTemp - minTemp) * chartHeight)
                val hY = paddingPx + chartHeight - ((entry.humidity.toFloat() - minHum) / (maxHum - minHum) * chartHeight)

                if (index == 0) { tempPath.moveTo(x, tY); humPath.moveTo(x, hY) }
                else { tempPath.lineTo(x, tY); humPath.lineTo(x, hY) }

                // Draw time labels on the bottom
                if (index % (orderedData.size / 4).coerceAtLeast(1) == 0 || index == orderedData.lastIndex) {
                    val time = entry.timestamp.substringAfter("T").substringBeforeLast(":")
                    drawText(textMeasurer, time, Offset(x - 15.sp.toPx(), height - 20.dp.toPx()), TextStyle(Color.Gray, 10.sp))
                }
            }

            // Render the colored paths
            drawPath(tempPath, Color.Red, style = Stroke(width = 3f))
            drawPath(humPath, Color.Blue, style = Stroke(width = 3f))

            // Show popup info when a point is selected
            selectedIndex?.let { index ->
                val entry = orderedData[index]
                val x = paddingPx + index * stepX
                val tY = paddingPx + chartHeight - ((entry.temperature.toFloat() - minTemp) / (maxTemp - minTemp) * chartHeight)
                
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(x, paddingPx),
                    end = Offset(x, height - bottomPaddingPx),
                    strokeWidth = 1f
                )
                drawCircle(Color.Red, 6f, Offset(x, tY))
                
                val label = "${entry.temperature}°C | ${entry.humidity}%"
                drawText(textMeasurer, label, Offset(x.coerceIn(paddingPx, width - 100.dp.toPx()), paddingPx - 20.dp.toPx()), TextStyle(fontWeight = FontWeight.Bold))
            }
        }
    }
}
