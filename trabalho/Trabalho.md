# Simulador de Escalonamento — Resumo das Classes e Funcionalidades

Este documento resume **a função de cada classe** do simulador de escalonamento de tarefas em um sistema operacional multitarefa preemptivo, desenvolvido em **Java 25**.

---

## 📦 Estrutura Geral

```

so.sim/
core/        → motor da simulação
model/       → entidades (tarefas, estados, timeline)
schedulers/  → algoritmos de escalonamento
io/          → leitura de configurações
ui/          → interface gráfica
metrics/     → cálculo de estatísticas
export/      → exportação de imagens (PNG)
Main.java    → ponto de entrada

```

---

## 🧱 Pacote `model`

### `Task`
Representa os **dados fixos de uma tarefa**: identificador, cor, tempo de chegada, duração e prioridade.

### `TaskState`
Enumeração dos **estados possíveis de uma tarefa**: NEW, READY, RUNNING, BLOCKED, FINISHED.

### `TCB` (Task Control Block)
Mantém o **estado dinâmico de execução** da tarefa: tempo restante, tempo de espera, início, término e resposta.

### `Interval`
Guarda **um período de execução** de uma tarefa (início e fim) usado no gráfico de Gantt.

### `Timeline`
Registra e organiza os **intervalos de execução de todas as tarefas**, servindo de base para o gráfico final.

---

## ⚙️ Pacote `core`

### `Clock`
Controla o **tempo global da simulação**, permitindo avançar ticks e reiniciar o contador.

### `ReadyQueue`
Gerencia a **fila de tarefas prontas**, facilitando a seleção e remoção pelo escalonador.

### `CPU`
Representa a **unidade de processamento** que executa uma tarefa por vez e controla o quantum.

### `Dispatcher`
Define **regras ou custo de troca de contexto** (zero no Projeto A, mas expansível).

### `Simulator`
É o **núcleo da simulação**: orquestra admissão, escalonamento, execução, preempção e atualização do tempo e métricas.

---

## 🧭 Pacote `schedulers`

### `Scheduler` (interface)
Define a **estrutura dos algoritmos de escalonamento**, com métodos para selecionar a próxima tarefa e decidir sobre preempção.

### `ReadyView`
Oferece uma **visão somente-leitura** das tarefas prontas, usada pelos escalonadores.

### `FIFOScheduler`
Implementa o algoritmo **First In, First Out** — executa tarefas na ordem de chegada.

### `SRTFScheduler`
Implementa o algoritmo **Shortest Remaining Time First**, priorizando a tarefa com menor tempo restante.

### `PriorityPreemptiveScheduler`
Executa tarefas segundo a **maior prioridade** (menor valor numérico), permitindo preempção.

---

## 📊 Pacote `metrics`

### `Stats`
Calcula e armazena as **métricas da simulação**, como:
- Tempo de espera (Waiting Time)
- Tempo de resposta (Response Time)
- Tempo de turnaround
- Utilização da CPU
- Throughput

Inclui uma classe interna `Report` que consolida esses resultados.

---

## 🗂 Pacote `io`

### `Config`
Representa os **parâmetros de configuração** da simulação (algoritmo, quantum, tarefas).

### `ConfigLoader`
Lê e valida o **arquivo de configuração texto** contendo:
- Primeira linha: `algoritmo;quantum`
- Demais linhas: `id;cor;ingresso;duracao;prioridade`

---

## 🖥 Pacote `ui`

### `GanttPanel`
Desenha o **gráfico de Gantt**, exibindo visualmente os intervalos de execução de cada tarefa.

### `AppFrame`
Janela principal do programa: contém os **controles da simulação** (botões, combobox, painel gráfico e status).

---

## 🖼 Pacote `export`

### `ImageExporter`
Permite **salvar o painel de Gantt como imagem PNG**, sem dependências externas.

---

## 🚀 `Main.java`

Classe principal que **inicializa a aplicação**, cria o `AppFrame` e executa a simulação.

---

## 🔄 Ciclo da Simulação (Resumo)

1. Admitir novas tarefas que chegam no tick atual.
2. Encerrar tarefas finalizadas.
3. Verificar preempção (por quantum ou política).
4. Selecionar nova tarefa e despachar para a CPU.
5. Executar um tick.
6. Atualizar métricas e timeline.
7. Avançar o clock até todas terminarem.

---

## 📝 Exemplo de Configuração

```

FIFO;3
T1;#4F81BD;0;7;2;
T2;#C0504D;2;5;1;
T3;#9AC23A;4;3;3;

```