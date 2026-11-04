package com.quicknotes.shahid.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quicknotes.shahid.app.R

@Composable
fun CheckmarkMorphIcon(
    isCopied: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    normalColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    AnimatedContent(
        targetState = isCopied,
        transitionSpec = {
            (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
        },
        label = "checkmarkMorph",
        modifier = modifier
    ) { copied ->
        if (copied) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_copy_key),
                tint = activeColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.cd_copy_key),
                tint = normalColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
