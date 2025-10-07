# Custom Chart Components for Debate App

This document provides an overview of the custom chart components that have been implemented for the Debate App, along with usage examples and integration details.

## Chart Components

### 1. VotingStatisticsChart
A horizontal bar chart that visualizes the distribution of yes/no votes for a debate or user statistics.

**Usage:**
```kotlin
VotingStatisticsChart(
    yesVotes = 42,
    noVotes = 18,
    modifier = Modifier.fillMaxWidth()
)
```

### 2. LeaderboardChart
A vertical bar chart that displays the top users in the leaderboard.

**Usage:**
```kotlin
LeaderboardChart(
    leaderboardData = listOf(
        "Alice" to 95,
        "Bob" to 87,
        "Charlie" to 78
    ),
    modifier = Modifier.fillMaxWidth()
)
```

### 3. VotingTrendChart
A line chart that shows voting trends over time with filled area and data points.

**Usage:**
```kotlin
VotingTrendChart(
    trendData = listOf(
        "Mon" to 20,
        "Tue" to 35,
        "Wed" to 30,
        "Thu" to 45
    ),
    modifier = Modifier.fillMaxWidth()
)
```

### 4. PieChart
A pie chart that visualizes data distribution in percentages using predefined colors.

**Usage:**
```kotlin
PieChart(
    data = listOf(
        "Agree" to 45f,
        "Disagree" to 30f,
        "Neutral" to 15f
    ),
    modifier = Modifier.fillMaxWidth()
)
```

### 5. RadarChart
A radar chart that displays multi-dimensional data, useful for showing skill comparisons with data points.

**Usage:**
```kotlin
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
```

### 6. HorizontalBarChart
A horizontal bar chart that displays comparative data with labels.

**Usage:**
```kotlin
HorizontalBarChart(
    data = listOf(
        "Wins" to 24,
        "Losses" to 8,
        "Draws" to 3
    ),
    modifier = Modifier.fillMaxWidth()
)
```

## Integration Examples

### Dashboard Screen
The DashboardScreen integrates multiple chart components to provide comprehensive analytics:

- VotingStatisticsChart for yes/no vote distribution
- VotingTrendChart for voting trends over time
- PieChart for voting distribution visualization
- RadarChart for skill assessments
- HorizontalBarChart for activity metrics
- LeaderboardChart for top users

### Profile Screen
The ProfileScreen includes:

- VotingStatisticsChart for personal vote distribution
- PieChart for visualizing personal voting patterns
- HorizontalBarChart for activity metrics

### Results Screen
The ResultsScreen displays:

- VotingStatisticsChart for debate results
- PieChart for visualizing debate results

## Customization

All chart components accept a `modifier` parameter for styling and layout customization. Additional customization options can be added by modifying the component implementations.

## Testing

Unit tests for the chart components are located in:
`app/src/test/java/com/debate_app/ui/components/CustomChartComponentsTest.kt`

To run tests:
```
./gradlew test
```

## Implementation Details

The chart components are implemented using Jetpack Compose Canvas API, which provides low-level drawing capabilities for custom graphics. Each component is designed to be:

- Responsive to different screen sizes
- Accessible with proper color contrast
- Compatible with dark/light themes through predefined color schemes
- Efficient in rendering performance

All chart components now properly use predefined colors instead of MaterialTheme colors in Canvas operations, ensuring they work correctly with the Canvas drawing functions while maintaining a consistent color scheme with the app's theme.

## Future Enhancements

Potential improvements for the chart components include:

1. Adding animations for data transitions
2. Implementing touch interactions for detailed data exploration
3. Adding more chart types (e.g., scatter plots, heatmaps)
4. Improving accessibility with voice descriptions
5. Adding export functionality for charts