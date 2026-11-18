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
resource = "ENERGY" # ENERGY, FLUID, GAS ou ITEM
fluid = ""         # vazio = minecraft:lava
gas = ""           # vazio = mekanism:hydrogen
item = ""          # vazio = minecraft:coal
```

Os campos de ID aceitam recursos registrados por qualquer mod instalado.
`GAS` usa a capability opcional de gás do Mekanism na 1.20.1 e de químicos
na 1.21.1. Sem uma API compatível instalada, a capability de gás fica
indisponível; o mod continua carregando. As demais integrações também são
opcionais. Não há limite máximo de versão para mods ou loaders nas dependências;
o Minecraft é restrito à versão correspondente de cada JAR.

Cada saldo é associado ao tipo e ao ID configurados. Trocar de lava para água,
por exemplo, não converte a lava que estava guardada. O saldo antigo continua
salvo para quando a configuração original voltar. Reduzir uma capacidade limita
o saldo utilizável; evite reduzir limites com os reservatórios cheios.

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
A rede paga um segundo inteiro antes de acelerar. Se o conjunto não tiver saldo
suficiente, nenhum recurso é descontado e a aceleração aguarda a próxima cobrança.
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
A Battery agora também guarda o recurso ativo; por isso ocupa uma unidade por
stack. Instalar uma Battery no Hub transfere o saldo dela e consome a bateria.
No Distributor, clicar com uma Battery apenas transfere seu saldo, preservando-a.

FE, fluidos, itens e gás/químicos são expostos pelas capabilities do loader e
do Mekanism, permitindo abastecimento por máquinas, tubos e cabos. Somente a
capability do recurso selecionado fica ativa. Os slots aceitam o ID configurado
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

Para abastecer manualmente, segure o item pessoal/Battery em uma mão e um
recipiente de fluido ou combustível na outra e clique direito no ar, sem Shift.
Uma Battery também pode transferir energia/fluido ao item pessoal se ambos
usarem o mesmo recurso. Shift + clique direito continua abrindo o multiplicador.
O item pessoal aceita FE/fluido diretamente pelas capabilities quando configurado
para esses consumos.

## Verificação

`tools/Test-2.5.ps1` executa os testes em servidores sem interface, dentro de
`.timelesss-2.5-tests` nas instâncias de teste. Esses mundos são separados dos
saves existentes e têm volume mestre zero. Os testes incluem capacidades,
expansão, filtros de combustível, simulação/transações, persistência por NBT,
consumo e falta de saldo, recarga, limites, XP, durabilidade e pagamento da rede.
Os fontes de teste só entram quando `-PtimelesssSelfTest` é passado ao Gradle;
os JARs de lançamento não contêm o executor de testes.
