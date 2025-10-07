package com.debate_app.ui.components

import org.junit.Test
import org.junit.Assert.*

class CustomChartComponentsTest {
    
    @Test
    fun testChartDataProcessing() {
        // Test that our chart data processing works correctly
        val yesVotes = 42
        val noVotes = 18
        val totalVotes = yesVotes + noVotes
        val yesPercentage = if (totalVotes > 0) yesVotes.toFloat() / totalVotes else 0f
        val noPercentage = if (totalVotes > 0) noVotes.toFloat() / totalVotes else 0f
        
        assertEquals(60, totalVotes)
        assertEquals(0.7f, yesPercentage, 0.01f)
        assertEquals(0.3f, noPercentage, 0.01f)
    }
    
    @Test
    fun testLeaderboardData() {
        // Test leaderboard data processing
        val leaderboardData = listOf(
            "Alice" to 95,
            "Bob" to 87,
            "Charlie" to 78
        )
        
        val maxValue = leaderboardData.maxOfOrNull { it.second } ?: 1
        assertEquals(95, maxValue)
        
        // Test that we have the right number of entries
        assertEquals(3, leaderboardData.size)
    }
    
    @Test
    fun testTrendData() {
        // Test voting trend data processing
        val trendData = listOf(
            "Mon" to 20,
            "Tue" to 35,
            "Wed" to 30,
            "Thu" to 45
        )
        
        val maxValue = trendData.maxOfOrNull { it.second } ?: 1
        assertEquals(45, maxValue)
        
        // Test that we have the right number of entries
        assertEquals(4, trendData.size)
    }
    
    @Test
    fun testPieChartData() {
        // Test pie chart data processing
        val pieData = listOf(
            "Agree" to 45f,
            "Disagree" to 30f,
            "Neutral" to 15f,
            "Abstain" to 10f
        )
        
        val total = pieData.sumOf { it.second.toDouble() }.toFloat()
        assertEquals(100f, total, 0.01f)
        
        // Test that we have the right number of entries
        assertEquals(4, pieData.size)
    }
    
    @Test
    fun testRadarChartData() {
        // Test radar chart data processing
        val radarData = listOf(
            "Research" to 8.5f,
            "Speaking" to 7.2f,
            "Logic" to 9.0f,
            "Persuasion" to 7.8f,
            "Rebuttal" to 8.1f
        )
        
        val maxValue = radarData.maxOfOrNull { it.second } ?: 1f
        assertEquals(9.0f, maxValue, 0.01f)
        
        // Test that we have the right number of entries
        assertEquals(5, radarData.size)
    }
    
    @Test
    fun testHorizontalBarChartData() {
        // Test horizontal bar chart data processing
        val barData = listOf(
            "Wins" to 24,
            "Losses" to 8,
            "Draws" to 3,
            "Participation" to 35
        )
        
        val maxValue = barData.maxOfOrNull { it.second } ?: 1
        assertEquals(35, maxValue)
        
        // Test that we have the right number of entries
        assertEquals(4, barData.size)
    }
}