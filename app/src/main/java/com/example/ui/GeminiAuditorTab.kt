package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.launch

// Chat message structure for local UI memory
data class ChatMessage(
    val sender: String, // "user" or "gemini"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun GeminiAuditorTab(
    viewModel: AuditViewModel,
    lang: String,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val isAr = lang == "ar"

    val sharedPrefs = remember { context.getSharedPreferences("audit_settings", android.content.Context.MODE_PRIVATE) }
    var customApiKeyInput by remember { mutableStateOf(sharedPrefs.getString("gemini_api_key", "") ?: "") }
    var apiKeyIsPlaceholder by remember {
        val key = sharedPrefs.getString("gemini_api_key", "") ?: ""
        val finalKey = if (key.isNotBlank()) key else com.example.BuildConfig.GEMINI_API_KEY
        mutableStateOf(finalKey.isEmpty() || finalKey == "MY_GEMINI_API_KEY" || finalKey == "GEMINI_API_KEY")
    }

    // Persistent collection of chat messages (since view model initialization or tab loading)
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    "gemini",
                    if (isAr) {
                        "أهلاً بك! أنا مدقق جيميناي الرقمي المساعد للـ QC. يمكنني إجابتك على أي استفسارات تخص التدقيق، وبنود نظام إدارة الجودة ISO 9001:2015، وإجراء تحليل الأسباب الجذرية (RCA). كيف يمكنني مساعدتك اليوم؟"
                    } else {
                        "Welcome! I am your Gemini QC Auditor assistant. I can answer all your questions about auditing methodologies, QMS ISO 9001:2015 requirements, and systematic Root Cause Analysis (RCA). How can I assist you today?"
                    }
                )
            )
        )
    }

    var userInput by rememberSaveable { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Lazy list scrolling controllers
    val scrollState = rememberLazyListState()

    // Auto-scroll on new message entry
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    // Function to handle message submission
    val onSendMessage: (String) -> Unit = { textToSend ->
        if (textToSend.trim().isNotEmpty() && !isAnalyzing) {
            val userMsg = ChatMessage("user", textToSend)
            messages = messages + userMsg
            userInput = ""
            isAnalyzing = true

            // Send to Gemini service
            coroutineScope.launch {
                val conversationHistory = messages.drop(1).dropLast(1).map { Pair(it.sender, it.text) }

                val replyText = GeminiService.generateResponse(
                    prompt = textToSend,
                    history = conversationHistory,
                    activeAuditContext = null,
                    context = context
                )

                messages = messages + ChatMessage("gemini", replyText)
                isAnalyzing = false
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF010E24) // Theme deep dark background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // =================== HEADER SECTION ===================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Return navigation Button
                IconButton(
                    onClick = onNavigateToHome,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF031633), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Header Center Logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "GEMINI AI",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Logo",
                            tint = Color(0xFF00BFA5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "QC INTERNAL AUDITOR",
                        style = TextStyle(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 2.sp
                        )
                    )
                }

                // Quick clear conversation history button
                IconButton(
                    onClick = {
                        messages = listOf(
                            ChatMessage(
                                "gemini",
                                if (isAr) {
                                    "تم مسح المحادثة. كيف يمكنني مساعدتك الآن بخصوص التدقيق والجودة؟"
                                } else {
                                    "Conversation cleared. How can I help you now with quality QMS audits?"
                                }
                            )
                        )
                        Toast.makeText(context, if (isAr) "تم مسح المحادثة" else "Conversation cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF031633), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // =================== INTRO & API KEY CONFIGURATION BANNER ===================
            var showApiKeyConfig by remember { mutableStateOf(false) }
            val isCurrentKeyPlaceholder = apiKeyIsPlaceholder

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentKeyPlaceholder) Color(0xFF3E2723) else Color(0xFF0D1E36)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCurrentKeyPlaceholder) Color(0xFFFF9100).copy(alpha = 0.3f) else Color(0xFF00BFA5).copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isCurrentKeyPlaceholder) Icons.Default.Warning else Icons.Default.Verified,
                                contentDescription = "Config Required",
                                tint = if (isCurrentKeyPlaceholder) Color(0xFFFF9100) else Color(0xFF00BFA5),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isCurrentKeyPlaceholder) {
                                    if (isAr) "الذكاء الاصطناعي غير متصل" else "AI Not Connected"
                                } else {
                                    if (isAr) "الذكاء الاصطناعي متصل وجاهز" else "Gemini AI Connected & Online"
                                },
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentKeyPlaceholder) Color(0xFFFF9100) else Color(0xFF00BFA5)
                                )
                            )
                        }
                        
                        TextButton(
                            onClick = { showApiKeyConfig = !showApiKeyConfig },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isCurrentKeyPlaceholder) Color(0xFFFF9100) else Color(0xFF00BFA5)
                            )
                        ) {
                            Text(
                                text = if (showApiKeyConfig) {
                                    if (isAr) "إغلاق" else "Close"
                                } else {
                                    if (isAr) "ضبط الخادم" else "Setup Key"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showApiKeyConfig) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (isAr) {
                                "أدخل مفتاح Google Gemini API لتفعيل الاتصال حياً بالإنترنت:"
                            } else {
                                "Enter your Google Gemini API key to enable live online QMS response queries:"
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customApiKeyInput,
                                onValueChange = { customApiKeyInput = it },
                                placeholder = { 
                                    Text(
                                        text = "AIzaSy...", 
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 12.sp
                                    ) 
                                },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isCurrentKeyPlaceholder) Color(0xFFFF9100) else Color(0xFF00BFA5),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    sharedPrefs.edit().putString("gemini_api_key", customApiKeyInput.trim()).apply()
                                    apiKeyIsPlaceholder = customApiKeyInput.trim().isEmpty() || 
                                                          customApiKeyInput.trim() == "MY_GEMINI_API_KEY" || 
                                                          customApiKeyInput.trim() == "GEMINI_API_KEY"
                                    GeminiService.setCustomApiKey(if (customApiKeyInput.trim().isEmpty()) null else customApiKeyInput.trim())
                                    showApiKeyConfig = false
                                    Toast.makeText(
                                        context, 
                                        if (isAr) "تم تحديث وحفظ مفتاح API بنجاح!" else "API Key updated & saved successfully!", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentKeyPlaceholder) Color(0xFFFF9100) else Color(0xFF00BFA5)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (isAr) "حفظ" else "Save",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (customApiKeyInput.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    customApiKeyInput = ""
                                    sharedPrefs.edit().remove("gemini_api_key").apply()
                                    GeminiService.setCustomApiKey(null)
                                    apiKeyIsPlaceholder = true
                                    showApiKeyConfig = false
                                    Toast.makeText(
                                        context, 
                                        if (isAr) "تم حذف المفتاح المخصص والرجوع للرسمي" else "Custom key cleared, fell back to default", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAr) "إلغاء المفتاح المخصص" else "Clear Custom Key",
                                    color = Color.Red.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else if (isCurrentKeyPlaceholder) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAr) {
                                "يعمل التطبيق حالياً بالوضع المحلي الذكي دون إنترنت. يرجى إدخال مفتاح API لربطه بالإنترنت مباشرة واستخدام خوادم للذكاء الاصطناعي."
                            } else {
                                "The app is currently playing offline with built-in QMS database. Tap ‘Setup Key’ above to add your own Gemini API key for live web assistance."
                            },
                            style = TextStyle(
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.82f),
                                lineHeight = 14.sp
                            )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAr) {
                                "متصل بالإنترنت وحصل على المفتاح. يمكنك الآن طرح أسئلة غير محدودة وسيقوم جيميناي بتحليل بنود الجودة حياً."
                            } else {
                                "Using customized or system API key. Real-time Gemini 3.5 queries are active. Ask ISO 9001 questions or analyze root causes in real-time."
                            },
                            style = TextStyle(
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.82f),
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================== PRESET CHIPS ROW ===================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preset Clause 9.2 Audit
                PresetChip(
                    text = "ISO Clause 9.2 (Audits)",
                    icon = Icons.Default.FactCheck,
                    onClick = {
                        onSendMessage("Explain the primary audit requirements mandated under ISO 9001:2015 Clause 9.2.")
                    }
                )

                // Preset Clause 10.2 CAPA
                PresetChip(
                    text = "ISO Clause 10.2 (CAPA)",
                    icon = Icons.Default.Build,
                    onClick = {
                        onSendMessage("Explain ISO 9001:2015 Clause 10.2 requirements for nonconformity and corrective actions.")
                    }
                )

                // Preset 5 Whys
                PresetChip(
                    text = "RCA: 5 Whys Method",
                    icon = Icons.Default.LiveHelp,
                    onClick = {
                        onSendMessage("Show me how to systematically perform a Root Cause Analysis using the 5 Whys technique.")
                    }
                )

                // Preset Fishbone Guide
                PresetChip(
                    text = "RCA: Fishbone (6M)",
                    icon = Icons.Default.Category,
                    onClick = {
                        onSendMessage("How do I structure a Fishbone (Ishikawa) diagram with the 6M categories?")
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================== CHAT CHANNELS MESSAGES LAZYCOLUMN ===================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .background(Color(0xFF030D1E)) // Darker feed channel background
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(messages) { idx, msg ->
                        MessageBubble(
                            message = msg,
                            onCopyText = {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Loading/Analyzing animated state indicator
                    if (isAnalyzing) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFF00BFA5).copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Loading API",
                                        tint = Color(0xFF00BFA5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isAr) "مدقق جيميناي يحلل البيانات..." else "Gemini QC is auditing quality parameters...",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = Color(0xFF00BFA5),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =================== BOTTOM MESSAGE FIELD INPUT BAR ===================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Text Field Input
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    placeholder = {
                        Text(
                            text = if (isAr) "اسأل جيميناي عن بنود ISO 9001، RCA..." else "Ask Gemini about ISO clauses, CAPAs, RCAs...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF031633),
                        unfocusedContainerColor = Color(0xFF031633),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp), // Pill input box
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (userInput.isNotEmpty()) {
                            IconButton(onClick = { userInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                // Send Button
                IconButton(
                    onClick = {
                        onSendMessage(userInput)
                    },
                    enabled = userInput.trim().isNotEmpty() && !isAnalyzing,
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            if (userInput.trim().isNotEmpty() && !isAnalyzing) Color(0xFF00BFA5) else Color(0xFF031633),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (userInput.trim().isNotEmpty() && !isAnalyzing) Color.Black else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// =================== PRESET MULTIPURPOSE CHIPS ===================
@Composable
fun PresetChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glowColor: Color = Color.White.copy(alpha = 0.12f),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (glowColor != Color.White.copy(alpha = 0.12f)) glowColor.copy(alpha = 0.15f) else Color(0xFF031633))
            .border(
                1.dp,
                if (glowColor != Color.White.copy(alpha = 0.12f)) glowColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (glowColor != Color.White.copy(alpha = 0.12f)) glowColor else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (glowColor != Color.White.copy(alpha = 0.12f)) glowColor else Color.White
            )
        }
    }
}

// =================== MESSAGE BUBBLE RENDERING COMPONENT ===================
@Composable
fun MessageBubble(
    message: ChatMessage,
    onCopyText: () -> Unit
) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Gemini Logo Badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF00BFA5).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini",
                    tint = Color(0xFF00BFA5),
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF00BFA5).copy(alpha = 0.12f) else Color(0xFF031633)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) Color(0xFF00BFA5).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
            ),
            modifier = Modifier.weight(1f, fill = false).widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Main Text body content
                Text(
                    text = message.text,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp,
                        fontFamily = if (!isUser) FontFamily.SansSerif else FontFamily.Default
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom actions within Card (like Copy Text button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "You" else "Gemini QC Auditor",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) Color(0xFF00BFA5).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f)
                    )

                    IconButton(
                        onClick = onCopyText,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message text",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

// Simple modifier helper scale function
private fun Modifier.scaleDown(factor: Float): Modifier = this.then(
    Modifier.scale(factor)
)
