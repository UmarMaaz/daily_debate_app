# Daily Debate App

A modern Android application that enables users to participate in daily debates, vote on topics, and engage with a community of debaters. Built with Jetpack Compose and Firebase for real-time functionality.

<div align="center">
  <img src="https://i.postimg.cc/8CBMG5Qw/admin-panel.png" alt="Authentication Screen" width="200" style="margin: 10px;">
  <img src="https://i.postimg.cc/0QkmysPn/debate-submission.png" alt="Home Screen" width="200" style="margin: 10px;">
  <img src="https://i.postimg.cc/j5xNSbty/debate-vote.png" alt="Results Screen" width="200" style="margin: 10px;">
</div>

<div align="center">
  <img src="https://i.postimg.cc/63WRpXtf/leaderboard.png" alt="Profile Screen" width="200" style="margin: 10px;">
  <img src="https://i.postimg.cc/Twdm36fq/login-page.png" alt="Leaderboard Screen" width="200" style="margin: 10px;">
  <img src="https://i.postimg.cc/63WRpXtg/profile-1.png" alt="Submit Debate Screen" width="200" style="margin: 10px;">
</div>

## 🌟 Features

- **Daily Debates**: Participate in thought-provoking daily debates
- **User Authentication**: Sign in with Google account or email/password
- **Real-time Voting**: Vote "Yes" or "No" on debate topics with real-time updates
- **User Profiles**: Track your voting history, streaks, and achievements
- **Leaderboard**: See top debaters and their scores
- **Comment System**: Discuss debate topics with other users
- **Achievements**: Earn badges for various milestones
- **Admin Panel**: Manage debates and review submissions

## 🛠️ Tech Stack

- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI toolkit
- **Firebase**: Backend services (Authentication, Firestore, Functions)
- **MVVM Architecture**: Clean, maintainable architecture pattern
- **Coroutines**: Asynchronous programming
- **Room Database**: Local data persistence
- **Work Manager**: Background task scheduling
- **Google Sign-In**: Third-party authentication

## 📋 Prerequisites

- Android Studio (Giraffe or later)
- Kotlin 2.0+
- Android API level 27+
- Google Services account with Firebase project

## 🚀 Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/[your-username]/debate_app.git
cd debate_app
```

### 2. Firebase Configuration
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add your Android app to the project
3. Download the `google-services.json` file and place it in `app/` directory
4. Enable Authentication methods (Email/Password, Google)
5. Set up Firestore database with appropriate security rules

### 3. Google Sign-In Setup
1. In Firebase Console, go to Authentication → Sign-in method
2. Enable Google sign-in provider
3. Add your SHA-1 fingerprint to the Firebase project settings
   - Get SHA-1: `./gradlew signingReport` (in project directory)

### 4. Build and Run
1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Build and run on an Android device or emulator

## 🏗️ Project Structure

```
app/src/main/java/com/debate_app/
├── GoogleAuthUiClient.kt          # Google Sign-In implementation
├── Debate_App.kt                  # Application class
├── MainActivity.kt                # Main activity and navigation
├── ui/
│   ├── components/               # Reusable UI components
│   │   ├── DebateComponents.kt   # Debate-specific components
│   │   ├── ProfileComponents.kt  # Profile components
│   │   └── ...                   # Other component files
│   ├── screens/                  # Screen composables
│   │   ├── HomeScreen.kt         # Main debate screen
│   │   ├── ProfileScreen.kt      # User profile
│   │   ├── LeaderboardScreen.kt  # Community leaderboard
│   │   └── ...                   # Other screens
│   └── theme/                    # Material Design theme
└── Repository.kt                # Data repository pattern
```

## 🔐 Authentication

The app supports two authentication methods:
- **Google Sign-In**: Seamless login with Google account
- **Email/Password**: Traditional username and password authentication

## 📊 Data Models

- **User**: Stores user information, voting statistics, and preferences
- **Debate**: Represents daily debate questions with vote counts
- **Comment**: User comments on specific debates
- **Achievement**: User achievements and badges

## 🎯 Core Functionality

### Voting System
- Users can vote once per debate
- Real-time vote updates using Firestore
- Streak tracking for consecutive voting days

### Achievement System
- First Vote: Cast your first vote
- Debate Master: Vote in 10 debates
- Streak Starter: Achieve a 3-day voting streak
- Commentator: Post 5 comments
- Balanced Voter: Cast at least 5 Yes and 5 No votes
- Debate Creator: Have a debate question approved

### Admin Features
- Create new debate topics
- Review and approve debate submissions
- Clear vote data for testing purposes

