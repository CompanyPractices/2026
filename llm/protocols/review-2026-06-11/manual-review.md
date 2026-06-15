
## Terminal Simulator

1.1. ErrorResponse.java
дублирует common

1.2. StanGenerator.java
здесь нужен AtomicInteger, плюс те моменты, которые обсуждали на демо

1.3. TerminalSimulatorService.java
cards будет неправильно работать при нескольких параллельных вызовах run.
видимо придется сделать создание экземпляра сценария при каждом запуске run, и в этом
экземпляре хранить контекстные переменные сценария, в частности коллекцию cards.

1.4. TransactionFactory.java, DateTimeGenerator.java
вызов ThreadLocalRandom.current() лучше делать однократно.
что еще в данном случае можно использовать вместо ThreadLocalRandom?


## Merchant Acquirer

2.1. application.yaml
зачем там gateway.url, он же из docker-compose приходит?
если для отладки, то тогда зачем писать ${GATEWAY_URL:...} - вроде как Spring увидев `@Value("${gateway.url}")` сначала проверит ENV, а в application.yaml пойдет только если нет переменной окружения GATEWAY_URL. и тогда получается, что ссылаться на GATEWAY_URL в application.yaml нет смысла. или я неправ и оно не так работает?

## Card Management

3.1. CardServiceEventListenerImpl.java
полагаемся на то, что Spring автоматически пропустит сам CardServiceEventListenerImpl при заполнении  CardServiceEventListenerImpl.listeners.
с одной стороны - так делают. с другой стороны, неудачный @Lazy или что-то аналогичное и получим зацикливание вызовов CardServiceEventListenerImpl.onEvent.
я бы переделал, убрал бы у CardServiceEventListenerImpl интерфейс CardServiceEventListener.
