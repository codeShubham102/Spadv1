package com.example.ui.profiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.GamepadProfile
import com.example.data.repository.ProfileRepository
import kotlinx.coroutines.launch

@Composable
fun ProfileManagerDialog(
    profileRepository: ProfileRepository,
    activeProfileId: String,
    onSelectProfile: (GamepadProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profiles by profileRepository.getAllProfilesFlow().collectAsState(initial = emptyList())
    var showImportDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Controller Profiles",
                    color = Color(0xFF00F0FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Switch between game presets or export/import custom layouts.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles, key = { it.id }) { p ->
                        val isActive = p.id == activeProfileId
                        ProfileItemRow(
                            profile = p,
                            isActive = isActive,
                            onSelect = { onSelectProfile(p) },
                            onDuplicate = {
                                scope.launch {
                                    profileRepository.duplicateProfile(p.id)
                                    Toast.makeText(context, "Duplicated ${p.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    profileRepository.deleteProfile(p.id)
                                    Toast.makeText(context, "Deleted ${p.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onExport = {
                                val json = profileRepository.exportProfileJson(p)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("S!pad Profile", json))
                                Toast.makeText(context, "Copied profile JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { showImportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                modifier = Modifier.testTag("btn_import_json")
            ) {
                Icon(Icons.Default.Download, contentDescription = "Import", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Import JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close", color = Color.White, fontSize = 12.sp)
            }
        }
    )

    if (showImportDialog) {
        ImportProfileSubDialog(
            onImport = { json ->
                scope.launch {
                    val result = profileRepository.importProfileJson(json)
                    result.onSuccess { imported ->
                        profileRepository.saveProfile(imported)
                        onSelectProfile(imported)
                        showImportDialog = false
                        Toast.makeText(context, "Imported ${imported.name} successfully!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Import failed: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
private fun ProfileItemRow(
    profile: GamepadProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0xFF1E293B) else Color(0xFF131D2E),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) Color(0xFF00F0FF) else Color(0xFF24344D),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (profile.isBuiltIn) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "PRESET",
                            color = Color(0xFF00F0FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(Color(0xFF003840), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                if (profile.targetGame.isNotBlank()) {
                    Text(
                        text = profile.targetGame,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF00F0FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }

                IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Export JSON", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }

                if (!profile.isBuiltIn) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF0055), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportProfileSubDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Text("Import Profile JSON", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    text = "Paste S!pad layout JSON content below:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    placeholder = { Text("{\"name\": \"Custom\", ...}", color = Color.Gray, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(jsonText) },
                enabled = jsonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
            ) {
                Text("Import", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
