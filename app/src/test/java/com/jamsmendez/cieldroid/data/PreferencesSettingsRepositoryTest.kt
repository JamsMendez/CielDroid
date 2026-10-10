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

    @Test fun wallpaperKeysAreIndependentOfPreviewKeys() = runTest {
        val file = testFile()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val repo = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY), repo.state.value.wallpaper)
            assertEquals(PreviewSettings.WALLPAPER_DEFAULT, repo.state.value.wallpaper)
            repo.setWallpaperScene(SceneId.RAPHAEL)
            repo.setWallpaperFps(PreviewFps.SIXTY)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.RAPHAEL, PreviewFps.SIXTY), repo.state.value.wallpaper)
            assertEquals(PreviewSettings(), repo.state.value.settings)
            val stored = store.updateData { it }
            assertEquals("raphael", stored[stringPreferencesKey("wallpaper_scene")])
            assertEquals(60, stored[intPreferencesKey("wallpaper_fps")])
            assertNull(stored[stringPreferencesKey("preview_scene")])
            assertNull(stored[intPreferencesKey("preview_fps")])
            repo.setScene(SceneId.EXPLORER)
            repo.setFps(PreviewFps.SIXTY)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.EXPLORER, PreviewFps.SIXTY), repo.state.value.settings)
            assertEquals(PreviewSettings(SceneId.RAPHAEL, PreviewFps.SIXTY), repo.state.value.wallpaper)
            // Unknown values and wrong types fall back to the wallpaper default, not the preview one.
            store.edit {
                it[stringPreferencesKey("wallpaper_scene")] = "../unknown"
                it[intPreferencesKey("wallpaper_fps")] = 45
            }
            advanceUntilIdle()
            assertEquals(PreviewSettings.WALLPAPER_DEFAULT, repo.state.value.wallpaper)
            store.edit {
                it.remove(stringPreferencesKey("wallpaper_scene"))
                it.remove(intPreferencesKey("wallpaper_fps"))
                it[intPreferencesKey("wallpaper_scene")] = 1
                it[stringPreferencesKey("wallpaper_fps")] = "60"
            }
            advanceUntilIdle()
            assertEquals(PreviewSettings.WALLPAPER_DEFAULT, repo.state.value.wallpaper)
            assertEquals(PreviewSettings(SceneId.EXPLORER, PreviewFps.SIXTY), repo.state.value.settings)
            assertNull(repo.state.value.error)
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
        try { repo.setWallpaperScene(SceneId.RAPHAEL); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        try { repo.setWallpaperFps(PreviewFps.SIXTY); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        assertEquals(PreviewSettings(), repo.state.value.settings)
        assertEquals(PreviewSettings.WALLPAPER_DEFAULT, repo.state.value.wallpaper)
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
            assertEquals(PreviewSettings.WALLPAPER_DEFAULT, repo.state.value.wallpaper)
            val stored = store.updateData { it }
            assertEquals(true, stored[booleanPreferencesKey("overlay_move_mode")])
            assertEquals(120, stored[intPreferencesKey("overlay_x")])
            assertEquals(340, stored[intPreferencesKey("overlay_y")])
            repo.setWallpaperScene(SceneId.RAPHAEL)
            repo.setScene(SceneId.EXPLORER)
            advanceUntilIdle()
            assertEquals(OverlaySettings(true, OverlayPosition(120, 340)), repo.state.value.overlay)
            repo.setOverlayMoveMode(false)
            advanceUntilIdle()
            assertEquals(OverlaySettings(false, OverlayPosition(120, 340)), repo.state.value.overlay)
            assertEquals(SceneId.RAPHAEL, repo.state.value.wallpaper.scene)
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

    @Test fun overlaySceneAndFpsDefaultToIdleThirtyAndAreIndependentOfWallpaper() = runTest {
        val file = testFile()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            // A stored wallpaper choice must not leak into an unset overlay: unset means the overlay default.
            store.edit {
                it[stringPreferencesKey("wallpaper_scene")] = "raphael"
                it[intPreferencesKey("wallpaper_fps")] = 60
            }
            val repo = PreferencesSettingsRepository(store, scope)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.IDLE, PreviewFps.THIRTY), repo.state.value.overlay.content)
            assertEquals(PreviewSettings.OVERLAY_DEFAULT, repo.state.value.overlay.content)
            repo.setOverlayScene(SceneId.EXPLORER)
            repo.setOverlayFps(PreviewFps.SIXTY)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.EXPLORER, PreviewFps.SIXTY), repo.state.value.overlay.content)
            assertEquals(PreviewSettings(SceneId.RAPHAEL, PreviewFps.SIXTY), repo.state.value.wallpaper)
            assertEquals(PreviewSettings(), repo.state.value.settings)
            val stored = store.updateData { it }
            assertEquals("explorer", stored[stringPreferencesKey("overlay_scene")])
            assertEquals(60, stored[intPreferencesKey("overlay_fps")])
            // Wallpaper writes leave the overlay alone, and overlay writes leave the wallpaper alone.
            repo.setWallpaperScene(SceneId.PROCESSING)
            repo.setWallpaperFps(PreviewFps.THIRTY)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.EXPLORER, PreviewFps.SIXTY), repo.state.value.overlay.content)
            repo.setOverlayScene(SceneId.RAPHAEL)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.PROCESSING, PreviewFps.THIRTY), repo.state.value.wallpaper)
            assertEquals(PreviewSettings(SceneId.RAPHAEL, PreviewFps.SIXTY), repo.state.value.overlay.content)
            // Move mode and position stay independent of the overlay scene/FPS.
            repo.setOverlayMoveMode(true)
            repo.setOverlayPosition(OverlayPosition(7, 9))
            advanceUntilIdle()
            assertEquals(
                OverlaySettings(true, OverlayPosition(7, 9), PreviewSettings(SceneId.RAPHAEL, PreviewFps.SIXTY)),
                repo.state.value.overlay,
            )
            // Unknown values and wrong types fall back to the overlay default, not the wallpaper value.
            store.edit {
                it[stringPreferencesKey("overlay_scene")] = "../unknown"
                it[intPreferencesKey("overlay_fps")] = 45
            }
            advanceUntilIdle()
            assertEquals(PreviewSettings.OVERLAY_DEFAULT, repo.state.value.overlay.content)
            store.edit {
                it.remove(stringPreferencesKey("overlay_scene"))
                it.remove(intPreferencesKey("overlay_fps"))
                it[intPreferencesKey("overlay_scene")] = 1
                it[stringPreferencesKey("overlay_fps")] = "60"
            }
            advanceUntilIdle()
            assertEquals(PreviewSettings.OVERLAY_DEFAULT, repo.state.value.overlay.content)
            assertEquals(OverlaySettings(true, OverlayPosition(7, 9)), repo.state.value.overlay)
            assertEquals(PreviewSettings(SceneId.PROCESSING, PreviewFps.THIRTY), repo.state.value.wallpaper)
            assertNull(repo.state.value.error)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
        // The overlay choice survives reopening the actual file.
        val reopenedScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val reopened = PreferenceDataStoreFactory.create(scope = reopenedScope, produceFile = { file })
        try {
            reopened.edit {
                it.remove(intPreferencesKey("overlay_scene"))
                it.remove(stringPreferencesKey("overlay_fps"))
                it[stringPreferencesKey("overlay_scene")] = "processing"
                it[intPreferencesKey("overlay_fps")] = 60
            }
            val repo = PreferencesSettingsRepository(reopened, reopenedScope)
            advanceUntilIdle()
            assertEquals(PreviewSettings(SceneId.PROCESSING, PreviewFps.SIXTY), repo.state.value.overlay.content)
        } finally { reopenedScope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun overlaySceneAndFpsWriteFailuresPropagateWithoutOptimisticState() = runTest {
        val failure = IOException("write denied")
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { emit(emptyPreferences()) }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw failure
        }
        val repo = PreferencesSettingsRepository(store, backgroundScope)
        runCurrent()
        try { repo.setOverlayScene(SceneId.RAPHAEL); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        try { repo.setOverlayFps(PreviewFps.SIXTY); fail("Must propagate write failure") }
        catch (e: IOException) { assertSame(failure, e) }
        runCurrent()
        assertEquals(PreviewSettings.OVERLAY_DEFAULT, repo.state.value.overlay.content)
    }
}
