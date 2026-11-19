# Arquitetura Baseada em Eventos para Resiliência Transacional de Microsserviços Assíncronos no Setor Financeiro

> **Projeto de TCC - MBA em Engenharia de Software (USP/Esalq)**  

---

## 1. Visão Geral da Arquitetura

Este repositório contém a implementação completa do protótipo laboratorial projetado para demonstrar e mensurar a **resiliência transacional**, **alta disponibilidade** e **consistência eventual estrita** em transferências financeiras assíncronas utilizando:

- **Java 21 (LTS)** e **Spring Boot 3.2.x**
- **Apache Kafka 3.7.0** em modo **KRaft** (Cluster autônomo sem ZooKeeper)
- **PostgreSQL 16 (Alpine)** com bancos de dados isolados por microsserviço (*Database-per-Service*)
- **Padrão Saga Orquestrada**: Coordenação centralizada do ciclo de vida transacional com compensações semânticas automáticas.
- **Padrão Transactional Outbox**: Garantia de entrega atômica entre alteração de estado no banco relacional e publicação de eventos no broker Kafka.
- **Mecanismos de Tolerância a Falhas**: Retentativas exponenciais (`@RetryableTopic`), roteamento para *Dead-Letter Topics* (DLT) e isolamento de falhas crônicas.
- **Motor de Engenharia do Caos (*Chaos Engine*)**: Injeção controlada e dinâmica de falhas e latências artificiais no serviço de crédito.
- **Framework de Benchmarking Empírico e Auditoria**: Script Python em 4 fases para cálculo automatizado de vazão (RPS), percentis de latência (p50, p90, p99), MTTR (Tempo Médio de Recuperação) e auditoria matemática da Invariante Financeira (`Saldo Origem + Saldo Destino == Saldo Inicial`).

---

## 2. Topologia de Microsserviços e Contêineres

```
                                      +------------------------------------+
                                      |          origination-db            |
                                      |        (PostgreSQL 5432)           |
                                      +-----------------+------------------+
                                                        |
                                                        v
+-----------------------+              +----------------+------------------+
|      Cliente HTTP     | -----------> |        origination-service        | (Porta 8081)
|  (Script Benchmark)   | <----------- |     (Orquestrador da Saga)        |
+-----------------------+              +--------+------------------+-------+
                                                |                  ^
                      (transfer-started)        |                  | (credit-failed /
                                                v                  |  transfer-credited)
                                       +--------+------------------+-------+
                                       |            Apache Kafka           | (Porta 9092)
                                       |             (KRaft)               |
                                       +--------+------------------+-------+
                                                |                  ^
                     (transfer-debited)         |                  | (transfer-debited)
                                                v                  |
   +------------------------------------+       |        +---------+--------------------------+
   |             debit-db               |       |        |                 credit-db          |
   |        (PostgreSQL 5433)           |       |        |             (PostgreSQL 5434)      |
   +-----------------+------------------+       |        +-----------------+------------------+
                     |                          |                          |
                     v                          v                          v
   +-----------------+------------------+                +-----------------+------------------+
   |          debit-service             |                |          credit-service            | (Porta 8083)
   |    (Débitos e Compensações)        | <------------- |     (Crédito & Motor de Caos)      |
   +------------------------------------+ (compensate)   +------------------------------------+
```

### Contêineres Orquestrados (`docker-compose.yml`)

| Contêiner | Tecnologia | Porta Host | Função Arquitetural |
| :--- | :--- | :--- | :--- |
| `kafka` | `apache/kafka:3.7.0` | `9092` / `9093` | Broker central em modo KRaft com criação dinâmica de tópicos. |
| `kafka-ui` | `provectuslabs/kafka-ui:latest` | `8080` | Console web de inspeção de tópicos, partições e mensagens DLT. |
| `origination-db` | `postgres:16-alpine` | `5432` | Banco relacional da Saga e Outbox do Orquestrador. |
| `debit-db` | `postgres:16-alpine` | `5433` | Banco de contas de origem e Outbox do serviço de débito. |
| `credit-db` | `postgres:16-alpine` | `5434` | Banco de contas de destino e Outbox do serviço de crédito. |
| `origination-service`| Java 21 / Spring Boot 3.2 | `8081` | Emissor de transferências e orquestrador de eventos de compensação. |
| `debit-service` | Java 21 / Spring Boot 3.2 | `8082` | Execução atômica de débitos e estornos compensatórios. |
| `credit-service` | Java 21 / Spring Boot 3.2 | `8083` | Execução de créditos com injeção de caos e roteamento para DLT. |
| `benchmark` *(profile)*| Python 3.11 / Alpine | - | Coleta automatizada de métricas e auditoria da invariante. |

---

