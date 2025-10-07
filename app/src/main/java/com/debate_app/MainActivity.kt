package com.debate_app


import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.debate_app.ui.theme.Debate_appTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.toObject
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// Import custom components
import com.debate_app.GoogleAuthUiClient
import com.debate_app.ui.components.*
import com.debate_app.ui.screens.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MyApplication
        val repository = app.repository
        val auth = repository.auth
        val firestore = repository.firestore
        val roomDb = repository.room
        // Schedule periodic sync
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicWork = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
        setContent {
            val isDarkTheme = remember { mutableStateOf(false) }
            Debate_appTheme(darkTheme = isDarkTheme.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DailyDebateApp(auth = auth, firestore = firestore, roomDb = roomDb, repository = repository)
                }
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            // Handle Google Sign-In result
            val auth = (application as MyApplication).repository.auth
            val googleAuthUiClient = GoogleAuthUiClient(this)
            lifecycleScope.launch {
                try {
                    val success = googleAuthUiClient.signInWithIntent(
                        intent = data ?: return@launch
                    )
                    if (success) {
                        val repository = (application as MyApplication).repository
                        repository.loadUserData()
                        // Navigate to home screen after successful Google Sign-In
                        // We need to update the NavHost programmatically, so we'll trigger a recomposition via state
                        // For now, we'll just log the success, as the DailyDebateApp composable will handle the navigation
                        Log.d("MainActivity", "Google Sign-In successful, navigating to home")
                    } else {
                        Log.e("MainActivity", "Google Sign-In failed")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Google Sign-In error: ${e.message}", e)
                }
            }
        }
    }
}
@Composable
fun DailyDebateApp(auth: FirebaseAuth, firestore: FirebaseFirestore, roomDb: AppDatabase, repository: AppRepository) {
    val navController = rememberNavController()
    val isAdmin = remember { mutableStateOf(false) }
    val isDarkTheme = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope() // Add coroutine scope for suspend calls
    LaunchedEffect(auth) {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                user.getIdToken(true).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val claims = task.result?.claims
                        isAdmin.value = claims?.get("admin") == true
                        Log.d("DailyDebateApp", "Admin check: email=${user.email}, isAdmin=${isAdmin.value}")
                        // Launch a coroutine to call suspend function
                        coroutineScope.launch {
                            repository.loadUserData(
                                userId = user.uid,
                            )
                        }
                    } else {
                        Log.e("DailyDebateApp", "Failed to get admin: ${task.exception?.message}")
                        isAdmin.value = false
                    }
                }
            } else {
                navController.navigate("auth") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalRepository provides repository,
        LocalNavController provides navController
    ) {
        NavHost(
            navController = navController, 
            startDestination = if (auth.currentUser == null) "auth" else "home",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            composable("auth") {
                AuthScreen(
                    onSignInSuccess = {
                        auth.currentUser?.getIdToken(true)?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val claims = task.result?.claims
                                isAdmin.value = claims?.get("admin") == true
                                Log.d("DailyDebateApp", "After sign-in, admin: ${isAdmin.value}")
                            }
                        }
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onVote = { debateId ->
                        if (debateId != null) {
                            navController.navigate("results/$debateId")
                            // Reset selectedDebate to avoid UI flash
                            // Note: This requires passing a callback to HomeScreen to reset selectedDebate
                        } else {
                            navController.navigate("results")
                        }
                    },
                    onProfileClick = { navController.navigate("profile") },
                    onAdminClick = {
                        if (isAdmin.value) {
                            Log.d("DailyDebateApp", "Navigating to admin screen")
                            navController.navigate("admin")
                        } else {
                            Log.d("DailyDebateApp", "Admin access denied - no admin")
                        }
                    },
                    onLeaderboardClick = { navController.navigate("leaderboard") },
                    onSubmitDebateClick = { navController.navigate("submit_debate") },
                    isAdmin = isAdmin.value
                )
            }
            composable("results") {
                ResultsScreen(
                    onBack = { navController.popBackStack() },
                    debateId = null
                )
            }
            composable("results/{debateId}") { backStackEntry ->
                val debateId = backStackEntry.arguments?.getString("debateId")
                ResultsScreen(
                    onBack = { navController.popBackStack() },
                    debateId = debateId
                )
            }
            composable("profile") {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("admin") {
                if (isAdmin.value) {
                    Log.d("DailyDebateApp", "Rendering AdminScreen")
                    AdminScreen(
                        db = firestore,
                        onBack = { navController.popBackStack() },
                        isAdmin = isAdmin.value
                    )
                } else {
                    Log.d("DailyDebateApp", "Admin route accessed but no admin, redirecting")
                    navController.navigate("home") {
                        popUpTo("admin") { inclusive = true }
                    }
                }
            }
            composable("leaderboard") {
                LeaderboardScreen(onBack = { navController.popBackStack() })
            }
            composable("submit_debate") {
                SubmitDebateScreen(auth = auth, db = firestore, onBack = { navController.popBackStack() })
            }
            composable("dashboard") {
                // In a real implementation, we would fetch the required data
                // For now, we'll use mock data to demonstrate the charts
                DashboardScreen(
                    onProfileClick = { navController.navigate("profile") },
                    onLeaderboardClick = { navController.navigate("leaderboard") },
                    onSubmitDebateClick = { navController.navigate("submit_debate") },
                    onBack = { navController.popBackStack() },
                    userStats = UserStats(
                        username = "Alex",
                        yesVotes = 42,
                        noVotes = 18,
                        streak = 5,
                        score = 1250
                    ),
                    recentDebates = listOf(
                        DebateStats(
                            question = "Should we implement a 4-day work week?",
                            yesCount = 124,
                            noCount = 87,
                            date = "2023-06-15"
                        ),
                        DebateStats(
                            question = "Is social media doing more harm than good?",
                            yesCount = 98,
                            noCount = 132,
                            date = "2023-06-14"
                        ),
                        DebateStats(
                            question = "Should college education be free?",
                            yesCount = 156,
                            noCount = 74,
                            date = "2023-06-13"
                        )
                    ),
                    leaderboardData = listOf(
                        "Alice" to 95,
                        "Bob" to 87,
                        "Charlie" to 78,
                        "Diana" to 92,
                        "Eve" to 88
                    ),
                    votingTrend = listOf(
                        "Mon" to 20,
                        "Tue" to 35,
                        "Wed" to 30,
                        "Thu" to 45,
                        "Fri" to 40,
                        "Sat" to 60,
                        "Sun" to 55
                    )
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    isDarkTheme = isDarkTheme.value,
                    onThemeToggle = { isDarkTheme.value = it }
                )
            }
        }
    }
}
val LocalRepository = compositionLocalOf<AppRepository> { error("No repository provided") }
val LocalNavController = compositionLocalOf<NavController> { error("No nav controller provided") }
@Composable
fun AuthScreen(
    onSignInSuccess: () -> Unit
) {
    val repository = LocalRepository.current
    val auth = repository.auth
    val db = repository.firestore
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var googleSignInLoading by remember { mutableStateOf(false) }
    val googleAuthUiClient = remember { GoogleAuthUiClient(context = context) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // App logo or title
        Icon(
            imageVector = Icons.Default.QuestionAnswer,
            contentDescription = "Debate App",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp)
        )
        
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isSignUp) "Join the debate community" else "Sign in to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Username field (only for signup)
        if (isSignUp) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Error message
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action button
        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        if (isSignUp) {
                            if (username.isBlank()) {
                                error = "Username is required"
                                return@launch
                            }
                            
                            val result = auth.createUserWithEmailAndPassword(email, password).await()
                            val uid = result.user?.uid ?: throw Exception("Failed to create user")
                            // CHANGE: Ensure email is included
                            val userDoc = hashMapOf(
                                "id" to uid,
                                "username" to username,
                                "email" to email,
                                "yes_count" to 0,
                                "no_count" to 0,
                                "streak" to 0,
                                "last_voted_date" to null,
                                "score" to 0
                            )
                            db.collection("users").document(uid).set(userDoc).await()
                            Log.d("AuthScreen", "User created: $uid, username=$username")
                            repository.loadUserData()
                            onSignInSuccess()
                        } else {
                            auth.signInWithEmailAndPassword(email, password).await()
                            Log.d("AuthScreen", "User logged in: ${auth.currentUser?.email}")
                            repository.loadUserData()
                            onSignInSuccess()
                        }
                    } catch (e: Exception) {
                        error = when {
                            e.message?.contains("SSL_HANDSHAKE_FAILURE") == true ->
                                "Failed to verify user. Check your internet connection and device time."
                            e.message?.contains("PERMISSION_DENIED") == true ->
                                "Authentication failed due to insufficient permissions."
                            else -> "Authentication failed: ${e.message}"
                        }
                        Log.e("AuthScreen", "Auth error: ${e.message}", e)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isSignUp) "Create Account" else "Sign In",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toggle button
        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(
                text = if (isSignUp) "Already have an account? Sign In" else "Need an account? Sign Up",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "OR",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Google Sign-In button
        GoogleSignInButton(
            loading = googleSignInLoading,
            onClick = {
                googleSignInLoading = true
                coroutineScope.launch {
                    try {
                        val signInIntent = googleAuthUiClient.signIn()
                        if (signInIntent != null) {
                            // We'll handle the result in the activity
                            (context as? ComponentActivity)?.startIntentSenderForResult(
                                signInIntent,
                                100,
                                Intent(),
                                0,
                                0,
                                0,
                                null
                            )
                        } else {
                            error = "Google Sign-In failed to initialize"
                        }
                    } catch (e: Exception) {
                        error = "Google Sign-In failed: ${e.message}"
                        Log.e("AuthScreen", "Google sign-in error: ${e.message}", e)
                    } finally {
                        googleSignInLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}


@Composable
fun HomeScreen(
    onVote: (String?) -> Unit,
    onProfileClick: () -> Unit,
    onAdminClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onSubmitDebateClick: () -> Unit,
    isAdmin: Boolean,
    onResetSelection: () -> Unit = {} // Keep reset callback for consistency
) {
    val repository = LocalRepository.current
    val coroutineScope = rememberCoroutineScope()
    val allDebates by repository.getAllDebates().collectAsState(initial = emptyList())
    var selectedDebate by remember { mutableStateOf<LocalDebate?>(null) }
    var hasVoted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isVoting by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val debates = allDebates.filter { debate ->
        searchQuery.isEmpty() || debate.question.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(debates, selectedDebate) {
        if (selectedDebate != null) {
            hasVoted = repository.hasVotedOnDebate(selectedDebate!!.id)
            Log.d("HomeScreen", "Checked hasVoted for debate ${selectedDebate!!.id}: $hasVoted")
        } else {
            hasVoted = false
            Log.d("HomeScreen", "No debates selected, hasVoted set to false")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom App Bar
        CustomAppBar(
            title = "Daily Debate",
            onProfileClick = onProfileClick,
            onLeaderboardClick = onLeaderboardClick,
            onSubmitDebateClick = onSubmitDebateClick,
            onAdminClick = if (isAdmin) onAdminClick else null
        )

        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { /* Search is handled by the filter above */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (debates.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(64.dp))
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "No debates",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No debates today" else "No debates found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Check back later or submit a debate question!" else "Try a different search term",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(debates) { debate ->
                    DebateCard(
                        question = debate.question,
                        isSelected = selectedDebate?.id == debate.id,
                        onClick = {
                            // Navigate to ResultsScreen instead of updating selectedDebate
                            onVote(debate.id)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (selectedDebate == null) {
                        Text(
                            text = "Select a debate to view details",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (hasVoted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Voted",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You already voted on this debate!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Add a button to navigate to ResultsScreen
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    onVote(selectedDebate?.id)
                                    onResetSelection()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View Results")
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = selectedDebate!!.question,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // Stats cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatsCard(
                                    title = "Yes Votes",
                                    value = selectedDebate!!.yes_count.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                StatsCard(
                                    title = "No Votes",
                                    value = selectedDebate!!.no_count.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Voting distribution chart
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    VotingStatisticsChart(
                                        yesVotes = selectedDebate!!.yes_count,
                                        noVotes = selectedDebate!!.no_count,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Pie chart for voting distribution
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    PieChart(
                                        data = listOf(
                                            "Yes Votes" to selectedDebate!!.yes_count.toFloat(),
                                            "No Votes" to selectedDebate!!.no_count.toFloat()
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Voting buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VoteButton(
                                    text = "YES",
                                    onClick = {
                                        if (!isVoting) {
                                            isVoting = true
                                            coroutineScope.launch {
                                                try {
                                                    repository.castVote(selectedDebate!!.id, "yes")
                                                    onVote(selectedDebate?.id)
                                                    hasVoted = true
                                                    onResetSelection()
                                                } catch (e: Exception) {
                                                    error = e.message
                                                    Log.e("HomeScreen", "Vote error: ${e.message}", e)
                                                } finally {
                                                    isVoting = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isVoting,
                                    isVoting = isVoting,
                                    modifier = Modifier.weight(1f)
                                )
                                VoteButton(
                                    text = "NO",
                                    onClick = {
                                        if (!isVoting) {
                                            isVoting = true
                                            coroutineScope.launch {
                                                try {
                                                    repository.castVote(selectedDebate!!.id, "no")
                                                    onVote(selectedDebate?.id)
                                                    hasVoted = true
                                                    onResetSelection()
                                                } catch (e: Exception) {
                                                    error = e.message
                                                    Log.e("HomeScreen", "Vote error: ${e.message}", e)
                                                } finally {
                                                    isVoting = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isVoting,
                                    isVoting = isVoting,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        }
                    }
                }
                error?.let {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }


@Composable
fun ResultsScreen(
    onBack: () -> Unit,
    debateId: String? = null
) {
    val repository = LocalRepository.current
    val auth = repository.auth
    val debate by if (debateId != null) {
        repository.getDebateById(debateId).collectAsState(initial = null)
    } else {
        repository.getCurrentDebate().collectAsState(initial = null)
    }
    val comments by repository.getComments().collectAsState(initial = emptyList())
    var newComment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val user by repository.getUser().collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(debateId) {
        if (debateId != null) {
            try {
                repository.loadDebate(debateId)
                Log.d("ResultsScreen", "Loaded debate with ID: $debateId")
            } catch (e: Exception) {
                error = "Failed to load debate: ${e.message}"
                Log.e("ResultsScreen", "Error loading debate: ${e.message}", e)
            }
        } else {
            Log.d("ResultsScreen", "No debateId provided, loading current debate")
        }
    }

    LaunchedEffect(debate) {
        if (debate != null) {
            try {
                repository.loadComments(debate!!.id)
                Log.d("ResultsScreen", "Loaded comments for debate ID: ${debate!!.id}")
            } catch (e: Exception) {
                error = "Failed to load comments: ${e.message}"
                Log.e("ResultsScreen", "Error loading comments: ${e.message}", e)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom App Bar
        CustomAppBar(
            title = "Results",
            onProfileClick = {},
            onLeaderboardClick = {},
            onSubmitDebateClick = {},
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (error != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else if (debate == null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(64.dp))
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "No results",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No debate results available",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = debate!!.question,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsCard(
                            title = "Yes Votes",
                            value = debate!!.yes_count.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatsCard(
                            title = "No Votes",
                            value = debate!!.no_count.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    StatsCard(
                        title = "Total Votes",
                        value = (debate!!.yes_count + debate!!.no_count).toString(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Voting distribution chart
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            VotingStatisticsChart(
                                yesVotes = debate!!.yes_count,
                                noVotes = debate!!.no_count,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pie chart for voting distribution
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            PieChart(
                                data = listOf(
                                    "Yes Votes" to debate!!.yes_count.toFloat(),
                                    "No Votes" to debate!!.no_count.toFloat()
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Check if user has voted on this debate
                    val hasVoted = remember { mutableStateOf(false) }
                    LaunchedEffect(debate) {
                        if (debate != null) {
                            hasVoted.value = repository.hasVotedOnDebate(debate!!.id)
                        }
                    }
                    
                    // Show voting buttons if user hasn't voted yet
                    if (!hasVoted.value) {
                        var isVoting by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VoteButton(
                                text = "YES",
                                onClick = {
                                    if (!isVoting) {
                                        isVoting = true
                                        coroutineScope.launch {
                                            try {
                                                repository.castVote(debate!!.id, "yes")
                                                hasVoted.value = true
                                            } catch (e: Exception) {
                                                error = e.message
                                                Log.e("ResultsScreen", "Vote error: ${e.message}", e)
                                            } finally {
                                                isVoting = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isVoting,
                                isVoting = isVoting,
                                modifier = Modifier.weight(1f)
                            )
                            VoteButton(
                                text = "NO",
                                onClick = {
                                    if (!isVoting) {
                                        isVoting = true
                                        coroutineScope.launch {
                                            try {
                                                repository.castVote(debate!!.id, "no")
                                                hasVoted.value = true
                                            } catch (e: Exception) {
                                                error = e.message
                                                Log.e("ResultsScreen", "Vote error: ${e.message}", e)
                                            } finally {
                                                isVoting = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isVoting,
                                isVoting = isVoting,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Show message that user has already voted
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Voted",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You've already voted!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Comment section
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        label = { Text("Add a comment") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    repository.addComment(debate!!.id, newComment)
                                    newComment = ""
                                } catch (e: Exception) {
                                    error = "Failed to add comment: ${e.message}"
                                    Log.e("ResultsScreen", "Comment error: ${e.message}", e)
                                }
                            }
                        },
                        enabled = newComment.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Comment")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(comments.filter { it.debate_id == debate!!.id }) { comment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = comment.username,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = comment.created_at,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = comment.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = "Likes",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${comment.likes}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun ProfileScreen(onBack: () -> Unit, onSettingsClick: () -> Unit) {
    val repository = LocalRepository.current
    val auth = repository.auth
    val user by repository.getUser().collectAsState(initial = null)
    val achievements by repository.getAchievements().collectAsState(initial = emptyList())
    var error by remember { mutableStateOf<String?>(null) }
    // CHANGE: Added error handling for user data loading
    LaunchedEffect(Unit) {
        try {
            repository.loadUserData()
        } catch (e: Exception) {
            error = "Failed to load user data: ${e.message}"
            Log.e("ProfileScreen", "Load user error: ${e.message}", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Custom App Bar
        CustomAppBar(
            title = "Profile",
            onProfileClick = {},
            onLeaderboardClick = {},
            onSubmitDebateClick = {},
            onAdminClick = onSettingsClick,
            onBackClick = onBack
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else if (user == null) {
                LoadingIndicator(text = "Loading profile...")
            } else {
                // Profile header
                ProfileHeader(
                    username = user!!.username,
                    email = user!!.email,
                    streak = user!!.streak,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats grid
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            title = "Yes Votes",
                            value = user!!.yes_count.toString(),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Yes Votes",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "No Votes",
                            value = user!!.no_count.toString(),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ThumbDown,
                                    contentDescription = "No Votes",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            title = "Score",
                            value = user!!.score.toString(),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Score",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Streak",
                            value = "${user!!.streak} days",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Voting distribution chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        VotingStatisticsChart(
                            yesVotes = user!!.yes_count,
                            noVotes = user!!.no_count,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pie chart for voting distribution
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        PieChart(
                            data = listOf(
                                "Yes Votes" to user!!.yes_count.toFloat(),
                                "No Votes" to user!!.no_count.toFloat()
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Horizontal bar chart for activity metrics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        HorizontalBarChart(
                            data = listOf(
                                "Total Votes" to (user!!.yes_count + user!!.no_count),
                                "Score" to user!!.score,
                                "Streak" to user!!.streak,
                                "Comments" to 12 // This would need to be fetched from the repository
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Achievements section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Achievements",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // CHANGE: Using provided allAchievements list
                    val allAchievements = listOf(
                        Triple("first_vote", "First Vote", "Cast your first vote"),
                        Triple("debate_master", "Debate Master", "Vote in 10 debates"),
                        Triple("streak_starter", "Streak Starter", "Achieve a 3-day voting streak"),
                        Triple("commentator", "Commentator", "Post 5 comments"),
                        Triple("balanced_voter", "Balanced Voter", "Cast at least 5 Yes and 5 No votes"),
                        Triple("debate_creator", "Debate Creator", "Have a debate question approved")
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allAchievements) { (achievement_id, name, description) ->
                            val achievement = achievements.find { it.achievement_id == achievement_id }
                                ?: LocalAchievement(
                                    user_id = user!!.id,
                                    achievement_id = achievement_id,
                                    name = achievement_id,
                                    description = description,
                                    awarded_at = ""
                                )

                            AchievementCard(
                                name = name,
                                description = description,
                                isUnlocked = achievement.awarded_at.isNotEmpty(),
                                awardedAt = if (achievement.awarded_at.isNotEmpty()) {
                                    try {
                                        val timestamp = achievement.awarded_at.toLongOrNull() ?: 0
                                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        dateFormat.format(Date(timestamp))
                                    } catch (e: Exception) {
                                        achievement.awarded_at
                                    }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { auth.signOut() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val repository = LocalRepository.current
    var leaders by remember { mutableStateOf<List<LocalUser>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentUser = repository.auth.currentUser
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                leaders = repository.getLeaderboardUsers()
                Log.d("LeaderboardScreen", "Loaded ${leaders.size} leaders")
            } catch (e: Exception) {
                error = "Failed to load leaderboard: ${e.message}"
                Log.e("LeaderboardScreen", "Error: ${e.message}", e)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom App Bar
        CustomAppBar(
            title = "Leaderboard",
            onProfileClick = {},
            onLeaderboardClick = {},
            onSubmitDebateClick = {},
            onBackClick = onBack
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                TrophyHeader(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (error != null) {
                item {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else if (leaders.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(64.dp))
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "No leaders",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No leaders yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start voting to appear on the leaderboard!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                itemsIndexed(leaders) { index, user ->
                    LeaderboardItem(
                        rank = index + 1,
                        username = user.username,
                        score = user.score,
                        isCurrentUser = user.id == currentUser?.uid
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
    }
}
@Composable
fun SubmitDebateScreen(auth: FirebaseAuth, db: FirebaseFirestore, onBack: () -> Unit) {
    var question by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom App Bar
        CustomAppBar(
            title = "Submit Debate",
            onProfileClick = {},
            onLeaderboardClick = {},
            onSubmitDebateClick = {},
            onBackClick = onBack
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Submit a Debate",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Share your thought-provoking questions with the community",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Debate Question") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text("e.g., Should we implement a 4-day work week?") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val user = auth.currentUser ?: throw Exception("Not signed in")
                            val userDoc = db.collection("users").document(user.uid).get().await()
                            val username = userDoc.getString("username") ?: "Anonymous"
                            db.collection("debate_submissions").add(
                                mapOf(
                                    "user_id" to user.uid,
                                    "username" to username,
                                    "question" to question,
                                    "status" to "pending",
                                    "submitted_at" to FieldValue.serverTimestamp()
                                )
                            ).await()
                            success = "Debate submitted for review!"
                            question = ""
                        } catch (e: Exception) {
                            error = "Failed to submit: ${e.message}"
                            Log.e("SubmitDebateScreen", "Error: ${e.message}", e)
                        }
                    }
                },
                enabled = question.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Submit",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            success?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tips section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tips for a great debate question:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = """
        • Be clear and concise
        • Avoid bias or leading language
        • Make it thought-provoking
        • Ensure it can be answered with Yes or No
    """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
fun AdminScreen(db: FirebaseFirestore, onBack: () -> Unit, isAdmin: Boolean) {
    val repository = LocalRepository.current
    val scope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var submissions by remember { mutableStateOf<List<DebateSubmission>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("debate_submissions")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                submissions = snapshot.documents.mapNotNull { doc ->
                    val submission = doc.toObject(DebateSubmission::class.java)
                    submission?.copy(id = doc.id)
                }
            }
            .addOnFailureListener { e ->
                error = "Failed to load submissions: ${e.message}"
                Log.e("AdminScreen", "Submissions error: ${e.message}", e)
            }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom App Bar
        CustomAppBar(
            title = "Admin Panel",
            onProfileClick = {},
            onLeaderboardClick = {},
            onSubmitDebateClick = {},
            onBackClick = onBack
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Admin Panel",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("New Debate Question") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isLoading) return@Button
                    isLoading = true
                    scope.launch {
                        try {
                            val debateId = UUID.randomUUID().toString()
                            db.collection("debates").document(debateId).set(
                                hashMapOf(
                                    "id" to debateId,
                                    "question" to question,
                                    "yes_count" to 0,
                                    "no_count" to 0,
                                    "date" to date
                                )
                            ).await()
                            repository.syncDebate(debateId)
                            success = "Debate created with ID $debateId for $date!"
                            question = ""
                        } catch (e: Exception) {
                            error = "Failed to create debate: ${e.message}"
                            Log.e("AdminScreen", "Create error: ${e.message}", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = question.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (isLoading) "Creating..." else "Create Debate",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            repository.clearVoteData()
                            repository.clearFirestoreVotes()
                            repository.clearFirestoreDebates()
                            success = "Cleared vote and debate data successfully!"
                        } catch (e: Exception) {
                            error = "Failed to clear data: ${e.message}"
                            Log.e("AdminScreen", "Clear data error: ${e.message}", e)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Clear Vote and Debate Data",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Pending Debate Submissions",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (submissions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "No submissions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No pending submissions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(submissions) { submission ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "By ${submission.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = submission.question,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val debateId = UUID.randomUUID().toString()
                                                    val debateDate = LocalDate.now().toString()
                                                    db.collection("debate_submissions").document(submission.id)
                                                        .update("status", "approved").await()
                                                    db.collection("debates").document(debateId).set(
                                                        mapOf(
                                                            "id" to debateId,
                                                            "question" to submission.question,
                                                            "yes_count" to 0,
                                                            "no_count" to 0,
                                                            "date" to debateDate
                                                        )
                                                    ).await()
                                                    repository.syncDebate(debateId)
                                                    success = "Debate approved and created with ID $debateId for $debateDate!"
                                                } catch (e: Exception) {
                                                    error = "Failed to approve: ${e.message}"
                                                    Log.e("AdminScreen", "Approve error: ${e.message}", e)
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Approve")
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    db.collection("debate_submissions").document(submission.id)
                                                        .update("status", "rejected").await()
                                                } catch (e: Exception) {
                                                    error = "Failed to reject: ${e.message}"
                                                    Log.e("AdminScreen", "Reject error: ${e.message}", e)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            success?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}


data class Debate(
    val id: String = "",
    val question: String = "",
    val yes_count: Int = 0,
    val no_count: Int = 0,
    val date: String = ""
)

data class Comment(
    val id: String = "",
    val debate_id: String = "",
    val user_id: String = "",
    val username: String = "",
    val content: String = "",
    val created_at: String = "",
    val likes: Int = 0
)

// CHANGE: Added @IgnoreExtraProperties to ignore unexpected fields like 'achievements' array
@IgnoreExtraProperties
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val yes_count: Int = 0,
    val no_count: Int = 0,
    val streak: Int = 0,
    val last_voted_date: String? = null,
    val score: Int = 0
)

data class Achievement(
    val name: String = "",
    val description: String = "",
    val awarded_at: String = "" // Changed to String to match LocalAchievement
)

data class DebateSubmission(
    val id: String = "",
    val user_id: String = "",
    val username: String = "",
    val question: String = "",
    val status: String = "pending",
    val submitted_at: Timestamp? = null
)



@Entity(tableName = "debates")
data class LocalDebate(
    @PrimaryKey val id: String,
    val question: String = "",
    val yes_count: Int = 0,
    val no_count: Int = 0,
    val date: String = ""
)

@Entity(tableName = "user_votes")
data class UserVote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user_id: String,
    val debate_id: String,
    val choice: String,
    val voted_at: String
)

@Entity(tableName = "comments")
data class LocalComment(
    @PrimaryKey val comment_id: String,
    val debate_id: String,
    val user_id: String,
    val username: String,
    val content: String,
    val created_at: String,
    val likes: Int
)

@Entity(tableName = "pending_votes")
data class PendingVote(
    @PrimaryKey val id: String,
    val debate_id: String,
    val choice: String,
    val synced: Boolean = false
)

@Entity(tableName = "pending_comments")
data class PendingComment(
    @PrimaryKey val id: String,
    val debate_id: String,
    val user_id: String,
    val username: String,
    val content: String,
    val created_at: String,
    val synced: Boolean = false
)

@Entity(tableName = "users")
data class LocalUser(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val yes_count: Int = 0,
    val no_count: Int = 0,
    val streak: Int = 0,
    val last_voted_date: String? = null,
    val score: Int = 0
)

@Entity(tableName = "achievements")
data class LocalAchievement(
    val user_id: String,
    @PrimaryKey val achievement_id: String,
    val name: String,
    val description: String,
    val awarded_at: String
)

class AppRepository(
    val auth: FirebaseAuth,
    val firestore: FirebaseFirestore,
    val room: AppDatabase,
    private val application: Application
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debatesListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var achievementsListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null

    init {
        setupDebatesListener()
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                Log.d("AppRepository", "Auth state changed: User ${firebaseAuth.currentUser?.uid} signed in")
                setupAuthDependentListeners()
                scope.launch {
                    try {
                        loadUserData()
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error initializing user data: ${e.message}", e)
                    }
                }
            } else {
                Log.d("AppRepository", "Auth state changed: No user signed in")
                removeAuthDependentListeners()
            }
        }
        scope.launch {
            try {
                val today = LocalDate.now().toString()
                retryFirestoreOperation {
                    val snapshot = firestore.collection("debates")
                        .whereEqualTo("date", today)
                        .orderBy("id", Query.Direction.DESCENDING)
                        .get(Source.SERVER)
                        .await()
                    if (snapshot.isEmpty) {
                        Log.d("AppRepository", "No debates found in Firestore for $today")
                        room.debateDao().deleteAll()
                    } else {
                        snapshot.documents.forEach { doc ->
                            val debate = doc.toObject(Debate::class.java) ?: return@forEach
                            room.debateDao().insert(LocalDebate(
                                id = doc.id,
                                question = debate.question,
                                yes_count = debate.yes_count,
                                no_count = debate.no_count,
                                date = debate.date
                            ))
                            Log.d("AppRepository", "Initialized debate for $today: ${debate.question}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "Error initializing debates: ${e.message}", e)
            }
        }
    }

    private fun setupDebatesListener() {
        debatesListener?.remove()
        debatesListener = firestore.collection("debates").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("AppRepository", "Debates listener error: ${e.message}", e)
                return@addSnapshotListener
            }
            scope.launch {
                if (snapshot?.isEmpty == true) {
                    Log.d("AppRepository", "Debates snapshot empty, clearing local debates")
                    room.debateDao().deleteAll()
                } else {
                    snapshot?.documents?.forEach { doc ->
                        try {
                            val debate = doc.toObject(Debate::class.java) ?: return@forEach
                            room.debateDao().insert(LocalDebate(
                                id = doc.id,
                                question = debate.question,
                                yes_count = debate.yes_count,
                                no_count = debate.no_count,
                                date = debate.date
                            ))
                            Log.d("AppRepository", "Synced debate: ${doc.id}, question: ${debate.question}")
                        } catch (e: Exception) {
                            Log.e("AppRepository", "Error syncing debate ${doc.id}: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }

    private fun setupAuthDependentListeners() {
        val user = auth.currentUser ?: return
        Log.d("AppRepository", "Setting up listeners for user ${user.uid}")

        userListener?.remove()
        userListener = firestore.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AppRepository", "User listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    try {
                        if (!snapshot?.exists()!!) {
                            Log.d("AppRepository", "User ${user.uid} does not exist, creating default")
                            val defaultUser = LocalUser(
                                id = user.uid,
                                username = user.email?.substringBefore("@") ?: "User",
                                email = user.email ?: "",
                                yes_count = 0,
                                no_count = 0,
                                streak = 0,
                                last_voted_date = "",
                                score = 0
                            )
                            room.userDao().insert(defaultUser)
                            firestore.collection("users").document(user.uid).set(defaultUser).await()
                        } else {
                            val userData = snapshot.toObject(User::class.java) ?: return@launch
                            room.userDao().insert(LocalUser(
                                id = snapshot.id,
                                username = userData.username,
                                email = userData.email ?: user.email ?: "",
                                yes_count = userData.yes_count,
                                no_count = userData.no_count,
                                streak = userData.streak,
                                last_voted_date = userData.last_voted_date,
                                score = userData.score
                            ))
                            Log.d("AppRepository", "Synced user: ${snapshot.id}, username=${userData.username}")
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error syncing user ${snapshot?.id}: ${e.message}", e)
                    }
                }
            }

        achievementsListener?.remove()
        achievementsListener = firestore.collection("users").document(user.uid)
            .collection("achievements")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AppRepository", "Achievements listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    if (snapshot?.isEmpty == true) {
                        Log.d("AppRepository", "Achievements snapshot empty, clearing local achievements")
                        room.achievementDao().deleteAll()
                    } else {
                        snapshot?.documents?.forEach { doc ->
                            try {
                                val ach = doc.toObject(Achievement::class.java) ?: return@forEach
                                room.achievementDao().insert(LocalAchievement(
                                    user_id = user.uid,
                                    achievement_id = doc.id,
                                    name = ach.name,
                                    description = ach.description,
                                    awarded_at = ach.awarded_at
                                ))
                                Log.d("AppRepository", "Synced achievement: ${doc.id}, name=${ach.name}")
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error syncing achievement ${doc.id}: ${e.message}", e)
                            }
                        }
                    }
                }
            }

        commentsListener?.remove()
        commentsListener = firestore.collectionGroup("comments")
            .whereEqualTo("user_id", user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AppRepository", "Comments listener error: ${e.message}", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    if (snapshot?.isEmpty == true) {
                        Log.d("AppRepository", "Comments snapshot empty, clearing local comments")
                        room.commentDao().deleteAll()
                    } else {
                        snapshot?.documents?.forEach { doc ->
                            try {
                                val comment = doc.toObject(Comment::class.java) ?: return@forEach
                                room.commentDao().insert(LocalComment(
                                    comment_id = doc.id,
                                    debate_id = comment.debate_id,
                                    user_id = comment.user_id,
                                    username = comment.username,
                                    content = comment.content,
                                    created_at = comment.created_at,
                                    likes = comment.likes
                                ))
                                Log.d("AppRepository", "Synced comment: ${doc.id}, debate=${comment.debate_id}")
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error syncing comment ${doc.id}: ${e.message}", e)
                            }
                        }
                    }
                }
            }
    }

    private fun removeAuthDependentListeners() {
        userListener?.remove()
        achievementsListener?.remove()
        commentsListener?.remove()
        Log.d("AppRepository", "Removed auth-dependent listeners")
    }

    suspend fun clearLocalDatabase() {
        room.debateDao().deleteAll()
        room.userDao().deleteAll()
        room.achievementDao().deleteAll()
        room.commentDao().deleteAll()
        room.userVoteDao().deleteAll()
        room.pendingVoteDao().deleteAll()
        room.pendingCommentDao().deleteAll()
        Log.d("AppRepository", "Cleared all local Room data")
    }

    suspend fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        loadUserData(userId)
    }

    suspend fun loadUserData(userId: String) {
        retryFirestoreOperation {
            val snapshot = firestore.collection("users").document(userId).get(Source.SERVER).await()
            if (!snapshot.exists()) {
                val defaultUser = LocalUser(
                    id = userId,
                    username = auth.currentUser?.email?.substringBefore("@") ?: "User",
                    email = auth.currentUser?.email ?: "",
                    yes_count = 0,
                    no_count = 0,
                    streak = 0,
                    last_voted_date = "",
                    score = 0
                )
                room.userDao().insert(defaultUser)
                syncUserToLeaderboard(userId)
                firestore.collection("users").document(userId).set(defaultUser).await()
            } else {
                val userData = snapshot.toObject(User::class.java) ?: return@retryFirestoreOperation
                room.userDao().insert(LocalUser(
                    id = snapshot.id,
                    username = userData.username,
                    email = userData.email ?: auth.currentUser?.email ?: "",
                    yes_count = userData.yes_count,
                    no_count = userData.no_count,
                    streak = userData.streak,
                    last_voted_date = userData.last_voted_date,
                    score = userData.score
                ))
                Log.d("AppRepository", "Loaded user data for $userId: ${userData.username}")
            }
        }
    }

    fun getCurrentDebate(date: String = LocalDate.now().toString()): Flow<LocalDebate?> =
        room.debateDao().getDebatesForDate(date).map { debates ->
            debates.maxByOrNull { it.id }
        }

    fun getDebateById(debateId: String): Flow<LocalDebate?> =
        room.debateDao().getDebate(debateId)

    fun getDebatesForDate(date: String): Flow<List<LocalDebate>> =
        room.debateDao().getDebatesForDate(date)

    fun getAllDebates(): Flow<List<LocalDebate>> =
        room.debateDao().getAllDebates()

    fun getUser(): Flow<LocalUser?> {
        val userId = auth.currentUser?.uid ?: return flowOf(null)
        return room.userDao().getUser(userId)
    }

    fun getAchievements(): Flow<List<LocalAchievement>> {
        val userId = auth.currentUser?.uid ?: return flowOf(emptyList())
        return room.achievementDao().getAchievements(userId)
    }

    fun getComments(): Flow<List<LocalComment>> = room.commentDao().getComments()

    suspend fun hasVotedOnDebate(debateId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        Log.d("AppRepository", "Checking vote for user $userId, debate $debateId")
        val localVote = room.userVoteDao().getVoteForDebate(userId, debateId)
        Log.d("AppRepository", "Local vote: $localVote")
        if (localVote != null) return true
        val pendingVote = room.pendingVoteDao().getPendingVotes().find { it.debate_id == debateId && !it.synced }
        Log.d("AppRepository", "Pending vote: $pendingVote")
        if (pendingVote != null) return true
        return try {
            val voteSnapshot = firestore.collection("votes")
                .whereEqualTo("debate_id", debateId)
                .whereEqualTo("user_id", userId)
                .limit(1)
                .get(Source.SERVER)
                .await()
            Log.d("AppRepository", "Firestore votes: ${voteSnapshot.documents}")
            !voteSnapshot.isEmpty
        } catch (e: Exception) {
            Log.e("AppRepository", "Error checking Firestore votes for debate $debateId: ${e.message}", e)
            false
        }
    }

    suspend fun loadDebate(debateId: String) {
        retryFirestoreOperation {
            val snapshot = firestore.collection("debates").document(debateId).get(Source.SERVER).await()
            val debate = snapshot.toObject(Debate::class.java) ?: return@retryFirestoreOperation
            room.debateDao().insert(LocalDebate(
                id = snapshot.id,
                question = debate.question,
                yes_count = debate.yes_count,
                no_count = debate.no_count,
                date = debate.date
            ))
            Log.d("AppRepository", "Loaded debate for $debateId: ${debate.question}")
        }
    }

    suspend fun syncDebate(debateId: String) {
        retryFirestoreOperation {
            val snapshot = firestore.collection("debates").document(debateId).get(Source.SERVER).await()
            val debate = snapshot.toObject(Debate::class.java) ?: return@retryFirestoreOperation
            room.debateDao().insert(LocalDebate(
                id = snapshot.id,
                question = debate.question,
                yes_count = debate.yes_count,
                no_count = debate.no_count,
                date = debate.date
            ))
            Log.d("AppRepository", "Force synced debate: $debateId, question: ${debate.question}")
            enqueueSync()
        }
    }

    suspend fun castVote(debateId: String, choice: String) {
        val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        if (hasVotedOnDebate(debateId)) throw Exception("Already voted on this debate")

        val debate = room.debateDao().getDebateSuspend(debateId)
        if (debate == null) {
            throw Exception("Debate not found")
        }

        var user = room.userDao().getUser(userId).first()
        if (user == null) {
            user = LocalUser(
                id = userId,
                username = auth.currentUser?.email?.substringBefore("@") ?: "User",
                email = auth.currentUser?.email ?: "",
                yes_count = 0,
                no_count = 0,
                streak = 0,
                last_voted_date = "",
                score = 0
            )
            room.userDao().insert(user)
            firestore.collection("users").document(userId).set(user).await()
        }

        val newDebate = debate.copy(
            yes_count = if (choice == "yes") debate.yes_count + 1 else debate.yes_count,
            no_count = if (choice == "no") debate.no_count + 1 else debate.no_count
        )
        room.debateDao().insert(newDebate)
        val newCountYes = if (choice == "yes") user.yes_count + 1 else user.yes_count
        val newCountNo = if (choice == "no") user.no_count + 1 else user.no_count
        val lastVoted = user.last_voted_date
        val newStreak = if (lastVoted == LocalDate.now().minusDays(1).toString()) user.streak + 1 else 1
        val newUser = user.copy(
            yes_count = newCountYes,
            no_count = newCountNo,
            streak = newStreak,
            last_voted_date = LocalDate.now().toString()
        )
        room.userDao().insert(newUser)
        syncUserToLeaderboard(userId)
        room.userVoteDao().insert(UserVote(user_id = userId, debate_id = debateId, choice = choice, voted_at = LocalDate.now().toString()))
        val voteId = UUID.randomUUID().toString()
        room.pendingVoteDao().insert(PendingVote(id = voteId, debate_id = debateId, choice = choice))
        try {
            firestore.collection("debates").document(debateId).set(newDebate).await()
            firestore.collection("users").document(userId).set(newUser).await()
            firestore.collection("votes").document(voteId).set(mapOf(
                "debate_id" to debateId,
                "user_id" to userId,
                "choice" to choice,
                "voted_at" to LocalDate.now().toString()
            )).await()
            room.pendingVoteDao().deleteById(voteId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error syncing vote: ${e.message}", e)
        }
        checkAndAwardAchievementsLocal(userId)
        updateUserScoreLocal(userId)
        enqueueSync()
    }

    suspend fun loadComments(debateId: String) {
        retryFirestoreOperation {
            val snapshot = firestore.collection("debates").document(debateId)
                .collection("comments")
                .get(Source.SERVER)
                .await()
            if (snapshot.isEmpty) {
                Log.d("AppRepository", "No comments found for debate $debateId")
            } else {
                snapshot.documents.forEach { doc ->
                    val comment = doc.toObject(Comment::class.java) ?: return@forEach
                    room.commentDao().insert(LocalComment(
                        comment_id = doc.id,
                        debate_id = comment.debate_id,
                        user_id = comment.user_id,
                        username = comment.username,
                        content = comment.content,
                        created_at = comment.created_at,
                        likes = comment.likes
                    ))
                }
            }
        }
    }

    suspend fun addComment(debateId: String, content: String) {
        val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        var user = room.userDao().getUser(userId).first()
        if (user == null) {
            user = LocalUser(
                id = userId,
                username = auth.currentUser?.email?.substringBefore("@") ?: "User",
                email = auth.currentUser?.email ?: "",
                yes_count = 0,
                no_count = 0,
                streak = 0,
                last_voted_date = "",
                score = 0
            )
            room.userDao().insert(user)
            syncUserToLeaderboard(userId)
            firestore.collection("users").document(userId).set(user).await()
        }
        val commentId = UUID.randomUUID().toString()
        val pendingComment = PendingComment(
            id = commentId,
            debate_id = debateId,
            user_id = userId,
            username = user.username,
            content = content,
            created_at = LocalDate.now().toString(),
            synced = false
        )
        room.pendingCommentDao().insert(pendingComment)
        room.commentDao().insert(LocalComment(
            comment_id = commentId,
            debate_id = debateId,
            user_id = userId,
            username = user.username,
            content = content,
            created_at = LocalDate.now().toString(),
            likes = 0
        ))
        try {
            firestore.collection("debates").document(debateId)
                .collection("comments").document(commentId)
                .set(pendingComment.copy(synced = true))
                .await()
            Log.d("AppRepository", "Comment synced to Firestore: $commentId")
        } catch (e: Exception) {
            Log.e("AppRepository", "Error syncing comment $commentId: ${e.message}", e)
        }
        enqueueSync()
        checkAndAwardAchievementsLocal(userId)
    }

    suspend fun getLeaderboardUsers(): List<LocalUser> {
        return try {
            val snapshot = firestore.collection("leaderboard")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            snapshot.mapNotNull { doc ->
                try {
                    LocalUser(
                        id = doc.id,
                        username = doc.get("username") as? String ?: "Unknown",
                        email = "",
                        yes_count = (doc.get("yes_count") as? Long)?.toInt() ?: 0,
                        no_count = (doc.get("no_count") as? Long)?.toInt() ?: 0,
                        streak = (doc.get("streak") as? Long)?.toInt() ?: 0,
                        last_voted_date = "",
                        score = (doc.get("score") as? Long)?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error parsing leaderboard user ${doc.id}: ${e.message}", e)
                    null
                }
            }.also {
                Log.d("AppRepository", "Fetched ${it.size} users from leaderboard")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error fetching leaderboard: ${e.message}", e)
            emptyList()
        }
    }

    // Sync public fields to leaderboard (call after any user update)
    suspend fun syncUserToLeaderboard(userId: String) {
        val user = room.userDao().getUser(userId).first() ?: return
        retryFirestoreOperation {
            firestore.collection("leaderboard").document(userId).set(
                mapOf(
                    "username" to user.username,
                    "score" to user.score,
                    "yes_count" to user.yes_count,
                    "no_count" to user.no_count,
                    "streak" to user.streak
                )
            ).await()
            Log.d("AppRepository", "Synced $userId to leaderboard (score: ${user.score})")
        }
    }

    suspend fun initialSyncLeaderboard() {
        retryFirestoreOperation {
            val usersSnapshot = firestore.collection("users").get().await()
            usersSnapshot.documents.forEach { doc ->
                val userData = doc.toObject(User::class.java) ?: return@forEach
                firestore.collection("leaderboard").document(doc.id).set(
                    mapOf(
                        "username" to userData.username,
                        "score" to userData.score,
                        "yes_count" to userData.yes_count,
                        "no_count" to userData.no_count,
                        "streak" to userData.streak
                    )
                ).await()
            }
            Log.d("AppRepository", "Initial leaderboard sync complete")
        }
    }

    private suspend fun checkAndAwardAchievementsLocal(userId: String) {
        val user = room.userDao().getUser(userId).first() ?: return
        val achievements = room.achievementDao().getAchievements(userId).first()
        val comments = room.commentDao().getCommentsByUser(userId)
        val totalVotes = user.yes_count + user.no_count
        val newAchievements = mutableListOf<LocalAchievement>()

        if (totalVotes >= 1 && !achievements.any { it.name == "first_vote" }) {
            newAchievements.add(LocalAchievement(
                user_id = userId,
                achievement_id = "first_vote",
                name = "first_vote",
                description = "Cast your first vote",
                awarded_at = LocalDate.now().toString()
            ))
        }
        if (totalVotes >= 10 && !achievements.any { it.name == "debate_master" }) {
            newAchievements.add(LocalAchievement(
                user_id = userId,
                achievement_id = "debate_master",
                name = "debate_master",
                description = "Vote in 10 debates",
                awarded_at = LocalDate.now().toString()
            ))
        }
        if (user.streak >= 3 && !achievements.any { it.name == "streak_starter" }) {
            newAchievements.add(LocalAchievement(
                user_id = userId,
                achievement_id = "streak_starter",
                name = "streak_starter",
                description = "Achieve a 3-day voting streak",
                awarded_at = LocalDate.now().toString()
            ))
        }
        if (comments.size >= 5 && !achievements.any { it.name == "commentator" }) {
            newAchievements.add(LocalAchievement(
                user_id = userId,
                achievement_id = "commentator",
                name = "commentator",
                description = "Post 5 comments",
                awarded_at = LocalDate.now().toString()
            ))
        }
        if (user.yes_count >= 5 && user.no_count >= 5 && !achievements.any { it.name == "balanced_voter" }) {
            newAchievements.add(LocalAchievement(
                user_id = userId,
                achievement_id = "balanced_voter",
                name = "balanced_voter",
                description = "Cast at least 5 Yes and 5 No votes",
                awarded_at = LocalDate.now().toString()
            ))
        }
        if (/* Add logic for approved debate submission */ false && !achievements.any { it.name == "debate_creator" }) {
            newAchievements.add(LocalAchievement(
                user_id = userId,
                achievement_id = "debate_creator",
                name = "debate_creator",
                description = "Have a debate question approved",
                awarded_at = LocalDate.now().toString()
            ))
        }
        newAchievements.forEach { ach ->
            room.achievementDao().insert(ach)
            firestore.collection("users").document(userId)
                .collection("achievements").document(ach.achievement_id)
                .set(mapOf(
                    "name" to ach.name,
                    "description" to ach.description,
                    "awarded_at" to ach.awarded_at
                ))
        }
        syncUserToLeaderboard(userId)
    }

    private suspend fun updateUserScoreLocal(userId: String) {
        val user = room.userDao().getUser(userId).first() ?: return
        val comments = room.commentDao().getCommentsByUser(userId)
        val commentLikes = comments.sumOf { it.likes }
        val totalVotes = user.yes_count + user.no_count
        val score = totalVotes * 10 + user.streak * 5 + commentLikes * 2
        val updatedUser = user.copy(score = score)
        room.userDao().insert(updatedUser)
        syncUserToLeaderboard(userId)
        try {
            firestore.collection("users").document(userId).set(updatedUser).await()
        } catch (e: Exception) {
            Log.e("AppRepository", "Error syncing user score: ${e.message}", e)
        }
    }

    private fun enqueueSync() {
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(application).enqueue(workRequest)
    }

    private suspend fun retryFirestoreOperation(block: suspend () -> Unit) {
        var attempts = 0
        while (attempts < 3) {
            try {
                block()
                return
            } catch (e: Exception) {
                attempts++
                if (attempts == 3) {
                    Log.e("AppRepository", "Firestore operation failed after $attempts attempts: ${e.message}", e)
                    throw e
                }
                delay(1000L)
                Log.w("AppRepository", "Retrying Firestore operation, attempt ${attempts + 1}")
            }
        }
    }

    suspend fun clearVoteData() {
        room.userVoteDao().deleteAll()
        room.pendingVoteDao().deleteAll()
        Log.d("AppRepository", "Cleared local vote data")
    }

    suspend fun clearFirestoreVotes() {
        retryFirestoreOperation {
            val votesSnapshot = firestore.collection("votes").get().await()
            for (doc in votesSnapshot.documents) {
                doc.reference.delete().await()
            }
            Log.d("AppRepository", "Cleared all votes in Firestore")
        }
    }

    suspend fun clearFirestoreDebates() {
        retryFirestoreOperation {
            val debatesSnapshot = firestore.collection("debates").get().await()
            for (doc in debatesSnapshot.documents) {
                doc.reference.delete().await()
            }
            Log.d("AppRepository", "Cleared all debates in Firestore")
        }
    }
}

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MyApplication
        val repository = app.repository
        val firestore = repository.firestore
        val room = repository.room
        val auth = repository.auth
        val userId = auth.currentUser?.uid ?: return Result.success()

        try {
            val pendingVotes = room.pendingVoteDao().getPendingVotes()
            for (vote in pendingVotes) {
                val existingVote = firestore.collection("votes")
                    .whereEqualTo("debate_id", vote.debate_id)
                    .whereEqualTo("user_id", userId)
                    .limit(1)
                    .get()
                    .await()
                if (!existingVote.isEmpty) {
                    room.pendingVoteDao().update(vote.copy(synced = true))
                    continue
                }
                firestore.collection("votes").add(
                    mapOf(
                        "debate_id" to vote.debate_id,
                        "user_id" to userId,
                        "choice" to vote.choice,
                        "created_at" to FieldValue.serverTimestamp()
                    )
                ).await()
                firestore.collection("debates").document(vote.debate_id).update(
                    mapOf(
                        (if (vote.choice == "yes") "yes_count" else "no_count") to FieldValue.increment(1L)
                    )
                ).await()
                val user = room.userDao().getUser(userId).first()
                if (user != null) {
                    val newCountYes = if (vote.choice == "yes") user.yes_count + 1 else user.yes_count
                    val newCountNo = if (vote.choice == "no") user.no_count + 1 else user.no_count
                    val lastVoted = user.last_voted_date
                    val newStreak = if (lastVoted == java.time.LocalDate.now().minusDays(1).toString()) user.streak + 1 else 1
                    val updatedUser = user.copy(
                        yes_count = newCountYes,
                        no_count = newCountNo,
                        streak = newStreak,
                        last_voted_date = java.time.LocalDate.now().toString()
                    )
                    room.userDao().insert(updatedUser)
                    firestore.collection("users").document(userId).set(updatedUser)
                }
                room.userVoteDao().insert(UserVote(
                    user_id = userId,
                    debate_id = vote.debate_id,
                    choice = vote.choice,
                    voted_at = java.time.LocalDate.now().toString()
                ))
                room.pendingVoteDao().update(vote.copy(synced = true))
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}", e)
            return Result.retry()
        }
    }
}