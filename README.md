# Just Gash Timelesss

Mod de Gasharaujo para três versões do Minecraft:

- `neoforge-26.1.2`: Minecraft 26.1.2 + NeoForge
- `neoforge-1.21.1`: Minecraft 1.21.1 + NeoForge
- `forge-1.20.1`: Minecraft 1.20.1 + Forge

O item `timelesss:timelesss` abre a tela de seleção de multiplicador com
Shift + clique direito no ar. O valor inteiro de 1x a 25x é salvo no próprio
item e sincronizado com o servidor.

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
- `F12` abre os Vínculos Temporais. Cada alvo aceita de 0x a 50x; 1x acrescenta
  1 tick extra em cada tick do servidor (20 por segundo), enquanto 0x o
  desativa. A tela mostra o gasto atual e permite selecionar e excluir vínculos.
- O custo é `100 * x² FE/s`. O servidor verifica e desconta a energia da rede
  inteira uma vez por segundo; se faltar energia para o conjunto, nada é
  acelerado durante esse segundo.
- O processamento ocorre no evento de tick do `ServerLevel` e somente para
  jogadores online.

## Bateria da rede

- `timelesss:battery` é um componente de craft feito com ferro e um bloco de
  redstone.
- `timelesss:battery_hub` pertence ao jogador que o colocou e estende a energia
  disponível para o Time Binds. As quatro Batteries da receita fornecem a
  capacidade inicial de 40.000 FE.
- Clique no Battery Hub segurando uma Battery para consumi-la e acrescentar
  10.000 FE de capacidade. O bloco aceita FE pelas capabilities do loader e sua
  textura acompanha as faixas de carga vazia, 50%, 75% e cheia.

Receita do distribuidor: netherita, estrela do Nether, blocos de redstone,
Timelesss e cabeça de dragão. A haste da Time Wrench usa papel, Timelesss e um
lingote de netherita.

Os ambientes de teste incluem Jade, JEI, AE2, AdvancedAE, ExtendedAE, Easy
Villagers, Powah, Curios e suas dependências obrigatórias. No Forge 1.20.1 os JARs de
teste ficam em `test-mods`, pois o ForgeGradle precisa deobfuscá-los antes de
iniciar o cliente.

Use os arquivos `.bat` da raiz para abrir cada ambiente de desenvolvimento ou
para gerar os três JARs. Os artefatos finais são copiados para `builds`.
