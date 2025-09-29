# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is "PetEmotions" - a Kotlin Multiplatform project targeting Android and iOS platforms. The app analyzes pet emotions from photos and videos using OpenAI's vision API and presents insights through a friendly UI. All data is stored locally with no server backend or login required.

## Project Structure

- `/composeApp` - Contains the shared Kotlin Multiplatform code
  - `src/commonMain/kotlin` - Shared code for all platforms
  - `src/androidMain/kotlin` - Android-specific implementations
  - `src/iosMain/kotlin` - iOS-specific implementations
- `/iosApp` - iOS native app entry point
- Package structure: `com.keak.petemotions`

## Key Architecture Components

### Data Layer
- Local-only storage with Room/SQLite database
- Entities: Pet, AnalysisRecord, UserPrefs
- Repository pattern for data access
- UUID for entity IDs
- kotlinx-datetime for timestamps

### AI Integration
- OpenAI GPT-4 Vision API for emotion analysis
- Ktor HTTP client for API calls
- Structured JSON responses for emotions, confidence, and details
- Support for both image and video analysis (frame extraction)

### Navigation
- Uses Androidx Navigation Compose for routing
- MVVM architecture with ViewModels
- Screen-based navigation flow

### Dependency Injection
- Uses Koin for dependency injection
- Module configuration for repositories, services, and ViewModels

### Premium Features
- RevenueCat integration for in-app purchases
- Monthly ¥980, Yearly ¥9,800 pricing
- Local premium status persistence
- Feature gating for advanced analysis

### UI Components
- Material 3 design system
- Compose Multiplatform for shared UI
- Charts with Vico library for analytics
- Camera integration with CameraX (Android)

## Development Commands

### Build Commands
```bash
# Build the project
./gradlew build

# Build Android app
./gradlew assembleDebug

# Build release
./gradlew assembleRelease
```

### Testing Commands
```bash
# Run tests
./gradlew test

# Run Android tests
./gradlew testDebugUnitTest
```

### Clean and Setup
```bash
# Clean build
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies
```

## Platform-Specific Notes

### Android
- Min SDK: 24
- Target/Compile SDK: 35
- Application ID: `com.keak.petemotions`
- Main activity: `MainActivity.kt`
- CameraX for camera functionality
- Room database for local storage

### iOS
- Framework name: `ComposeApp`
- Static framework configuration
- Main entry point: `MainViewController.kt`
- Native camera integration

## Key Dependencies
- Compose Multiplatform 1.8.2
- Kotlin 2.2.0
- Koin 4.1.0 for DI
- Ktor 3.2.3 for OpenAI API calls
- RevenueCat KMP for purchases
- Room 2.7.1 for database
- kotlinx-datetime for timestamps
- Vico 1.2.0 for charts
- CameraX 1.5.0 for camera (Android)
- MOKO Permissions for camera access

## App Features
- Photo/video emotion analysis using OpenAI Vision
- Local pet management
- History tracking and analytics
- Advanced analysis with charts and insights
- Premium subscription model
- Bilingual support (English/Japanese)
- Offline-first architecture

## Development Tips
- All data is stored locally - no server backend
- OpenAI API key required for emotion analysis
- Use MVVM pattern with clean architecture
- Platform-specific implementations for camera/database
- RevenueCat handles subscription management