package com.codestudio.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

@Composable
fun FileExplorerScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("codestudio_workspace", Context.MODE_PRIVATE) }
    var treeUri by remember { mutableStateOf(prefs.getString("tree_uri", null)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            prefs.edit().putString("tree_uri", uri.toString()).apply()
            treeUri = uri.toString()
        }
    }

    val internal = remember {
        DocumentFile.fromFile(File(context.filesDir, "projects").apply { mkdirs() })
    }
    var external by remember { mutableStateOf(false) }
    val root = remember(external, treeUri) {
        if (external && treeUri != null) DocumentFile.fromTreeUri(context, Uri.parse(treeUri!!)) ?: internal
        else internal
    }
    var stack by remember { mutableStateOf(listOf(root)) }
    LaunchedEffect(root.uri) { stack = listOf(root) }
    val current = stack.last()
    var refresh by remember { mutableIntStateOf(0) }
    val files = remember(current.uri, refresh) {
        current.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase().orEmpty() })
    }

    var dialog by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DocumentFile?>(null) }
    var editing by remember { mutableStateOf<DocumentFile?>(null) }
    var editorText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun read(doc: DocumentFile): String =
        if (doc.uri.scheme == "file") File(doc.uri.path!!).readText()
        else context.contentResolver.openInputStream(doc.uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IOException("Não foi possível abrir o arquivo")

    fun write(doc: DocumentFile, value: String) {
        if (doc.uri.scheme == "file") File(doc.uri.path!!).writeText(value)
        else context.contentResolver.openOutputStream(doc.uri, "wt")?.bufferedWriter()?.use { it.write(value) }
            ?: throw IOException("Não foi possível salvar o arquivo")
    }

    fun copy(doc: DocumentFile, destination: DocumentFile, newName: String) {
        val outputDoc = destination.createFile(doc.type ?: "application/octet-stream", newName)
            ?: throw IOException("Não foi possível criar a cópia")
        try {
            val input = if (doc.uri.scheme == "file") File(doc.uri.path!!).inputStream()
                else context.contentResolver.openInputStream(doc.uri) ?: throw IOException("Falha ao ler origem")
            val output = if (outputDoc.uri.scheme == "file") File(outputDoc.uri.path!!).outputStream()
                else context.contentResolver.openOutputStream(outputDoc.uri) ?: throw IOException("Falha ao gravar cópia")
            input.use { source -> output.use { target -> source.copyTo(target) } }
        } catch (e: Exception) {
            outputDoc.delete()
            throw e
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Explorador de arquivos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (external && treeUri != null) "Pasta escolhida no dispositivo" else "Área privada do CodeStudio",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { picker.launch(null) }) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Escolher pasta")
            }
        }
        if (treeUri != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !external, onClick = { external = false }, label = { Text("Interno") })
                FilterChip(selected = external, onClick = { external = true }, label = { Text("Pasta externa") })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (stack.size > 1) {
                IconButton(onClick = { stack = stack.dropLast(1) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
            }
            Text(stack.drop(1).joinToString(" / ") { it.name ?: "Pasta" }.ifBlank { "Projetos" },
                Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            IconButton(onClick = { name = ""; dialog = "folder" }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Nova pasta")
            }
            IconButton(onClick = { name = ""; dialog = "file" }) {
                Icon(Icons.Default.NoteAdd, contentDescription = "Novo arquivo")
            }
        }
        HorizontalDivider()
        if (files.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.FolderOpen, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("Esta pasta está vazia", fontWeight = FontWeight.SemiBold)
                Text("Crie um arquivo ou uma pasta para começar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(files, key = { it.uri.toString() }) { item ->
                    var expanded by remember(item.uri) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().clickable {
                        if (item.isDirectory) stack = stack + item
                        else runCatching { read(item) }.onSuccess { editorText = it; editing = item }
                            .onFailure { error = it.message ?: "Falha ao abrir arquivo" }
                    }.padding(vertical = 10.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description, null,
                            tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name ?: "Sem nome", fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(if (item.isDirectory) "Pasta" else "${item.length()} bytes",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, "Ações") }
                            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("Renomear") }, onClick = {
                                    expanded = false; selected = item; name = item.name.orEmpty(); dialog = "rename"
                                })
                                if (!item.isDirectory) DropdownMenuItem(text = { Text("Fazer cópia") }, onClick = {
                                    expanded = false; selected = item
                                    val old = item.name ?: "arquivo"
                                    val dot = old.lastIndexOf('.')
                                    name = if (dot > 0) old.substring(0, dot) + "_copia" + old.substring(dot) else old + "_copia"
                                    dialog = "copy"
                                })
                                DropdownMenuItem(text = { Text("Excluir") }, onClick = {
                                    expanded = false; selected = item; dialog = "delete"
                                })
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }

    if (dialog in listOf("file", "folder", "rename", "copy")) {
        val title = when (dialog) {
            "file" -> "Novo arquivo"; "folder" -> "Nova pasta"; "rename" -> "Renomear"; else -> "Copiar arquivo"
        }
        AlertDialog(onDismissRequest = { dialog = null }, title = { Text(title) },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val clean = name.trim()
                        require(clean.isNotBlank() && clean != "." && clean != ".." && !clean.contains("/")) { "Informe um nome válido." }
                        when (dialog) {
                            "file" -> check(current.createFile("text/plain", clean) != null) { "Não foi possível criar o arquivo." }
                            "folder" -> check(current.createDirectory(clean) != null) { "Não foi possível criar a pasta." }
                            "rename" -> check(selected?.renameTo(clean) == true) { "Não foi possível renomear." }
                            "copy" -> copy(selected ?: error("Arquivo não selecionado"), current, clean)
                        }
                        dialog = null; refresh++
                    } catch (e: Exception) { error = e.message ?: "Operação não concluída" }
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } })
    }

    if (dialog == "delete") AlertDialog(onDismissRequest = { dialog = null },
        title = { Text("Excluir item?") },
        text = { Text("O item ${selected?.name} será excluído. Esta ação não pode ser desfeita.") },
        confirmButton = { TextButton(onClick = {
            if (selected?.delete() == true) refresh++ else error = "Não foi possível excluir o item."
            dialog = null
        }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } })

    editing?.let { doc ->
        AlertDialog(onDismissRequest = { editing = null }, title = { Text(doc.name ?: "Editar arquivo", maxLines = 1) },
            text = {
                OutlinedTextField(editorText, { editorText = it }, Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 440.dp),
                    placeholder = { Text("Conteúdo do arquivo") }, minLines = 10, maxLines = 20)
            },
            confirmButton = { TextButton(onClick = {
                runCatching { write(doc, editorText) }.onSuccess { editing = null; refresh++ }
                    .onFailure { error = it.message ?: "Não foi possível salvar" }
            }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text("Salvar") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Fechar") } })
    }

    error?.let { message ->
        AlertDialog(onDismissRequest = { error = null }, title = { Text("Erro") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } })
    }
}
