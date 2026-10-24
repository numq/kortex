package io.github.numq.kortex.core.compose.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun DefaultEditorMenu(data: EditorMenuData) {
    Box(modifier = Modifier.offset { IntOffset(data.x.roundToInt(), data.y.roundToInt()) }.size(0.dp)) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = data.dismiss,
            offset = DpOffset.Zero,
            properties = PopupProperties(focusable = false),
        ) {
            if (data.canCut) {
                DropdownMenuItem(
                    text = { Text("Cut") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCut, null) },
                    onClick = data.cut,
                )
            }

            if (data.canCopy) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                    onClick = data.copy,
                )
            }

            if (data.canPaste) {
                DropdownMenuItem(
                    text = { Text("Paste") },
                    leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) },
                    onClick = data.paste,
                )
            }

            if (!data.isReadOnly) {
                if (data.canUndo) {
                    DropdownMenuItem(
                        text = { Text("Undo") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Undo, null) },
                        onClick = data.undo,
                    )
                }

                if (data.canRedo) {
                    DropdownMenuItem(
                        text = { Text("Redo") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Redo, null) },
                        onClick = data.redo,
                    )
                }
            }

            DropdownMenuItem(
                text = { Text("Select All") },
                leadingIcon = { Icon(Icons.Rounded.SelectAll, null) },
                onClick = data.selectAll,
            )
        }
    }
}