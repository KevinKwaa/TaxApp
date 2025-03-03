package com.example.taxapp.receiptcategory

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import com.example.taxapp.user.AppUtil
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadReceiptScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    receiptViewModel: ReceiptViewModel = viewModel()
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.upload_receipt),
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    //home
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    //upload receipt
                    IconButton(onClick = { navController.navigate("uploadReceipt") }) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "Upload Receipt",
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    //category
                    IconButton(onClick = { navController.navigate("category") }) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Profile",
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    //profile
                    IconButton(onClick = { navController.navigate("editProfile") }) {
                        Icon(
                            Icons.Filled.Face,
                            contentDescription = "Profile",
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        UploadReceiptContent(
            modifier = modifier.padding(innerPadding),
            navController = navController,
            receiptViewModel = receiptViewModel
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UploadReceiptContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    receiptViewModel: ReceiptViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity

    // State for tracking the selected image URI
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // State for showing loading indicator
    var isLoading by remember { mutableStateOf(false) }

    // Create a launcher for content selection (gallery)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isLoading = true

            // Process the receipt image using the ViewModel
            receiptViewModel.processReceiptImage(
                uri = uri,
                context = context,
                onSuccess = {
                    isLoading = false
                    navController.navigate("receiptSummary")
                },
                onError = { errorMessage ->
                    isLoading = false
                    AppUtil.showToast(context, errorMessage)
                }
            )
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            isLoading = true

            // Process the receipt image using the ViewModel
            receiptViewModel.processReceiptImage(
                uri = selectedImageUri!!,
                context = context,
                onSuccess = {
                    isLoading = false
                    navController.navigate("receiptSummary")
                },
                onError = { errorMessage ->
                    isLoading = false
                    AppUtil.showToast(context, errorMessage)
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

    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }
    // Access shared repositories
    val languageManager = remember { AppLanguageManager.getInstance(context) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Observe the current language
    var currentLanguageCode by remember(languageManager.currentLanguageCode) {
        mutableStateOf(languageManager.getCurrentLanguageCode())
    }

    // Observe accessibility settings
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )

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
    val isDarkMode = LocalDarkMode.current
    ScreenReader("Home Screen")
    val ttsManager = LocalTtsManager.current

    // Reset the ViewModel state when navigating to this screen
    LaunchedEffect(Unit) {
        receiptViewModel.resetState()
    }
    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Language button with improved styling
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            accessibleColors.buttonBackground.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = accessibleColors.calendarBorder,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { showLanguageSelector = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🌐",
                        style = MaterialTheme.typography.titleMedium,
                        color = accessibleColors.buttonText
                    )
                }

                // Accessibility button with improved styling
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            accessibleColors.buttonBackground.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = accessibleColors.calendarBorder,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { showAccessibilitySettings = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚙️",
                        style = MaterialTheme.typography.titleMedium,
                        color = accessibleColors.buttonText
                    )
                }
            }

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
                Text(
                    text = stringResource(id = R.string.receipt_add), //How would you like to add your receipt?
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                // Option Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Take photo option
                    UploadOption(
                        icon = Icons.Default.AccountCircle,
                        title = "Take Photo",
                        description = "Capture receipt using camera",
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
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Upload from gallery option
                    UploadOption(
                        icon = Icons.Default.Face,
                        title = "From Gallery",
                        description = "Select receipt from gallery",
                        onClick = {
                            ttsManager?.speak("Browse Gallery")
                            galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

            }

            if (isDarkMode) {
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accessibleColors.calendarText.copy(alpha = 0.7f)
                )
            }
        }

        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { languageCode ->
                    currentLanguageCode = languageCode
                },
                onDismiss = { showLanguageSelector = false },
                activity = activity  // Pass the activity
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

@Composable
fun UploadOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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

            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = TextStyle(
                    fontSize = 12.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

