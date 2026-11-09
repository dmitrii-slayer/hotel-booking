# Система бронирования отелей

REST API для бронирования отелей на микросервисах: Spring Boot 3, Spring Cloud,
Java 21

## Стек

- Spring Boot, Spring Cloud Gateway, Eureka
- Spring Data JPA + Hibernate, H2
- Spring Security + JWT (Resource Server)
- Resilience4j (retry, circuit breaker)
- Lombok, MapStruct, springdoc-openapi
- Docker, docker compose

## Запуск

Через Docker:

```bash
docker compose up -d --build
```

Без Docker - сначала собрать, потом поднять в таком порядке:

```bash
mvn clean install

java -jar eureka-server/target/eureka-server-1.0.0.jar
java -jar hotel-service/target/hotel-service-1.0.0.jar
java -jar booking-service/target/booking-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

| Сервис          | Порт |
|-----------------|------|
| eureka-server   | 8761 |
| api-gateway     | 8080 |
| hotel-service   | 8081 |
| booking-service | 8082 |

Swagger: http://localhost:8081/swagger-ui/index.html и http://localhost:8082/swagger-ui/index.html

Первые запросы через gateway могут вернуть `503`, пока клиент Eureka не получит реестр
(30-60 секунд после старта).

При старте hotel-service создаёт 3 отеля и 10 номеров с разным `times_booked`,
booking-service - пользователей `admin/admin123` (ADMIN), `ivan/ivan123` и `maria/maria123` (USER).

## Аутентификация

Токен выдаёт booking-service, срок действия - 1 час.

```bash
curl -X POST http://localhost:8080/user/register \
-H 'Content-Type: application/json' \
-d '{"username":"newuser","password":"secret123"}'

curl -X POST http://localhost:8080/user/auth \
-H 'Content-Type: application/json' \
-d '{"username":"ivan","password":"ivan123"}'
```

Дальше токен передаётся в заголовке `Authorization: Bearer <token>`.

## Эндпойнты

Все запросы идут через gateway на порт 8080.

### Отели (hotel-service)

| Метод  | Путь               | Доступ      |
|--------|--------------------|-------------|
| POST   | `/api/hotels`      | ADMIN       |
| GET    | `/api/hotels`      | USER, ADMIN |
| GET    | `/api/hotels/{id}` | USER, ADMIN |
| PUT    | `/api/hotels/{id}` | ADMIN       |
| DELETE | `/api/hotels/{id}` | ADMIN       |

### Номера (hotel-service)

| Метод  | Путь                         | Доступ      |
|--------|------------------------------|-------------|
| POST   | `/api/rooms`                 | ADMIN       |
| GET    | `/api/rooms`                 | USER, ADMIN |
| GET    | `/api/rooms/recommend`       | USER, ADMIN |
| GET    | `/api/rooms/statistics`      | ADMIN       |
| GET    | `/api/rooms/hotel/{hotelId}` | USER, ADMIN |
| GET    | `/api/rooms/{id}`            | USER, ADMIN |
| PATCH  | `/api/rooms/{id}`            | ADMIN       |
| DELETE | `/api/rooms/{id}`            | ADMIN       |

```bash
curl "http://localhost:8080/api/rooms/recommend" -H "Authorization: Bearer $TOKEN"
curl "http://localhost:8080/api/rooms?startDate=2027-02-01&endDate=2027-02-05" \
-H "Authorization: Bearer $TOKEN"
```

### Бронирования (booking-service)

| Метод  | Путь            | Доступ              |
|--------|-----------------|---------------------|
| POST   | `/booking`      | USER, ADMIN         |
| GET    | `/bookings`     | USER, ADMIN         |
| GET    | `/booking/{id}` | свои; ADMIN - любые |
| DELETE | `/booking/{id}` | свои; ADMIN - любые |

```bash
curl -X POST http://localhost:8080/booking \
-H "Authorization: Bearer $TOKEN" \
-H 'Content-Type: application/json' \
-d '{"autoSelect":true,"startDate":"2027-02-01","endDate":"2027-02-05","requestId":"demo-1"}'
```

`autoSelect: true` - номер подбирается автоматически по наименьшему `times_booked`,
поле `roomId` игнорируется. `requestId` нужен для идемпотентности: повторный запрос
с тем же значением вернёт существующую бронь.

### Пользователи (booking-service, ADMIN)

| Метод  | Путь                                   |
|--------|----------------------------------------|
| GET    | `/users`                               |
| POST   | `/user?username=&password=&role=`      |
| PATCH  | `/user/{id}?username=&password=&role=` |
| DELETE | `/user/{id}`                           |

### Внутренние (hotel-service, для саги)

| Метод | Путь                                     |
|-------|------------------------------------------|
| POST  | `/api/rooms/{id}/confirm-availability`   |
| POST  | `/api/rooms/{id}/release`                |
| POST  | `/api/rooms/booking/{bookingId}/confirm` |

## Как работает бронирование

Создание брони - сага из локальных транзакций:

1. booking-service создаёт бронь в статусе `PENDING` и фиксирует её;
2. вызывает hotel-service для подтверждения доступности (тайм-аут 3 с, повторы, circuit breaker);
3. при успехе переводит в `CONFIRMED`, при ошибке или `409` - компенсация: `CANCELLED`
   и снятие блокировки через `/release`.

Идемпотентность держится на `requestId`: уникальный индекс в БД плюс проверка
перед вставкой. Повторная доставка не создаёт дубликат.

Конкурентные брони одного номера на пересекающиеся даты не проходят: подтверждение
берёт `SELECT ... FOR UPDATE` по строке номера, поэтому проверка конфликтов и вставка
слота сериализуются. Проигравший получает `409`.

Все запросы маркируются `X-Trace-Id` (генерируется на gateway), идентификатор попадает
в логи сервисов и в тело ошибки.

## Тестирование

```bash
mvn test
```

## Структура

```
hotel-booking/
|-- pom.xml
|-- docker-compose.yml
|-- eureka-server/          # реестр сервисов
|-- api-gateway/            # маршрутизация, проверка JWT
|-- hotel-service/          # отели, номера, слоты, статистика
|-- booking-service/        # бронирования, auth, сага
`-- openapi/                # спецификации API
```

Внутри сервиса: `domain` (entity, enums, repository), `rest` (api, controller, dto, handler, mapper),
`service`, `config`, `exception`.
