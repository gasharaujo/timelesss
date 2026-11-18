# Validação 2.5 — 18/11/2026

| Ambiente | Cenários anteriores | Novos cenários de múltiplos recursos | Integração opcional |
| --- | --- | --- | --- |
| Forge 1.20.1 / 47.4.10 | 121 aprovados | 82 aprovados | Mekanism 10.4.16.80 e Configured 2.2.3 |
| NeoForge 1.21.1 / 21.1.248 | 121 aprovados | 82 aprovados | Mekanism 10.7.19 e Configured 2.6.3 |
| NeoForge 26.1.2 / 26.1.2.98 | 118 aprovados | 79 aprovados | Configured 2.7.5, sem Mekanism |

Total: **603 verificações principais aprovadas**, além dos testes dos adaptadores
e dos modelos de edição do Configured. Estes modelos foram instanciados com as
classes do próprio Configured, validando conversão de listas, nomes traduzíveis,
validação dos valores e edição sem perder o formato das linhas.

Os novos cenários cobrem recursos alternativos em prioridade, vários materiais
em tanques/slots compartilhados, IDs separados, conservação dos saldos da 2.5
anterior, listas de pares e tabelas associadas validadas pelo loader, modificadores
fracionários com crédito persistente, rollback de cobrança parcial, transferência
de vários recursos por Battery, consumo pessoal com outro fluido, múltiplos
tanques do Mekanism e os dados de cada alternativa enviados à tela F12.

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
`.disabled-before-2.5` e `.disabled-before-multiple-resources`.

Logs da atualização: `.multi-test20.log`, `.multi-test21.log`, `.multi-test26.log`
e `.multi-release20.log`, `.multi-release21.log`, `.multi-release26.log`
(ignorados pelo Git).
