package com.jamsmendez.cieldroid.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// One process-wide DataStore per file: the launcher ViewModel and the wallpaper engine share it.
// The file keeps its historical "preview" name so stored preview settings survive.
val Context.settingsPreferences by preferencesDataStore(name = "preview")
