# DevOps | Item Service

> Projeto: **Achados e Perdidos** · Microsserviço: **item-service** · Java 21 / Spring Boot / Maven · Monorepo

Este README é um **guia de demonstração do Trabalho 2 de DevOps**: para cada prática, explica **o que é**, **onde verificar**, **qual comando executar** e **qual evidência apresentar**. Não confunda um comando de verificação com uma funcionalidade já implementada. Onde a implementação não foi confirmada, a seção informa o que conferir.

## 0. Preparação: onde executar os comandos

Abra o **PowerShell na raiz do repositório que contém `services/` e `.github/`**. No projeto, há histórico de uma pasta `achados-perdidos` dentro de outra: verifique a raiz antes de executar comandos.

```powershell
Get-Location
Get-ChildItem -Name
Test-Path .\services\item-service\pom.xml
Test-Path .\.github\workflows
```

Os dois `Test-Path` devem retornar `True`. Caso contrário, entre na pasta correta com `cd .\achados-perdidos` (somente se essa subpasta existir). Para evitar repetir caminhos:

```powershell
$ITEM = '.\services\item-service'
```

**Regra de apresentação:** primeiro mostre o arquivo no projeto, depois rode o comando e, por último, explique o resultado.

## 1. Organização em monorepo

**O que é:** manter os microsserviços, a infraestrutura e os pipelines em **um repositório Git**, sem exigir um repositório por serviço.

**Onde:** `services/`, `.github/`, `docker/` (quando existir).

```powershell
Get-ChildItem .\services -Directory | Select-Object Name
Get-ChildItem .\.github -Recurse -File | Select-Object FullName
Get-ChildItem . -Recurse -File -Include 'compose.yml','compose.yaml','docker-compose.yml','docker-compose.yaml' | Select-Object FullName
```

**Evidência:** estrutura das pastas, mostrando `item-service` ao lado dos outros serviços. **Explique:** o repositório é compartilhado, mas cada microsserviço possui seu próprio `pom.xml` e código.

## 2. Controle de versão e fluxo Git

**O que é:** registrar mudanças em commits e trabalhar em branches antes de integrá-las à `develop`/`main`.

**Onde:** histórico do Git, branches e Pull Requests do GitHub.

```powershell
git status
git branch -a
git log --oneline --decorate -10
git remote -v
git log --oneline --all -- services/item-service
```

**Para demonstrar um commit de versão:**

```powershell
git log --all --oneline --grep='semver'
git show --stat <HASH_DO_COMMIT>
```

Substitua `<HASH_DO_COMMIT>` por um hash real retornado pelo primeiro comando (não digite os sinais `< >`). **Evidência:** branch `feature/semver`, commit da versão e Pull Request, se existente. **Não faça merge durante a apresentação sem necessidade.**

## 3. Versionamento semântico (SemVer)

**O que é:** usar `MAJOR.MINOR.PATCH` para comunicar mudanças de compatibilidade. Ex.: `1.0.0`.

**Onde:** `services/item-service/pom.xml`, tag Git (se criada) e histórico de commits.

```powershell
Select-String -Path "$ITEM\pom.xml" -Pattern '<version>'
mvn -f "$ITEM\pom.xml" help:evaluate '-Dexpression=project.version' -q '-DforceStdout'
git tag --list
git log --all --oneline -- services/item-service/pom.xml
```

**Resultado esperado conhecido:** a versão `1.0.0` foi configurada anteriormente; confirme que continua assim. **Explique:** `MAJOR` = mudança incompatível; `MINOR` = funcionalidade compatível; `PATCH` = correção compatível. **Atenção:** versão no `pom.xml` não prova que uma tag Git ou release tenha sido criada.

## 4. Build e gerenciamento de dependências com Maven

**O que é:** o Maven resolve dependências, compila, testa e empacota a aplicação.

**Onde:** `services/item-service/pom.xml` e pasta `target/` gerada pelo build.

```powershell
java -version
mvn -version
mvn -f "$ITEM\pom.xml" clean verify
Get-ChildItem "$ITEM\target" -Filter '*.jar'
```

**Evidência:** `BUILD SUCCESS` e arquivo `.jar`. **Explique:** `verify` executa o ciclo Maven até a verificação, incluindo testes configurados; não garante, por si só, cobertura mínima ou qualidade do código.

## 5. Testes automatizados e cobertura

