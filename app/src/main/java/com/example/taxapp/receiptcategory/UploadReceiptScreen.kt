package com.example.taxapp.receiptcategory

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import com.example.taxapp.accessibility.scaledSp
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import com.example.taxapp.user.AppUtil
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadReceiptScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    receiptViewModel: ReceiptViewModel = viewModel()
) {
    val context = LocalContext.current

    //language stuff
    // Move these variables up to this function
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }

    // Get the languageManager and accessibilityRepository here too
    val languageManager = remember { AppLanguageManager.getInstance(context) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Move currentLanguageCode here
    var currentLanguageCode by remember(languageManager.currentLanguageCode) {
        mutableStateOf(languageManager.getCurrentLanguageCode())
    }

    // Observe accessibility settings here too
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )

    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.upload_receipt),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                //
                actions = {
                    // Language button with improved styling
                    IconButton(
                        onClick = { showLanguageSelector = true },
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language, // Use the standard language icon
                            contentDescription = "Change Language",
                            //tint = accessibleColors.buttonText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Accessibility button with improved styling
                    IconButton(
                        onClick = { showAccessibilitySettings = true },
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,  // Standard settings icon
                            contentDescription = "Accessibility Settings",
                            //tint = accessibleColors.buttonText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { navController.navigate("calendar") }) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = "Calendar"
                        )
                    }

                    IconButton(onClick = { /* Already on Receipt */ })  {
                        Icon(
                            Icons.Filled.Receipt,
                            contentDescription = "Upload Receipt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { navController.navigate("category") }) {
                        Icon(
                            Icons.Filled.Category,
                            contentDescription = "Categories"
                        )
                    }

                    IconButton(onClick = { navController.navigate("editProfile") }) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Account"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        UploadReceiptContent(
            modifier = modifier.padding(innerPadding),
            navController = navController,
            receiptViewModel = receiptViewModel,
            currentLanguageCode = currentLanguageCode,
            accessibilityState = accessibilityState
        )

        // Handle the dialogs here
        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { languageCode ->
                    currentLanguageCode = languageCode
                },
                onDismiss = { showLanguageSelector = false },
                activity = activity
            )
        }

        if (showAccessibilitySettings) {
            AccessibilitySettings(
                currentSettings = accessibilityState,
                onSettingsChanged = { newSettings ->
                    coroutineScope.launch {
                        accessibilityRepository.updateSettings(newSettings)
                    }
                },
                onDismiss = { showAccessibilitySettings = false }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UploadReceiptContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    receiptViewModel: ReceiptViewModel,
    currentLanguageCode: String,
    accessibilityState: AccessibilityState
) {
    val context = LocalContext.current

    // State for tracking the selected image URI
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // State for showing loading indicator
    var isLoading by remember { mutableStateOf(false) }

    // State for showing invalid receipt dialog
    var showInvalidReceiptDialog by remember { mutableStateOf(false) }
    var invalidReceiptMessage by remember { mutableStateOf("") }

    // Add debug logging
    LaunchedEffect(Unit) {
        Log.d("UploadReceiptContent", "Starting receipt upload screen with viewModel: $receiptViewModel")
        // Reset view model state when navigating to this screen
        receiptViewModel.resetState()
    }

    // Create a launcher for content selection (gallery)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.d("UploadReceiptContent", "Image selected from gallery: $uri")
            selectedImageUri = uri
            isLoading = true

            // Store the URI in the ViewModel for later reference
            receiptViewModel.currentReceiptUri = uri

            // Process the receipt image using the ViewModel - don't navigate here yet
            receiptViewModel.processReceiptImage(
                uri = uri,
                context = context,
                onSuccess = {
                    // Log success before navigation
                    Log.d("UploadReceiptContent", "Receipt processing successful")
                    Log.d("UploadReceiptContent", "Extracted data: merchantName=${receiptViewModel.merchantName}, items=${receiptViewModel.expenseItems.size}, total=${receiptViewModel.expenseItems.sumOf { it.amount }}")

                    isLoading = false
                    // Only navigate when processing is complete and successful
                    navController.navigate("receiptSummary")
                },
                onError = { errorMessage ->
                    Log.e("UploadReceiptContent", "Receipt processing error: $errorMessage")
                    isLoading = false

                    // Check if this is our special error type for invalid receipt images
                    if (errorMessage.startsWith("INVALID_RECEIPT_IMAGE:")) {
                        invalidReceiptMessage = errorMessage.substringAfter("INVALID_RECEIPT_IMAGE:").trim()
                        showInvalidReceiptDialog = true
                    } else {
                        AppUtil.showToast(context, errorMessage)
                    }
                }
            )
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            Log.d("UploadReceiptContent", "Image captured from camera: $selectedImageUri")
            isLoading = true

            // Store the URI in the ViewModel for later reference
            receiptViewModel.currentReceiptUri = selectedImageUri

            // Process the receipt image using the ViewModel - don't navigate here yet
            receiptViewModel.processReceiptImage(
                uri = selectedImageUri!!,
                context = context,
                onSuccess = {
                    // Log success before navigation
                    Log.d("UploadReceiptContent", "Receipt processing successful")
                    Log.d("UploadReceiptContent", "Extracted data: merchantName=${receiptViewModel.merchantName}, items=${receiptViewModel.expenseItems.size}, total=${receiptViewModel.expenseItems.sumOf { it.amount }}")

                    isLoading = false
                    // Only navigate when processing is complete and successful
                    navController.navigate("receiptSummary")
                },
                onError = { errorMessage ->
                    Log.e("UploadReceiptContent", "Receipt processing error: $errorMessage")
                    isLoading = false

                    // Check if this is our special error type for invalid receipt images
                    if (errorMessage.startsWith("INVALID_RECEIPT_IMAGE:")) {
                        invalidReceiptMessage = errorMessage.substringAfter("INVALID_RECEIPT_IMAGE:").trim()
                        showInvalidReceiptDialog = true
                    } else {
                        AppUtil.showToast(context, errorMessage)
                    }
                }
            )
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted, proceed with camera
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, "New Receipt")
                put(MediaStore.Images.Media.DESCRIPTION, "From Smart Tax Handler Camera")
            }

            val imageUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (imageUri != null) {
                selectedImageUri = imageUri
                cameraLauncher.launch(imageUri)
            } else {
                AppUtil.showToast(context, "Failed to create image file")
            }
        } else {
            // Permission denied
            AppUtil.showToast(context, "Camera permission is required to take pictures")
        }
    }


    // Create a TTS instance if text-to-speech is enabled
    val tts = remember(accessibilityState.textToSpeech) {
        if (accessibilityState.textToSpeech) {
            TextToSpeech(context) { status ->
                // Initialize TTS engine
            }
        } else null
    }

    // Clean up TTS when not needed
    DisposableEffect(accessibilityState.textToSpeech) {
        onDispose {
            tts?.shutdown()
        }
    }

    // Get the custom colors
    val accessibleColors = LocalThemeColors.current
    //ScreenReader("Upload Receipt Screen")
    val ttsManager = LocalTtsManager.current

    // Reset the ViewModel state when navigating to this screen
    LaunchedEffect(Unit) {
        receiptViewModel.resetState()
    }

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(accessibleColors.calendarBackground)
                .padding(20.dp), // Match HomeScreen padding
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.receipt_processing),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                // Using MaterialTheme typography for proper scaling
                // Option Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Take photo option
                    UploadOption(
                        icon = Icons.Default.PhotoCamera,
                        title = stringResource(id = R.string.take_photo),
                        description = stringResource(id = R.string.capture_receipt),
                        onClick = {
                            ttsManager?.speak("Capture receipt")
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) -> {
                                    // Permission already granted, proceed with camera
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.Images.Media.TITLE, "New Receipt")
                                        put(
                                            MediaStore.Images.Media.DESCRIPTION,
                                            "From Smart Tax Handler Camera"
                                        )
                                    }

                                    val imageUri = context.contentResolver.insert(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    )

                                    if (imageUri != null) {
                                        selectedImageUri = imageUri
                                        cameraLauncher.launch(imageUri)
                                    } else {
                                        AppUtil.showToast(context, "Failed to create image file")
                                    }
                                }

                                else -> {
                                    // Request permission
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth() // Changed from weight to fillMaxWidth
                    )

                    // No need for horizontal Spacer, vertical spacing is handled by Column's arrangement

                    // Upload from gallery option
                    UploadOption(
                        icon = Icons.Default.PhotoLibrary,
                        title = stringResource(id = R.string.from_gallery),
                        description = stringResource(id = R.string.select_from_gallery),
                        onClick = {
                            ttsManager?.speak("Browse Gallery")
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth() // Changed from weight to fillMaxWidth
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

            }
        }
    }

    // Invalid Receipt Dialog
    if (showInvalidReceiptDialog) {
        AlertDialog(
            onDismissRequest = { showInvalidReceiptDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.invalid_receipt_image),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = invalidReceiptMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showInvalidReceiptDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            containerColor = accessibleColors.calendarSurface
        )
    }
}

@Composable
fun UploadOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accessibleColors = LocalThemeColors.current
    Card(
        modifier = modifier
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = accessibleColors.calendarSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Using scaledSp() for proper font scaling
            Text(
                text = title,
                fontSize = scaledSp(16),
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Using scaledSp() for proper font scaling
            Text(
                text = description,
                fontSize = scaledSp(12),
                textAlign = TextAlign.Center
            )
        }
    }
}