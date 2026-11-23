# Just Gash Timelesss

Mod de Gasharaujo, versão **2.5.1**, para quatro versões do Minecraft:

- `neoforge-26.3`: Minecraft 26.3 + NeoForge
- `neoforge-26.1.2`: Minecraft 26.1.2 + NeoForge
- `neoforge-1.21.1`: Minecraft 1.21.1 + NeoForge
- `forge-1.20.1`: Minecraft 1.20.1 + Forge

O item `timelesss:timelesss` abre a tela de seleção de multiplicador com
Shift + clique direito no ar. O valor inteiro de 1x até o máximo configurado (25x por padrão) é salvo no próprio
item e sincronizado com o servidor.

O mod permite configurar os máximos, o combustível da rede (FE, fluido,
gás do Mekanism ou item), capacidades e consumo pessoal (nenhum, fluido,
durabilidade, XP, FE ou tempo acumulado), além das curvas padrão e linear. A rede aceita múltiplos recursos alternativos
e múltiplos materiais com modificadores de consumo; as listas possuem nomes
e ajuda em PT-BR para edição pelo Configured.
Veja os padrões, exemplos e instruções em [Configuração 2.5](docs/CONFIG-2.5.md).

## Uso

- Clique direito em um bloco ou entidade para executar `20 * multiplicador` ticks.
- Villagers e Traders do Easy Villagers têm as trocas reabastecidas em vez de
  serem acelerados.
- Uma block entity sem ticker compatível responde `Deu ruim` no chat.
- Com Curios instalado, o item pode ser equipado no slot `Timelesss`.
- `NumPad 5` abre a tela do item e `NumPad Enter` o aciona no alvo sob a mira.
  Ambos são remapeáveis em Controles; a tecla de ativação também funciona com
  uma GUI aberta.

Os atalhos localizam o item nesta ordem: Curio, mão principal, mão secundária,
hotbar e restante do inventário.

## Rede temporal

- `timelesss:time_distributor` armazena 100.000 FE e pertence ao jogador que o
  colocou. Colocações por fake player quebram e derrubam o bloco imediatamente.
- `timelesss:time_wrench` armazena dimensão e coordenadas com Shift + clique
  direito. Clique normal ou Shift + clique em um distribuidor próprio adiciona
  o alvo armazenado à rede.
- `F12` abre os Vínculos Temporais. Cada alvo aceita de 0x até o máximo configurado (50x por padrão); 1x acrescenta
  1 tick extra em cada tick do servidor (20 por segundo), enquanto 0x o
  desativa. A tela mostra o gasto atual e permite selecionar e excluir vínculos.
- Por padrão, o custo é `100 * x² FE/s`. O servidor verifica e desconta a energia da rede
  inteira uma vez por segundo; se faltar energia para o conjunto, nada é
  acelerado durante esse segundo.
- O processamento ocorre no evento de tick do `ServerLevel` e somente para
  jogadores online.

## Bateria da rede

- `timelesss:battery` é um componente de craft feito com ferro e um bloco de
  redstone. Cada pilha comporta até 64 baterias. As baterias antigas perdem
  a carga que guardavam como item e podem voltar a empilhar.
- `timelesss:battery_hub` pertence ao jogador que o colocou e estende a energia
  disponível para o Time Binds. As quatro Batteries da receita fornecem a
  capacidade inicial de 40.000 FE.
- Clique no Battery Hub segurando uma Battery para consumi-la e acrescentar
  10.000 FE de capacidade. O bloco aceita o recurso configurado pelas capabilities do loader/Mekanism e sua
  textura acompanha as faixas de carga vazia, 50%, 75% e cheia.

## Conquistas

- **Sem tempo irmão** — fabrique um Timelesss.
- **Usando energia** — abasteça sua rede temporal com FE.
- **Mestre do tempo** — fabrique um Time Distributor.
- **Gerenciando o tempo** — acelere um alvo usando, em um segundo, toda a
  capacidade ativa de um recurso da sua rede.
- **Aceleração em massa** — instale 640 baterias extras nos hubs ativos da sua rede.
  As quatro baterias de cada receita de Hub ficam fora dessa contagem.

Receita do distribuidor: netherita, estrela do Nether, blocos de redstone,
Timelesss e cabeça de dragão. A haste da Time Wrench usa papel, Timelesss e um
lingote de netherita.

As integrações são opcionais. Os JARs exigem apenas o Minecraft e o loader
da versão correspondente.

Execute `criar-jars.bat` para compilar os quatro JARs. A pasta `builds/2.5.1`
contém um `timelesss-2.5.1.jar` por versão do Minecraft. O ZIP agrupa os
quatro JARs com a mesma estrutura de pastas.

Os testes sem interface podem ser repetidos com `tools/Test-Timelesss.ps1`.
