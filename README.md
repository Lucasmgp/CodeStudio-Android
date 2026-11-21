# CodeStudio Android

IDE móvel para Android, inspirada no VS Code.

## Decisões do projeto
- Nome: CodeStudio Android
- Tema claro/escuro selecionável
- Edição e Git local planejados para funcionar offline
- Extensões próprias
- Projetos no armazenamento privado ou em pastas escolhidas pelo usuário
- Terminal próprio e integração opcional com Termux
- Compatibilidade planejada com ARM32 e ARM64

## Etapa 1
Esta base contém o aplicativo Android inicial, navegação visual de módulos e workflow de CI para gerar APK debug.

Ainda não implementa editor, Git, terminal ou conexão GitHub dentro do app. Esses módulos serão adicionados em etapas próprias.


## APK de desenvolvimento
- Nome instalado: CodeStudio - Editor de Código
- Arquivo gerado: CodeStudio-Editor-de-Codigo.apk
- As compilações debug reutilizam uma chave de assinatura persistida no cache do GitHub Actions para permitir instalar atualizações por cima da versão anterior.
- O número da versão de compilação é incrementado automaticamente pelo GitHub Actions.
