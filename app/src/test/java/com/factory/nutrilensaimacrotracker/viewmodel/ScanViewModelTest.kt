package com.factory.nutrilensaimacrotracker.viewmodel

import app.cash.turbine.test
import com.factory.nutrilensaimacrotracker.NutriLensApp
import com.factory.nutrilensaimacrotracker.ai.AnalysisState
import com.factory.nutrilensaimacrotracker.ai.ClaudeAIService
import com.factory.nutrilensaimacrotracker.ai.FoodAnalysisResult
import com.factory.nutrilensaimacrotracker.data.database.FoodEntryDao
import com.factory.nutrilensaimacrotracker.data.database.NutriLensDatabase
import com.factory.nutrilensaimacrotracker.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ScanViewModel
    private lateinit var mockClaudeService: ClaudeAIService
    private val mockFoodEntryDao = mockk<FoodEntryDao>(relaxed = true)
    private val mockDatabase = mockk<NutriLensDatabase>(relaxed = true)
    private val mockApp = mockk<NutriLensApp>(relaxed = true)

    private val fakeFoodResult = FoodAnalysisResult(
        foodName = "Grilled Chicken",
        servingSize = "200g",
        calories = 330f,
        protein = 62f,
        carbs = 0f,
        fat = 7f,
        fiber = 0f,
        sugar = 0f,
        notes = "Healthy lean protein",
        confidence = 0.95f
    )

    @Before
    fun setup() {
        // Intercept ClaudeAIService() constructor so the ViewModel gets a mock
        mockkConstructor(ClaudeAIService::class)

        every { mockDatabase.foodEntryDao() } returns mockFoodEntryDao
        every { mockApp.database } returns mockDatabase
        every { mockFoodEntryDao.getFoodEntriesForDate(any()) } returns flowOf(emptyList())
        coEvery { mockFoodEntryDao.insert(any()) } returns 1L

        viewModel = ScanViewModel(mockApp)

        // Retrieve the mock ClaudeAIService instance via reflection so we can stub it
        val serviceField = ScanViewModel::class.java.getDeclaredField("claudeService")
        serviceField.isAccessible = true
        mockClaudeService = serviceField.get(viewModel) as ClaudeAIService

        coEvery { mockClaudeService.analyzeFoodFromText(any()) } returns Result.success(fakeFoodResult)
        coEvery { mockClaudeService.analyzeFoodFromImage(any(), any()) } returns Result.success(fakeFoodResult)
    }

    @After
    fun teardown() {
        unmockkConstructor(ClaudeAIService::class)
    }

    // --- Initial State Tests ---

    @Test
    fun `analysisState initial value is Idle`() = runTest {
        viewModel.analysisState.test {
            assertTrue(awaitItem() is AnalysisState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `savedSuccess initial value is false`() = runTest {
        viewModel.savedSuccess.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- analyzeFromText Tests ---

    @Test
    fun `analyzeFromText with valid input sets state to Success`() = runTest {
        viewModel.analyzeFromText("Grilled chicken breast")

        viewModel.analysisState.test {
            val state = awaitItem()
            assertTrue(state is AnalysisState.Success)
            assertEquals("Grilled Chicken", (state as AnalysisState.Success).result.foodName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeFromText with blank input does not trigger analysis`() = runTest {
        viewModel.analyzeFromText("   ")

        viewModel.analysisState.test {
            assertTrue(awaitItem() is AnalysisState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeFromText with empty string does not change state`() = runTest {
        viewModel.analyzeFromText("")

        viewModel.analysisState.test {
            assertTrue(awaitItem() is AnalysisState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeFromText with AI failure sets state to Error`() = runTest {
        val errorMessage = "API key not configured"
        coEvery { mockClaudeService.analyzeFoodFromText(any()) } returns
                Result.failure(Exception(errorMessage))

        viewModel.analyzeFromText("Something")

        viewModel.analysisState.test {
            val state = awaitItem()
            assertTrue(state is AnalysisState.Error)
            assertEquals(errorMessage, (state as AnalysisState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeFromText failure with null message uses default error message`() = runTest {
        coEvery { mockClaudeService.analyzeFoodFromText(any()) } returns
                Result.failure(Exception())

        viewModel.analyzeFromText("Something")

        viewModel.analysisState.test {
            val state = awaitItem()
            assertTrue(state is AnalysisState.Error)
            assertEquals("Analysis failed", (state as AnalysisState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeFromText success contains correct macro values`() = runTest {
        viewModel.analyzeFromText("Grilled chicken")

        viewModel.analysisState.test {
            val state = awaitItem() as AnalysisState.Success
            assertEquals(330f, state.result.calories)
            assertEquals(62f, state.result.protein)
            assertEquals(0f, state.result.carbs)
            assertEquals(7f, state.result.fat)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- saveFoodEntry Tests ---

    @Test
    fun `saveFoodEntry inserts entry into database`() = runTest {
        viewModel.saveFoodEntry(fakeFoodResult)

        coVerify {
            mockFoodEntryDao.insert(
                match { entry ->
                    entry.name == "Grilled Chicken" &&
                    entry.calories == 330f &&
                    entry.protein == 62f &&
                    entry.carbs == 0f &&
                    entry.fat == 7f
                }
            )
        }
    }

    @Test
    fun `saveFoodEntry sets savedSuccess to true`() = runTest {
        viewModel.saveFoodEntry(fakeFoodResult)

        viewModel.savedSuccess.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveFoodEntry stores image URI when provided`() = runTest {
        val imageUri = "content://media/external/images/1234"

        viewModel.saveFoodEntry(fakeFoodResult, imageUri)

        coVerify {
            mockFoodEntryDao.insert(match { it.imageUri == imageUri })
        }
    }

    @Test
    fun `saveFoodEntry stores null image URI when not provided`() = runTest {
        viewModel.saveFoodEntry(fakeFoodResult)

        coVerify {
            mockFoodEntryDao.insert(match { it.imageUri == null })
        }
    }

    @Test
    fun `saveFoodEntry stores serving size and notes from result`() = runTest {
        viewModel.saveFoodEntry(fakeFoodResult)

        coVerify {
            mockFoodEntryDao.insert(
                match { it.servingSize == "200g" && it.notes == "Healthy lean protein" }
            )
        }
    }

    @Test
    fun `saveFoodEntry calls dao insert exactly once`() = runTest {
        viewModel.saveFoodEntry(fakeFoodResult)

        coVerify(exactly = 1) { mockFoodEntryDao.insert(any()) }
    }

    // --- resetState Tests ---

    @Test
    fun `resetState clears analysisState to Idle`() = runTest {
        viewModel.analyzeFromText("Apple")
        viewModel.resetState()

        viewModel.analysisState.test {
            assertTrue(awaitItem() is AnalysisState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetState clears savedSuccess to false`() = runTest {
        viewModel.saveFoodEntry(fakeFoodResult)
        viewModel.resetState()

        viewModel.savedSuccess.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetState when already Idle does not throw`() = runTest {
        viewModel.resetState()

        viewModel.analysisState.test {
            assertTrue(awaitItem() is AnalysisState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- AnalysisState sealed class tests ---

    @Test
    fun `AnalysisState Success holds analysis result`() {
        val state: AnalysisState = AnalysisState.Success(fakeFoodResult)
        assertTrue(state is AnalysisState.Success)
        assertEquals("Grilled Chicken", (state as AnalysisState.Success).result.foodName)
        assertEquals(0.95f, state.result.confidence)
    }

    @Test
    fun `AnalysisState Error holds error message`() {
        val state: AnalysisState = AnalysisState.Error("Network timeout")
        assertTrue(state is AnalysisState.Error)
        assertEquals("Network timeout", (state as AnalysisState.Error).message)
    }

    @Test
    fun `AnalysisState Loading is correct sealed subtype`() {
        val state: AnalysisState = AnalysisState.Loading
        assertTrue(state is AnalysisState.Loading)
    }

    @Test
    fun `AnalysisState Idle is correct sealed subtype`() {
        val state: AnalysisState = AnalysisState.Idle
        assertTrue(state is AnalysisState.Idle)
    }
}
