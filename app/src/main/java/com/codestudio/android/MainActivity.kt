package com.codestudio.android

import android.os.Bundle
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
    TERMINAL("Terminal", Icons.Default.Terminal)
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
                        WorkspaceTab.TERMINAL
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
                        WorkspaceTab.TERMINAL
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
                WorkspaceContent(selectedTab, Modifier.weight(1f))
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
private fun WorkspaceContent(tab: WorkspaceTab, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text(
            tab.label,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        when (tab) {
            WorkspaceTab.EXPLORER -> {
                Text(
                    "Seus projetos aparecerão aqui.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = { }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir projeto — próxima etapa")
                }
                EmptyState(
                    Icons.Default.FolderOpen,
                    "Nenhum projeto aberto",
                    "A seleção de pastas e o explorador de arquivos serão implementados na etapa 2."
                )
            }
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
