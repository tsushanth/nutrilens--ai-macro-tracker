package com.factory.nutrilensaimacrotracker.ai

import android.graphics.Bitmap
import android.util.Base64
import com.factory.nutrilensaimacrotracker.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class ClaudeAIService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val apiKey = BuildConfig.ANTHROPIC_API_KEY

    suspend fun analyzeFoodFromText(foodDescription: String): Result<FoodAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildTextPrompt(foodDescription)
                val requestBody = buildTextRequestBody(prompt)
                val response = makeApiCall(requestBody)
                val result = parseResponse(response)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun analyzeFoodFromImage(bitmap: Bitmap, additionalContext: String = ""): Result<FoodAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                val base64Image = bitmapToBase64(bitmap)
                val prompt = buildImagePrompt(additionalContext)
                val requestBody = buildImageRequestBody(base64Image, prompt)
                val response = makeApiCall(requestBody)
                val result = parseResponse(response)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildTextPrompt(foodDescription: String): String {
        return """
            Analyze this food and provide detailed macronutrient information.
            Food: $foodDescription

            Respond ONLY with a JSON object in this exact format, no other text:
            {
              "foodName": "exact food name",
              "servingSize": "serving size description",
              "calories": 000,
              "protein": 00.0,
              "carbs": 00.0,
              "fat": 00.0,
              "fiber": 0.0,
              "sugar": 0.0,
              "notes": "any relevant notes",
              "confidence": 0.9
            }

            All numeric values must be numbers (not strings). Calories in kcal, macros in grams.
        """.trimIndent()
    }

    private fun buildImagePrompt(additionalContext: String): String {
        val contextPart = if (additionalContext.isNotBlank()) "\nAdditional context: $additionalContext" else ""
        return """
            Analyze this food image and provide detailed macronutrient information.$contextPart

            Identify what food items are visible, estimate portion sizes, and calculate macros.

            Respond ONLY with a JSON object in this exact format, no other text:
            {
              "foodName": "exact food name",
              "servingSize": "estimated serving size",
              "calories": 000,
              "protein": 00.0,
              "carbs": 00.0,
              "fat": 00.0,
              "fiber": 0.0,
              "sugar": 0.0,
              "notes": "any relevant notes about the analysis",
              "confidence": 0.85
            }

            All numeric values must be numbers (not strings). Calories in kcal, macros in grams.
        """.trimIndent()
    }

    private fun buildTextRequestBody(prompt: String): String {
        val message = JsonObject().apply {
            addProperty("role", "user")
            add("content", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("type", "text")
                    addProperty("text", prompt)
                })
            })
        }

        val requestJson = JsonObject().apply {
            addProperty("model", "claude-opus-4-6")
            addProperty("max_tokens", 1024)
            add("messages", JsonArray().apply { add(message) })
        }

        return gson.toJson(requestJson)
    }

    private fun buildImageRequestBody(base64Image: String, prompt: String): String {
        val message = JsonObject().apply {
            addProperty("role", "user")
            add("content", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("type", "image")
                    add("source", JsonObject().apply {
                        addProperty("type", "base64")
                        addProperty("media_type", "image/jpeg")
                        addProperty("data", base64Image)
                    })
                })
                add(JsonObject().apply {
                    addProperty("type", "text")
                    addProperty("text", prompt)
                })
            })
        }

        val requestJson = JsonObject().apply {
            addProperty("model", "claude-opus-4-6")
            addProperty("max_tokens", 1024)
            add("messages", JsonArray().apply { add(message) })
        }

        return gson.toJson(requestJson)
    }

    private fun makeApiCall(requestBodyJson: String): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Anthropic API key not configured. Please add ANTHROPIC_API_KEY to local.properties")
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from API")

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $responseBody")
        }

        return responseBody
    }

    private fun parseResponse(responseJson: String): FoodAnalysisResult {
        val responseObj = gson.fromJson(responseJson, JsonObject::class.java)
        val content = responseObj
            .getAsJsonArray("content")
            .get(0)
            .asJsonObject
            .get("text")
            .asString

        // Extract JSON from the response text (in case there's extra text)
        val jsonStart = content.indexOf('{')
        val jsonEnd = content.lastIndexOf('}')
        val cleanJson = if (jsonStart >= 0 && jsonEnd > jsonStart) {
            content.substring(jsonStart, jsonEnd + 1)
        } else {
            content
        }

        val resultObj = gson.fromJson(cleanJson, JsonObject::class.java)

        return FoodAnalysisResult(
            foodName = resultObj.get("foodName")?.asString ?: "Unknown Food",
            servingSize = resultObj.get("servingSize")?.asString ?: "1 serving",
            calories = resultObj.get("calories")?.asFloat ?: 0f,
            protein = resultObj.get("protein")?.asFloat ?: 0f,
            carbs = resultObj.get("carbs")?.asFloat ?: 0f,
            fat = resultObj.get("fat")?.asFloat ?: 0f,
            fiber = resultObj.get("fiber")?.asFloat ?: 0f,
            sugar = resultObj.get("sugar")?.asFloat ?: 0f,
            notes = resultObj.get("notes")?.asString ?: "",
            confidence = resultObj.get("confidence")?.asFloat ?: 0.9f
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Scale down to reduce payload size while maintaining quality
        val scaledBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val scale = minOf(1024f / bitmap.width, 1024f / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
