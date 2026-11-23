# Configuração da versão 2.5

O servidor define as regras e o loader sincroniza os valores com os clientes.
O arquivo é criado ao abrir um mundo/servidor pela primeira vez:

- Forge 1.20.1: `<mundo>/serverconfig/timelesss-server.toml`.
- NeoForge 1.21.1 e 26.1.2: `config/timelesss-server.toml` na instância.

Feche o mundo antes de alterar tipos, IDs, capacidades e bases de consumo.
Os limites de multiplicador e a curva também são usados na interface e
validados novamente no servidor. Os máximos configuráveis aceitam de 1 a 10.000.

## Limites e recurso da rede

```toml
[limits]
personalMax = 25
distributorMax = 50

[network]
resource = ["ENERGY"] # aceita uma string ou lista: ENERGY, FLUID, GAS, ITEM
fluid = ["minecraft:lava"]
gas = ["mekanism:hydrogen"]
item = ["minecraft:coal"]
```

Vários tipos são **alternativas**, em ordem de prioridade. Por exemplo,
`resource = ["ENERGY", "GAS", "ITEM"]` tenta energia, depois gás e depois item.
A primeira combinação tipo/material capaz de pagar um segundo inteiro é usada.
Não cobra todos os tipos juntos e não mistura saldos de materiais diferentes
para completar uma cobrança. O saldo de vários Hubs/Distributors do mesmo
material continua sendo somado.

Cada material aceita um modificador que multiplica a base de consumo:
`1` mantém a base, `0.25` usa um quarto e `2` usa o dobro. Por exemplo, a base
100 mB com `mekanism:ethene = 0.25` custa 25 mB em 1x. O modificador deve ser
positivo, finito e ter até 12 casas decimais. IDs repetidos são rejeitados.

Estes formatos são aceitos para fluido, gás e item:

```toml
# Um ID usa o consumo padrão (modificador 1).
gas = "mekanism:hydrogen"

# Lista fácil de editar pelo Configured, uma linha por material.
gas = ["mekanism:hydrogen = 1", "mekanism:ethene = 0.25"]

# Lista associada por pares.
gas = [["mekanism:hydrogen", 1], ["mekanism:ethene", 0.25]]

# Lista alternando ID e modificador.
gas = ["mekanism:hydrogen", 1, "mekanism:ethene", 0.25]

# Tabela associada.
gas = { "mekanism:hydrogen" = 1, "mekanism:ethene" = 0.25 }
```

Use apenas uma atribuição por campo; os exemplos acima mostram alternativas.
Campo vazio mantém os padrões anteriores: lava, hidrogênio ou carvão.
Os campos aceitam IDs de qualquer mod instalado. `GAS` usa a capability
opcional de gás do Mekanism na 1.20.1 e de químicos na 1.21.1. Sem API
compatível, as demais alternativas continuam disponíveis.

Fluidos e gases têm capacidades independentes. Os materiais de cada tipo
compartilham sua capacidade total; configurar dois fluidos não duplica o tanque.
Itens diferentes ocupam slots diferentes e respeitam seus próprios limites de
stack. O saldo de cada material permanece identificado por tipo e ID, inclusive
no salvamento do mundo e das Batteries.

Consumos fracionários são preservados como crédito pré-pago, sem arredondar
cada cobrança para uma unidade inteira perdida. Em itens com modificador
`0.25`, a primeira cobrança desconta um item e deixa crédito para outras três.
O crédito da rede é salvo por jogador e material; o do item pessoal fica no item.

## Edição pelo Configured

1. Abra as configurações de **Just Gash Timelesss** no Configured.
2. Entre em **Recursos da rede** e edite **Recursos aceitos (ordem de prioridade)**.
   Adicione `ENERGY`, `FLUID`, `GAS` e/ou `ITEM`, uma opção por linha.
3. Em **Fluidos**, **Gases/químicos** ou **Itens e modificadores**, use uma linha
   por material: `mekanism:hydrogen` ou `mekanism:ethene = 0.25`.
4. Ajuste as bases em **Consumo** e as capacidades em **Armazenamento**, salve
   e reabra o mundo para aplicar as opções que exigem reinício de mundo.

Os formatos antigos e as listas associadas são normalizados, ao carregar,
para listas simples de texto que o editor do Configured pode adicionar,
remover e alterar. Campos e categorias possuem nomes e instruções em PT-BR
com exemplos e unidades. Na 1.20.1, as configurações de servidor pertencem ao
mundo; abra o mundo desejado para editá-las. Configured continua opcional.

A tela F12 permite alternar entre os materiais com as setas do cabeçalho,
mostrando o saldo e o consumo com o modificador correspondente.

## Consumo

```toml
[consumption]
curve = "DEFAULT" # DEFAULT = base * nível²; LINEAR = base * nível
energyBase = 100  # FE
fluidBase = 100   # mB
gasBase = 100     # mB
itemBase = 1      # unidade do item
```

