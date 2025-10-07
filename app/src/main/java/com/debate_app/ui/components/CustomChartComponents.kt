package com.debate_app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VotingStatisticsChart(
    yesVotes: Int,
    noVotes: Int,
    modifier: Modifier = Modifier
) {
    val totalVotes = yesVotes + noVotes
    val yesPercentage = if (totalVotes > 0) yesVotes.toFloat() / totalVotes else 0f
    val noPercentage = if (totalVotes > 0) noVotes.toFloat() / totalVotes else 0f
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voting Distribution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bar chart representation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yes bar
            Box(
                modifier = Modifier
                    .weight(if (yesPercentage > 0) yesPercentage else 0.01f)
                    .fillMaxHeight()
                    .padding(end = 4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            
            // No bar
            if (noPercentage > 0) {
                Box(
                    modifier = Modifier
                        .weight(noPercentage)
                        .fillMaxHeight()
                        .padding(start = 4.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Yes: $yesVotes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "No: $noVotes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun LeaderboardChart(
    leaderboardData: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    if (leaderboardData.isEmpty()) return
    
    val maxValue = leaderboardData.maxOfOrNull { it.second } ?: 1
    val barWidth = 30.dp
    val barSpacing = 12.dp
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Leaderboard",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val margin = 20f
            val barAreaWidth = leaderboardData.size * (barWidth.toPx() + barSpacing.toPx()) - barSpacing.toPx()
            val startX = (canvasWidth - barAreaWidth) / 2
            
            leaderboardData.forEachIndexed { index, (_, value) -> 
                val barHeight = if (maxValue > 0) (value.toFloat() / maxValue) * (canvasHeight - margin * 2) else 0f
                val x = startX + index * (barWidth.toPx() + barSpacing.toPx())
                val y = canvasHeight - barHeight - margin
                
                // Determine bar color using predefined colors that work with Canvas
                val barColor = when (index) {
                    0 -> androidx.compose.ui.graphics.Color(0xFF6200EE) // Purple (primary)
                    1 -> androidx.compose.ui.graphics.Color(0xFF03DAC6) // Teal (secondary)
                    2 -> androidx.compose.ui.graphics.Color(0xFFBB86FC) // Light Purple (tertiary)
                    else -> androidx.compose.ui.graphics.Color(0xFF6200EE).copy(alpha = 0.7f + (0.3f * (leaderboardData.size - index) / leaderboardData.size))
                }
                
                // Draw bar
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth.toPx(), barHeight)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // X-axis labels (first 5 users)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            leaderboardData.take(5).forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun VotingTrendChart(
    trendData: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) return
    
    val maxValue = trendData.maxOfOrNull { it.second } ?: 1
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voting Trend",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val margin = 20f
            
            if (trendData.size > 1) {
                val path = Path()
                val xStep = (canvasWidth - 2 * margin) / (trendData.size - 1)
                
                trendData.forEachIndexed { index, (_, value) ->
                    val x = margin + index * xStep
                    val y = canvasHeight - margin - if (maxValue > 0) (value.toFloat() / maxValue) * (canvasHeight - 2 * margin) else 0f
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                // Draw filled area under the line
                val fillPath = Path()
                fillPath.addPath(path)
                fillPath.lineTo(canvasWidth - margin, canvasHeight - margin)
                fillPath.lineTo(margin, canvasHeight - margin)
                fillPath.close()
                
                drawPath(
                    path = fillPath,
                    color = androidx.compose.ui.graphics.Color(0xFF6200EE).copy(alpha = 0.2f) // Purple with alpha
                )
                
                drawPath(
                    path = path,
                    color = androidx.compose.ui.graphics.Color(0xFF6200EE), // Purple
                    style = Stroke(width = 4f)
                )
                
                // Draw data points
                trendData.forEachIndexed { index, (_, value) ->
                    val x = margin + index * xStep
                    val y = canvasHeight - margin - if (maxValue > 0) (value.toFloat() / maxValue) * (canvasHeight - 2 * margin) else 0f
                    
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color(0xFF6200EE), // Purple
                        radius = 8f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White, // White center
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            trendData.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val total = data.sumOf { it.second.toDouble() }.toFloat()
    if (total <= 0) return
    
    // Use predefined colors that work with Canvas
    val colors = listOf(
        androidx.compose.ui.graphics.Color(0xFF6200EE), // Purple
        androidx.compose.ui.graphics.Color(0xFF03DAC6), // Teal
        androidx.compose.ui.graphics.Color(0xFFBB86FC), // Light Purple
        androidx.compose.ui.graphics.Color(0xFF018786), // Dark Teal
        androidx.compose.ui.graphics.Color(0xFF3700B3), // Dark Purple
        androidx.compose.ui.graphics.Color(0xFF00B3A6)  // Light Teal
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Distribution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(150.dp)
            ) {
                val canvasSize = size
                val radius = (canvasSize.minDimension / 2f) * 0.8f
                val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                
                var startAngle = 0f
                
                data.forEachIndexed { index, (_, value) ->
                    val sweepAngle = (value / total) * 360f
                    val color = colors.getOrElse(index) { androidx.compose.ui.graphics.Color(0xFF6200EE) }
                    
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = center - Offset(radius, radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    
                    startAngle += sweepAngle
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            data.forEachIndexed { index, (label, value) ->
                val percentage = (value / total) * 100
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors.getOrElse(index) { androidx.compose.ui.graphics.Color(0xFF6200EE) })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$label: ${String.format("%.1f", percentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun RadarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val maxValue = data.maxOfOrNull { it.second } ?: 1f
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Skills Comparison",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Canvas(
            modifier = Modifier.size(200.dp)
        ) {
            val canvasSize = size
            val centerX = canvasSize.width / 2f
            val centerY = canvasSize.height / 2f
            val radius = (canvasSize.minDimension / 2f) * 0.8f
            
            // Draw grid lines
            for (i in 1..4) {
                val gridRadius = radius * (i / 4f)
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF757575), // Gray for grid lines
                    radius = gridRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1f)
                )
            }
            
            // Draw axes
            data.forEachIndexed { index, _ ->
                val angle = (index * (360f / data.size) - 90) * (Math.PI / 180).toFloat()
                val x = centerX + radius * cos(angle)
                val y = centerY + radius * sin(angle)
                
                drawLine(
                    color = androidx.compose.ui.graphics.Color(0xFF757575), // Gray for axes
                    start = Offset(centerX, centerY),
                    end = Offset(x, y),
                    strokeWidth = 1f
                )
            }
            
            // Draw data polygon
            val dataPath = Path()
            data.forEachIndexed { index, (_, value) ->
                val normalizedValue = value / maxValue
                val angle = (index * (360f / data.size) - 90) * (Math.PI / 180).toFloat()
                val dataRadius = radius * normalizedValue
                val x = centerX + dataRadius * cos(angle)
                val y = centerY + dataRadius * sin(angle)
                
                if (index == 0) {
                    dataPath.moveTo(x, y)
                } else {
                    dataPath.lineTo(x, y)
                }
            }
            dataPath.close()
            
            drawPath(
                path = dataPath,
                color = androidx.compose.ui.graphics.Color(0xFF6200EE).copy(alpha = 0.3f) // Purple with alpha
            )
            
            drawPath(
                path = dataPath,
                color = androidx.compose.ui.graphics.Color(0xFF6200EE), // Purple
                style = Stroke(width = 3f)
            )
            
            // Draw data points
            data.forEachIndexed { index, (_, value) ->
                val normalizedValue = value / maxValue
                val angle = (index * (360f / data.size) - 90) * (Math.PI / 180).toFloat()
                val dataRadius = radius * normalizedValue
                val x = centerX + dataRadius * cos(angle)
                val y = centerY + dataRadius * sin(angle)
                
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF6200EE), // Purple
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun HorizontalBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val maxValue = data.maxOfOrNull { it.second } ?: 1
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Performance Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, (label, value) ->
                val percentage = if (maxValue > 0) value.toFloat() / maxValue else 0f
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Label
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                    ) {
                        // Background bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        
                        // Data bar
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(percentage)
                                .background(
                                    when (index % 3) {
                                        0 -> androidx.compose.ui.graphics.Color(0xFF6200EE) // Purple
                                        1 -> androidx.compose.ui.graphics.Color(0xFF03DAC6) // Teal
                                        else -> androidx.compose.ui.graphics.Color(0xFFBB86FC) // Light Purple
                                    }
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Value
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// Preview composables

@Preview(showBackground = true)
@Composable
private fun VotingStatisticsChartPreview() {
    MaterialTheme {
        VotingStatisticsChart(
            yesVotes = 42,
            noVotes = 18,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardChartPreview() {
    MaterialTheme {
        LeaderboardChart(
            leaderboardData = listOf(
                "Alice" to 95,
                "Bob" to 87,
                "Charlie" to 78,
                "Diana" to 92,
                "Eve" to 88
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VotingTrendChartPreview() {
    MaterialTheme {
        VotingTrendChart(
            trendData = listOf(
                "Mon" to 20,
                "Tue" to 35,
                "Wed" to 30,
                "Thu" to 45,
                "Fri" to 40,
                "Sat" to 60,
                "Sun" to 55
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PieChartPreview() {
    MaterialTheme {
        PieChart(
            data = listOf(
                "Agree" to 45f,
                "Disagree" to 30f,
                "Neutral" to 15f,
                "Abstain" to 10f
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarChartPreview() {
    MaterialTheme {
        RadarChart(
            data = listOf(
                "Research" to 8.5f,
                "Speaking" to 7.2f,
                "Logic" to 9.0f,
                "Persuasion" to 7.8f,
                "Rebuttal" to 8.1f
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HorizontalBarChartPreview() {
    MaterialTheme {
        HorizontalBarChart(
            data = listOf(
                "Wins" to 24,
                "Losses" to 8,
                "Draws" to 3,
                "Participation" to 35
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}