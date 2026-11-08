# kafka-redis-demo - Flow

```
                              Client
                                |
          POST /orders?orderId=..&orderData=..
                                |
                                v
                     +---------------------+
                     | RequestLoggingFilter|  (method, URI, param names, status, ms)
                     +----------+----------+
                                v
                     +---------------------+
                     |   OrderController   |
                     +----------+----------+
                                v
                     +---------------------+       Postgres down
                     | OrderService        |-------------------------> 503 "Order storage unavailable"
                     | @Transactional      |       (CannotCreateTransactionException, ~2s)
                     +----------+----------+
                                |
                     existsById(orderId)?
                        |              |
                       yes             no
                        |              |
                        v              v
          409 "Order .. already    +--------------------------------------+
          exists"                  | ONE DB transaction (Postgres)        |
          (also PK race ->         |  INSERT orders (order_id, data, ts)  |
           DataIntegrityViolation) |  INSERT outbox (topic, key, payload, |
                                   |                 published_at = null) |
                                   +------------------+-------------------+
                                                      |
                                        200 "Order accepted" (no Kafka call)
                                                      .
                                                      . async
                                                      v
                     +-----------------------------------------------------+
                     | OutboxRelay  @Scheduled(fixedDelay = 1000 ms)       |
                     | findTop100ByPublishedAtIsNullOrderByIdAsc()         |
                     +---------------------------+-------------------------+
                                                 |
                              for each row: publish + header eventId=<outbox id>
                                   |                               |
                             Kafka down                        sent OK
                       (PublishException, 5s)                      |
                                   |                               v
                                   v                   set published_at = now()
                       WARN + break (row waits,                    |
                       retried next run, order kept)               |
                                                                   v
                                     +-------------------------------------------+
                                     |  Kafka topic: orders (3 partitions)       |
                                     |  key = orderId -> same partition          |
                                     +---------+-------------------------+-------+
                                               |                         |
                          group: order-service |                         | group: payment-service
                                               v                         v
                       +-----------------------------+   +------------------------------------+
                       | OrderConsumer               |   | PaymentConsumer                    |
                       +--------------+--------------+   +------------------+-----------------+
                                      |                                     |
                                 key null/blank?                       key null/blank?
                                  |         |                           |          |
                                 yes        no                         yes         no
                                  |         |                           |          |
                                  v         v                           v          v
                            WARN skip   Redis SET order:<id>       WARN skip   eventId processed?
                                        Redis SETNX                            (Redis EXISTS
                                        order-status:<id> CREATED               processed:payment-service:<eventId>)
                                        (skipped if PAID/FAILED                   |              |
                                         already there)                          yes             no
                                                                                  |              |
                                                                                  v              v
                                                                         DEBUG "Skip        value null/blank
                                                                         duplicate"         or contains "fail"?
                                                                         (no 2nd payment)     |          |
                                                                                             yes         no
                                                                                              |          |
                                                                                              v          v
                                                                                           FAILED      PAID
                                                                                              |          |
                                                                                              +----+-----+
                                                                                                   |
                                                                                    publish to payments (key = orderId)
                                                                                                   |
                                                                                    Redis SET processed:payment-service:
                                                                                    <eventId> "1" (TTL 7 days)
                                                                                                   |
                                                                                                   v
                                     +-------------------------------------------+
                                     |  Kafka topic: payments (2 partitions)     |
                                     +---------------------+---------------------+
                                                           |
                                                           | group: notification-service
                                                           v
                                     +-------------------------------------------+
                                     | NotificationConsumer                      |
                                     +---------------------+---------------------+
                                                           |
                                          PaymentStatus.from(value)
                                     |                     |                     |
                                   PAID                  FAILED              null / unknown
                                     |                     |                     |
                                     v                     v                     v
                          "Notify: order <id>     "Notify: payment       WARN "Unknown
                           confirmed"              FAILED for <id>"       payment status"
                          Redis SET               Redis SET
                          order-status:<id> PAID  order-status:<id> FAILED


 Consumer error (any listener throws)
 ------------------------------------
   DefaultErrorHandler -> retry every 2000 ms, max 2 retries -> DeadLetterPublishingRecoverer
                       -> topic <topic>-dlt (e.g. orders-dlt, same partition)


 Read APIs (Redis)
 -----------------
   GET /orders/{id}         -> Redis GET order:<id>         -> 200 {key, value}       | missing -> 404
   GET /orders/{id}/status  -> Redis GET order-status:<id>  -> 200 {orderId, status}  | missing -> 404
   Redis down (any call)    -> 503 "Order storage unavailable"
   Any other exception      -> 500 "Something went wrong. Please try again later."
```