## 3. Estratégia de Volumes e Persistência

1. **Acesso Local a Logs (`./<servico>/logs:/app/logs`)**:
   - Cada serviço gera logs estruturados via Logback gravados diretamente nas pastas locais do repositório no host.
2. **Cache Maven (`~/.m2:/root/.m2`)**:
   - Mapeamento do repositório local Maven para evitar downloads redundantes na compilação dos contêineres Docker.
3. **Scripts SQL de Inicialização (`./docker/postgres/init-*.sql`)**:
   - DDL e carga inicial de contas executados automaticamente na subida dos bancos PostgreSQL.
4. **Relatórios do Benchmark (`./scripts/output:/app/output`)**:
   - Os artefatos numéricos gerados pelo benchmark são salvos diretamente no diretório do desenvolvedor.
5. **Volumes Nomeados Docker**:
   - `origination_db_data`, `debit_db_data`, `credit_db_data` e `kafka_data`.

---

## 4. Como Executar a Infraestrutura

### Pré-requisitos
- **Docker Desktop** (com suporte a Docker Compose v2)
- **IntelliJ IDEA** (2023.3+ com plugin Java 21)

### Passo 1: Subir toda a infraestrutura conteinerizada
No terminal do projeto (ou no terminal integrado do IntelliJ IDEA):

```bash
docker compose up -d --build
```

Aguarde cerca de 30 a 60 segundos para que todos os bancos, o broker Kafka e os três microsserviços passem pelas checagens de *healthcheck*.

### Passo 2: Verificar o status dos serviços
```bash
docker compose ps
```

Todos os serviços deverão exibir o status `Up (healthy)`:
- `kafka` -> `http://localhost:9092`
- `kafka-ui` -> `http://localhost:8080`
- `origination-service` -> `http://localhost:8081/actuator/health`
- `debit-service` -> `http://localhost:8082/actuator/health`
- `credit-service` -> `http://localhost:8083/actuator/health`

---

## 5. Execução do Benchmark e Auditoria Financeira

O framework de benchmark executa as 4 fases de experimentação descritas na especificação da Fase 04:
1. **Fase 1 (Setup)**: Registra saldos iniciais (Origem: R$ 10.000,00, Destino: R$ 0,00) e fixa a Invariante Inicial (R$ 10.000,00).
2. **Fase 2 (Baseline)**: Dispara 50 transferências simultâneas sem falhas, calculando RPS e latências (p50, p90, p99).
3. **Fase 3 (Caos & Saga)**: Ativa o motor de caos no `credit-service`, dispara 50 transferências sob falha forçada, aciona retentativas no Kafka, DLT e estorno compensatório, medindo o MTTR.
4. **Fase 4 (Auditoria)**: Verifica nos bancos se `Saldo Origem + Saldo Destino == R$ 10.000,00` com tolerância zero para divergências.

### Opção A: Executar via Docker Compose (Sem necessidade de Python no host)
```bash
docker compose --profile benchmark run --rm benchmark
```

### Opção B: Executar via Python diretamente no Host
```bash
python scripts/benchmark.py
```

---

## 6. Artefatos de Saída Gerados para a Fase 04

Após a execução do benchmark, os seguintes arquivos são gerados em `./scripts/output/`:

1. [`raw_metrics.json`](file:///D:/event-driven-financial-resilience-tcc/scripts/output/raw_metrics.json):
   - Dados brutos completos de todas as 100 requisições, latências individuais em ms, timestamps e auditoria.
2. [`fase04_resultados_preliminares.csv`](file:///D:/event-driven-financial-resilience-tcc/scripts/output/fase04_resultados_preliminares.csv):
   - Tabela em formato delimitado por ponto e vírgula pronta para importação no Excel / Word.
3. [`tabela_comparativa.md`](file:///D:/event-driven-financial-resilience-tcc/scripts/output/tabela_comparativa.md):
   - Tabela comparativa formatada em Markdown pronta para inclusão direta no relatório da **Fase 04 (Resultados Preliminares)**.

---

## 7. Suíte de Testes Automatizados

Cada microsserviço conta com testes unitários cobrindo serviços e máquinas de estado, além de teste de integração end-to-end com Kafka embutido:

- `origination-service`:
  - `TransferSagaOrchestratorServiceTest`: Validação unitária das transições de estado da Saga e geração de eventos Outbox.
  - `TransferSagaIntegrationTest`: Validação de integração com `@SpringBootTest` e `@EmbeddedKafka`.
- `debit-service`:
  - `DebitAccountServiceTest`: Validação de débitos atômicos, validação de saldo e estornos compensatórios.
- `credit-service`:
  - `CreditAccountServiceTest`: Validação de créditos normais, disparo de exceção sob caos e geração de eventos de falha.
