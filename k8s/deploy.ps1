[CmdletBinding()]
param([string]$ClusterProfile='achados-tp3',[string]$Minikube='minikube',[switch]$SkipBuild,[string]$BaseImage)
$ErrorActionPreference='Stop'
$ns='achados-perdidos'
$services='auth-service','item-service','ocorrencia-service','api-gateway'
$repo=Split-Path -Parent $PSScriptRoot
function Run($program,[string[]]$arguments) {
    & $program @arguments
    if ($LASTEXITCODE -ne 0) { throw "$program $($arguments -join ' ') falhou com codigo $LASTEXITCODE" }
}
function K { Run kubectl (@('--context',$ClusterProfile)+$args) }
function App { K -n $ns @args }

Run docker @('info','--format','{{.OSType}}')
& $Minikube status -p $ClusterProfile -o json 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    if ($IsWindows -and (Get-PSDrive C).Free -lt 5GB) { throw 'Reserve pelo menos 5 GiB livres no disco C:.' }
    $startArgs=@('start','-p',$ClusterProfile,'--driver=docker','--memory=3400','--cpus=2','--kubernetes-version=v1.34.0','--keep-context')
    if ($BaseImage) { $startArgs += @("--base-image=$BaseImage",'--preload=false','--cache-images=false') }
    Run $Minikube $startArgs
}
K apply -f (Join-Path $PSScriptRoot 'namespace.yaml')
if (!(App get secret app-secrets --ignore-not-found -o name)) {
    $values=@{}
    $rng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        foreach ($key in 'POSTGRES_PASSWORD','RABBITMQ_PASSWORD','JWT_SECRET','ADMIN_PASSWORD','ADMIN_KEY','GRAFANA_PASSWORD') {
            $bytes=New-Object byte[] 32
            $rng.GetBytes($bytes)
            $values[$key]='Aa1!'+[BitConverter]::ToString($bytes).Replace('-','')
        }
    } finally {
        $rng.Dispose()
    }
    @{
        apiVersion='v1'
        kind='Secret'
        metadata=@{name='app-secrets';namespace=$ns}
        type='Opaque'
        stringData=$values
    } | ConvertTo-Json -Depth 4 -Compress | kubectl --context $ClusterProfile create -f -
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar as credenciais do cluster.' }
}
App apply -f (Join-Path $PSScriptRoot 'config/configmap.yaml')
foreach ($dependency in 'postgres','rabbitmq') {
    App apply -f (Join-Path $PSScriptRoot $dependency)
}
foreach ($service in $services) {
    if ($IsWindows -and (Get-PSDrive C).Free -lt 3GB) { throw 'O disco C: ficou com menos de 3 GiB livres.' }
    $image=$service+':tp3'
    if (!$SkipBuild) { Run docker @('build','-t',$image,(Join-Path $repo "services/$service")) }
    Run $Minikube @('image','load','--daemon','-p',$ClusterProfile,'--overwrite=true',$image)
}
foreach ($dependency in 'postgres','rabbitmq') {
    App rollout status "deployment/$dependency" --timeout=420s
}
App exec deployment/postgres '--' psql -U postgres -d itens_db -v ON_ERROR_STOP=1 -c 'CREATE SCHEMA IF NOT EXISTS auth; CREATE SCHEMA IF NOT EXISTS itens; CREATE SCHEMA IF NOT EXISTS ocorrencias; CREATE SCHEMA IF NOT EXISTS gateway;'

# Inicialize uma replica por vez na primeira implantacao para evitar DDL e seeders concorrentes.
foreach ($service in $services) {
    if (!(App get "deployment/$service" --ignore-not-found -o name)) {
        $path=Join-Path $PSScriptRoot "$service/deployment.yaml"
        (Get-Content -LiteralPath $path -Raw) -replace '(?m)^  replicas: 2\r?$','  replicas: 1' |
            kubectl --context $ClusterProfile apply -f -
        if ($LASTEXITCODE -ne 0) { throw "Falha ao inicializar $service." }
        App rollout status "deployment/$service" --timeout=420s
    } else {
        App rollout status "deployment/$service" --timeout=420s
    }
}
K apply -k $PSScriptRoot
if (!$SkipBuild) {
    foreach ($service in $services) { App rollout restart "deployment/$service" }
}
foreach ($service in ($services + @('prometheus','grafana','traefik'))) {
    App rollout status "deployment/$service" --timeout=420s
}
App get 'deployments,pods,services,ingress'
if ($IsWindows) { Get-PSDrive C | Select-Object Name,Free }