**O que é:** executar testes sem depender de testes manuais no Postman. **Cobertura** mede quanto do código foi exercitado; testes passando não significam 50% de cobertura.

**Onde:** `services/item-service/src/test/java/`, especialmente `ItemServiceTest.java` (já utilizado no projeto).

```powershell
Get-ChildItem "$ITEM\src\test" -Recurse -Filter '*Test.java'
mvn -f "$ITEM\pom.xml" test
Get-ChildItem "$ITEM\target\surefire-reports" -ErrorAction SilentlyContinue
```

**Evidência:** resumo `Tests run`, `Failures: 0`, `Errors: 0` e relatórios Surefire.

**Cobertura (somente se JaCoCo estiver configurado no `pom.xml`):**

```powershell
Select-String -Path "$ITEM\pom.xml" -Pattern 'jacoco'
mvn -f "$ITEM\pom.xml" verify
Test-Path "$ITEM\target\site\jacoco\index.html"
Start-Process "$ITEM\target\site\jacoco\index.html"
```

Se o relatório não existir, **não apresente 50% como comprovados**. É necessário configurar a coleta e consultar o percentual real; o comando `Start-Process` só deve ser usado se `Test-Path` retornar `True`.

## 6. Docker: imagem e container do item-service

**O que é:** imagem = pacote usado para iniciar a aplicação; container = instância em execução dessa imagem.

**Onde:** `Dockerfile` do serviço ou outro caminho indicado no Compose.

```powershell
Get-ChildItem . -Recurse -Filter Dockerfile | Select-Object FullName
docker version
docker images
docker ps -a
```

**Evidência:** Dockerfile, imagem correspondente e container `item-service` em execução. **Atenção:** se o Docker Desktop estiver parado, `docker version` poderá mostrar erro de conexão; inicie o Docker antes.

## 7. Docker Compose: subir o ambiente integrado

**O que é:** definir e iniciar vários containers juntos (por exemplo: serviço, PostgreSQL e gateway).

**Onde:** arquivo Compose localizado na etapa 1. Execute os comandos **na pasta onde está o arquivo Compose** ou indique-o com `-f`.

```powershell
docker compose config --services
docker compose up --build -d
docker compose ps
docker compose logs --tail=60 item-service
```

Se `docker compose config --services` falhar por não encontrar o arquivo, localize-o e use, por exemplo, `docker compose -f .\docker\compose.yml ...` **somente se esse for o caminho real**. O nome do serviço também deve ser igual ao listado por `config --services`.

**Evidência:** containers `Up` e logs do Spring Boot iniciando. **Não use** `docker compose down -v` para encerrar a apresentação: `-v` pode remover volumes e dados do banco.

## 8. PostgreSQL e persistência

**O que é:** armazenar os itens de forma persistente em banco de dados.

**Onde:** `src/main/resources/application.properties` ou `.yml`, variáveis de ambiente e Compose.

```powershell
Get-ChildItem "$ITEM\src\main\resources" -File
Get-ChildItem "$ITEM\src\main\resources" -File | Select-String -Pattern 'datasource|postgres' -ErrorAction SilentlyContinue
docker compose ps
```

**Evidência:** configuração da conexão e container PostgreSQL. **Não exponha senhas ou o conteúdo integral do `.env`**. Para comprovar persistência, cadastre um item válido, reinicie apenas o container da aplicação e consulte o item novamente; faça isso somente após confirmar o contrato do POST.

## 9. API REST: demonstrar o item-service funcionando

**O que é:** disponibilizar operações de itens por endpoints HTTP.

**Onde:** classes `*Controller.java` em `src/main/java/`.

```powershell
Get-ChildItem "$ITEM\src\main\java" -Recurse -Filter '*Controller.java' | Select-String -Pattern '@(Get|Post|Patch|Put|Delete|Request)Mapping'
Invoke-RestMethod -Uri 'http://localhost:8081/itens' -Method Get
```

**Endpoints conhecidos (confirme no controller):**

| Método | Rota | Objetivo | Exemplo de teste |
|---|---|---|---|
| GET | `/itens` | Listar | `Invoke-RestMethod http://localhost:8081/itens` |
| GET | `/itens/{id}` | Buscar por ID | `Invoke-RestMethod http://localhost:8081/itens/1` |
| POST | `/itens` | Cadastrar | JSON deve seguir os campos reais do DTO/entidade |
| PATCH | `/itens/{id}/status` | Alterar estado | Exemplo abaixo, se o controller receber string JSON |

