package com.quicknotes.shahid.app.ui.screens.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.shahid.app.R

@Composable
fun MarkdownToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit,
    onH1: () -> Unit,
    onH2: () -> Unit,
    onH3: () -> Unit,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    onChecklist: () -> Unit,
    onCodeBlock: () -> Unit,
    onLink: () -> Unit,
    onHighlight: () -> Unit,
    onFindReplace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.action_undo),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.action_redo),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onH1, modifier = Modifier.size(36.dp)) {
                Text(text = "H1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            IconButton(onClick = onH2, modifier = Modifier.size(36.dp)) {
                Text(text = "H2", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            IconButton(onClick = onH3, modifier = Modifier.size(36.dp)) {
                Text(text = "H3", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            IconButton(onClick = onBold, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatBold,
                    contentDescription = stringResource(R.string.toolbar_bold),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onItalic, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatItalic,
                    contentDescription = stringResource(R.string.toolbar_italic),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onUnderline, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatUnderlined,
                    contentDescription = stringResource(R.string.toolbar_underline),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onStrikethrough, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatStrikethrough,
                    contentDescription = stringResource(R.string.toolbar_strikethrough),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onHighlight, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Highlight,
                    contentDescription = stringResource(R.string.toolbar_highlight),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onBulletList, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = stringResource(R.string.toolbar_bullet_list),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onNumberedList, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatListNumbered,
                    contentDescription = stringResource(R.string.toolbar_numbered_list),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onChecklist, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.CheckBox,
                    contentDescription = stringResource(R.string.toolbar_checklist),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onCodeBlock, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = stringResource(R.string.toolbar_code),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onLink, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = stringResource(R.string.toolbar_link),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onFindReplace, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.FindReplace,
                    contentDescription = stringResource(R.string.toolbar_find),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
