## Архитектура

- Коннектор — это отдельный сервис/продукт, независимый от других систем.
    
- Имеет свою базу данных и отдельное REST API.
    
- Все взаимодействия построены через очереди (Kafka) — для масштабируемости, отказоустойчивости и асинхронной обработки.
    
- Выгрузка данных настраивается через API, позволяя указывать, куда именно отправлять результат (MidPoint, CMDB, др.).
    

---

## Основные функции

1. Опрос серверов (Linux и Windows):
    
    - Получение данных об учётных записях пользователей.
        
    - Сбор информации о ПК (состояние, характеристики, uptime и пр.).
        
2. Управление через API:
    
    - Создание и удаление учёток на серверах через команды API.
        
    - Команды обрабатываются асинхронно через Kafka.
        
3. Выгрузка данных:
    
    - Гибкий механизм: конфигурируемые DataSink-и (REST, Kafka, файлы и др.).
        
    - Управление конфигурацией выгрузки — через отдельное API.
        
    - Интеграции масштабируются независимо от основного цикла сбора.
        
4. Реконсиляция:
    
    - При повторном сборе данные **не удаляются**, а обновляются по ключам.
        
    - Вся логика insert/update реализуется через consumer по `raw-data`.
        

---

## Нефункциональные требования

- Минимальное потребление ресурсов, быстрая обработка.
    
- Горизонтально масштабируемые воркеры и очередь Kafka.
    
- Обработка ошибок с fallback и retry.
    
- YAML/JSON-конфиги, возможность обновления без перезапуска.
    
- Поддержка dev/prod конфигураций (в т.ч. через docker-compose).
    
### 📌 Название проекта

"Коннектор для сбора информации с серверов и выгрузки в внешние системы"

### 🎯 Цель проекта

Создание масштабируемого, отказоустойчивого и расширяемого сервиса (коннектора), который:

- Периодически (по расписанию) опрашивает серверы Linux и Windows;
    
- Собирает данные об учетных записях пользователей и состоянии машин (CPU, RAM, диск, uptime);
    
- Сохраняет эти данные в локальную БД;
    
- Позволяет управлять процессом через REST API (фильтрация, удаление, выгрузка и пр.).
    

--    |           \---connector
    |               |   ConnectorApplication.class
    |               |
    |               +---config
    |               |   |   RetryConfig.class
    |               |   |
    |               |   \---kafka
    |               |           KafkaConsumerConfig.class
    |               |           KafkaProducerConfig.class
    |               |
    |               +---connection
    |               |   |   Connection.class
    |               |   |   ConnectionFactory.class
    |               |   |
    |               |   \---ssh
    |               |           SshConnection.class
    |               |           SshConnectionFactory.class
    |               |
    |               +---connector
    |               |   |   ServerConnector.class
    |               |   |
    |               |   +---linux
    |               |   |       UbuntuConnector$1.class
    |               |   |       UbuntuConnector.class
    |               |   |
    |               |   \---windows
    |               |           WindowsConnector.class
    |               |
    |               +---controller
    |               |       AccountController.class
    |               |
    |               +---dto
    |               |       AccountDto$AccountDtoBuilder.class
    |               |       AccountDto.class
    |               |       FilterParams.class
    |               |       MachineDto.class
    |               |       ScanResultMessage.class
    |               |
    |               +---model
    |               |   |   Group$GroupBuilder.class
    |               |   |   Group.class
    |               |   |   MachineInfo.class
    |               |   |   Pair.class
    |               |   |   Server$ServerBuilder.class
    |               |   |   Server.class
    |               |   |   TaskLog$TaskLogBuilder.class
    |               |   |   TaskLog.class
    |               |   |
    |               |   +---account
    |               |   |       Account$AccountBuilder.class
    |               |   |       Account.class
    |               |   |       AccountBatch.class
    |               |   |
    |               |   \---enums
    |               |           OperatingSystemType.class
    |               |
    |               +---repository
    |               |       AccountRepository.class
    |               |       MachineInfoRepository.class
    |               |       ServerRepository.class
    |               |       TaskLogRepository.class
    |               |
    |               +---service
    |               |   |   AccountService$1.class
    |               |   |   AccountService$2.class
    |               |   |   AccountService$3.class
    |               |   |   AccountService$GroupRecord.class
    |               |   |   AccountService$Pair.class
    |               |   |   AccountService.class
    |               |   |   ServerScannerRegistry.class
    |               |   |
    |               |   +---kafka
    |               |   |       AccountConsumer.class
    |               |   |       ScanTaskCreator.class
    |               |   |       ScanTaskWorker.class
    |               |   |
    |               |   \---sheduler
    |               |           Scheduler.class
    |               |
    |               +---task
    |               |       ScanTask$ScanTaskBuilder.class
    |               |       ScanTask.class
    |               |
    |               \---util
    |                       Encrypter.class
    |                       EncryptionUtils.class