```powershell
Invoke-RestMethod -Uri 'http://localhost:8081/itens/1/status' -Method Patch -ContentType 'application/json' -Body '"RESERVADO"'
```

**Evidência:** resposta HTTP e atualização consultada depois. O ID `1` deve existir; `RESERVADO` deve ser um status aceito. Se a API estiver protegida por autenticação ou acessível apenas via gateway, adapte URL e token.

## 10. API Gateway e integração

**O que é:** um ponto de entrada para encaminhar chamadas aos microsserviços. **Não é o mesmo que o item-service.**

**Onde:** `services/api-gateway/` e configurações de rotas.

```powershell
Get-ChildItem .\services\api-gateway -Recurse -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'application|pom.xml' } | Select-Object FullName
Get-ChildItem .\services\api-gateway -Recurse -File -Include '*.yml','*.yaml','*.properties' -ErrorAction SilentlyContinue | Select-String -Pattern 'item-service|8081|/itens'
```

**Evidência:** rota configurada e uma requisição passando pelo gateway. A porta `8080` foi usada anteriormente para o gateway, mas **a rota pública precisa ser lida na configuração** antes de testar. Não presuma que `http://localhost:8080/itens` exista.

## 11. CI — integração contínua com GitHub Actions

**O que é:** executar build/testes automaticamente após `push` ou Pull Request, conforme os gatilhos do workflow.

**Onde:** `.github/workflows/ci.yml` (ou outro workflow real).

```powershell
Get-ChildItem .\.github\workflows -File
Get-Content .\.github\workflows\ci.yml
Select-String -Path .\.github\workflows\*.yml -Pattern 'push:|pull_request:|workflow_dispatch:|item-service|mvn|mvnw|setup-java'
```

**O que mostrar no GitHub:** aba **Actions** → workflow de CI → execução recente → job correspondente a `item-service` → logs de checkout, configuração Java, Maven e testes.

**Evidência:** execução concluída com sucesso. **Cuidado:** o workflow geral já teve ajustes nos nomes dos serviços; confira se a matriz contém `item-service` e se os comandos funcionam no monorepo. Workflow escrito, mas sem execução bem-sucedida, não prova CI funcional.

## 12. CD — entrega ou implantação contínua (se exigida e implementada)

**O que é:** automatizar a entrega de um artefato ou sua implantação depois da validação. **CI não é automaticamente CD.**

**Onde:** `.github/workflows/`, Dockerfile, registro de imagens ou ambiente de deploy.

```powershell
Select-String -Path .\.github\workflows\*.yml -Pattern 'docker/build-push-action|docker push|packages: write|ghcr.io|deploy|release|upload-artifact' -ErrorAction SilentlyContinue
```

**Evidência possível:** job que publica imagem em registro, artefato anexado a uma execução ou deploy registrado. **Se não houver publicação/deploy configurado, marque como pendente; não diga que o `mvn package` sozinho implementa CD.**

## 13. Dependabot — atualização de dependências

**O que é:** criar Pull Requests de atualização de dependências automaticamente.

**Onde:** `.github/dependabot.yml`, preferencialmente na branch padrão do repositório.

```powershell
Test-Path .\.github\dependabot.yml
Get-Content .\.github\dependabot.yml
git remote show origin
```

**O que verificar no YAML:** `package-ecosystem: maven`, `directory` apontando para `/services/item-service` (ou configuração equivalente), agendamento `schedule` e outras entradas relevantes. **No GitHub:** aba **Pull requests** e, quando disponível, **Insights → Dependency graph → Dependabot**.

**Evidência:** arquivo na branch padrão e PR de atualização gerado, se houver. O arquivo existente em `develop` não garante que o Dependabot esteja ativo na branch padrão `main`.

## 14. Swagger / OpenAPI — documentação da API

**O que é:** descrever e experimentar endpoints pela interface da API, se a dependência e a configuração estiverem presentes.

**Onde:** `pom.xml`, classes de configuração e controllers.

```powershell
Select-String -Path "$ITEM\pom.xml" -Pattern 'springdoc|swagger|openapi'
Get-ChildItem "$ITEM\src\main\java" -Recurse -Filter '*.java' | Select-String -Pattern '@Operation|@Tag|@ApiResponse|@OpenAPIDefinition'
```

