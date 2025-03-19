package com.example.taxapp

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.taxapp.user.FirebaseManager
import com.example.taxapp.receiptcategory.CategoryScreen
import com.example.taxapp.receiptcategory.ReceiptSummaryScreen
import com.example.taxapp.receiptcategory.ReceiptViewModel
import com.example.taxapp.receiptcategory.UploadReceiptScreen
import com.example.taxapp.taxinformation.TaxInfoViewModel
import com.example.taxapp.taxinformation.TaxInformationScreen
import com.example.taxapp.taxplan.TaxPlanScreen
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val editProfileViewModel: EditProfileViewModel = viewModel()

    // IMPORTANT: Create a shared ReceiptViewModel instance
    val receiptViewModel: ReceiptViewModel = viewModel()

    // Collect user ID from FirebaseManager's StateFlow
    val currentUserId by FirebaseManager.currentUserFlow.collectAsState()

    // Log user change for debugging
    LaunchedEffect(currentUserId) {
        Log.d("AppNavigation", "User changed to: $currentUserId")

        // Reset event repository on user change
        if (currentUserId == null) {
            EventRepository.resetInstance()
        }
    }

    // Now we use the StateFlow value instead of calling getCurrentUserId directly
    val eventRepository = remember(currentUserId) {
        // Force reset and get fresh instance for new user
        EventRepository.resetInstance()
        EventRepository.getInstance()
    }

    // Collect events specific to the current user
    val eventsMap by produceState(
        initialValue = mapOf<LocalDate, MutableList<Event>>(),
        key1 = currentUserId
    ) {
        // Only collect events if there's a user logged in
        if (currentUserId != null) {
            eventRepository.getAllEvents(currentUserId)
                .collect { events ->
                    value = events
                }
        } else {
            // Empty map when no user is logged in
            value = emptyMap()
        }
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

    val profileUpdated = remember { mutableStateOf(false) }

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
                "receiptSummary" -> {
                    ttsManager?.speak("Receipt summary screen")
                }
                "category" -> {
                    ttsManager?.speak("Tax categories screen")
                }
                "taxPlan" -> {
                    ttsManager?.speak("Tax plan screen")
                }
                "taxInformation" -> {
                    ttsManager?.speak("Tax information screen")
                }
            }
        }
    }

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
                LoginScreen(modifier, navController, authViewModel)
            }

            composable("register") {
                RegisterScreen(modifier, navController, authViewModel)
            }

            composable("profile") {
                ProfileScreen(modifier, navController, authViewModel)
            }

            composable("editProfile") {
                val authViewModel: AuthViewModel = viewModel()
                val editProfileViewModel: EditProfileViewModel = viewModel()

                EditProfileScreen(
                    modifier = modifier,
                    navController = navController,
                    editProfileViewModel = editProfileViewModel,
                    //authViewModel = authViewModel,
//                    onProfileSaved = {
//                        // Set the flag to indicate profile was updated
//                        profileUpdated.value = true
//                        Log.d("Navigation", "Profile updated flag set to true")
//
//                        // Force reset the repository to ensure fresh data
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            EventRepository.resetInstance()
//                        }
//
//                        // Navigate back to home
//                        navController.navigate("home") {
//                            popUpTo("editProfile") { inclusive = true }
//                        }
//                    }
                )
            }

            // Home Screen (central hub)
            composable("home") {
                val authViewModel: AuthViewModel = viewModel()
                HomeScreen(
                    //modifier = modifier,
                    navController = navController,
                    //authViewModel = authViewModel
                )
            }

            // Calendar Screen
            composable("calendar") {
                // STRONG reset and refresh when entering calendar
                val lifecycleOwner = LocalLifecycleOwner.current
                val coroutineScope = rememberCoroutineScope()

                // This will trigger EVERY time the calendar is displayed on screen
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            Log.d("Navigation", "⚠️ Calendar screen RESUMED - FORCING COMPLETE REFRESH")

                            coroutineScope.launch {
                                // Complete reset and refresh sequence
                                EventRepository.resetInstance()
                                delay(300)
                                val repo = EventRepository.getInstance()
                                repo.forceRefresh()

                                // Add a second refresh after a brief delay to ensure data is loaded
                                delay(500)
                                repo.forceRefresh()

                                // Log to verify refresh was triggered
                                Log.d("Navigation", "✅ Calendar force refresh completed")
                            }
                        }
                    }

                    // Register the observer
                    lifecycleOwner.lifecycle.addObserver(observer)

                    // Clean up when leaving the screen
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Check if we're coming from a profile update
                LaunchedEffect(profileUpdated.value) {
                    if (profileUpdated.value) {
                        Log.d("Navigation", "Calendar detected profile was updated, performing thorough refresh")

                        // Reset repository to force fresh data load
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Complete cleanup sequence
                            EventRepository.resetInstance()
                            delay(300) // Allow cleanup to complete
                            val repo = EventRepository.getInstance()
                            repo.forceRefresh()
                            delay(500) // Allow refresh to complete
                        }

                        // Reset the flag
                        profileUpdated.value = false
                    }
                }

                // Force fresh data load when entering calendar
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Log.d("Navigation", "Calendar screen entered, performing thorough refresh")

                        // Complete cleanup sequence
                        EventRepository.resetInstance()
                        delay(300) // Allow cleanup to complete
                        val repo = EventRepository.getInstance()
                        repo.forceRefresh()
                    }
                }

                val refreshKey = System.currentTimeMillis()
                if (currentUserId != null) {
                    // Wrap events in a key based on a timestamp to force recomposition
                    CalendarScreen(
                        events = eventsMap,
                        currentUserId = currentUserId!!,
                        refreshKey = refreshKey, // Enable this parameter!
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
                        },
                        // Add the back navigation callback
                        onNavigateBack = {
                            // Announce navigation back to home
                            ttsManager?.speak("Going back to home screen")

                            navController.navigate("home") {
                                // Pop up to home to avoid building up back stack
                                popUpTo("home") { inclusive = false }
                            }
                        },

                        modifier = modifier,
                        navController = navController
                    )
                }
            }

            composable("taxPlan") {
                TaxPlanScreen(
                    modifier = modifier,
                    navController = navController,
                    onNavigateBack = {
                        // Announce navigation back to home
                        ttsManager?.speak("Going back home screen")

                        navController.navigate("home") {
                            // Pop up to home to avoid building up back stack
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }

            composable("taxInformation") {
                val taxInfoViewModel: TaxInfoViewModel = viewModel()
                TaxInformationScreen(
                    modifier = modifier,
                    navController = navController,
                    taxInfoViewModel = taxInfoViewModel
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

                        // Use lifecycleScope instead of a general coroutineScope
                        coroutineScope.launch {
                            try {
                                val result = eventRepository.addEvent(event)
                                if (result) {
                                    // Update status to success
                                    statusMessage = "Event saved successfully!"
                                    operationStatus = OperationStatus.SUCCESS

                                    // Announce successful event creation
                                    ttsManager?.speak("Event saved: ${event.title}")

                                    // IMPORTANT: Use a small delay to ensure the status message is visible
                                    // before navigating back
                                    delay(500)
                                    navController.popBackStack()
                                } else {
                                    // Update status to error
                                    statusMessage = "Failed to save event. Please try again."
                                    operationStatus = OperationStatus.ERROR

                                    // Announce failure
                                    ttsManager?.speak("Failed to save event. Please try again.")
                                }
                            } catch (e: Exception) {
                                Log.e("AppNavigation", "Error saving event", e)
                                statusMessage = "Error: ${e.localizedMessage ?: "Unknown error"}"
                                operationStatus = OperationStatus.ERROR
                            } finally {
                                showStatusFeedback = true
                            }
                        }
                    },
                    navController = navController
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
                        },
                        onTodoStatusChange = { updatedEvent, isCompleted ->
                            // Show loading status
                            val statusText = if (isCompleted) "Marking as completed..." else "Marking as not completed..."
                            statusMessage = statusText
                            operationStatus = OperationStatus.LOADING
                            showStatusFeedback = true

                            // Update the todo status in Firebase
                            coroutineScope.launch {
                                val result = eventRepository.updateEvent(updatedEvent)
                                if (result) {
                                    // Update status to success
                                    val successText = if (isCompleted) "Marked as completed!" else "Marked as not completed!"
                                    statusMessage = successText
                                    operationStatus = OperationStatus.SUCCESS
                                    showStatusFeedback = true

                                    // Announce successful update
                                    ttsManager?.speak(successText)

                                    // Update the current event value to reflect the change
                                    currentEvent.value = updatedEvent
                                } else {
                                    // Update status to error
                                    statusMessage = "Failed to update task status. Please try again."
                                    operationStatus = OperationStatus.ERROR
                                    showStatusFeedback = true

                                    // Announce failure
                                    ttsManager?.speak("Failed to update task status. Please try again.")
                                }
                            }
                        },
                        navController = navController
                    )
                }
            }

            // Receipt screens - UPDATED TO USE SHARED VIEWMODEL
            composable("uploadReceipt") {
                UploadReceiptScreen(
                    modifier = modifier,
                    navController = navController,
                    receiptViewModel = receiptViewModel // Pass the shared ViewModel
                )
            }

            composable("receiptSummary") {
                ReceiptSummaryScreen(
                    modifier = modifier,
                    navController = navController,
                    receiptViewModel = receiptViewModel // Pass the shared ViewModel
                )
            }

            // Categories screen
            composable("category") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    CategoryScreen(
                        modifier = modifier,
                        navController = navController
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("This feature requires Android N or higher")
                    }
                }
            }
        }

        // 1. First, collect the user state as a state object that will trigger recomposition
        val currentUserState by FirebaseManager.currentUserFlow.collectAsState()

        // 2. Log the user state to help diagnose issues
        LaunchedEffect(currentUserState) {
            Log.d("Navigation", "User state changed: ${currentUserState != null}")
        }

        // 3. Define routes with chatbot using the state variable
        val routesWithChatbot = listOf("home", "calendar", "event_details")
        val currentRoute = navController.currentDestination?.route
        val showChatbot = (currentRoute in routesWithChatbot ||
                currentRoute?.startsWith("add_event/") == true) &&
                currentUserState != null  // Use the state variable here

        // 4. Log the chatbot visibility decision
        Log.d("Navigation", "ChatFAB visibility: $showChatbot (route: $currentRoute, user: ${currentUserState != null})")

        // 5. Show the chatbot based on the state
        if (showChatbot) {
            ChatFAB()
        }
    }
}