Na rede, a base é o consumo **por segundo em 1x**, somado para todos os vínculos.
A rede escolhe uma alternativa e paga um segundo inteiro antes de acelerar.
Se nenhuma alternativa tiver saldo suficiente, nenhum recurso é descontado e
a aceleração aguarda a próxima cobrança.
Alterar um vínculo invalida o segundo já pago. `0x` desativa o vínculo.

No item pessoal, a base é cobrada por ativação. Alvos sem aceleração compatível
não gastam recurso. Reabastecer trocas de villagers também é uma ativação.
As cobranças se aplicam inclusive no criativo.

XP e tempo acumulado têm unidades próprias: XP é por nível e tempo é por tick
executado, independentemente da curva quadrática/linear dos outros recursos.
Por exemplo, em 3x, a rede gasta 900 FE/s no padrão ou 300 FE/s no linear;
o item pessoal gasta 6 pontos de XP ou 60 ticks acumulados por ativação.

## Armazenamento e baterias

```toml
[storage]
hubTank = 16000
batteryTank = 1000
distributorTank = 1000
hubSlots = 16
batterySlots = 2
distributorSlots = 2
```

As capacidades de fluido/gás usam mB: **1.000 mB = 1 B**. Slots respeitam o
limite real de empilhamento do item configurado. A configuração de slots aceita
até 32.768 slots; esse também é o teto de slots do Hub expandido.

| Recurso | Hub inicial | Cada bateria extra | Distributor |
| --- | ---: | ---: | ---: |
| Energia | 40.000 FE | +10.000 FE | 100.000 FE |
| Fluido/gás | 16 B | +1 B | 1 B |
| Item | 16 stacks | +2 stacks | 2 stacks |

As quatro baterias da receita fazem parte da capacidade inicial do Hub.
Uma bateria adicionada depois aumenta a capacidade conforme a tabela.
Na versão 2.5.1, cada pilha comporta 64 baterias. O Hub consome uma bateria
ao instalar a ampliação de capacidade. As chaves de configuração
`batteryTank` e `batterySlots` definem quanto cada bateria extra acrescenta ao Hub.

FE, fluidos, itens e gás/químicos são expostos pelas capabilities do loader e
do Mekanism, permitindo abastecimento por máquinas, tubos e cabos. As capabilities de todos os
tipos selecionados ficam ativas simultaneamente. Os slots aceitam o ID configurado
com seus componentes padrão, preservando o significado do combustível.

Também é possível clicar no Hub/Distributor com o item combustível ou com um
recipiente de fluido. A tela F12 mostra o saldo e o consumo na unidade ativa.
A propriedade do bloco é respeitada nas interações manuais.

## Item pessoal

```toml
[personal]
resource = "NONE" # NONE, FLUID, DURABILITY, XP, ENERGY ou TIME
durabilityLimit = 2048
durabilityBase = 1
fluidLimit = 16000
energyLimit = 100000
xpPerLevel = 2
timePerTick = 1
timeChargePerTick = 1
timeLimit = 0 # 0 = sem limite de ticks acumulados
```

- `NONE`: uso gratuito, preservando o comportamento anterior.
- `FLUID`: usa `network.fluid`, lava quando vazio; tanque padrão de 16 B.
- `DURABILITY`: consome a base conforme a curva; ao esgotar a durabilidade,
  o item quebra. A barra exibe o saldo restante.
- `XP`: desconta pontos de XP do jogador, sem reservatório interno ou limite
  adicional. Campo omitido usa 2 pontos por nível de aceleração.
- `ENERGY`: usa a mesma base e curva do Distributor, com 100.000 FE de capacidade.
- `TIME`: ganha 1 tick por tick de servidor enquanto estiver no inventário ou
  no Curios e gasta 1 tick por tick acelerado. Não recarrega fora do jogador ou
  offline. Não tem limite por padrão.

Para abastecer o item pessoal com fluido, segure o Timelesss em uma mão e
o recipiente na outra e clique direito no ar, sem Shift. Shift + clique
direito abre o multiplicador.
O item pessoal aceita FE/fluido diretamente pelas capabilities quando configurado
para esses consumos.

## Verificação

`tools/Test-Timelesss.ps1` executa os testes em servidores sem interface, dentro de
`.timelesss-2.5.1-tests` nas instâncias de teste. Esses mundos são separados dos
saves existentes e têm volume mestre zero. Os testes incluem capacidades,
expansão, filtros de combustível, simulação/transações, persistência por NBT,
consumo e falta de saldo, recarga, limites, XP, durabilidade e pagamento da rede.
Os fontes de teste só entram quando `-PtimelesssSelfTest` é passado ao Gradle;
os JARs de lançamento não contêm o executor de testes.
