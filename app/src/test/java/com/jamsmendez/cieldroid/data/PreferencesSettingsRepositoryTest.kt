package com.jamsmendez.cieldroid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jamsmendez.cieldroid.domain.*
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesSettingsRepositoryTest {
    private fun testFile(): File = File("build/settings-tests/${UUID.randomUUID()}.preferences_pb").also {
        checkNotNull(it.parentFile).mkdirs()
    }

    @Test fun defaultsWritesAndActualFileReopenPreserveIndependentKeys() = runTest {
        val file = testFile()
        val key = stringPreferencesKey("unrelated")
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val repo = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            assertTrue(repo.state.value.loaded)
            assertEquals(PreviewSettings(), repo.state.value.settings)
            store.edit { it[key] = "retained" }
            coroutineScope {
                launch { repo.setScene(SceneId.IDLE) }
                launch { repo.setFps(PreviewFps.SIXTY) }
            }
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.IDLE, PreviewFps.SIXTY), repo.state.value.settings)
            // Recreating a repository on the same store must not reset stored values.
            val recreated = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            assertEquals(repo.state.value.settings, recreated.state.value.settings)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
        val reopenedScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val reopened = PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
        try {
            val repo = PreferencesSettingsRepository(reopened, reopenedScope)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.IDLE, PreviewFps.SIXTY), repo.state.value.settings)
            assertEquals("retained", reopened.updateData { it }[key])
        } finally { reopenedScope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun unknownValuesAndWrongTypesUseValidatedDefaults() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val file = testFile()
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            store.edit {
                it[stringPreferencesKey("preview_scene")] = "../unknown"
                it[intPreferencesKey("preview_fps")] = 45
            }
            val repo = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            assertTrue(repo.state.value.loaded)
            assertEquals(PreviewSettings(), repo.state.value.settings)
            store.edit {
                it[intPreferencesKey("preview_scene")] = 1
                it[stringPreferencesKey("preview_fps")] = "60"
            }
            advanceUntilIdle()
            assertEquals(PreviewSettings(), repo.state.value.settings)
            assertNull(repo.state.value.error)
            assertNull(SceneId.parse("PROCESSING"))
            assertNull(PreviewFps.parse(0))
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun activeModeDefaultsToWallpaperRoundTripsAndRejectsInvalidValues() = runTest {
        val file = testFile()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val repo = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            assertEquals(ActiveMode.WALLPAPER, repo.state.value.mode)
            repo.setActiveMode(ActiveMode.MINI)
            advanceUntilIdle()
            assertEquals(ActiveMode.MINI, repo.state.value.mode)
            assertEquals("mini", store.updateData { it }[stringPreferencesKey("active_mode")])
            // The mode is independent of the shared scene/FPS config.
            assertEquals(PreviewSettings(), repo.state.value.settings)
            repo.setScene(SceneId.RAPHAEL)
            advanceUntilIdle()
            assertEquals(ActiveMode.MINI, repo.state.value.mode)
            repo.setActiveMode(ActiveMode.WALLPAPER)
            advanceUntilIdle()
            assertEquals(ActiveMode.WALLPAPER, repo.state.value.mode)
            assertEquals("wallpaper", store.updateData { it }[stringPreferencesKey("active_mode")])
            assertEquals(SceneId.RAPHAEL, repo.state.value.settings.scene)
            // Unknown values and wrong types mean the default, never an error.
            store.edit { it[stringPreferencesKey("active_mode")] = "MINI" }
            advanceUntilIdle()
            assertEquals(ActiveMode.WALLPAPER, repo.state.value.mode)
            store.edit {
                it.remove(stringPreferencesKey("active_mode"))
                it[intPreferencesKey("active_mode")] = 1
            }
            advanceUntilIdle()
            assertEquals(ActiveMode.WALLPAPER, repo.state.value.mode)
            assertNull(repo.state.value.error)
            store.edit {
                it.remove(intPreferencesKey("active_mode"))
                it[stringPreferencesKey("active_mode")] = "mini"
            }
            advanceUntilIdle()
            assertEquals(ActiveMode.MINI, repo.state.value.mode)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
        // The mode survives reopening the actual file.
        val reopenedScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val reopened = PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
        try {
            val repo = PreferencesSettingsRepository(reopened, reopenedScope)
            advanceUntilIdle()
            assertEquals(ActiveMode.MINI, repo.state.value.mode)
        } finally { reopenedScope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun legacyWallpaperAndOverlayKeysAreIgnoredAndLeftInPlace() = runTest {
        val file = testFile()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            store.edit {
                it[stringPreferencesKey("wallpaper_scene")] = "raphael"
                it[intPreferencesKey("wallpaper_fps")] = 60
                it[stringPreferencesKey("overlay_scene")] = "explorer"
                it[intPreferencesKey("overlay_fps")] = 60
            }
            val repo = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            // Stored per-surface choices never leak into the one shared config.
            assertEquals(PreviewSettings(), repo.state.value.settings)
            repo.setScene(SceneId.IDLE)
            repo.setFps(PreviewFps.SIXTY)
            repo.setActiveMode(ActiveMode.MINI)
            repo.setOverlayMoveMode(false)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.IDLE, PreviewFps.SIXTY), repo.state.value.settings)
            // No migration or deletion: the legacy keys stay exactly as they were.
            val stored = store.updateData { it }
            assertEquals("raphael", stored[stringPreferencesKey("wallpaper_scene")])
            assertEquals(60, stored[intPreferencesKey("wallpaper_fps")])
            assertEquals("explorer", stored[stringPreferencesKey("overlay_scene")])
            assertEquals(60, stored[intPreferencesKey("overlay_fps")])
            assertEquals("idle", stored[stringPreferencesKey("preview_scene")])
            assertEquals(60, stored[intPreferencesKey("preview_fps")])
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun corruptedPreferenceFileIsAnObservableErrorNotLoadedDefaults() = runTest {
        val file = testFile()
        file.writeBytes(byteArrayOf(-1, -1, -1))
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val repo = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            assertNotNull(repo.state.value.error)
            assertFalse(repo.state.value.loaded)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun writeFailureDoesNotOptimisticallyChangeLoadedSettings() = runTest {
        val failure = IOException("write denied")
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { emit(emptyPreferences()) }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw failure
        }
        val repo = PreferencesSettingsRepository(store, backgroundScope)
        runCurrent()
        assertTrue(repo.state.value.loaded)
        try { repo.setFps(PreviewFps.SIXTY); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        runCurrent()
        assertEquals(PreviewSettings(), repo.state.value.settings)
    }

    @Test fun readAndWriteFailuresAreNotSuccessfulPersistence() = runTest {
        val failure = IOException("disk unavailable")
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw failure }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw failure
        }
        val repo = PreferencesSettingsRepository(broken, backgroundScope)
        runCurrent()
        assertSame(failure, repo.state.value.error)
        assertFalse(repo.state.value.loaded)
        try { repo.setFps(PreviewFps.SIXTY); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        try { repo.setScene(SceneId.RAPHAEL); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        try { repo.setActiveMode(ActiveMode.MINI); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        assertEquals(PreviewSettings(), repo.state.value.settings)
        assertEquals(ActiveMode.WALLPAPER, repo.state.value.mode)
    }

    @Test fun overlayMoveModeAndPositionPersistIndependentlyWithValidatedDefaults() = runTest {
        val file = testFile()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val repo = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            assertEquals(OverlaySettings(moveMode = true, position = null), repo.state.value.overlay)
            repo.setOverlayMoveMode(true)
            repo.setOverlayPosition(OverlayPosition(120, 340))
            advanceUntilIdle()
            assertEquals(OverlaySettings(true, OverlayPosition(120, 340)), repo.state.value.overlay)
            assertEquals(PreviewSettings(), repo.state.value.settings)
            assertEquals(ActiveMode.WALLPAPER, repo.state.value.mode)
            val stored = store.updateData { it }
            assertEquals(true, stored[booleanPreferencesKey("overlay_move_mode")])
            assertEquals(120, stored[intPreferencesKey("overlay_x")])
            assertEquals(340, stored[intPreferencesKey("overlay_y")])
            repo.setActiveMode(ActiveMode.MINI)
            repo.setScene(SceneId.EXPLORER)
            advanceUntilIdle()
            assertEquals(OverlaySettings(true, OverlayPosition(120, 340)), repo.state.value.overlay)
            repo.setOverlayMoveMode(false)
            advanceUntilIdle()
            assertEquals(OverlaySettings(false, OverlayPosition(120, 340)), repo.state.value.overlay)
            assertEquals(ActiveMode.MINI, repo.state.value.mode)
            // Wrong types, negatives and half-written positions fall back to the defaults.
            store.edit {
                it.remove(booleanPreferencesKey("overlay_move_mode"))
                it[stringPreferencesKey("overlay_move_mode")] = "true"
                it[intPreferencesKey("overlay_x")] = -5
            }
            advanceUntilIdle()
            assertEquals(OverlaySettings(), repo.state.value.overlay)
            store.edit {
                it.remove(intPreferencesKey("overlay_x"))
                it[stringPreferencesKey("overlay_x")] = "10"
            }
            advanceUntilIdle()
            assertEquals(OverlaySettings(), repo.state.value.overlay)
            store.edit { it.remove(stringPreferencesKey("overlay_x")) }
            advanceUntilIdle()
            assertNull(repo.state.value.overlay.position)
            assertNull(repo.state.value.error)
            assertEquals(SceneId.EXPLORER, repo.state.value.settings.scene)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun overlayWriteFailuresPropagateWithoutOptimisticState() = runTest {
        val failure = IOException("write denied")
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { emit(emptyPreferences()) }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw failure
        }
        val repo = PreferencesSettingsRepository(store, backgroundScope)
        runCurrent()
        try { repo.setOverlayMoveMode(true); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        try { repo.setOverlayPosition(OverlayPosition(1, 2)); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        runCurrent()
        assertEquals(OverlaySettings(), repo.state.value.overlay)
    }
}
