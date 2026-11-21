package com.codestudio.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField

data class EditorDocument(
    val uri: String,
    val name: String,
    val content: String,
    val dirty: Boolean = false
)

@Composable
fun CodeEditorScreen(
    documents: List<EditorDocument>,
    selectedUri: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onChange: (String, String) -> Unit,
    onSave: (String) -> Unit
) {
    val current = documents.firstOrNull { it.uri == selectedUri }
    if (documents.isEmpty() || current == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Editor de código", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text("Abra um arquivo pelo explorador para começar a editar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(documents, key = { it.uri }) { doc ->
                Surface(
                    color = if (doc.uri == selectedUri) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    onClick = { onSelect(doc.uri) }
                ) {
                    Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text((if (doc.dirty) "● " else "") + doc.name,
                            style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        IconButton(onClick = { onClose(doc.uri) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar aba",
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(languageLabel(current.name), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onSave(current.uri) }, enabled = current.dirty) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Salvar")
            }
        }
        val lines = current.content.count { it == '\n' } + 1
        val scroll = rememberScrollState()
        Row(Modifier.fillMaxSize().padding(bottom = 4.dp)) {
            Column(
                Modifier.width(44.dp).fillMaxHeight().verticalScroll(scroll)
                    .padding(end = 8.dp, top = 8.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.End
            ) {
                repeat(lines) { index ->
                    Text((index + 1).toString(), fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp, lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                }
            }
            BasicTextField(
                value = TextFieldValue(current.content, selection = TextRange(current.content.length)),
                onValueChange = { onChange(current.uri, it.text) },
                modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(scroll)
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 21.sp
                ),
                visualTransformation = SyntaxHighlightTransformation(current.name),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun languageLabel(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt", "kts" -> "Kotlin"
        "java" -> "Java"
        "py" -> "Python"
        "js", "jsx", "mjs" -> "JavaScript"
        "ts", "tsx" -> "TypeScript"
        "json" -> "JSON"
        "html", "htm" -> "HTML"
        "css" -> "CSS"
        "sh", "bash" -> "Shell"
        "xml" -> "XML"
        "md" -> "Markdown"
        else -> if (ext.isBlank()) "Texto" else ext.uppercase()
    }
}

private class SyntaxHighlightTransformation(private val fileName: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val keywords = when (ext) {
            "kt", "kts", "java" -> "\\b(val|var|fun|class|object|interface|if|else|when|for|while|return|public|private|protected|static|void|new|this|import|package|override|suspend|true|false|null)\\b"
            "py" -> "\\b(def|class|if|elif|else|for|while|return|import|from|as|try|except|with|lambda|pass|True|False|None|and|or|not|in|is)\\b"
            "js", "jsx", "mjs", "ts", "tsx" -> "\\b(const|let|var|function|class|if|else|for|while|return|import|export|from|async|await|new|this|true|false|null|undefined)\\b"
            "sh", "bash" -> "\\b(if|then|else|fi|for|do|done|case|esac|function|export|local|return)\\b"
            else -> "\\b(true|false|null)\\b"
        }
        val source = text.text
        val result = buildAnnotatedString {
            append(text)
            Regex("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'").findAll(source).forEach {
                addStyle(SpanStyle(color = Color(0xFF6A9955)), it.range.first, it.range.last + 1)
            }
            Regex("//[^\\n]*|#[^\\n]*").findAll(source).forEach {
                addStyle(SpanStyle(color = Color(0xFF6A9955)), it.range.first, it.range.last + 1)
            }
            Regex(keywords).findAll(source).forEach {
                addStyle(SpanStyle(color = Color(0xFF569CD6)), it.range.first, it.range.last + 1)
            }
            Regex("\\b[0-9]+(\\.[0-9]+)?\\b").findAll(source).forEach {
                addStyle(SpanStyle(color = Color(0xFFB5CEA8)), it.range.first, it.range.last + 1)
            }
        }
        return TransformedText(result, OffsetMapping.Identity)
    }
}
