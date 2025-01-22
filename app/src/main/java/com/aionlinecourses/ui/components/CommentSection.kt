package com.aionlinecourses.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aionlinecourses.data.dao.CommentWithUser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSection(
    comments: List<CommentWithUser>,
    currentUserId: Int,
    onAddComment: (String) -> Unit,
    onReply: (Int, String) -> Unit,
    onLike: (Int) -> Unit,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    Column(modifier = modifier) {
        Text(
            text = "Comments (${comments.size})",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Comment input
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Add a comment...") },
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (commentText.isNotBlank()) {
                        onAddComment(commentText)
                        commentText = ""
                        focusManager.clearFocus()
                    }
                }
            )
        )
        
        LazyColumn {
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    currentUserId = currentUserId,
                    onReply = onReply,
                    onLike = onLike,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: CommentWithUser,
    currentUserId: Int,
    onReply: (Int, String) -> Unit,
    onLike: (Int) -> Unit,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplyInput by remember { mutableStateOf(false) }
    var showEditInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var editText by remember { mutableStateOf(comment.content) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User info and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = comment.profilePicture,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.username,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = formatTimestamp(comment.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (comment.userId == currentUserId) {
                    IconButton(
                        onClick = { showEditInput = !showEditInput }
                    ) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(
                        onClick = { onDelete(comment.id) }
                    ) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Comment content
            if (showEditInput) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editText.isNotBlank()) {
                                onEdit(comment.id, editText)
                                showEditInput = false
                            }
                        }
                    )
                )
            } else {
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = { onLike(comment.id) }
                ) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${comment.likes}")
                }
                
                TextButton(
                    onClick = { showReplyInput = !showReplyInput }
                ) {
                    Icon(
                        Icons.Default.Reply,
                        contentDescription = "Reply",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reply")
                }
            }
            
            // Reply input
            if (showReplyInput) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    placeholder = { Text("Write a reply...") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (replyText.isNotBlank()) {
                                onReply(comment.id, replyText)
                                replyText = ""
                                showReplyInput = false
                            }
                        }
                    )
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val date = Date(timestamp)
            val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            format.format(date)
        }
    }
}
