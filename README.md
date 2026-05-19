# MTG Event Manager

Aplicação desktop Java + Swing para gerir torneios de Magic: The Gathering com rondas suíças, standings, persistência JSON e Top Cut.

## Requisitos

- JDK 17 ou superior
- Maven 3.9 ou superior

## Executar

```bash
mvn clean compile exec:java
```

## Gerar jar

```bash
mvn clean package
```

## Funcionalidades

- Criação de eventos suíços ou suíços com Top Cut.
- Gestão de jogadores com validação de e-mail e bloqueio de duplicados.
- Pairings suíços com bye, prevenção de repetição de adversários e preferência por evitar equipas iguais.
- Inserção de resultados em jogos.
- Standings com OMW%, GW% e OGW%.
- Top Cut por seed suíço.
- Guardar e carregar eventos em JSON.
- Exportação CSV de jogadores, pairings, standings e resultados.