Se estiver configurado e o serviço estiver em execução, teste no navegador:

- `http://localhost:8081/swagger-ui/index.html`
- `http://localhost:8081/v3/api-docs`

**Evidência:** interface Swagger mostrando os endpoints e/ou JSON OpenAPI. **Se der 404, não trate a documentação como concluída**: confira dependências, versão compatível e configurações de acesso.

## 15. Segurança e arquivos sensíveis

**O que é:** impedir que segredos sejam publicados no Git e verificar como a aplicação recebe suas configurações.

```powershell
Get-Content .\.gitignore
git ls-files '*\.env' '*application*.properties' '*application*.yml'
git status --short
```

**Evidência:** `.env` ignorado quando aplicável e ausência de credenciais nos arquivos versionados. **Não execute comandos que imprimam tokens ou senhas na apresentação.**

## 16. Matriz de comprovação para o Trabalho 2

Preencha a coluna **Situação** após executar cada verificação. Os estados abaixo são **a confirmar**, não notas de avaliação do trabalho.

| Tema | Onde está | Comando ou prova | Situação |
|---|---|---|---|
| Monorepo | `services/` | `Get-ChildItem .\services` | A confirmar |
| Git e branches | repositório / GitHub | `git branch -a`, `git log` | A confirmar |
| SemVer | `item-service/pom.xml` | `mvn help:evaluate` com `-f` | 1.0.0 configurado anteriormente; reconfirmar |
| Build Maven | `pom.xml`, `target/` | `mvn -f "$ITEM\pom.xml" clean verify` | A confirmar |
| Testes | `src/test/` | `mvn -f "$ITEM\pom.xml" test` | Testes existentes; reconfirmar |
| Cobertura | JaCoCo/relatório | `target/site/jacoco/index.html` | A confirmar |
| Docker | Dockerfile / imagens | `docker images`, `docker ps` | A confirmar |
| Compose | arquivo Compose | `docker compose ps` | A confirmar |
| API | controllers | `Invoke-RestMethod` | A confirmar |
| Gateway | `services/api-gateway` | configuração + chamada | A confirmar |
| CI | `.github/workflows/ci.yml` | GitHub Actions / logs | A confirmar |
| CD | workflows / registro | artefato, imagem ou deploy | A confirmar |
| Dependabot | `.github/dependabot.yml` | branch padrão + PR | A confirmar |
| Swagger | `pom.xml` / Swagger UI | `/swagger-ui/index.html` | A confirmar |

> **Importante:** esta tabela organiza as práticas discutidas no desenvolvimento do Trabalho 2. Sem a especificação original aberta nesta conversa, não é possível afirmar que cada item seja um requisito obrigatório nem garantir que a lista esteja completa.

## 17. Roteiro de apresentação (5–8 minutos)

1. **Estrutura (30 s):** mostre `services/item-service` dentro do monorepo e explique o papel do serviço.
2. **Git + SemVer (1 min):** `git branch -a`, histórico da feature e versão efetiva `1.0.0`.
3. **Maven + testes (1–2 min):** execute `mvn -f "$ITEM\pom.xml" clean verify` e mostre os relatórios. Se houver JaCoCo, mostre o percentual.
4. **Docker + Compose (1 min):** `docker compose ps` e logs do serviço.
5. **API (1 min):** `Invoke-RestMethod http://localhost:8081/itens` e, se possível, uma atualização de status válida.
6. **CI + Dependabot (1–2 min):** abra `.github/workflows/ci.yml`, execução no Actions e `.github/dependabot.yml` na branch padrão.
7. **Documentação e conclusão (30 s):** mostre Swagger, se estiver ativo, e diferencie funcionalidades comprovadas de pendências.

### Comandos de encerramento

```powershell
git status
docker compose ps
# Opcional, para parar o ambiente sem remover volumes:
docker compose down
```

**Dica:** prepare uma captura de tela do último workflow bem-sucedido e do relatório de testes. Assim, se a internet ou o Docker falharem na apresentação, você ainda consegue explicar o resultado observado anteriormente.

---

**Escopo deste documento:** `item-service` como evidência prática de DevOps no Trabalho 2, sem afirmar que recursos ainda não verificados estejam implementados. Atualize a matriz após testar no repositório real.

