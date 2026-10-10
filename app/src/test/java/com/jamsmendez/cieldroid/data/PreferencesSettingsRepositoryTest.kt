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
}
