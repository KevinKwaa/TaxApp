package com.example.taxapp

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.chatbot.ChatFAB
import com.example.taxapp.chatbot.ChatViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SchedulerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Store events in a mutable state map
    val eventsMap = remember {
        mutableStateMapOf<LocalDate, MutableList<Event>>()
    }

    // Get the TTS manager from the composition
    val ttsManager = LocalTtsManager.current

    // Current event for editing/viewing
    val currentEvent = remember { mutableStateOf<Event?>(null) }

    // Shared ChatViewModel
    val chatViewModel: ChatViewModel = viewModel()

    // Track navigation to speak screen changes
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            when (destination.route) {
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
            }
        }
    }

    // Wrap the NavHost with a Box to allow overlay of the chat button
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "calendar") {
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
                        // Add new event to the map
                        val dateEvents = eventsMap.getOrPut(event.date) { mutableListOf() }
                        dateEvents.add(event)

                        // Announce successful event creation
                        ttsManager?.speak("Event saved: ${event.title}")

                        navController.popBackStack()
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
                            // Update the event in the map
                            val dateEvents = eventsMap[updatedEvent.date]
                            dateEvents?.let { list ->
                                val index = list.indexOfFirst { it == event }
                                if (index != -1) {
                                    list[index] = updatedEvent
                                    currentEvent.value = updatedEvent
                                }
                            }

                            // Announce successful update
                            ttsManager?.speak("Event updated: ${updatedEvent.title}")

                            navController.popBackStack()
                        },
                        onDeleteEvent = { eventToDelete ->
                            // Remove the event from the map
                            val dateEvents = eventsMap[eventToDelete.date]
                            dateEvents?.remove(eventToDelete)
                            if (dateEvents?.isEmpty() == true) {
                                eventsMap.remove(eventToDelete.date)
                            }

                            // Announce deletion
                            ttsManager?.speak("Event deleted")

                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        // Add ChatFAB to overlay on all screens
        ChatFAB(
            //chatViewModel = chatViewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}