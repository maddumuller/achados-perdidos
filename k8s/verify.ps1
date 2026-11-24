[CmdletBinding()]
param([string]$ClusterProfile='achados-tp3')
$ErrorActionPreference='Stop'
$ns='achados-perdidos'
$services='auth-service','item-service','ocorrencia-service','api-gateway'
$forwards=@()
$kubectl=(Get-Command kubectl).Source

function K {
    & $kubectl --context $ClusterProfile -n $ns @args
    if ($LASTEXITCODE -ne 0) { throw "kubectl $($args -join ' ') falhou" }
}
function Forward($service,$ports) {
    $parameters=@{FilePath=$kubectl;ArgumentList=@('--context',$ClusterProfile,'-n',$ns,'port-forward',"svc/$service",$ports);PassThru=$true}
    if ($IsWindows) { $parameters.WindowStyle='Hidden' }
    Start-Process @parameters
}
function Wait-For($condition,$description) {
    $deadline=(Get-Date).AddSeconds(90)
    do {
        try { if (& $condition) { return } } catch {}
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Tempo excedido: $description"
}

try {
    $deployments=(K get deployments -o json | ConvertFrom-Json).items
    foreach ($service in $services) {
        $deployment=$deployments | Where-Object {$_.metadata.name -eq $service}
        if ($deployment.status.readyReplicas -lt 2) { throw "Menos de 2 replicas prontas: $service" }
    }
    $config=K get configmap app-config -o name
    $secret=K get secret app-secrets -o name
    "Configuracao presente: $config e $secret"
    K get 'pods,services,ingress'

    $forwards+=Forward traefik '18080:80'
    $forwards+=Forward prometheus '19090:9090'
    $forwards+=Forward grafana '13000:3000'
    $base='http://127.0.0.1:18080'
    $headers=@{Host='achados-perdidos.local'}
    Wait-For { (Invoke-RestMethod "$base/actuator/health/readiness" -Headers $headers -TimeoutSec 3).status -eq 'UP' } 'Ingress e gateway'

    $identity=@{email="tp3-$([guid]::NewGuid().ToString('N'))@example.test";senha="Tp3!$([guid]::NewGuid().ToString('N'))"}
    $registration=$identity.Clone()
    $registration.nome='Teste TP3'
    Invoke-RestMethod "$base/api/auth/register" -Headers $headers -Method Post -ContentType application/json -Body ($registration | ConvertTo-Json) | Out-Null
    $login=Invoke-RestMethod "$base/api/auth/login" -Headers $headers -Method Post -ContentType application/json -Body ($identity | ConvertTo-Json)
    $headers.Authorization="Bearer $($login.accessToken)"
    $me=Invoke-RestMethod "$base/api/auth/me" -Headers $headers
    if ($me.email -ne $identity.email) { throw 'O login nao persistiu o usuario.' }

    $item=Invoke-RestMethod "$base/api/itens" -Headers $headers -Method Post -ContentType application/json -Body '{"nome":"Item teste TP3","descricao":"Item para validar a integracao"}'
    $occurrence=Invoke-RestMethod "$base/api/ocorrencias" -Headers $headers -Method Post -ContentType application/json -Body (@{itemId=$item.id;justificativa='Validacao da comunicacao e da reserva no cluster.'} | ConvertTo-Json)
    Wait-For {
        (Invoke-RestMethod "$base/api/ocorrencias/$($occurrence.id)" -Headers $headers -TimeoutSec 3).status -eq 'EM_ANALISE'
    } 'Reserva via RabbitMQ'
    if ((Invoke-RestMethod "$base/api/itens/$($item.id)" -Headers $headers).status -ne 'RESERVADO') { throw 'Reserva ausente no item.' }
    Invoke-RestMethod "$base/api/ocorrencias/$($occurrence.id)/cancelar" -Headers $headers -Method Post -ContentType application/json -Body '{"motivo":"Encerramento do teste de integracao"}' | Out-Null
    Wait-For {
        (Invoke-RestMethod "$base/api/itens/$($item.id)" -Headers $headers -TimeoutSec 3).status -eq 'DISPONIVEL'
    } 'Liberacao do item'
    'Ingress, gateway, cadastro, login, item, ocorrencia, reserva e cancelamento OK'

    Wait-For {
        $targets=(Invoke-RestMethod 'http://127.0.0.1:19090/api/v1/targets' -TimeoutSec 3).data.activeTargets
        $up=@($targets | Where-Object {$_.health -eq 'up'} | Group-Object {$_.labels.job})
        @($up | Where-Object {$_.Name -in $services -and $_.Count -ge 2}).Count -eq $services.Count
    } 'Dois alvos ativos por microsservico no Prometheus'
    $metrics=(Invoke-RestMethod ('http://127.0.0.1:19090/api/v1/query?query='+[uri]::EscapeDataString('jvm_memory_used_bytes{area="heap"}'))).data.result
    if ($metrics.Count -lt 8) { throw 'Prometheus ainda nao tem metricas JVM dos oito pods.' }
    'Prometheus coleta metricas JVM dos microsservicos'

    Wait-For { (Invoke-RestMethod 'http://127.0.0.1:13000/api/health' -TimeoutSec 3).database -eq 'ok' } 'Grafana'
    $encoded=K get secret app-secrets -o 'jsonpath={.data.GRAFANA_PASSWORD}'
    $password=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encoded))
    $authorization=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("admin:$password"))
    $grafanaHeaders=@{Authorization="Basic $authorization"}
    $dashboard=Invoke-RestMethod 'http://127.0.0.1:13000/api/dashboards/uid/achados-tp3' -Headers $grafanaHeaders
    $datasource=Invoke-RestMethod 'http://127.0.0.1:13000/api/datasources/uid/achados-prometheus/health' -Headers $grafanaHeaders
    if (!$dashboard.dashboard.title -or $datasource.status -ne 'OK') { throw 'Falha no painel ou na fonte de dados do Grafana.' }
    "Grafana OK, dashboard $($dashboard.dashboard.title) e datasource validado"
} finally {
    foreach ($process in $forwards) {
        if (!$process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
    }
}
