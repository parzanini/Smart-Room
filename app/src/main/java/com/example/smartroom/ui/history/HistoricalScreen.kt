package com.example.smartroom.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartroom.data.model.CurrentDataResponse
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// Displays historical sensor records returned by GET /api/data.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalScreen(
    modifier: Modifier = Modifier, // Modifier for layout styling
    uiState: HistoricalUiState, // Current state holds data and UI variables
    onStartDateSelected: (Long?) -> Unit, // Callback for when start date is picked
    onEndDateSelected: (Long?) -> Unit, // Callback for when end date is picked
    onLoadHistoryClicked: () -> Unit // Callback to trigger API fetch
) {
    // State to toggle the start date picker dialog
    var showStartDateDialog by remember { mutableStateOf(false) }
    // State to toggle the end date picker dialog
    var showEndDateDialog by remember { mutableStateOf(false) }

    // Check if we need to show the start date dialog
    if (showStartDateDialog) {
        // Remember the state of the start date picker, init with currently selected timestamp
        val startDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showStartDateDialog = false }, // Close dialog on outside tap
            confirmButton = {
                // Button to confirm date selection
                TextButton(onClick = {
                    onStartDateSelected(startDatePickerState.selectedDateMillis) // Invoke callback with chosen date
                    showStartDateDialog = false // Hide dialog
                }) {
                    Text("OK") // Confirm text
                }
            },
            dismissButton = {
                // Button to cancel out
                TextButton(onClick = { showStartDateDialog = false }) {
                    Text("Cancel") // Cancel text
                }
            }
        ) {
            // The actual calendar picker visual component
            DatePicker(state = startDatePickerState)
        }
    }

    // Check if we need to show the end date dialog
    if (showEndDateDialog) {
        // Remember the state of the end date picker, init with currently selected timestamp
        val endDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.endDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showEndDateDialog = false }, // Close dialog on outside tap
            confirmButton = {
                // Button to confirm date selection
                TextButton(onClick = {
                    onEndDateSelected(endDatePickerState.selectedDateMillis) // Invoke callback with chosen date
                    showEndDateDialog = false // Hide dialog
                }) {
                    Text("OK") // Confirm text
                }
            },
            dismissButton = {
                // Button to cancel out
                TextButton(onClick = { showEndDateDialog = false }) {
                    Text("Cancel") // Cancel text
                }
            }
        ) {
            // The actual calendar picker visual component
            DatePicker(state = endDatePickerState)
        }
    }

    Scaffold(
        // Top App Bar with screen title
        topBar = {
            TopAppBar(title = { Text("Historical Data") })
        }
    ) { padding ->
        // Main container mapped to scaffold padding
        Column(
            modifier = modifier
                .fillMaxSize() // Take up entire screen
                .padding(padding) // Apply scaffold insets
                .padding(16.dp), // Add extra margin
            verticalArrangement = Arrangement.spacedBy(16.dp) // Space children evenly
        ) {
            // Row grouping the two date picker buttons
            Row(
                modifier = Modifier.fillMaxWidth(), // Stretch across full width
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Gap between buttons
            ) {
                // Outlined button for selecting Start Date
                OutlinedButton(
                    onClick = { showStartDateDialog = true }, // Opens start date dialog
                    modifier = Modifier.weight(1f) // Takes up equal half width
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null) // Calendar Icon
                    Text(
                        // Show formatted date label or fallback placeholder
                        text = if (uiState.startDateLabel.isBlank()) "Start Date" else uiState.startDateLabel,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Outlined button for selecting End Date
                OutlinedButton(
                    onClick = { showEndDateDialog = true }, // Opens end date dialog
                    modifier = Modifier.weight(1f) // Takes up equal half width
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null) // Calendar Icon
                    Text(
                        // Show formatted date label or fallback placeholder
                        text = if (uiState.endDateLabel.isBlank()) "End Date" else uiState.endDateLabel,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Primary action button to load the history
            Button(
                onClick = onLoadHistoryClicked, // Triggers API call
                modifier = Modifier.fillMaxWidth(), // Fills width
                enabled = !uiState.isLoading // Disable tap when already loading
            ) {
                Text("Load History") // Button label
            }

            // Shows a loading spinner while API fetches data
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator() // Material circular spinner
                }
            }

            // Displays error message if API fails
            if (uiState.errorMessage.isNotBlank()) {
                Text(
                    text = uiState.errorMessage, // The error string
                    color = MaterialTheme.colorScheme.error, // Semantic error color
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // If there is data, show the customized line chart
            if (uiState.historicalData.isNotEmpty()) {
                HistoryLineChart(data = uiState.historicalData, modifier = Modifier.weight(1f)) // Takes remaining space
            }
        }
    }
}

@Composable
fun HistoryLineChart(data: List<CurrentDataResponse>, modifier: Modifier = Modifier) {
    // Helper to measure text size dynamically for canvas rendering
    val textMeasurer = rememberTextMeasurer()
    // Reverse data so it goes chronological from left (oldest) to right (newest)
    val orderedData = data.reversed()

    // Map the JSON objects into a flat list of temperature floats
    val temps = orderedData.map { it.temperature.toFloat() }
    // Map the JSON objects into a flat list of humidity floats
    val hums = orderedData.map { it.humidity.toFloat() }

    // Add +/- padding values to min and max temperature boundaries for better visual spacing
    val minTemp = (temps.minOrNull() ?: 0f) - 2f
    val maxTemp = (temps.maxOrNull() ?: 100f) + 2f
    // Add +/- padding values to min and max humidity boundaries
    val minHum = (hums.minOrNull() ?: 0f) - 5f
    val maxHum = (hums.maxOrNull() ?: 100f) + 5f

    // Main structural container for Chart and Legend
    Column(modifier = modifier.fillMaxWidth().background(Color.White)) {
        // Map Legend row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center, // Center legend contents
            verticalAlignment = Alignment.CenterVertically // Vertically align row
        ) {
            // Small red circle indicating temperature
            Box(modifier = Modifier.size(12.dp).background(Color.Red, CircleShape))
            Spacer(modifier = Modifier.width(4.dp)) // Padding
            Text("Temperature (°C)", style = MaterialTheme.typography.bodySmall) // Label

            Spacer(modifier = Modifier.width(16.dp)) // Middle spacing

            // Small blue circle indicating humidity
            Box(modifier = Modifier.size(12.dp).background(Color.Blue, CircleShape))
            Spacer(modifier = Modifier.width(4.dp)) // Padding
            Text("Humidity (%)", style = MaterialTheme.typography.bodySmall) // Label
        }

        // Draw the main line chart on a 2D Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width // Get current canvas width
            val height = size.height // Get current canvas height

            // Define borders to prevent drawing text cutoffs
            val padding = 40.dp.toPx()
            val bottomPadding = 30.dp.toPx()

            // Calculate actual usable chart area bounds
            val chartWidth = width - padding * 2
            val chartHeight = height - bottomPadding - padding

            // Define how many horizontal grid segments we want
            val gridLines = 5
            for (i in 0..gridLines) {
                // Compute y-coordinate for the current horizontal line segment
                val y = padding + chartHeight - (i * chartHeight / gridLines)
                
                // Draw a faint gray horizontal guideline
                drawLine(
                    color = Color.LightGray,
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 1f
                )

                // Calculate numeric temperature label mapped to this grid y-axis
                val tempVal = minTemp + (maxTemp - minTemp) * i / gridLines
                // Calculate numeric humidity label mapped to this grid y-axis
                val humVal = minHum + (maxHum - minHum) * i / gridLines

                // Render the Left Y-axis text label (Temperature in Red)
                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format("%.1f", tempVal),
                    topLeft = Offset(0f, y - 8.sp.toPx()),
                    style = TextStyle(color = Color.Red, fontSize = 10.sp)
                )

                // Render the Right Y-axis text label (Humidity in Blue)
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${humVal.toInt()}",
                    topLeft = Offset(width - padding + 4.dp.toPx(), y - 8.sp.toPx()),
                    style = TextStyle(color = Color.Blue, fontSize = 10.sp)
                )
            }

            // Short-circuit if array has no data
            if (orderedData.isEmpty()) return@Canvas

            // Compute horizontal spacing interval delta X for plotting points
            val stepX = if (orderedData.size > 1) chartWidth / (orderedData.size - 1) else chartWidth

            // Define graphic sequence containers for plotting continuous lines
            val tempPath = Path()
            val humPath = Path()

            // Connect X/Y coordinates dynamically mapping data to screen paths
            orderedData.forEachIndexed { index, entry ->
                // Next X plotting position
                val x = padding + index * stepX
                // Calculate relative vertical Y coordinate for Temp scaling min/max
                val tempY = padding + chartHeight - ((entry.temperature.toFloat() - minTemp) / (maxTemp - minTemp) * chartHeight)
                // Calculate relative vertical Y coordinate for Hum scaling min/max
                val humY = padding + chartHeight - ((entry.humidity.toFloat() - minHum) / (maxHum - minHum) * chartHeight)

                // For the very first element, start paths directly at respective points
                if (index == 0) {
                    tempPath.moveTo(x, tempY)
                    humPath.moveTo(x, humY)
                } else {
                    // For subsequent elements, draw continuing line from last point to current
                    tempPath.lineTo(x, tempY)
                    humPath.lineTo(x, humY)
                }

                // Append an X-axis timestamp label selectively (e.g., ~every 15 min or final label)
                if (index % 15 == 0 || index == orderedData.lastIndex) {
                    // Parse "2026-04-05T14:30:00Z" snippet to just "14:30"
                    val timeParts = entry.timestamp.substringAfter("T").substringBeforeLast(":")
                    if (timeParts.isNotBlank()) {
                        // Draw text representation at the bottom chart timeline
                        drawText(
                            textMeasurer = textMeasurer,
                            text = timeParts, // The short "HH:mm" time scale string
                            topLeft = Offset(x - 12.sp.toPx(), height - bottomPadding + 4.dp.toPx()),
                            style = TextStyle(color = Color.Black, fontSize = 10.sp)
                        )
                    }
                }
            }

            // Finalize paths by actually coloring the strokes onto the viewport
            // Temperature Line in Red
            drawPath(
                path = tempPath,
                color = Color.Red,
                style = Stroke(width = 3f) // Thicker stroke
            )

            // Humidity Line in Blue
            drawPath(
                path = humPath,
                color = Color.Blue,
                style = Stroke(width = 3f) // Thicker stroke
            )
        }
    }
}
