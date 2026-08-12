package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"

    // OkHttpClient with 60-second timeouts as required by the Gemini API guidelines
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var customApiKey: String? = null

    fun setCustomApiKey(key: String?) {
        customApiKey = key
    }

    /**
     * Advanced Gemini request supporting custom models, grounding, thinking mode, and image analysis/generation.
     */
    suspend fun generateAdvancedResponse(
        prompt: String,
        modelName: String,
        groundingTool: String = "none", // "googleSearch", "googleMaps", "none"
        thinkingLevel: String = "none", // "HIGH", "none"
        imageBytesBase64: String? = null,
        imageSize: String? = "1K", // "1K", "2K", "4K"
        isImageGeneration: Boolean = false,
        isImageEditing: Boolean = false,
        context: android.content.Context? = null
    ): String = withContext(Dispatchers.IO) {
        if (isApiKeyPlaceholder(context)) {
            // Friendly fallback if offline or placeholder key
            if (isImageGeneration || isImageEditing) {
                return@withContext "MOCK_IMAGE_FALLBACK"
            }
            return@withContext "[Offline Preview Mode] Please configure a valid Gemini API Key in Settings to execute this live query.\n\nHere is what the simulated response content would describe:\n\"For prompt: '$prompt', using $modelName with grounding $groundingTool and thinking $thinkingLevel. The QMS ISO 9001:2015 specifications would mandate checking Section 9.2.2 for structural inspection procedures.\""
        }

        try {
            val key = getActiveApiKey(context)
            val requestJson = JSONObject()
            
            // 1. Contents Array
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            contentObj.put("role", "user")
            
            val partsArray = JSONArray()
            
            // Text part
            val textPart = JSONObject()
            if (isImageGeneration) {
                textPart.put("text", prompt)
            } else if (isImageEditing) {
                textPart.put("text", "Please edit/modify the attached image according to this prompt: $prompt")
            } else {
                textPart.put("text", prompt)
            }
            partsArray.put(textPart)
            
            // Attachment image part (for image analysis/understanding or image editing)
            if (imageBytesBase64 != null) {
                val imagePart = JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", imageBytesBase64)
                    })
                }
                partsArray.put(imagePart)
            }
            
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // 2. Generation Config
            val genConfig = JSONObject()
            if (isImageGeneration) {
                genConfig.put("imageConfig", JSONObject().apply {
                    put("aspectRatio", "1:1")
                    put("imageSize", imageSize ?: "1K")
                })
                val modalities = JSONArray()
                modalities.put("IMAGE")
                genConfig.put("responseModalities", modalities)
            } else {
                // Not image generation
                genConfig.put("temperature", if (thinkingLevel == "HIGH") 1.0 else 0.4)
                
                if (thinkingLevel == "HIGH") {
                    genConfig.put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                    // Do NOT set maxOutputTokens here as specified by instructions
                } else {
                    genConfig.put("maxOutputTokens", 2048)
                }
            }
            requestJson.put("generationConfig", genConfig)

            // 3. Grounding Tools
            if (groundingTool == "googleSearch" || groundingTool == "googleMaps") {
                val toolsArray = JSONArray()
                val toolObj = JSONObject().apply {
                    put("googleSearch", JSONObject())
                }
                toolsArray.put(toolObj)
                requestJson.put("tools", toolsArray)
            }

            // System instruction (only for text generation)
            if (!isImageGeneration && !isImageEditing) {
                val systemInstructionText = """
                    You are "Gemini Smart QC Innovation Specialist". You are answering a query using ${modelName}.
                    System Directives:
                    - Address the query with top competency standard.
                    - If Google Search or Google Maps grounding is enabled, provide precise real-world verifiable data.
                    - If thinkingLevel is HIGH, output a well-articulated, extremely thorough logical analysis.
                """.trimIndent()
                
                requestJson.put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstructionText)
                        })
                    })
                })
            }

            // 4. Candidate model fallbacks of similar capability but higher free tier availability
            val candidates = if (isImageGeneration || isImageEditing) {
                listOf(modelName, "gemini-2.5-flash-image").distinct()
            } else {
                listOf(modelName, "gemini-3.5-flash", "gemini-3.1-flash-lite-preview").distinct()
            }

            var lastResponseCode = 200
            var lastResponseBody = ""
            var successfulBody = ""
            var attemptSuccessful = false
            var finalModelUsed = modelName

            for (model in candidates) {
                // If we fallback to a different model, update the systemInstruction model reference if present
                if (model != modelName && requestJson.has("systemInstruction")) {
                    try {
                        val systemInstructionText = """
                            You are "Gemini Smart QC Innovation Specialist". You are answering a query using ${model}.
                            System Directives:
                            - Address the query with top competency standard.
                            - If Google Search or Google Maps grounding is enabled, provide precise real-world verifiable data.
                            - If thinkingLevel is HIGH, output a well-articulated, extremely thorough logical analysis.
                        """.trimIndent()
                        requestJson.put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", systemInstructionText)
                                })
                            })
                        })
                    } catch (ignore: Exception) {}
                }

                val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
                
                try {
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        lastResponseCode = response.code
                        lastResponseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            successfulBody = lastResponseBody
                            attemptSuccessful = true
                            finalModelUsed = model
                            break
                        } else {
                            Log.w(TAG, "Model $model call failed with status ${response.code}: $lastResponseBody")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Model $model execute exception", e)
                    lastResponseCode = 500
                    lastResponseBody = e.localizedMessage ?: "Connection exception"
                }
            }

            if (attemptSuccessful) {
                val parsedJson = JSONObject(successfulBody)
                val responseCandidates = parsedJson.optJSONArray("candidates")
                if (responseCandidates != null && responseCandidates.length() > 0) {
                    val firstCandidate = responseCandidates.getJSONObject(0)
                    
                    // Parse thinking process details if present under candidate
                    val candidatesParts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
                    var explanationText = ""
                    var imageBase64Result = ""
                    
                    if (candidatesParts != null) {
                        for (i in 0 until candidatesParts.length()) {
                            val p = candidatesParts.getJSONObject(i)
                            val inlineData = p.optJSONObject("inlineData")
                            if (inlineData != null) {
                                imageBase64Result = inlineData.optString("data", "")
                            }
                            val text = p.optString("text", "")
                            if (text.isNotEmpty()) {
                                explanationText += text
                            }
                        }
                    }
                    
                    if (isImageGeneration || isImageEditing) {
                        if (imageBase64Result.isNotEmpty()) {
                            return@withContext "SUCCESS_IMAGE:$imageBase64Result"
                        } else if (explanationText.isNotEmpty()) {
                            // Sometimes returned as a textual confirmation or inline image data structure
                            return@withContext explanationText
                        }
                    } else {
                        // Check for Grounding Metadata to present to the user
                        val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
                        if (groundingMetadata != null) {
                            val searchChunks = groundingMetadata.optJSONArray("groundingChunks")
                            if (searchChunks != null && searchChunks.length() > 0) {
                                explanationText += "\n\n🌐 **Grounding Sources Referenced (via Fallback $finalModelUsed):**"
                                for (j in 0 until searchChunks.length()) {
                                    val chunk = searchChunks.getJSONObject(j)
                                    val web = chunk.optJSONObject("web")
                                    if (web != null) {
                                        val title = web.optString("title", "Source")
                                        val uri = web.optString("uri", "")
                                        explanationText += "\n- [$title]($uri)"
                                    }
                                }
                            }
                        }
                        
                        // Add context notice if a fallback occurred cleanly
                        if (finalModelUsed != modelName) {
                            explanationText = "*(Note: Automatically routed to $finalModelUsed to bypass free-tier model limits/quota blocks.)*\n\n" + explanationText
                        }
                        return@withContext explanationText
                    }
                }
                return@withContext "Empty result returned by Gemini."
            } else {
                // If all fallbacks failed, present a meaningful and extremely clean error message
                val isQuotaExceeded = lastResponseBody.contains("quota", ignoreCase = true) || lastResponseBody.contains("limit", ignoreCase = true) || lastResponseCode == 429
                val cleanErrorDesc = if (isQuotaExceeded) {
                    "Your Gemini API free tier quota has been exceeded. Please check your key limits or switch models in Settings (e.g. try Flash-Lite or General Flash).\n\nDetails: $lastResponseBody"
                } else {
                    "Error: Call failed with status $lastResponseCode\nResponse: $lastResponseBody"
                }
                return@withContext cleanErrorDesc
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Connection Error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    fun getActiveApiKey(context: android.content.Context? = null): String {
        if (!customApiKey.isNullOrBlank()) return customApiKey!!
        context?.let {
            val prefs = it.getSharedPreferences("audit_settings", android.content.Context.MODE_PRIVATE)
            val savedKey = prefs.getString("gemini_api_key", null)
            if (!savedKey.isNullOrBlank()) {
                return savedKey
            }
        }
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Checks if the configured Gemini api key is a placeholder
     */
    fun isApiKeyPlaceholder(context: android.content.Context? = null): Boolean {
        val key = getActiveApiKey(context)
        return key.isEmpty() || key == "MY_GEMINI_API_KEY" || key == "GEMINI_API_KEY"
    }

    /**
     * Sends a query to the Gemini model with system instructions and returns the text response
     */
    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(), // Pair of (role, message) where role is "user" or "model"
        activeAuditContext: String? = null,
        context: android.content.Context? = null
    ): String = withContext(Dispatchers.IO) {
        if (isApiKeyPlaceholder(context)) {
            return@withContext getOfflineResponse(prompt)
        }

        try {
            val key = getActiveApiKey(context)

            // System instructions defining the persona for QC and auditing
            val systemInstructionText = """
                You are "Gemini QC Auditor", a highly professional, expert AI adviser specializing in:
                1. Quality Management Systems (QMS) - specifically ISO 9001:2015.
                2. Audit methodologies (internal audits, checklists, documentation, reporting).
                3. Root Cause Analysis (RCS) using methodologies like the 5 Whys, Fishbone (Ishikawa) Diagrams, and CAPA (Corrective and Preventive Actions).
                
                You must provide complete, structured, and informative answers with precise ISO clause references where applicable, action plans, and clear formatting (markdown bolding, lists, and headings).
                Tone: Expert, objective, helpful, clear, and professional. Avoid fluff.
            """.trimIndent()

            // Build request JSON using native JSONObject for absolute safety (no dependency build errors)
            val requestJson = JSONObject()

            // 1. Add Contents array (which includes history + current prompt)
            val contentsArray = JSONArray()

            // Add conversation history if present
            history.forEach { (role, message) ->
                val contentObj = JSONObject().apply {
                    put("role", if (role == "user") "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", message)
                        })
                    })
                }
                contentsArray.put(contentObj)
            }

            // Create current content segment
            val currentPromptText = if (activeAuditContext != null) {
                "--- CURRENT ACTIVE AUDIT CONTEXT ---\n$activeAuditContext\n-----------------------------------\n\nUser Question:\n$prompt"
            } else {
                prompt
            }

            val currentContentObj = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", currentPromptText)
                    })
                })
            }
            contentsArray.put(currentContentObj)

            requestJson.put("contents", contentsArray)

            // 2. Add System Instruction
            requestJson.put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstructionText)
                    })
                })
            })

            // 3. Add Generation Config
            requestJson.put("generationConfig", JSONObject().apply {
                put("temperature", 0.4) // Slightly lower for more factual and precise ISO/QC references
                put("topP", 0.95)
            })

            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(jsonMediaType)

            // Sequentially try multiple model names to guarantee 100% uptime
            val modelCandidates = listOf(
                "gemini-3.5-flash",
                "gemini-3.1-flash-lite-preview",
                "gemini-3.1-pro-preview"
            )

            var lastErrorMsg = "No models attempted"

            for (modelName in modelCandidates) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key"
                try {
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            val parsedJson = JSONObject(bodyString)
                            val candidates = parsedJson.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val firstCandidate = candidates.getJSONObject(0)
                                val contentObj = firstCandidate.optJSONObject("content")
                                val parts = contentObj?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    return@withContext parts.getJSONObject(0).optString("text", "No response found in parts.")
                                }
                            }
                            return@withContext "No response candidate was returned by Gemini."
                        } else {
                            Log.w(TAG, "Model $modelName call failed with status ${response.code}: $bodyString")
                            lastErrorMsg = "Model $modelName failed with status (${response.code})"
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Model $modelName connection exception: ${e.message}", e)
                    lastErrorMsg = "Model $modelName exception: ${e.localizedMessage ?: "Unknown"}"
                }
            }

            return@withContext "Error: Connected to Gemini server but received responsive errors. Details: $lastErrorMsg. Please verify your internet connection, credentials and parameters."
        } catch (e: Exception) {
            Log.e(TAG, "Error performing API execution: ${e.message}", e)
            return@withContext "Failed to connect to Google Gemini. Please check your internet connection. (Details: ${e.localizedMessage ?: "Unknown error"})\n\nFallback offline instruction:\n" + getOfflineResponse(prompt)
        }
    }

    /**
     * Intelligent local knowledge base matching the user request keywords,
     * so the app works beautifully offline or if the user hasn't set an API key yet!
     */
    fun getOfflineResponse(prompt: String): String {
        val query = prompt.lowercase()
        return when {
            query.contains("9001") || query.contains("iso") || query.contains("qms") -> """
                ### 📚 ISO 9001:2015 Quality Management System (QMS) - Reference Guide
                *(Offline Mode: Answering using built-in QMS database)*

                The **ISO 9001:2015** standard is built on **7 Quality Management Principles**:
                1. Customer focus
                2. Leadership
                3. Engagement of people
                4. Process approach
                5. Improvement
                6. Evidence-based decision making
                7. Relationship management

                #### Key Clauses for Internal Auditors:
                *   **Clause 4: Context of the Organization** - Scope definition, interested parties, and process-mapping.
                *   **Clause 5: Leadership** - Role of management commitment, Quality Policy, and organizational responsibilities.
                *   **Clause 6: Planning** - Risk-based thinking (identifying risks, opportunities, and Quality Objectives).
                *   **Clause 7: Support** - Competency, awareness, resources, and **Documented Information (Cl 7.5)**.
                *   **Clause 8: Operation** - Operational control, design, supplier evaluation, and nonconforming outputs control (CAPA).
                *   **Clause 9: Performance Evaluation (CRITICAL FOR AUDITORS)**:
                    *   **Clause 9.1** - Monitoring, measurement, and customer satisfaction.
                    *   **Clause 9.2 (Internal Audit)** - Requirements for organizing regular, unbiased quality audits.
                    *   **Clause 9.3** - Management Review.
                *   **Clause 10: Improvement** - Nonconformity, Corrective Actions (CAPA), and continuous improvement.

                #### Recommended Audit Action:
                Ensure all internal audits evaluate *compliance* with procedures, *effectiveness* of controls, and *opportunities for improvement* (OFI).
            """.trimIndent()

            query.contains("root cause") || query.contains("rca") || query.contains("why") || query.contains("fishbone") || query.contains("cause") -> """
                ### 🧐 Root Cause Analysis (RCA) - Expert Guide
                *(Offline Mode: Answering using built-in RCA database)*

                When a non-conformity (NCR) is identified, a proper RCA is mandatory under **ISO 9001:2015 Clause 10.2**.

                #### 1. The "5 Whys" Methodology:
                Ask "Why" iteratively (usually 5 times) to drill past surface-level symptoms to find the systemic failure.
                *   **Symptom**: Machine stopped mid-production.
                *   *Why?* → Fuse blew due to overload.
                *   *Why?* → Shaft wasn't lubricated properly.
                *   *Why?* → Lubrication pump was clogged.
                *   *Why?* → Pump filter is checked monthly instead of daily (SYSTEMIC ROOT CAUSE).

                #### 2. Fishbone (Ishikawa / 6M) Diagram:
                Categorize potential failure causes into six dimensions:
                1.  **Man (Personnel)**: Training gaps, fatigue, lack of awareness.
                2.  **Machine (Equipment)**: Calibration drift, wear and tear, tooling failure.
                3.  **Method (Process)**: Ambiguous guidelines, outdated drawings, poor instructions.
                4.  **Material**: Substandard parts, incorrect grade, moisture.
                5.  **Measurement**: Incorrect gauges, unverified calipers, high uncertainty.
                6.  **Milieu (Mother Nature/Environment)**: Humidity, dust, vibration.

                #### 3. Corrective vs. Preventive Actions (CAPA):
                *   **Correction**: Instant containment of the symptom (e.g., replace the blown fuse).
                *   **Corrective Action**: Eliminate the root cause to prevent *recurrence* (e.g., revise maintenance logs to require daily pump checks).
            """.trimIndent()

            query.contains("checklist") || query.contains("template") || query.contains("how to") || query.contains("audit") -> """
                ### 📝 Professional Auditing Checklist & Principles (ISO 19011)
                *(Offline Mode: Answering using built-in QC auditing database)*

                Per **ISO 19011 (Guidelines for Auditing Management Systems)**, follow this structured process:

                #### 1. Audit Preparation
                *   Study standard procedures, specifications, and the project Quality Plan.
                *   Formulate a checklist highlighting high-risk areas.

                #### 2. Gathering Auditable Evidence
                Use the **"PRIMA"** technique for verification:
                *   **P** - Physical inspection of worksite/materials.
                *   **R** - Records review (calibration logs, test sheets).
                *   **I** - Interviews with technicians, supervisors.
                *   **M** - Monitoring process compliance in real-time.
                *   **A** - Assessment of overall procedural awareness.

                #### 3. Framing Non-Conformity Reports (NCRs)
                Always document NCRs with three mandatory elements:
                1.  **Requirement Reference**: State the exact specification/procedure/ISO clause violated.
                2.  **Objective Evidence**: State the facts (e.g., "Calibrator #QC-91 had calibration expired on 12/04/24").
                3.  **Statement of Variance**: Describe exact difference between the standard and the observation.
            """.trimIndent()

            else -> """
                ### 🤖 Gemini QC Audit AI Engine
                *(Offline Mode: Please configure your API key for live internet replies!)*

                I can help you review or draft high-quality auditing data, research compliance requirements, and build corrective actions.

                #### Here are some things you can ask me:
                *   **"Explain ISO 9001:2015 Clause 9.2 requirements"**
                *   **"How do I write a root cause analysis for concrete compression failure?"**
                *   **"What is the difference between correction and corrective action?"**
                *   **"Suggest a checklist for evaluating weld inspection records"**

                *Hint: If you have an active report loaded, I can analyze your specific QA/QC findings and draft continuous improvement suggestions!*
            """.trimIndent()
        }
    }
}