## ⚙️ **Пошаговая работа системы при запуске**

---

### **1. Старт приложения**

- Приложение запускается (`Spring Boot`).
    
- Инициализируются
    
    - Kafka-конфигурация;
        
    - Базы данных (PostgreSQL);
        
    - Все бины и компоненты (`@Service`, `@Component`);
        
    - Планировщик `ReaderScheduler`.
        

---

### **2. Создание задач сканирования**

- `ScanTaskCreator` (или REST API) запускает цикл сбора
    
    - Читает список серверов из таблицы `servers` (IP, OS, login, пароль).
        
    - На каждый активный сервер формирует `ScanTask`.
        
    - Отправляет `ScanTask` в Kafka topic `scan-tasks`.
        

---

### **3. Получение ScanTask и сбор данных**

- Kafka-консьюмер (внутри `ScanTaskWorker`) получает `ScanTask`.
    
- Определяет, какая реализация сканера нужна (LinuxScanner или WindowsScanner).
    
- Подключается к серверу:
    
    - Считывает учётные записи (user accounts).
        
    - Считывает информацию о ПК (CPU, RAM, диск, uptime).
        
- Сформированные DTO (`AccountDto`, `MachineInfoDto`) **не пишутся сразу в базу**, а:
    
    - Отправляются в Kafka topic `raw-data` (через `KafkaProducerService`).
        

---

### **4. Запись данных в БД через Kafka (raw-data)**

- Консьюмер `AccountConsumer` читает сообщения из topic `raw-data`.
    
- Выполняет **реконсиляцию**:
    
    - Проверяет, есть ли записи с таким ключом.
        
    - Если есть — обновляет, если нет — вставляет.
        
- Данные сохраняются в PostgreSQL (`tables: users, machines`).

### **6. Мониторинг и управление**

- REST API позволяет:
    
    - Создавать/блокировать учётки (отправляются в Kafka `account-commands`);
	    - /api/accounts/{serverId}/create
	    - /api/accounts/{serverId}/block/{userId}

- /swagger-ui/index.html - для просмотра документации REST API
- `/actuator, /health`, `/metrics` — для мониторинга состояния.
### **7. Обработка ошибок и устойчивость**

- Все Kafka-консьюмеры используют retry-политику или `DeadLetterQueue`.
    
- Ошибки логируются и могут быть повторно обработаны.
    
- Отказоустойчивость достигается через независимость компонентов, Kafka, и горизонтально масштабируемые консьюмеры.

### **БЭКЛОГ**

Сделать возможность реализовывать на разных машинах коннетор

Многопоточное подключение: 1 сервис на 1 ScanTask
В таблицах необходимо делать связь с таблицей (Серверы) через server_id
Необходимо сделать таблицу (Роли) и таблицу связей с таблицей (Пользователи)

Создание учетной записи - при запросе на создание пользователя необходимо создать запись в таблице с пометкой в поле (создать) - далее (раз в retry_timeout минут) пытаться отправить запрос на создание с пометкой (номер попытки, ошибка (если есть))

Нужно расширить REST API - удаление, управление конфигами и серверами


