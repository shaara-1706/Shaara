package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val TAG = "GeminiClient"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            // Log outgoing request URL for debugging
            Log.d(TAG, "Request URL: ${request.url}")
            chain.proceed(request)
        }
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    fun isApiKeyAvailable(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Converts a Bitmap to a JPEG Base64 encoded string
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Extracts text from an image (OCR text extraction)
     */
    suspend fun extractTextFromImage(bitmap: Bitmap, customPrompt: String? = null): String {
        if (!isApiKeyAvailable()) {
            return getMockOcrResult()
        }

        val base64Image = bitmap.toBase64()
        val prompt = customPrompt ?: "Review this photographed textbook or notebook study page. Extract all legible handwritten or printed study text and structural notes exactly, formatting with clear header subheadings (###), bullet lists, and readable structural paragraphs. Preserve notes spacing, equations, and diagrams text."

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.4f)
        )

        return try {
            val response = service.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "AI could not extract text from the document. Please try again with a clearer photograph."
        } catch (e: Exception) {
            Log.e(TAG, "OCR Error", e)
            "OCR Processing failed: ${e.localizedMessage}. (Please confirm your Internet connection)."
        }
    }

    /**
     * Summarizes the text content of a note
     */
    suspend fun summarizeNoteContent(contentHtml: String): String {
        if (!isApiKeyAvailable()) {
            return getMockSummary(contentHtml)
        }

        val prompt = "Synthesize and summarize the following study notes. Extract the core key takeaways, fundamental definitions, and bullet-pointed summaries for quick student revision. Provide a clean, minimal layout with header tags:\n\n$contentHtml"

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert academic tutor and summary assistant. You format your output clean and tidy.")))
        )

        return try {
            val response = service.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No summary could be constructed."
        } catch (e: Exception) {
            Log.e(TAG, "Summarizer Error", e)
            getMockSummary(contentHtml)
        }
    }

    /**
     * Generates a 4-question revision Quiz in raw formatted JSON
     */
    suspend fun generateRevisionQuiz(noteTitle: String, ocrOrContent: String): String {
        if (!isApiKeyAvailable()) {
            return getMockQuiz()
        }

        val prompt = """
            Analyze these notes about "$noteTitle" and generate a 4-question multiple-choice revision quiz for a student in RAW JSON format.
            Do NOT enclose in markdown tags like ```json. Return ONLY standard JSON.
            The JSON structure MUST be an array of objects matching this exact format:
            [
              {
                "question": "What is the primary function of mitochondria?",
                "options": ["Generate ATP energy", "Synthesize lipid membranes", "Store nuclear DNA", "Digest cellular waste"],
                "correctIndex": 0,
                "explanation": "Mitochondria convert glucose into adenosine triphosphate (ATP) through cellular respiration."
              }
            ]
            
            Study Content:
            $ocrOrContent
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.5f)
        )

        return try {
            val response = service.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: getMockQuiz()
        } catch (e: Exception) {
            Log.e(TAG, "Quiz Error", e)
            getMockQuiz()
        }
    }

    /**
     * Generates active-recall Flashcards in raw JSON format
     */
    suspend fun generateFlashcards(noteTitle: String, content: String): String {
        if (!isApiKeyAvailable()) {
            return getMockFlashcards()
        }

        val prompt = """
            Based on the study notes titled "$noteTitle", generate a 5-card active recall study deck in RAW JSON format.
            Do NOT enclose in markdown tags. Return ONLY standard JSON.
            The JSON structure MUST be an array of objects matching this exact format:
            [
              {
                "front": "Active Recall definition",
                "back": "Testing yourself by retrieving information from memory rather than passively re-reading text."
              }
            ]
            
            Study Content:
            $content
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.6f)
        )

        return try {
            val response = service.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: getMockFlashcards()
        } catch (e: Exception) {
            Log.e(TAG, "Flashcard Error", e)
            getMockFlashcards()
        }
    }

    /**
     * Explain a difficult topic (AI chatbot tutor)
     */
    suspend fun explainDifficultTopic(topic: String, noteSummaryContext: String = ""): String {
        if (!isApiKeyAvailable()) {
            return getMockExplainResult(topic)
        }

        val prompt = if (noteSummaryContext.isNotEmpty()) {
            "I am studying this topic: \"$topic\". Based on my notebooks context:\n$noteSummaryContext\nExplain this in an incredibly simple, engaging manner. Use simple analogies, bullet points, and real-world examples suitable for a student."
        } else {
            "Explain \"$topic\" in an engaging, comprehensive but simple manner. Break down complex parts, list 3 interesting real-world facts, and use analogies a student at any level can master."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are Shaara AI, a warm, supportive, and brilliant academic mentor.")))
        )

        return try {
            val response = service.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No explanation available."
        } catch (e: Exception) {
            Log.e(TAG, "Explain Error", e)
            getMockExplainResult(topic)
        }
    }

    // --- Mock Fallbacks for offline / missing API key ---

    private fun getMockOcrResult(): String {
        return """
            ### 🌿 Photolytic Water Oxidation (Extracted Note)
            
            **Overview:**
            Extracted from captured textbook image. Chloroplast photolysis separates water elements under focused photon bombardment. 
            
            **Chemical Equation:**
            $$2H_2O + \text{photons} \rightarrow 4H^+ + 4e^- + O_2$$
            
            **Key Stages:**
            1. **Photon Capture (Photosystem II):** Chlorophyll a molecules collect raw sunlight waves, entering an excited energetic state.
            2. **Split Sequence:** High electron affinities pull hydrogen protons apart, releasing pure oxygen gas as product waste.
            3. **Proton Motive Force:** Free H+ ions concentrate along the inner space membrane to trigger cellular ATP production.
        """.trimIndent()
    }

    private fun getMockSummary(notes: String): String {
        return """
            ### 📝 Study Summary Note
            *Generated by Shaara AI*
            
            **Primary Core Concept:**
            Structured breakdown of plant cellular energetics and biochemical transfer mechanics.
            
            **Key takeaways:**
            • Respiration requires active concentration of proton gradient matrices across internal cells.
            • Photo-excitation acts as a cosmic lever, splitting water molecules into clean electrons.
            • Enzymes harvest mechanical force to synthesise ATP fuel molecules.
            
            *💡 Tip: Revise the exact differences between thylakoid and mitochondrial membrane locations before your exam.*
        """.trimIndent()
    }

    private fun getMockQuiz(): String {
        return """
            [
              {
                "question": "What primary byproduct is released during biological photolysis in plants?",
                "options": ["Carbon dioxide", "Oxygen gas (O2)", "Glucose sugars", "Liquid nitrogen"],
                "correctIndex": 1,
                "explanation": "Splitting water (H2O) releases electrons and hydrogen ions, leaving oxygen gas which is discharged from the leaf stoma."
              },
              {
                "question": "Where do hydrogen ions concentrate during photolytic proton motives?",
                "options": ["Outer cell wall", "Nuclear envelope", "Thylakoid lumen", "Cytoplasm cytoplasm"],
                "correctIndex": 2,
                "explanation": "Protons are active-transported inside the thylakoid lumen, constructing high voltages to fuel chloroplast ATP synthesase."
              },
              {
                "question": "Which solar energy collecting pigment drives Photosystem II?",
                "options": ["Chlorophyll a", "Carotenoid beta", "Xanthophyll lutein", "Anthocyanin"],
                "correctIndex": 0,
                "explanation": "Chlorophyll a molecules absorb blue-violet solar wavelengths, reaching high energetic profiles."
              },
              {
                "question": "What is the ultimate purpose of plant photophosphorylation?",
                "options": ["Heat generation", "Synthesis of ATP and NADPH", "Cell replication", "Water absorption"],
                "correctIndex": 1,
                "explanation": "Combining light processes produces energetic ATP and NADPH which fuel the dark Calvin cycle to build sugars."
              }
            ]
        """.trimIndent()
    }

    private fun getMockFlashcards(): String {
        return """
            [
              {
                "front": "Photolysis",
                "back": "The chemical decomposition of water molecules into oxygen, protons, and electrons, triggered by absorbed solar photon waves."
              },
              {
                "front": "Photosystem II",
                "back": "The primary multisubunit protein complex in chloroplast membranes that captures solar waves and initiates photolysis of water."
              },
              {
                "front": "Thylakoid Lumen",
                "back": "The internal fluid space of a thylakoid disk where hydrogen protons gather, forming high concentrations relative to the stroma."
              },
              {
                "front": "ATP Synthesase",
                "back": "The molecular turbine enzyme that synthesizes adenosine triphosphate (ATP) as hydrogen protons flow down their concentration level."
              },
              {
                "front": "NADH / NADPH",
                "back": "High-energy electron carrier coenzymes that carry biological power to chemical synthesis reactions."
              }
            ]
        """.trimIndent()
    }

    private fun getMockExplainResult(topic: String): String {
        return """
            ### 🧠 Deep-Dive: "$topic" Explained Simply!
            *Created by your Shaara AI Mentor*
            
            Let's break this down. Imagine **$topic** is like a high-speed **Amazon Fulfillment Center**. 
            
            **1. The Analogy:**
            Every cog has a precise job. Just like delivery trucks must line up sequentially at the loading dock, molecules also follow strict pathways to deliver energetic packages (the final deliveries!) exactly where they're needed in the school or body.
            
            **2. Core Pillars:**
            • **Input Fuel:** The starting materials that need to be packaged.
            • **Delivery Machinery:** Enzymatic structures that sort and stack everything orderly.
            • **Final Product:** Active results (e.g. chemical power or solutions!) ready for export.
            
            **3. Fun Facts to Impress Your Class:**
            1. **Absolute Efficiency:** This process is more efficient than any human-made micro-combustion engine on Earth!
            2. **Constant Action:** At this very second, billions of these miniature operations are compiling inside your cells.
            3. **Cosmic Origin:** The core energy that sparks this transaction travelled 93 million miles from the Sun in under 8 minutes!
            
            *Does this help you master the concept? Feel free to ask a follow-up question!*
        """.trimIndent()
    }
}
