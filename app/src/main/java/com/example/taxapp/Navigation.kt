package com.example.taxapp

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taxapp.CalendarEvent.AddEventScreen
import com.example.taxapp.CalendarEvent.CalendarScreen
import com.example.taxapp.CalendarEvent.Event
import com.example.taxapp.CalendarEvent.EventDetailScreen
import com.example.taxapp.CalendarEvent.EventMigrationUtil
import com.example.taxapp.CalendarEvent.EventRepository
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.chatbot.ChatFAB
import com.example.taxapp.chatbot.ChatViewModel
import com.example.taxapp.user.AuthScreen
import com.example.taxapp.user.AuthViewModel
import com.example.taxapp.user.EditProfileScreen
import com.example.taxapp.user.EditProfileViewModel
import com.example.taxapp.user.HomeScreen
import com.example.taxapp.user.LoginScreen
import com.example.taxapp.user.ProfileScreen
import com.example.taxapp.user.RegisterScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.taxapp.firebase.FirebaseManager

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val editProfileViewModel: EditProfileViewModel = viewModel()

    // Initialize the event repository
    val eventRepository = remember { EventRepository.getInstance() }
    val eventMigrationUtil = remember { EventMigrationUtil.getInstance() }

    // Flag to track if migration has been attempted
    var migrationAttempted by remember { mutableStateOf(false) }

    // Perform event migration when logged in
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null && !migrationAttempted) {
            // Launch in a coroutine to avoid blocking UI
            coroutineScope.launch {
                try {
                    eventMigrationUtil.migrateEvents()
                } catch (e: Exception) {
                    Log.e("AppNavigation", "Error during event migration", e)
                } finally {
                    migrationAttempted = true
                }
            }
        }
    }

    // Collect events from Firestore as a state
    val eventsMap = remember { mutableStateMapOf<LocalDate, MutableList<Event>>() }
    val eventsFlow by eventRepository.getAllEvents().collectAsState(initial = mapOf())

    // Update the events map when the flow emits new data
    LaunchedEffect(eventsFlow) {
        eventsMap.clear()
        eventsMap.putAll(eventsFlow)
    }

    // Get the TTS manager from the composition
    val ttsManager = LocalTtsManager.current

    // Current event for editing/viewing
    val currentEvent = remember { mutableStateOf<Event?>(null) }

    // Status feedback state
    var showStatusFeedback by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var operationStatus by remember { mutableStateOf(OperationStatus.SUCCESS) }

    // Shared ChatViewModel
    val chatViewModel: ChatViewModel = viewModel()

    // Authentication check for start destination
    val isLoggedIn = Firebase.auth.currentUser != null
    val startDestination = if(isLoggedIn) "home" else "auth"

    // Track navigation to speak screen changes
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            when (destination.route) {
                "home" -> {
                    ttsManager?.speak("Home screen")
                }
                "calendar" -> {
                    ttsManager?.speak("Calendar screen, showing your schedule")
                }
                "event_details" -> {
                    val event = currentEvent.value
                    ttsManager?.speak("Event details for ${event?.title ?: "selected event"}")
                }
                "add_event/{date}" -> {
                    // Extract date from arguments
                    val dateString = arguments?.getString("date")
                    val dateText = if (dateString != null) {
                        try {
                            val date = LocalDate.parse(dateString)
                            val formatter = DateTimeFormatter.ofPattern("MMMM d")
                            "for ${date.format(formatter)}"
                        } catch (e: Exception) {
                            ""
                        }
                    } else ""

                    ttsManager?.speak("Add new event screen $dateText")
                }
                "editProfile" -> {
                    ttsManager?.speak("Edit profile screen")
                }
                "uploadReceipt" -> {
                    ttsManager?.speak("Upload receipt screen")
                }
                "category" -> {
                    ttsManager?.speak("Tax categories screen")
                }
            }
        }
    }

    // Success effect for login/registration to trigger event migration
    fun onAuthSuccess() {
        // Reset migration flag when a new user logs in
        migrationAttempted = false

        // Trigger migration in background
        coroutineScope.launch {
            try {
                eventMigrationUtil.migrateEvents()
            } catch (e: Exception) {
                Log.e("AppNavigation", "Error during event migration after login", e)
            } finally {
                migrationAttempted = true
            }
        }
    }

    // Wrap the NavHost with a Box to allow overlay of the chat button and status feedback
    Box(modifier = Modifier.fillMaxSize()) {
        // Status feedback
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            StatusSnackbar(
                visible = showStatusFeedback,
                message = statusMessage,
                status = operationStatus,
                onDismiss = { showStatusFeedback = false }
            )
        }

        NavHost(navController = navController, startDestination = startDestination) {
            // Authentication routes
            composable("auth") {
                AuthScreen(modifier, navController)
            }

            composable("login") {
                LoginScreen(
                    modifier = modifier,
                    navController = navController,
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        onAuthSuccess()
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    modifier = modifier,
                    navController = navController,
                    authViewModel = authViewModel,
                    onRegistrationSuccess = {
                        onAuthSuccess()
                        navController.navigate("profile") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }

            composable("profile") {
                ProfileScreen(modifier, navController, authViewModel)
            }

            composable("editProfile") {
                EditProfileScreen(modifier, navController, editProfileViewModel)
            }

            // Home Screen (central hub)
            composable("home") {
                HomeScreen(modifier, navController)
            }

            // Calendar Screen
            composable("calendar") {
                CalendarScreen(
                    events = eventsMap,
                    onNavigateToAddEvent = { date ->
                        // Capture the date for announcement
                        val dateFormat = DateTimeFormatter.ofPattern("MMMM d")
                        val formattedDate = date.format(dateFormat)

                        // Announce navigation
                        ttsManager?.speak("Adding event for $formattedDate")

                        navController.navigate("add_event/${date}")
                    },
                    onNavigateToEventDetails = { event ->
                        currentEvent.value = event

                        // Announce navigation with event title
                        ttsManager?.speak("Opening details for event: ${event.title}")

                        navController.navigate("event_details")
                    }
                )
            }

            // Add Event Screen
            composable(
                route = "add_event/{date}",
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val dateString = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
                val date = LocalDate.parse(dateString)

                AddEventScreen(
                    date = date,
                    onNavigateBack = {
                        // Announce navigation back
                        ttsManager?.speak("Returning to calendar")

                        navController.popBackStack()
                    },
                    onEventSaved = { event ->
                        // Show loading status
                        statusMessage = "Saving event..."
                        operationStatus = OperationStatus.LOADING
                        showStatusFeedback = true

                        // Save event to Firebase
                        coroutineScope.launch {
                            val result = eventRepository.addEvent(event)
                            if (result) {
                                // Update status to success
                                statusMessage = "Event saved successfully!"
                                operationStatus = OperationStatus.SUCCESS
                                showStatusFeedback = true

                                // Announce successful event creation
                                ttsManager?.speak("Event saved: ${event.title}")
                                navController.popBackStack()
                            } else {
                                // Update status to error
                                statusMessage = "Failed to save event. Please try again."
                                operationStatus = OperationStatus.ERROR
                                showStatusFeedback = true

                                // Announce failure
                                ttsManager?.speak("Failed to save event. Please try again.")
                            }
                        }
                    }
                )
            }

            // Event Details Screen
            composable("event_details") {
                currentEvent.value?.let { event ->
                    EventDetailScreen(
                        event = event,
                        onNavigateBack = {
                            // Announce navigation back
                            ttsManager?.speak("Returning to calendar")

                            navController.popBackStack()
                        },
                        onEditEvent = { updatedEvent ->
                            // Show loading status
                            statusMessage = "Updating event..."
                            operationStatus = OperationStatus.LOADING
                            showStatusFeedback = true

                            // Update the event in Firebase
                            coroutineScope.launch {
                                val result = eventRepository.updateEvent(updatedEvent)
                                if (result) {
                                    // Update status to success
                                    statusMessage = "Event updated successfully!"
                                    operationStatus = OperationStatus.SUCCESS
                                    showStatusFeedback = true

                                    // Announce successful update
                                    ttsManager?.speak("Event updated: ${updatedEvent.title}")
                                    currentEvent.value = updatedEvent
                                    navController.popBackStack()
                                } else {
                                    // Update status to error
                                    statusMessage = "Failed to update event. Please try again."
                                    operationStatus = OperationStatus.ERROR
                                    showStatusFeedback = true

                                    // Announce failure
                                    ttsManager?.speak("Failed to update event. Please try again.")
                                }
                            }
                        },
                        onDeleteEvent = { eventToDelete ->
                            // Show loading status
                            statusMessage = "Deleting event..."
                            operationStatus = OperationStatus.LOADING
                            showStatusFeedback = true

                            // Delete the event from Firebase
                            coroutineScope.launch {
                                val result = eventRepository.deleteEvent(eventToDelete)
                                if (result) {
                                    // Update status to success
                                    statusMessage = "Event deleted successfully!"
                                    operationStatus = OperationStatus.SUCCESS
                                    showStatusFeedback = true

                                    // Announce deletion
                                    ttsManager?.speak("Event deleted")
                                    navController.popBackStack()
                                } else {
                                    // Update status to error
                                    statusMessage = "Failed to delete event. Please try again."
                                    operationStatus = OperationStatus.ERROR
                                    showStatusFeedback = true

                                    // Announce failure
                                    ttsManager?.speak("Failed to delete event. Please try again.")
                                }
                            }
                        }
                    )
                }
            }

//            // Category Screen
//            composable("category") {
//                CategoryScreen(modifier, navController)
//            }
//
//            // Receipt handling routes
//            composable("uploadReceipt") {
//                UploadReceiptScreen(modifier, navController)
//            }
//
//            composable(
//                route = "receiptSummary/{imageUri}",
//                arguments = listOf(
//                    navArgument("imageUri") { type = NavType.StringType }
//                )
//            ) { backStackEntry ->
//                val imageUriString = backStackEntry.arguments?.getString("imageUri")
//                val imageUri = if (imageUriString != null) Uri.parse(imageUriString) else Uri.EMPTY
//                ReceiptSummaryScreen(modifier, navController, imageUri = imageUri)
//            }
        }

        // Add ChatFAB to overlay on all screens
        ChatFAB(
            modifier = Modifier.fillMaxSize()
        )
    }
}