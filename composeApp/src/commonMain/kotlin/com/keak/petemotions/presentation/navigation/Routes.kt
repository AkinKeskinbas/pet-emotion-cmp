package com.keak.petemotions.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object WelcomeRoute

@Serializable
object HomeRoute

@Serializable
object CameraRoute

@Serializable
data class ResultDetailRoute(val analysisRecordId: String)

@Serializable
object HistoryRoute

@Serializable
object MyPetsRoute

@Serializable
data class AddEditPetRoute(val petId: String? = null)

@Serializable
object AdvancedAnalysisRoute

@Serializable
object PaywallRoute

@Serializable
object PermissionsRoute

@Serializable
object CompareRoute