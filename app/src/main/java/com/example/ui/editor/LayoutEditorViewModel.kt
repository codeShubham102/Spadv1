package com.example.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.ControlElementConfig
import com.example.core.model.ControlShape
import com.example.core.model.ControlType
import com.example.core.model.DefaultProfiles
import com.example.core.model.GamepadProfile
import com.example.data.local.AppDatabase
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ProfileRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LayoutEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val profileRepository: ProfileRepository = ProfileRepositoryImpl(database.profileDao())

    private val _draftProfile = MutableStateFlow(DefaultProfiles.BALANCED)
    val draftProfile: StateFlow<GamepadProfile> = _draftProfile.asStateFlow()

    private val _selectedControlId = MutableStateFlow<String?>(null)
    val selectedControlId: StateFlow<String?> = _selectedControlId.asStateFlow()

    private var initialProfile: GamepadProfile? = null

    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            val p = profileRepository.getProfileById(profileId) ?: DefaultProfiles.BALANCED
            val baseControls = if (p.controls.none { it.type == ControlType.SETTINGS_BUTTON }) {
                p.controls + ControlElementConfig(
                    id = "settings_btn",
                    type = ControlType.SETTINGS_BUTTON,
                    xPercent = 0.50f,
                    yPercent = 0.48f,
                    sizeDp = 48f,
                    shape = ControlShape.CIRCLE,
                    colorHex = if (p.id == DefaultProfiles.XBOX.id) "#107C10" else "#00F0FF"
                )
            } else {
                p.controls
            }
            val ensuredControls = baseControls.map { ctrl ->
                if (ctrl.type == ControlType.SETTINGS_BUTTON && ctrl.yPercent < 0.10f) {
                    ctrl.copy(xPercent = 0.50f, yPercent = 0.48f, sizeDp = 48f)
                } else {
                    ctrl
                }
            }
            val ensured = p.copy(controls = ensuredControls)
            initialProfile = ensured
            _draftProfile.value = ensured
            _selectedControlId.value = ensured.controls.firstOrNull()?.id
        }
    }

    fun selectControl(id: String) {
        _selectedControlId.value = id
    }

    fun updateControlPosition(id: String, newXPercent: Float, newYPercent: Float) {
        val current = _draftProfile.value
        val updated = current.controls.map {
            if (it.id == id) {
                it.copy(
                    xPercent = newXPercent.coerceIn(0.02f, 0.98f),
                    yPercent = newYPercent.coerceIn(0.04f, 0.96f)
                )
            } else it
        }
        _draftProfile.value = current.copy(controls = updated)
    }

    fun moveControlByDelta(id: String, deltaXPercent: Float, deltaYPercent: Float) {
        val current = _draftProfile.value
        val updated = current.controls.map {
            if (it.id == id) {
                it.copy(
                    xPercent = (it.xPercent + deltaXPercent).coerceIn(0.02f, 0.98f),
                    yPercent = (it.yPercent + deltaYPercent).coerceIn(0.04f, 0.96f)
                )
            } else it
        }
        _draftProfile.value = current.copy(controls = updated)
    }

    fun nudgeControl(id: String, deltaXPercent: Float, deltaYPercent: Float) {
        moveControlByDelta(id, deltaXPercent, deltaYPercent)
    }

    fun updateControlSize(id: String, sizeDp: Float) {
        val current = _draftProfile.value
        val updated = current.controls.map {
            if (it.id == id) it.copy(sizeDp = sizeDp.coerceIn(40f, 260f)) else it
        }
        _draftProfile.value = current.copy(controls = updated)
    }

    fun updateControlRotation(id: String, rotationDeg: Float) {
        val current = _draftProfile.value
        val updated = current.controls.map {
            if (it.id == id) it.copy(rotationDeg = rotationDeg.coerceIn(-180f, 180f)) else it
        }
        _draftProfile.value = current.copy(controls = updated)
    }

    fun updateControlColor(id: String, colorHex: String) {
        val current = _draftProfile.value
        val updated = current.controls.map {
            if (it.id == id) it.copy(colorHex = colorHex) else it
        }
        _draftProfile.value = current.copy(controls = updated)
    }

    fun resetToDefaults() {
        val default = DefaultProfiles.ALL_DEFAULTS.find { it.id == _draftProfile.value.id }
            ?: DefaultProfiles.BALANCED
        _draftProfile.value = default.copy(id = _draftProfile.value.id, name = _draftProfile.value.name)
    }

    fun saveChanges(onComplete: () -> Unit) {
        viewModelScope.launch {
            profileRepository.saveProfile(_draftProfile.value)
            onComplete()
        }
    }
}
