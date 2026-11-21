package com.codestudio.android

import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class WorkspaceTab(val label: String, val icon: ImageVector) {
    EXPLORER("Arquivos", Icons.Default.Folder),
    SEARCH("Buscar", Icons.Default.Search),
    SOURCE("Git", Icons.Default.AccountTree),
    GITHUB("GitHub", Icons.Default.Cloud),
    TERMINAL("Terminal", Icons.Default.Terminal),
    EDITOR("Editor", Icons.Default.Code)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CodeStudioApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeStudioApp() {
    var darkTheme by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(WorkspaceTab.EXPLORER) }
    var showSettings by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val documents = remember { mutableStateListOf<EditorDocument>() }
    var selectedUri by remember { mutableStateOf<String?>(null) }

    fun openEditorFile(uri: String, name: String, content: String) {
        val existing = documents.indexOfFirst { it.uri == uri }
        if (existing >= 0) selectedUri = uri
        else { documents.add(EditorDocument(uri, name, content)); selectedUri = uri }
        selectedTab = WorkspaceTab.EDITOR
    }

    fun saveEditorFile(uri: String) {
        val index = documents.indexOfFirst { it.uri == uri }
        if (index < 0) return
        val doc = documents[index]
        runCatching {
            val parsed = Uri.parse(doc.uri)
            if (parsed.scheme == "file") File(parsed.path!!).writeText(doc.content)
            else context.contentResolver.openOutputStream(parsed, "wt")?.bufferedWriter()?.use { it.write(doc.content) }
                ?: error("Não foi possível salvar o arquivo")
        }.onSuccess {
            documents[index] = doc.copy(dirty = false)
            Toast.makeText(context, "Arquivo salvo", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, it.message ?: "Falha ao salvar", Toast.LENGTH_LONG).show()
        }
    }

    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("CodeStudio", fontWeight = FontWeight.Bold)
                            Text(
                                "Android IDE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { darkTheme = !darkTheme }) {
                            Icon(
                                if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Alternar tema"
                            )
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Configurações")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf(
                        WorkspaceTab.EXPLORER,
                        WorkspaceTab.SOURCE,
                        WorkspaceTab.TERMINAL,
                        WorkspaceTab.EDITOR
                    ).forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                NavigationRail {
                    listOf(
                        WorkspaceTab.EXPLORER,
                        WorkspaceTab.SEARCH,
                        WorkspaceTab.SOURCE,
                        WorkspaceTab.GITHUB,
                        WorkspaceTab.TERMINAL,
                        WorkspaceTab.EDITOR
                    ).forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
                VerticalDivider()
                WorkspaceContent(
                    selectedTab, Modifier.weight(1f),
                    documents, selectedUri,
                    onOpenFile = ::openEditorFile,
                    onSelectFile = { selectedUri = it },
                    onCloseFile = { uri -> documents.removeAll { it.uri == uri }; if (selectedUri == uri) selectedUri = documents.lastOrNull()?.uri },
                    onChangeFile = { uri, value -> val i = documents.indexOfFirst { it.uri == uri }; if (i >= 0) documents[i] = documents[i].copy(content = value, dirty = true) },
                    onSaveFile = ::saveEditorFile
                )
            }
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Configurações") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Aparência")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tema escuro", modifier = Modifier.weight(1f))
                            Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                        }
                        Text(
                            "Armazenamento: interno ou pasta escolhida (em implementação futura).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) { Text("Fechar") }
                }
            )
        }
    }
}

@Composable
private fun WorkspaceContent(
    tab: WorkspaceTab, modifier: Modifier = Modifier,
    documents: List<EditorDocument>, selectedUri: String?,
    onOpenFile: (String, String, String) -> Unit,
    onSelectFile: (String) -> Unit, onCloseFile: (String) -> Unit,
    onChangeFile: (String, String) -> Unit, onSaveFile: (String) -> Unit
) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text(
            tab.label,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        when (tab) {
            WorkspaceTab.EXPLORER -> FileExplorerScreen(onOpenFile)
            WorkspaceTab.EDITOR -> CodeEditorScreen(documents, selectedUri, onSelectFile, onCloseFile, onChangeFile, onSaveFile)
            WorkspaceTab.SEARCH -> EmptyState(
                Icons.Default.Search,
                "Busca do projeto",
                "A busca em arquivos será adicionada junto ao explorador."
            )
            WorkspaceTab.SOURCE -> EmptyState(
                Icons.Default.AccountTree,
                "Controle de versão",
                "Git local, alterações, branches e commits serão implementados na etapa 5."
            )
            WorkspaceTab.GITHUB -> EmptyState(
                Icons.Default.Cloud,
                "Conectar ao GitHub",
                "Autenticação segura e gerenciamento de repositórios serão implementados na etapa 6."
            )
            WorkspaceTab.TERMINAL -> EmptyState(
                Icons.Default.Terminal,
                "Terminal integrado",
                "O terminal próprio e a integração com Termux serão desenvolvidos nas etapas 3 e 4."
            )
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, description: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
