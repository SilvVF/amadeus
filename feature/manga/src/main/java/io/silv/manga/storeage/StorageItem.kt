package io.silv.manga.storeage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.silv.common.model.MangaCover
import io.silv.ui.theme.LocalSpacing


data class StorageItem(
    val id: String,
    val title: String,
    val size: Long,
    val cover: MangaCover,
    val entriesCount: Int,
    val color: Color,
)

@Composable
fun StorageItem(
    item: StorageItem,
    modifier: Modifier = Modifier,
    onDelete: (String) -> Unit,
) {
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    val space = LocalSpacing.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space.med),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            AsyncImage(
                modifier = Modifier
                    .height(58.dp)
                    .aspectRatio(1f)
                    .clip(
                        RoundedCornerShape(22)
                    ),
                contentScale = ContentScale.Crop,
                model = item.cover,
                contentDescription = item.title,
            )
            Column(
                modifier = Modifier.weight(1f),
                content = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.W700,
                        maxLines = 1,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Box(
                                modifier = Modifier
                                    .background(item.color, CircleShape)
                                    .size(12.dp),
                            )
                            Spacer(Modifier.width(space.small))
                            Text(
                                text = item.size.toSize(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = space.small / 2)
                                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .size(space.small / 2),
                            )
                            Text(
                                text = "${item.entriesCount} chapters",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                    )
                },
            )
            IconButton(
                onClick = {
                    showDeleteDialog = true
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                    )
                },
            )
        },
    )

    if (showDeleteDialog) {
        ItemDeleteDialog(
            title = item.title,
            onDismissRequest = { showDeleteDialog = false },
            onDelete = {
                onDelete(item.id)
            },
        )
    }
}

@Composable
private fun ItemDeleteDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismissRequest()
                },
                content = {
                    Text(text = "OK")
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                content = {
                    Text(text = "Cancel")
                },
            )
        },
        title = {
            Text(
                text = "Delete downloaded chapters?",
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete chapters from \"$title\"?",
            )
        },
    )
}