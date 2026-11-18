# Validação 2.5 — 18/11/2026

| Ambiente | Resultado dos cenários principais | Integração opcional |
| --- | --- | --- |
| Forge 1.20.1 / 47.4.10 | 121 verificações aprovadas | Mekanism 10.4.16.80, Curios e mods de desenvolvimento carregados |
| NeoForge 1.21.1 / 21.1.248 | 121 verificações aprovadas | Mekanism 10.7.19 carregado |
| NeoForge 26.1.2 / 26.1.2.98 | 118 verificações aprovadas | Sem Mekanism; carregamento e ausência da capability de gás verificados |

Além desses cenários, os adaptadores de cada versão verificaram as capabilities
reais dos blocos e da Battery, filtros de item/fluido, endereçamento de slots,
simulações e commit/rollback de transações. Na 26.1.2 também foi verificado o
commit/rollback de fluido dentro de um item por `ItemAccess`.

Os cenários principais cobrem limites e clamps no servidor, curvas de consumo,
capacidades iniciais e expansão por bateria, limites reais de stack, preservação
dos saldos por ID, serialização por Minecraft, abastecimento do item pessoal
com balde na outra mão, ativação bem-sucedida, falha por saldo insuficiente,
alvo incompatível sem cobrança, XP em pontos, quebra por durabilidade, recarga
de tempo no inventário, limite opcional de tempo e cobrança conjunta da rede.
Também verificam a invalidação do segundo pago ao editar um vínculo e o
isolamento de recursos pertencentes a outro jogador.

Os testes rodaram em `.timelesss-2.5-tests` dentro das três instâncias indicadas,
com mundos separados, `--nogui` e volume mestre zero. Nenhum cliente foi aberto
e não houve controle da tela. A aparência das telas no cliente não foi validada
visualmente nesta execução.

Os três `build` de lançamento finalizaram com sucesso, incluindo `reobfJar`
no Forge. Os JARs foram inspecionados: versão 2.5, classes comuns presentes,
executor de testes ausente e dependências obrigatórias apenas do loader e do
Minecraft. Os JARs foram copiados para `builds` e para as três pastas `mods`, com
conferência SHA-256. As versões anteriores foram preservadas com o sufixo
`.disabled-before-2.5`.

Logs locais: `.test20.log`, `.test21.log`, `.test26.log` e `.release20.log`,
`.release21.log`, `.release26.log` (ignorados pelo Git).
