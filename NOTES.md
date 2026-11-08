# Learning Notes

> 📌 **Command Cheat Sheet is right below.** Step-by-step notes follow after it.

---

# 📌 Command Cheat Sheet

> **Know where you are by the prompt:**
> | Prompt | Where | Use for |
> |---|---|---|
> | `C:\...>` / `D:\...>` | Laptop (Command Prompt) | `docker`, `curl`, `mvnw` |
> | `/opt/kafka/bin $` | Inside Kafka container | `./kafka-*.sh` |
> | `127.0.0.1:6379>` | Inside Redis container | `GET`, `SET`, `KEYS` |


### Consumer groups
```
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group payment-service
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups
```

### Postgres (Step 15)
```
docker compose up -d postgres
docker logs postgres --tail 5
docker exec -it postgres psql -U app -d orders_db -c "select version();"
docker exec -it postgres psql -U app -d orders_db        # interactive (\dt = tables, \q = quit)
docker compose down -v                                   # WARNING: deletes pgdata volume (all DB data)
```

## 🐳 Docker (Step 2) — run in project folder `D:\repository\java\kafka-redis-learning`
```bash
docker compose up -d                 # start Kafka + Redis
docker ps                            # check both are "Up"
docker compose logs kafka            # see Kafka errors
docker compose down                  # stop everything
docker stop redis                    # stop only Redis (used to test 500)
docker start redis                   # start Redis again
```

## 📨 Kafka CLI (Steps 3, 7, 11)
**Enter the container first:**
```bash
docker exec --workdir /opt/kafka/bin -it kafka sh
```
**Topics:**
```bash
./kafka-topics.sh --create   --topic orders --partitions 3 --bootstrap-server localhost:9092
./kafka-topics.sh --describe --topic orders   --bootstrap-server localhost:9092
./kafka-topics.sh --describe --topic payments --bootstrap-server localhost:9092
./kafka-topics.sh --list --bootstrap-server localhost:9092          # all topic names (find the DLT)
```
**Produce / consume:**
```bash
./kafka-console-producer.sh --topic orders --bootstrap-server localhost:9092
#   -> type messages AFTER the ">" prompt, Ctrl+C to stop

./kafka-console-consumer.sh --topic orders --bootstrap-server localhost:9092 --from-beginning
./kafka-console-consumer.sh --topic orders --bootstrap-server localhost:9092 --from-beginning --property print.key=true --property print.partition=true
#   -> without --from-beginning = only NEW messages
```
**Consumer groups:**
```bash
./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group order-service
#   -> CURRENT-OFFSET, LOG-END-OFFSET, LAG

./kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group order-service --topic orders --reset-offsets --to-earliest --execute
#   -> ⚠️ STOP the app first. Makes the group re-read everything
```

## 🔴 Redis CLI (Steps 4, 8, 9)
**Enter:**
```bash
docker exec -it redis redis-cli
docker exec -it redis redis-cli GET order:6      # one-shot, no need to enter
```
**Inside `127.0.0.1:6379>`:**
```bash
SET order:1 Laptop          # save (overwrites!)
SET temp hello EX 10        # save with 10 s expiry (TTL)
GET order:1                 # read -> (nil) if missing
KEYS order:*                # list keys by pattern (GET has no patterns)
DEL order:1                 # delete (DEL, not DELETE)
DEL "[KEY|VALUE]"           # quotes for special characters
exit
```
> ⚠️ Keys are **case-sensitive**: `order:6` ≠ `ORDER:6`

## 📮 Postman: import `kafka-redis-demo.postman_collection.json` (project folder) -> File > Import. All curl calls below are in it.

## 🌐 API calls with curl (Steps 6, 9, error handling) — **Command Prompt, not PowerShell** (in PowerShell use `curl.exe`)
```bash
curl -X POST "http://localhost:8080/orders?orderId=1&orderData=Laptop"   # send order
curl    http://localhost:8080/orders/6                                   # read order
curl -i http://localhost:8080/orders/6                                   # -i = show status + headers
```
**Error-handling tests:**
```bash
curl -i http://localhost:8080/orders/99                                  # 404 not found
curl -i -X POST "http://localhost:8080/orders?orderData=Pen"             # 400 missing orderId
curl -i -X GET  "http://localhost:8080/orders"                           # 405 wrong method
curl -i http://localhost:8080/abc                                        # 404 unknown URL
# 500 test: docker stop redis -> curl -i http://localhost:8080/orders/6 -> docker start redis
```

## ☕ Maven / Java (Step 5, Spotless) — run in `...\kafka-redis-demo\kafka-redis-demo`
```bash
cd /d D:\repository\java\kafka-redis-learning\kafka-redis-demo\kafka-redis-demo
mvnw clean compile          # build (also runs Spotless formatter)
mvnw clean install          # build + run tests (contextLoads starts the app -> catches config errors)
mvnw clean install -DskipTests   # build without tests (e.g. when Kafka/Redis are down)
mvnw help:evaluate -Dexpression=settings.localRepository -q -DforceStdout   # which local repo Maven uses
mvnw spotless:apply         # format code only
mvnw spring-boot:run        # run app from terminal
```
**Windows checks:**
```bash
java --version
echo %JAVA_HOME%            # Command Prompt (must NOT end with \bin)
echo $env:JAVA_HOME         # PowerShell
```

## 💀 DLT test (Step 12a)
```bash
# 0. yaml: spring.data.redis.timeout: 2s  (fail fast), restart app
# 1. Kafka terminal (/opt/kafka/bin $):
./kafka-console-consumer.sh --topic orders-dlt --bootstrap-server localhost:9092 --from-beginning
# 2. Laptop terminal:
docker stop redis
docker ps                                   # redis must NOT be listed
curl -X POST "http://localhost:8080/orders?orderId=9&orderData=Tablet"
# 3. IntelliJ log: 3 failed attempts ~2 s apart, then publish to orders-dlt
# 4. Kafka terminal shows: Tablet
# 5. Laptop terminal:
docker start redis
```

---

# 📝 Step-by-step notes

Format for each step:

- What I did
- What I learned (1-2 lines)
- Error I faced + how I fixed it

---

## Step 0: Setup check

- What I did: Checked versions with `java --version` and `docker --version`
- Result: Java 25.0.4.1 LTS, Docker 29.6.2
- What I learned: Java 25 is LTS, so it's fine for Spring Boot
- Errors + fixes: none

## Step 1: Concepts
| Concept | My first answer | Correct meaning |
|---|---|---|
| Topic | Kind of logs of messages | ✅ A named log (stream) of messages, like a category, e.g. `orders` |
| Partition | That message is called partition | ❌ A topic is split into partitions. Each partition is an ordered log. Partitions allow parallel reading |
| Offset | Message is combination of offset | ❌ The position number of a message inside a partition (0, 1, 2...). Unique only within that partition |
| Broker | (asked) | A Kafka server that stores partitions and serves producers/consumers. Many brokers = a cluster |
| Producer | Sends message to topic | ✅ |
| Consumer | Consumes message from topic | ✅ |
| Consumer group | (asked) | Consumers sharing one group-id. Each partition goes to only ONE consumer in the group, so work is split. Different groups each get ALL messages |
| Redis | (asked) | An in-memory key-value store (like a HashMap outside your app). Fast because data is in RAM |

Memory trick: Topic = folder, Partition = file in folder, Offset = line number in file.

Quick check (passed):
- 3 partitions + 3 consumers in one group -> 1 partition each
- 4 consumers, 3 partitions -> 4th consumer sits idle (no partition)
- 2 different groups -> each group gets ALL messages

Example of 2 groups: topic `orders`
- Group `notification-service` reads every order -> sends email
- Group `analytics-service` reads every order -> updates sales report
- Both get all orders. Each keeps its own offset, so they don't affect each other.

## Step 2: Kafka + Redis in Docker

- What I did:
- What I learned:
- Errors + fixes:

## Step 3: Kafka CLI

- What I did: Created topic `orders` (3 partitions), sent ms1, ms2, ms3 with console producer, read them with console consumer --from-beginning
- What I learned:
  - Without --from-beginning, a NEW consumer group reads only messages that arrive AFTER it joins (auto.offset.reset=latest)
  - ms4 was missing because I typed it BEFORE the producer showed `>`. Text typed before the prompt is not sent. Always wait for `>` then type
  - With --from-beginning, a new group starts at offset 0 and reads everything
  - Console consumer makes a random group each run, so it never remembers its offset
  - --describe output:
    - Leader = the broker that handles all reads/writes for that partition
    - Replicas = list of brokers that keep a copy of that partition
    - ReplicationFactor = number of copies. Mine is 1 -> only 1 copy -> if the broker dies, data is lost/unavailable. Production usually uses 3
    - Example with 3 brokers + RF 3: Partition 0 -> Leader: 2, Replicas: 2,1,3. If broker 2 dies, broker 1 or 3 becomes leader
- Errors + fixes:
  - `parttitions is not a recognized option` -> typo in flag name. Kafka rejects unknown flags. Read the FIRST line of the error, ignore the Java stack trace
  - `Failed to create new KafkaAdminClient` -> real cause was in the `Caused by:` line: `Invalid url in bootstrap.servers: localhost:909clear2`. The word "clear" got typed into the port. Rule: if the first line is vague, look for the LAST `Caused by:` line
  - Producer: typed `ms1 ms2 ms3` on the command line too. Not needed: messages go AFTER the `>` prompt, one per line. CONFIRMED: consumer said "Processed a total of 7 messages" = 3 real + 4 empty (the 4 empty Enter presses). Empty lines ARE sent as messages
  - Consumer: `Connection to node -1 (localhost:8082) could not be established` repeating -> wrong port. Kafka keeps retrying forever instead of failing. Repeating "could not be established" = check host/port

## Step 4: Redis CLI

- What I did: SET order:1 laptop, GET order:1 -> "laptop", SET order:2 phone, KEYS *, DEL order:1 -> (integer) 1, GET order:1 -> (nil), SET order:1 laptop EX 10 -> key disappeared after 10 sec
- What I learned:
  - Format: SET <key> <value> [EX seconds]. Options go AFTER the value
  - GET on a missing/deleted key returns (nil) (like null in Java)
  - DEL returns how many keys were deleted (1)
  - EX = TTL (time to live). Redis auto-deletes the key after that time
  - `set order:1=laptop` fails: Redis splits by SPACE, not by =
  - Command is DEL, not DELETE. Keys with special chars need "quotes"
  - Why `order:1` not `1`: one Redis holds everything (orders, users, payments). Prefix avoids clashes (order:1 vs user:1) and lets me find a group with KEYS order:*
- Errors + fixes:
  - Copied doc syntax literally: `SET [KEY|VALUE] [ORDER:1|LAPTOP]` -> Redis saved a key literally named "[KEY|VALUE]". How to read Redis docs: lowercase word (key, value) = replace with your own value; [ ] = optional, do not type the brackets; | = pick one option
  - Redis keys are case-sensitive: ORDER:1 and order:1 are different keys

## Step 5: Spring Boot project

- What I did: Generated project on start.spring.io (Spring Boot 4.1.1, Java 25, Maven), opened in IntelliJ 2025.3
- What I learned:
  - App started: `Started KafkaRedisDemoApplication in 1.9 seconds`, Tomcat on port 8080
  - Spring Boot 4.1.1 -> Spring Kafka 4.1.1, kafka-clients 4.2.1, Lettuce (Redis client) 7.5.2, Tomcat 11, Jackson 3
  - Boot 4 starters are split: spring-boot-starter-webmvc, spring-boot-starter-kafka, spring-boot-starter-data-redis
  - Setup checklist for any new machine: JAVA_HOME = JDK folder (no \bin), Path = %JAVA_HOME%\bin, IntelliJ Project SDK + Module language level, Maven home = Bundled
  - Final IntelliJ fix: Maven home path "Use Maven wrapper" -> "Bundled (Maven 3)", then Reload Project + Rebuild
  - IntelliJ "Vulnerable Dependencies" (Mend.io) warnings on transitive deps (tomcat, jackson, lz4, zstd...) = WARNINGS, not errors. Ignore for a local learning project. In real projects: upgrade Spring Boot to its latest patch version, don't override single library versions by hand
- Errors + fixes:
  - `Cannot run program "C:/Program Files/Java/jdk-1.8/bin/java"` + `JVM target 5` -> IntelliJ used an old, missing JDK 1.8 as Project SDK and did not load the project as Maven. Fix: set Project SDK to JDK 25 (File > Project Structure) and load pom.xml as a Maven project. Lesson: pom.xml says java.version 25, but IntelliJ has its OWN SDK setting
  - JAVA_HOME was `D:\software\java\jdk-25_windows-x64_bin\jdk-25.0.4.1\bin` -> WRONG, must NOT end with \bin. Maven looks for %JAVA_HOME%\bin\java.exe, so it searched ...\bin\bin\java.exe. Correct: JAVA_HOME = JDK folder, PATH = %JAVA_HOME%\bin
  - Check JAVA_HOME: cmd -> `echo %JAVA_HOME%`, PowerShell -> `echo $env:JAVA_HOME`
  - After fixing JAVA_HOME, `java` stopped working: Path had `%JAVA_HOME%` -> must be `%JAVA_HOME%\bin`. Env var changes need a NEW terminal window
  - IntelliJ: `Guice/MissingImplementation: No implementation for ArtifactHandlerManager` -> IntelliJ's Maven integration failing with the Maven it was set to use (wrapper, Maven 3.10.0). Try: Settings > Maven > Maven home path = Bundled. settings.xml at D:\software\essentials\settings.xml is fine (only sets localRepository D:\dependencies\m2) - keep it
  - pom.xml shows `Dependency '...:spring-boot-starter-kafka:' not found` with EMPTY version after `:` -> versions come from the parent (spring-boot-starter-parent 4.1.1). Empty = parent not downloaded = Maven import failed. Fix the Maven import first, then all 11 errors go away together
  - Compile still uses jdk-1.8: the SDK NAME "25" in IntelliJ is only a label. Check its real JDK home path in Project Structure > SDKs
  - Real cause found: Modules > kafka-redis-demo > Sources > Language level = 5. Modules > Dependencies showed NO Maven libraries -> proof Maven import never happened, so IntelliJ treated it as a plain Java module with default level 5. Fix: Language level = Project default, then fix Maven import (it sets this from pom.xml automatically)
- docker-compose.yml lines:
  - `services:` = list of containers to run
  - `image:` = which software to download from Docker Hub
  - `container_name:` = short name, used in `docker exec -it kafka ...`
  - `ports: "9092:9092"` = laptop port : container port, so Spring Boot can reach localhost:9092
- Commands: `docker compose up -d` (start), `docker ps` (check), `docker compose logs kafka` (errors), `docker compose down` (stop)

## Step 6: Producer (KafkaTemplate)
- 6a application.yaml: `spring.kafka.bootstrap-servers` -> in YAML each dot = new indented level (2 spaces), `kafka:` same level as `application:`
- Mistake: wrote `http://localhost:9092`. Kafka is NOT HTTP, it uses its own TCP protocol. Value is just host:port, same as in CLI
- 6a DONE: bootstrap-servers: localhost:9092
- 6b DONE: OrderProducer (@Component, @RequiredArgsConstructor, @Slf4j, KafkaTemplate<String,String>, send("orders", orderId, orderData))
  - Review: fields should be `private final` (Lombok injects `final` fields only)
  - `log.debug` is NOT shown by default: Spring Boot logs at INFO level. Use log.info, or set `logging.level.com.learning: debug`
  - @Component works; @Service says "business logic layer" (same behaviour, clearer meaning)
  - send() is async: it returns a CompletableFuture straight away, the message goes to Kafka in the background
- 6b v2: moved topic name to yaml (`topic.name: orders`) + @Value. Good idea.
  - BUG: `@Value` on a `static` field does NOT work. Spring injects into objects (beans), static fields belong to the class -> TOPIC stays null -> send(null, ...) fails
  - Fix: non-static, private field (not final, so Lombok's constructor skips it and Spring sets it after creating the bean)
  - Naming: UPPER_CASE is for real constants (static final). An injected value is a normal field -> camelCase
- 6b FIXED: @Value now on non-static private field; logging.level.com.learning: debug added -> debug logs visible
- 6c OrderController: @RestController + @RequiredArgsConstructor + @PostMapping + 2 @RequestParam, returns ResponseEntity.ok(...) (good: ResponseEntity lets me control the HTTP status later)
  - BUG: `@RestController("/orders")` -> the value of @RestController is the BEAN NAME, not a URL. So the endpoint was mapped to POST "/" not "/orders"
  - Fix: URL goes in @RequestMapping("/orders") on the class, or @PostMapping("/orders") on the method
- 6c DONE: @RestController + @RequestMapping("/orders") on class, @PostMapping on method -> POST /orders
- Side task: code formatter during build -> Spotless Maven plugin (diffplug), palantirJavaFormat (4-space, close to IntelliJ style)
  - Goals: `spotless:apply` = fixes formatting, `spotless:check` = fails build if not formatted
  - Bind `apply` to a phase (e.g. process-sources) so every `mvnw compile` formats code
  - IntelliJ green Run button does NOT run Maven phases -> formatter won't run unless I use mvnw, or tick "Delegate IDE build/run actions to Maven"
- Spotless DONE: plugin in pom.xml, mvnw compile reformatted OrderController (e.g. 'orderId,orderData' -> 'orderId, orderData')
- 6d test: curl POST /orders?orderId=1&orderData=Laptop -> "Order Message sent orderId: 1"; log shows debug line + ProducerConfig
  - Producer is created LAZILY: ProducerConfig only printed on the FIRST send, not at startup
  - Useful ProducerConfig values (Kafka 4.2.1 client defaults):
    - bootstrap.servers = [localhost:9092] -> my yaml value was used
    - key/value.serializer = StringSerializer -> Spring Boot default, matches KafkaTemplate<String,String>
    - acks = -1 (= all) -> broker confirms only after all in-sync replicas have the message (safest)
    - enable.idempotence = true -> retries never create duplicates
    - retries = 2147483647 + delivery.timeout.ms = 120000 -> keeps retrying up to 2 min
    - linger.ms = 5 -> waits up to 5 ms to batch messages together
- 6d DONE: Laptop appeared in console consumer -> REST -> Spring -> Kafka works end to end
- Why orderId as KEY: Kafka hashes the key to pick the partition. Same key -> same partition -> messages for one order stay IN ORDER (created -> paid -> shipped). No key -> spread across partitions, no order guarantee across them
- Windows PowerShell: `curl` is an alias for Invoke-WebRequest (different syntax). Use `curl.exe` there, or use Command Prompt
- 6 BONUS: whenComplete logs partition + offset. Results:
  - orderId 1 Laptop -> P0 offset 1 | orderId 2 TV -> P2 offset 0 | orderId 3 Monitor -> P2 offset 1
  - Offsets are PER PARTITION (P2 starts at 0 even though the topic has older messages elsewhere)
  - Different keys CAN share a partition (2 and 3 both -> P2). Rule is one-way: same key -> same partition, NOT same partition -> same key
  - Callback log runs on thread [demo-producer-1], not the HTTP thread [nio-8080-exec-x] -> proof send() is async
  - Console consumer prints partition by partition, so the output is NOT in global send order. Order is only guaranteed inside one partition
  - Code review bugs:
    - no `else` -> on success it falls through to log.error with throwable = null -> NullPointerException inside the callback (hidden, because nobody reads the returned future). `assert` does nothing: Java assertions are OFF by default (-ea flag)
    - log.error("msg", throwable) -> pass the exception as last arg so SLF4J prints the full stack trace. fillInStackTrace() not needed
    - unused local variable `send`
- 'ProducerConfig values:' block = the Kafka client library prints ALL its settings at INFO once, when the producer is created (first send). Normal, not an error. Hide with logging.level.org.apache.kafka: warn (but keep it while learning/debugging)
- 6 BONUS DONE: else-branch bug fixed, log.error passes throwable as last arg
- Key test CONFIRMED: orderId 1 x3 (Laptop-Mac, Laptop-Linux, Laptop-Unix) -> Partition 0, offsets 2, 3, 4. Partition = hash(key) % partitions; offset = next number in that partition
- Kafka client creates a NEW producer after app restart -> ProducerConfig printed again (ProducerId 1000 -> 1001)

## Step 7: Consumer (@KafkaListener)
- 7a yaml: spring.kafka.consumer.group-id: order-service, auto-offset-reset: latest
  - auto-offset-reset is used ONLY when the group has no saved offset yet (first start). latest = skip old messages, earliest = read all old ones
- 7b first draft review:
  - Missing @KafkaListener -> without it the method is a normal method, nobody calls it
  - @Value field for topic not needed: annotation attributes can't read a field, but @KafkaListener(topics = "${topic.name}") resolves the placeholder itself
  - Unused import SimpleTriggerContext (IDE auto-import by mistake)
  - No log yet
- 7b v2 OK: @KafkaListener(topics = "${topic.name}"), ConsumerRecord<String,String>, logs key/value/partition/offset. groupId comes from yaml (not repeated in annotation)
  - Leftover unused import @Value. Spotless can remove unused imports with the <removeUnusedImports/> step (palantirJavaFormat alone only formats)
  - logging.level.org.apache.kafka: warn ALSO hides consumer-group join logs -> check the group with CLI instead: kafka-consumer-groups.sh --describe --group order-service
- kafka-consumer-groups.sh --describe columns:
  - CURRENT-OFFSET = next offset the GROUP will read (saved bookmark) in that partition
  - LOG-END-OFFSET = next offset Kafka will WRITE (= messages ever written there)
  - LAG = LOG-END-OFFSET - CURRENT-OFFSET = messages not read yet. LAG 0 = up to date. Growing LAG = consumer too slow/stopped
- My output (after Headphones): P0 6/6, P1 9/9, P2 2/2, LAG 0 everywhere, SAME CONSUMER-ID on all 3 -> 1 consumer reads all 3 partitions
  - P1 = 9 = ms1, ms2, ms3, 4 empty, ms4, ms5 -> all keyless console messages landed in ONE partition (sticky partitioner: no key -> fill one partition's batch, not round-robin per message)
  - P2 = 2 = TV, Monitor. P0 = 6 = Laptop x2, Laptop-Mac/Linux/Unix, Headphones
  - HOST /172.18.0.1 = Docker network gateway: Kafka sees my laptop app through Docker's network
- Restart test: consumer does NOT re-read old messages. My first reason ("because latest") was WRONG:
  - Real reason: the group's offsets (CURRENT-OFFSET) are COMMITTED and stored IN KAFKA (internal topic __consumer_offsets), not in my app. On restart the group continues from its saved offset
  - auto-offset-reset is used ONLY when a group has NO saved offset (brand-new group, or offsets expired). Even with earliest, a restart would not re-read
  - To re-read: use a new group-id, or reset: kafka-consumer-groups.sh --reset-offsets --to-earliest --group order-service --topic orders --execute (app must be stopped)
  - Console consumer re-read with --from-beginning because it makes a NEW random group every run
- Reset test DONE: --reset-offsets --to-earliest, restart -> all 17 messages re-read (P0 0-5, P1 0-8, P2 0-1)
  - Log `order-service: partitions assigned: [orders-0, orders-1, orders-2]` = 1 consumer got all 3 partitions
  - Listener thread name [ntainer#0-0-C-1] = Spring's listener container thread (not HTTP thread, not producer thread)
  - Console-producer messages have Key null (no key sent). P1 offset 6 value was a single space
- STEP 7 DONE

## Step 8: Save to Redis
- 8a yaml: spring.data.redis.host/port OK
- 8b first draft review (StringRedisTemplate + opsForValue().set):
  - BUG: null key -> logs warn but still calls set(null, ...) -> Spring Data Redis throws IllegalArgumentException (non-null key required) -> listener fails -> Spring Kafka error handler RETRIES the same record several times, then skips it. Fix: `return` after the warn (guard clause)
  - Key format: used record.key() ("6") instead of "order:" + key ("order:6") -> no prefix = clashes with other data types later
  - yaml typo: `Kafka:` with capital K. Spring Boot keys should be lowercase kebab-case -> write `kafka:`
  - auto-offset-reset changed to earliest: no effect for group order-service, it already has committed offsets
- 8b v2: prefix "order:" added. Null key -> fallback key "default" + System.currentTimeMillis()
  - Problem: messages are consumed in the SAME millisecond (Step 7 log: 9 records at 17:45:48.837) -> same key -> each overwrites the previous one, data lost silently
  - Problem: random keys can never be looked up by GET /orders/{id} -> useless data in Redis
  - Better: skip (return) + warn. A message with no orderId isn't a valid order
  - Style: `String key = null;` then reassign -> declare once: String key = record.key();
  - yaml `Kafka:` still capital K
- Does `Kafka:` (capital) matter? Spring Boot relaxed binding is lenient, so it may still bind, but the documented format is lowercase kebab-case. Test: if it is NOT bound, group-id is missing -> listener fails to start ("No group.id found"). Keep lowercase to avoid doubt
- Unique key for null-key messages (for understanding):
  - currentTimeMillis -> NOT unique (many records per ms)
  - UUID.randomUUID() -> unique, but a re-read (offset reset) creates NEW keys -> duplicates in Redis
  - partition + offset (e.g. order:nokey:1-3) -> unique AND the same every time the record is re-read -> no duplicates (idempotent) + traceable back to the exact Kafka record. BEST option
  - Lesson: a good fallback key is unique AND deterministic (same input -> same key)
- 8 test: GET order:6 -> "Keyboard" ✅ (order:2 is old, from Redis CLI practice in Step 4)
  - `GET key:*` -> (nil): GET takes ONE exact key, no patterns. Patterns only with KEYS
  - `GET ORDER:6` -> (nil): keys are case-sensitive
- 8b v3: fallback key "default" + partition + offset -> no separator = collisions: P1 offset 23 -> "default123", P12 offset 3 -> "default123". Use a separator: nokey:1-23 vs nokey:12-3
  - Old keyless messages were NOT re-read (group offsets already at end), so no nokey keys in Redis yet. To test: reset offsets (Step 7 command)
- 8 DONE: after offset reset, all 19 records re-read -> 9 keyless saved as order:noKey1-0 .. order:noKey1-8 (separator fixed, partition-offset)
- Overwrite test: orderId 6 Keyboard, then Mouse -> GET order:6 = "Mouse"
  - Redis SET OVERWRITES: one key = one value = LATEST state ("last write wins")
  - order:1 = Laptop-Unix (last of 5 messages with key 1)
  - BIG IDEA: Kafka = full HISTORY (every event kept: Laptop, Laptop-Mac, ...). Redis = CURRENT STATE (only the latest). This is why the pair is common: Kafka for events, Redis for fast lookup of now
  - Kafka keeps order per key (same partition), so "latest" in Redis really is the last event sent for that key

## Step 9: GET /orders/{id}
- Refactor: RedisService (save/read) used by consumer + controller; producer/consumer moved to service.producer / service.consumer; model/Order with @Data @Builder
- Review of first draft:
  - BUG: save() adds "order:" prefix, read() does NOT -> GET /orders/6 looks up key "6" -> always 404. Keep the prefix logic in ONE place (private method/constant) so save and read can't drift apart
  - COMPILE ERROR: ResponseEntity.ok().body() needs an argument -> pass the Order built with Order.builder()...build()
  - Import clash: org.springframework.core.annotation.Order imported in controller, but my model is com.learning.model.Order -> remove the Spring one. Also unused lombok.Builder import, wildcard import
  - ResponseEntity<?> -> use ResponseEntity<Order> (typed, clearer)
  - @Data already includes @ToString -> @ToString redundant
  - Step 10 note: @Builder alone = no no-args constructor -> Jackson can SERIALIZE (getters) but cannot DESERIALIZE JSON into Order. Will need @NoArgsConstructor + @AllArgsConstructor (or @Jacksonized)
  - Optional style alternative: read.map(...).orElse(...) instead of isEmpty()/get()
- 9 DONE: GET /orders/6 -> 200 {"key":"6","value":"Mouse"} (Content-Type application/json: Spring + Jackson turned Order into JSON)
  - read() prefix fixed. Still: "order:" written in 2 places -> move to one private method/constant (small refactor)
  - Order now has @NoArgsConstructor + @AllArgsConstructor (needed with @Builder for Jackson to read JSON in Step 10)
  - 404 test DONE: /orders/99 -> HTTP 404, Content-Length: 0 (notFound().build() = no body)

## Step 10: JSON messages - SKIPPED (my choice). Kafka keeps carrying plain Strings.

## Step 11: TopicBuilder

## Side task: Global exception handling (before Step 11)
- @RestControllerAdvice + @ExceptionHandler = one place for all REST errors
- Covers ONLY the web layer (controllers). Exceptions inside @KafkaListener are NOT handled here -> Kafka side uses Spring Kafka error handlers (Step 12)
- OrderNotFoundException: extends RuntimeException, constructors (message) and (message, cause) calling super(...) -> getMessage() works
  - Design note: taking a full message means every caller builds the text. Taking orderId keeps the message format in one place. Both are valid
  - Still to do: throw it (orElseThrow in controller), GlobalExceptionHandler with @RestControllerAdvice
- Why 500 instead of 404: no @RestControllerAdvice class exists yet -> nobody catches OrderNotFoundException -> Spring treats any uncaught exception as 500
  - Correct status for "not found" is 404 (400 = Bad Request = client sent invalid input, e.g. missing orderId)
  - Message bug: "Order with id" + id + "not found" -> "Order with id99not found" (missing spaces)
- ApiErrorHandler (@RestControllerAdvice) with @ExceptionHandler(OrderNotFoundException) -> ProblemDetail 404. OrderNotFoundException now takes orderId (message format in one place)
  - Unused import org.apache.kafka.common.requests.ApiVersionsResponse (IDE auto-import mistake)
  - Message says "Message with OrderId" -> should say "Order"
- ResponseEntityExceptionHandler = Spring's ready-made base class. It already has @ExceptionHandler methods for Spring MVC's own exceptions (missing @RequestParam, wrong HTTP method, unreadable body, validation...) and returns ProblemDetail for them
  - Use: `extends ResponseEntityExceptionHandler` -> those handlers are inherited
  - Do NOT also write my own @ExceptionHandler(MissingServletRequestParameterException) -> 2 handlers for same exception -> "Ambiguous @ExceptionHandler" error at startup. To customize, OVERRIDE the inherited method instead (e.g. handleMissingServletRequestParameter)
  - @ExceptionHandler(Exception.class) catch-all is still needed for my own/unknown exceptions -> 500 + log.error
- orElseThrow mistakes (compile errors):
  - orElseThrow expects a Supplier (a function that CREATES the exception), not an exception object -> pass a lambda () -> new X(id). Bonus: the lambda runs only when empty, so no exception object is created on success
  - orElseThrow RETURNS the unwrapped value (String), not Optional<String> -> variable type is String and read.get() is no longer needed
- orElseThrow lambda fixed. But Optional.of(...orElseThrow(...)) then .get() = unwrap -> wrap again -> unwrap again. Pointless: keep the String from orElseThrow
- Empty string case: Redis CAN store "" (my order:noKey1-3..1-5 = "", noKey1-6 = " "). Optional.ofNullable("") is PRESENT -> GET returns 200 {"value":""}
  - null = key missing; "" = key exists with empty value. Optional only treats null as empty
  - Fix at READ: Optional.filter(...) keeps the value only if a condition is true -> filter out blank values -> becomes empty -> orElseThrow -> 404
  - String.isEmpty() = length 0 only; String.isBlank() = empty OR only spaces (catches " ")
  - Better root fix at WRITE: consumer skips blank values, so they never reach Redis. Validate at write + defend at read
- filter(String::isBlank) is INVERTED: filter KEEPS values where the condition is TRUE -> keeps only blank values -> "Mouse" becomes 404, "" becomes 200. Need the negation: keep values that are NOT blank (lambda with ! or Predicate.not(...))
- Consumer: added blank-KEY check (good extra), but blank VALUE still saved -> the write-side fix is about the value
- Leftover unused import java.util.Objects in controller
- filter fixed: filter(r -> !r.isBlank()) -> real values kept, blank -> 404
- Can a Kafka value be null? YES:
  - A record with key + null value = "tombstone" (used in compacted topics to mean "delete this key")
  - KafkaTemplate.send(topic, key, null) sends one. My REST API can't (@RequestParam is required), console producer can't (empty line = "", not null)
  - If null reaches redisTemplate.set(key, null) -> Spring Data Redis throws (non-null value required) -> listener error -> retries
  - Check: value == null || value.isBlank(). Null check FIRST: || short-circuits, so isBlank() is never called on null (else NullPointerException)
- How Spring picks an @ExceptionHandler: the MOST SPECIFIC matching type wins. OrderNotFoundException -> its own handler; MissingServletRequestParameterException -> inherited handler; anything else -> Exception.class catch-all
- 404/400 = expected (client's fault) -> no stack trace needed. 500 = unexpected (my bug/infra) -> log.error WITH the exception, so I can debug
- In a 500 response, do NOT send ex.getMessage() to the client: it can leak internals (hostnames, SQL, class names). Send a generic text, keep details in the log
- ApiErrorHandler DONE: extends ResponseEntityExceptionHandler + OrderNotFound (404) + Exception catch-all (500, log.error, generic message)
- 500 test (Redis stopped): HTTP 500, Content-Type application/problem+json (the standard media type for ProblemDetail), generic detail, no internals leaked
- Still pending: consumer blank/null VALUE check
- Error handling tests ALL PASS: /orders/6 -> 200 | /orders/noKey1-3 -> 404 | POST without orderId -> 400 (inherited) | GET /orders -> 405 + header `Allow: POST` (inherited)
- Why noKey1-3 is 404: MY OWN filter(r -> !r.isBlank()) turns "" into empty -> orElseThrow -> 404. It is a DESIGN CHOICE, not a Spring rule:
  - 404 = "a blank value is not a real order" (data is useless)
  - 200 with "" = "the key exists, return whatever is stored"
  - Pick one, keep it consistent with the write side (if blank values are skipped on write, they never exist -> 404 is consistent)
- DECISION: validate ONLY on write (consumer). Read returns exactly what is stored, no filter
  - Pro: one place for the rule, read stays simple, read never hides data
  - Con: old blank data already in Redis (order:noKey1-3, 1-4, 1-5 = "", 1-6 = " ") will return 200 with "" -> clean it up once with DEL
- FINAL RULE (my choice): keys must never be null/blank (fallback noKey<p>-<o> already does this). Values may be null or empty
  - Empty "" value: Redis stores it fine -> GET returns 200 {"value":""}. No cleanup/DEL needed
  - null value: Redis CANNOT store null -> redisTemplate.set(key, null) throws -> listener retries. Must decide what null means:
    a) store "" instead (null -> empty text)
    b) Kafka meaning: null value = tombstone = "delete this key" -> DELETE the Redis key (Redis mirrors Kafka)
- Why Redis rejects null: Redis stores values as BYTES. "" = 0 bytes (a real value). null = no value at all, nothing to store. Redis already uses nil to mean 'key does not exist', so a stored null would be indistinguishable from a missing key. Spring Data Redis checks this and throws before sending
- DECISION: consumer converts null value -> "" before saving (option A). Trade-off: null and "" look the same in Redis (tombstone meaning lost), acceptable for this project
- Side task DONE: no filter on read; consumer saves Objects.requireNonNullElse(value, ""); key fallback for null/blank. Leftover: 2 constructors in OrderNotFoundException have different messages ('Message with OrderId' vs 'Order')
- Global exception handling COMPLETE (messages consistent)
- Kafka layer error handling plan (Step 12):
  - Producer: (a) wait for send result with timeout -> on failure throw custom exception -> 503, or (b) stay async and return 202 Accepted (honest: "received, not yet confirmed"). Lower max.block.ms so POST doesn't hang 60 s
  - Consumer: DefaultErrorHandler (retry with back-off) + DeadLetterPublishingRecoverer (send failed record to <topic>-dlt)
- Why a NewTopic bean if the producer already has the topic name?
  - Producer only USES a name. It doesn't decide partitions/replicas/retention
  - Missing topic -> broker auto-creates it with BROKER DEFAULTS (num.partitions = 1) -> not what I want
  - Production usually disables auto-create (auto.create.topics.enable=false) -> send fails
  - NewTopic bean = topic definition as code -> same settings on every new environment
- Replicas = copies of each partition, each on a DIFFERENT broker
  - 1 broker -> max 1 copy. replicas(3) -> InvalidReplicationFactorException
  - 3 brokers + replicas 3 -> each partition on all 3 brokers, 1 leader + 2 followers; 1 broker dies -> a follower becomes leader, no data lost
  - 3 brokers + replicas 1 -> broker with that partition dies -> partition unavailable
- auto.create.topics.enable=false only stops IMPLICIT creation (when a producer/consumer asks for an unknown topic). The NewTopic bean is an EXPLICIT create request via KafkaAdmin (AdminClient.createTopics) -> works even with auto-create off, IF my app's Kafka user has permission (ACL "Create")
  - In many companies apps don't get that permission: a platform team creates topics (Terraform, Strimzi, scripts). Then the app just uses the topic
- 11 DONE: KafkaTopicConfig (@Configuration, @Bean NewTopic via TopicBuilder, name+partitions from yaml topic.payments.*). payments: 2 partitions, RF 1. yaml regrouped: topic.orders.name / topic.payments.name+partitions, producer/consumer placeholders updated
  - Tip: method name = bean name. With several NewTopic beans, `createTopic` is vague -> `paymentsTopic`
- Restart test: app restarted with no error. KafkaAdmin checks which topics exist and creates only the MISSING ones -> safe to restart
- Declaring orders with 4 partitions: KafkaAdmin can INCREASE partitions (3 -> 4) but never decrease. After that, hash(key) % 4 != hash(key) % 3 -> keys may move to a new partition -> ordering per key breaks across old/new messages

## Step 12: Error handling for Kafka + Redis
### 12a: Consumer retry + Dead Letter Topic (DLT)
- DefaultErrorHandler bean (CommonErrorHandler) -> Spring Boot plugs it into the listener container automatically
- FixedBackOff(interval ms, maxRetries) -> e.g. 2000, 2 = 3 attempts total, 2 s apart
- DeadLetterPublishingRecoverer(kafkaTemplate) -> after last retry, publishes the failed record to the DLT
- DLT is written to the SAME partition number as the original -> DLT needs at least as many partitions as orders (3) -> create it with a NewTopic bean
- 12a review (stuck):
  - COMPILE ERROR: errorHandler() is declared to return DefaultErrorHandler but returns a FixedBackOff. FixedBackOff is only an INGREDIENT -> new DefaultErrorHandler(recoverer, backOff)
  - Recoverer needs KafkaTemplate -> add it as a PARAMETER of the @Bean method (Spring injects bean-method params)
  - Missing: NewTopic bean for the DLT (3 partitions). Classic default DLT name = <topic>.DLT -> confirm with kafka-topics.sh --list after the test
  - Naming: `topic.defaultErrorHandler.*` in yaml is not a topic setting -> e.g. `kafka.retry.interval` / `kafka.retry.max-retries`. Error-handler beans fit better in their own class (KafkaErrorConfig)
- 12a v2: DefaultErrorHandler(recoverer, FixedBackOff) built correctly, KafkaTemplate injected as @Bean method param. Still missing: NewTopic bean for orders.DLT (yaml has topic.orders.DLT.partitions but no bean and no name). Use lowercase yaml key dlt
- Side task: replace @Value with @ConfigurationProperties (type-safe binding of a whole yaml block into one object)
  - record + @ConfigurationProperties(prefix = "...") -> Spring binds via the record's constructor
  - Register it: @ConfigurationPropertiesScan on the main class (or @EnableConfigurationProperties(X.class))
  - yaml kebab-case `orders-dlt` binds to Java field `ordersDlt`; `max-retries` -> `maxRetries`
  - Pending for 12a: DLT test + log check
- @ConfigurationProperties review:
  - KafkaProperties() has NO record components -> nothing is bound. Nested records declared inside are only TYPES, not fields
  - Need 3 levels: Props(Topics topics, Retry retry) -> Topics(Topic orders, Topic ordersDlt, Topic payments) -> Topic(String name, int partitions)
  - yaml TYPO `max-retires` -> unknown keys are IGNORED silently -> maxRetries = 0 -> no retries, no error! Must be `max-retries`
  - Name clash: Spring Boot already has a class called KafkaProperties (spring.kafka.*) -> easy to import the wrong one -> rename mine e.g. AppKafkaProperties
  - @ConfigurationPropertiesScan on main class ✅
- v2: outer components added (Topics topics, Retry retry), yaml max-retries fixed. Still: Topics(name, partitions) binds to app.kafka.topics.name -> doesn't exist -> null/0. Need Topics(Topic orders, Topic ordersDlt, Topic payments) + Topic(name, partitions)
- AppKafkaProperties v3: 3 levels correct (Topics -> Topic, Retry). Extra outer component `Topic topic` has no yaml key (app.kafka.topic) -> always null -> remove. Next: Step 4 use it in KafkaTopicConfig via accessors props.topics().payments().name()
- mvn install BUILD FAILURE: test contextLoads starts the full app -> `Could not resolve placeholder 'topic.orders.name'` -> yaml moved to app.kafka.topics.orders.name but OrderProducer @Value and OrderConsumer @KafkaListener still use old ${topic.orders.name}. Renaming a yaml key = update EVERY placeholder (search with Ctrl+Shift+F)
- Placeholders fixed to app.kafka.topics.orders.name. KafkaTopicConfig uses AppKafkaProperties accessors. Still missing: ordersDltTopic bean
- 12a DLT test result:
  - Retries WORK: 3 attempts at 22:30:51, :53, :55 (2 s apart) + "Record in retry and not yet recovered"
  - Then: `Destination resolver returned non-existent partition orders-dlt-2` -> in Spring Kafka 4.x the default DLT name is `<topic>-dlt` (orders-dlt), NOT orders.DLT (my earlier assumption was wrong)
  - orders-dlt did not exist -> broker auto-created it with 1 partition -> partition 2 missing -> producer picked a partition itself. My NewTopic orders.DLT was never used
  - Fix: yaml ordersDlt name -> orders-dlt. On restart KafkaAdmin increases orders-dlt 1 -> 3 partitions. orders.DLT is now an unused topic
- 12a DONE: retries + orders-dlt verified; after Redis restart order 9 = 404 (stays only in DLT), new orders work, LAG 0. DLT stores failed messages, does NOT replay them
### 12b: Producer send failure
- DECISION: option A (sync wait) -> 200 only when Kafka confirms, 503 on failure. B (202 async) can lose orders silently
- Transactional Outbox added to plan as Step 15 (needs DB + JPA + scheduler; duplicates possible -> consumer must be idempotent, mine is: Redis SET overwrites)
- 12b review:
  - COMPILE ERROR: future.get(...) returns the RESULT (SendResult), not a future -> no .whenComplete on it. After get(), log result.getRecordMetadata() directly
  - Handler returns INTERNAL_SERVER_ERROR (500) -> should be SERVICE_UNAVAILABLE (503) = "dependency down, retry later". Also log.error(..., ex) there
  - yaml: `max.block.ms` directly under spring.kafka.producer -> NOT a Spring Boot property -> silently ignored. Raw Kafka client settings go under spring.kafka.producer.properties
  - InterruptedException: call Thread.currentThread().interrupt() before throwing (restores the interrupt flag)
- Interrupt flag: thread.interrupt() = polite "please stop" signal (used on app shutdown, task cancel). Blocking calls (get, sleep) throw InterruptedException AND CLEAR the flag. If I catch it and don't restore it, code higher up never knows a stop was requested -> thread keeps running, shutdown hangs. Fix: Thread.currentThread().interrupt() in the catch
- 12b v3: producer clean (sync get 5 s, split catch with interrupt restore), yaml max.block.ms under producer.properties ✅. Handler still 500 -> 503 pending
- 12b test (Kafka stopped): got 500 with the GENERIC message -> the catch-all handled it, not OrderPublishException
  - Likely cause: with no cached topic metadata (app just restarted), send() ITSELF blocks max.block.ms (5 s) and THROWS (unchecked Kafka exception) before get() is reached -> not in my catch list. Check IntelliJ "Unexpected error" stack for the class name, add it to the catch
  - Endless WARN "Connection to node -1 ... could not be established" = the CONSUMER reconnecting in the background. Normal, it resumes when Kafka is back. Not caused by the curl
- CONFIRMED from log: send() threw org.springframework.kafka.KafkaException: Send failed, Caused by TimeoutException: Topic orders not present in metadata after 5000 ms (= max.block.ms working). KafkaException is unchecked and NOT in my catch -> catch-all 500. Fix: also catch KafkaException (org.springframework.kafka.KafkaException, not the Apache one)
- 12b DONE: Kafka stopped -> POST /orders -> 503 after ~5 s
  - TWO waits: Phase 1 inside send() = fetch metadata + buffer, limit max.block.ms, fails with KafkaException (no future returned). Phase 2 = get(timeout) waits for broker ack, fails with TimeoutException/ExecutionException
  - Phase 1 fails when the app never reached Kafka since restart; Phase 2 fails when Kafka died after metadata was cached -> catch BOTH
- 12c: Redis down on GET -> org.springframework.data.redis.RedisConnectionFailureException: Unable to connect to Redis (root: ConnectException: Connection refused :6379). Spring translates the Lettuce error into its own exception -> handle RedisConnectionFailureException -> 503
- 12c DONE: Redis down -> GET -> 503 via @ExceptionHandler(RedisConnectionFailureException)
- STEP 12 DONE (12a consumer retry + DLT, 12b producer sync send + 503, 12c Redis down 503)
- Rule: return ex.getMessage() only for MY OWN exceptions (I control the text, library details stay in cause -> log). Library exceptions (Redis/Lettuce) -> fixed text. Redis handler now returns 'Order storage unavailable'
- Log flood when Kafka is down (NetworkClient WARN every ~1 s per client) -> real cost in Splunk/Datadog. Options:
  1. Slow the reconnects: spring.kafka.properties.reconnect.backoff.max.ms (default 1000) -> e.g. 30000. Backoff grows exponentially up to this max -> far fewer attempts + logs
  2. Raise level for ONLY that logger: logging.level.org.apache.kafka.clients.NetworkClient: error (keeps other Kafka warnings)
  3. Logback DuplicateMessageFilter (turbo filter) to drop repeated identical messages
  4. Detect outages with metrics/alerts (Actuator health, consumer lag), not by reading log lines
- Log flood fix applied: NetworkClient: error -> ZERO reconnect logs (all were WARN). Trade-off: no signal at all that Kafka is down. reconnect.backoff.max.ms set to 3000 (3 s, I suggested 30000). Balanced option: keep NetworkClient at warn + backoff 30000 -> ~1 line per 30 s per client
- mvn install with Kafka STOPPED: test contextLoads starts the full app -> KafkaAdmin tries to create NewTopic beans at startup -> no broker -> ~30 s wait -> ERROR 'Could not configure topics' TimeoutException. Root problem: tests depend on REAL Kafka/Redis running -> fixed in Step 13 with embedded Kafka. Quick workaround: docker start kafka, or mvnw install -DskipTests
- Backoff verified in startup log: AdminClient reconnect gaps 0.1 -> 0.1 -> 0.2 -> 0.5 -> 0.8 -> 1.6 -> 3.2 -> 6.5 -> 12.5 s = exponential backoff (doubles each time) up to reconnect.backoff.max.ms. Before (max 1 s) gaps stayed ~1 s forever
- Analogy: backoff = calling a friend who doesn't answer. 1 s limit = redial every second (~3600 calls/h = 3600 log lines). 30 s limit = wait doubles 0.1, 0.2, 0.5, 1, 2, 4, 8, 16, 30, 30... (~120/h). Same signal, ~30x fewer lines. Kafka back -> next call works, auto-reconnect
- Package refactor DONE: config/KafkaConfig (topics + error handler), repository/OrderRepository (@Repository), service/messaging/OrderProducer + OrderConsumer. Annotations don't decide packages; packages are only for readers
- Later (Step 14): consider package-by-feature (order/, payment/, notification/, common/)
- Package refactor verified: mvn install BUILD SUCCESS (Tests run: 1, Failures 0)
- Red lines in build = WARNINGS printed on stderr (IntelliJ shows stderr in red), not errors:
  - sun.misc.Unsafe ... guice-5.1.0 -> IntelliJ's bundled Maven uses an old internal Java API, Java 25 warns. Harmless
  - Mockito self-attaching / "Java agent loaded dynamically" -> Mockito attaches itself at runtime; future JDKs will block this. Fix later: add Mockito as -javaagent in maven-surefire-plugin argLine
  - "Sharing is only supported for boot loader classes" -> side effect of the agent, harmless
  - "Picked up JAVA_TOOL_OPTIONS" -> an env variable on my machine, info only
- Mockito agent DONE: maven-dependency-plugin `properties` goal + surefire argLine -javaagent:${org.mockito:mockito-core:jar} -> "self-attaching" + "dynamic agent" warnings GONE. BUILD SUCCESS
  - IntelliJ red underline on ${org.mockito:mockito-core:jar} = property exists only during build. Hide with <!--suppress MavenModelInspection -->
  - Remaining "Sharing is only supported for boot loader classes" = JVM class-data sharing (CDS) note caused by any -javaagent. Harmless. Can hide with -Xshare:off in argLine
- mvnw from cmd: BUILD SUCCESS, NO "JAVA_TOOL_OPTIONS" line, NO sun.misc.Unsafe warning -> both come from IntelliJ's Maven runner only. But it re-downloaded EVERYTHING (2 min) -> went to C:\Users\ajynk\.m2\repository because cmd Maven doesn't use D:\software\essentials\settings.xml
  - Fix: copy settings.xml to C:\Users\ajynk\.m2\settings.xml (Maven's default user settings) -> all Maven runs use D:\dependencies\m2. Then delete C:\Users\ajynk\.m2\repository
- Profiles: renamed application.yaml -> application-local.yaml. A file named application-<profile>.yaml is loaded ONLY when that profile is active. With no application.yaml and no active profile -> no config at all -> placeholders like ${app.kafka.topics.orders.name} fail, startup + test fail
  - Usual setup: application.yaml = shared settings (all environments), application-local.yaml = local-only overrides (localhost Kafka/Redis)
  - Activate: IntelliJ Run config "Active profiles: local", or env SPRING_PROFILES_ACTIVE=local, or mvnw spring-boot:run -Dspring-boot.run.profiles=local. Tests: @ActiveProfiles("local")
- Test profile: src/test/resources/application.yaml with spring.profiles.active: local (option #1) -> tests use application-local.yaml, main app unaffected

## Step 13: Tests with embedded Kafka
- spring-boot-starter-kafka-test already in pom -> gives @EmbeddedKafka (a real Kafka broker started INSIDE the test JVM, no Docker)
- Embedded broker address is exposed as property ${spring.embedded.kafka.brokers}
- Redis in tests: replace OrderRepository with a mock (@MockitoBean) -> test needs no Redis either
- Async check: consumer runs on another thread -> verify(mock, timeout(5000)).save(...) waits up to 5 s
- 13a RESULT: OrderFlowTest PASSED ✅ profile "test", bootstrap.servers = [localhost:54965] (embedded broker, random port), consumer logged Key 1 Laptop P0 O0, verify(save) succeeded. No Docker needed
- KafkaRedisDemoApplicationTests (contextLoads) FAILED: test application.yaml was renamed to application-test.yaml -> contextLoads has no profile -> no config -> placeholder error. Fix: give it @ActiveProfiles("test") + @EmbeddedKafka, or delete it (OrderFlowTest already starts the whole context)
- 13a DONE: mvn install BUILD SUCCESS with Docker stopped (contextLoads test removed/fixed). Next 13b: DLT test (mock save throws -> record in orders-dlt)

## Step 14: Final project (Order -> Payment -> Notification)
Flow:
POST /orders -> topic orders --+--> OrderConsumer        (group order-service)        -> Redis order:<id> = data, status CREATED
                               +--> PaymentConsumer      (group payment-service)      -> "charge" -> topic payments (PAID / FAILED)
topic payments ----------------+--> NotificationConsumer (group notification-service) -> log "notify customer" + Redis status PAID/FAILED
GET /orders/{id}/status -> Redis
- Key idea: 2 DIFFERENT groups on orders -> each gets ALL messages (Step 1 example, now real)
- Same key (orderId) on both topics -> events of one order stay in order
- 14a review (PaymentConsumer):
  - ✅ @KafkaListener on orders with own groupId -> gets ALL order messages independently of order-service
  - Missing: payment rule. It forwards orderData as-is -> should send PAID / FAILED
  - null value -> value.contains("fail") would throw NullPointerException -> check null first
  - Send code copied from OrderProducer -> duplication. Better: one reusable publish method (e.g. a small producer class taking topic, key, value)
  - Exception thrown inside a listener -> DefaultErrorHandler retries -> after retries record goes to orders-dlt (same as Redis failure)
  - Unused import java.util.Objects
- 14a v2: payment rule added (null/blank/contains fail -> FAILED, else PAID), sends status to payments. Pending: duplicated send code, simplify status with ternary, log 'Payment X for order Y'
- 14a review v3:
  - BUG: payment group-id = order-service (copy-paste) -> PaymentConsumer + OrderConsumer in the SAME group on orders -> partitions SPLIT between them -> each gets only part of the orders! Different services MUST have different group ids
  - `spring.kafka.consumers.*` (plural) is NOT a Spring Boot property -> the real spring.kafka.consumer.group-id was removed -> OrderConsumer (no groupId in annotation) has no group. Own custom keys belong under app.*, not spring.* (spring.* = Boot's namespace)
  - auto-offset-reset: latest is in yaml but not used: needs properties = "auto.offset.reset=${...}" in @KafkaListener
  - Status PAID ✅ (ternary). Keyless orders still paid -> skip them
- 14a v4: payment group-id fixed to payment-service, BUT OrderConsumer now also uses ${spring.kafka.consumers.payment.group-id} -> both in payment-service -> same split bug. OrderConsumer must use ...order.group-id. Note: placeholders resolve ANY key, so spring.kafka.consumers.* works technically, but app.* is the right home for own keys
- 14a v5: group ids fixed (order-service / payment-service). Pending: auto.offset.reset via properties attr, skip keyless in PaymentConsumer, add consumers.* keys to application-test.yaml (else tests fail: unresolved placeholder)
- 14a v6 OK: guard clause skips keyless orders; global spring.kafka.consumer.auto-offset-reset: latest (local) so new payment-service group won't replay history; test yaml keeps earliest (test sends before listener may be ready) + has consumers.* keys
- 14a DONE. Next 14b NotificationConsumer
- 14b review: NotificationConsumer on payments, own group notification-service in both yamls ✅, guard clause ✅
  - status.equals("FAILED") -> NullPointerException if value is null. Use "FAILED".equals(status): constant first, null-safe
  - else = "confirmed" for ANYTHING not FAILED (old "PASSED" messages, typos, garbage) -> check PAID explicitly, warn for unknown values
  - last log "Payment X for order Y" repeats PaymentConsumer's text -> confusing when searching logs, remove
- 14b v2: switch with 'case null, default' (Java 21+ switch handles null without NPE) ✅. BUG: case "PASSED" but PaymentConsumer sends "PAID" -> every successful payment logged as Unknown. Shared values belong in ONE place (enum or constants class) so producer and consumer can't drift
- valueOf vs from: PaymentStatus.valueOf("PASSED") -> IllegalArgumentException, valueOf(null) -> NullPointerException -> exception inside listener -> retries -> DLT. Safe converter from() returning null for unknown is better for data coming from outside
- 14b v3 review: `String status = from(...)` -> from returns PaymentStatus -> type must be PaymentStatus (compile error). Enum: extra `name` field + constructor duplicate the built-in name() -> remove; unused import Objects. PaymentConsumer still types "PAID"/"FAILED" strings
- 14b v4: enum simplified (PAID, FAILED + null-safe from()), NotificationConsumer switches on enum ✅. Pending: PaymentConsumer still uses string literals; warn log prints status (null) -> print record.value() to see the bad text
- 14b DONE ✅ log proof:
  - 3 groups, 3 listener threads: order-service (orders), payment-service (orders), notification-service (payments 0,1)
  - payment-service + notification-service new with latest -> NO history replay
  - Order 23 failCard -> PaymentConsumer FAILED -> payments-0 -> "Notify customer: payment FAILED for order 23"
  - Order 22 Shoes -> PAID -> payments-1 -> "order 22 confirmed". Whole chain ~30 ms
  - For 22, PaymentConsumer logged BEFORE OrderConsumer -> different groups run in parallel, NO ordering between groups. Order is only guaranteed within one partition for one consumer
- 14c review:
  - OrderStatus enum doesn't exist yet: it was a suggestion -> create OrderStatus { CREATED, PAID, FAILED }. Separate from PaymentStatus: payment result vs order lifecycle
  - BUG: NotificationConsumer saves status AFTER the switch with Objects.requireNonNull(status) -> unknown status = null -> NullPointerException inside listener -> retries -> DLT. Save only in PAID/FAILED branches
  - RACE CONDITION: OrderConsumer (CREATED) and NotificationConsumer (PAID) run in parallel groups. If CREATED is written AFTER PAID, it overwrites it -> order stuck at CREATED. Seen in 14b log: PaymentConsumer ran before OrderConsumer
    - Fix: write CREATED only if no status exists yet -> Redis SETNX = opsForValue().setIfAbsent(key, value)
- 14c v2: OrderStatus enum + saveStatusIfAbsent (setIfAbsent) added ✅. But: saveStatus now calls setIfAbsent when status is null/blank, then set anyway -> mixed logic, and Redis rejects null. Keep 2 clean methods: saveStatus = always overwrite (PAID/FAILED), saveStatusIfAbsent = first write only (CREATED). Caller decides. OrderConsumer still uses saveStatus("CREATED") -> race not fixed. NotificationConsumer save still after switch
- 14c v3 OK: saveStatus = overwrite, saveStatusIfAbsent = CREATED only (race fixed), status saved inside PAID/FAILED branches (null crash fixed). requireNonNull inside branches redundant
- Why setIfAbsent: OrderConsumer (CREATED) and Payment->Notification (PAID) run in parallel. If PAID lands first, a plain set of CREATED would overwrite it -> order stuck at CREATED. CREATED = starting value -> write only if absent. PAID/FAILED = result -> always overwrite. (Parcel tracker: "placed" must never overwrite "delivered")
- Kafka can NOT control which GROUP processes first. Order is guaranteed only within one partition for one consumer. Options: chain steps via a new topic (order-created), one consumer doing both, or make writes order-independent (what I did). Rule: don't rely on timing
- 14c DONE ✅: /orders/24/status PAID, /orders/25/status FAILED, /orders/999/status 404. STEP 14 COMPLETE (Order -> Payment -> Notification + status in Redis)
- Postman collection has a fixed _postman_id -> re-importing offers Replace instead of creating a duplicate
- Cleanup 1: KafkaPublisher.publish(topic, key, value) shared by OrderProducer + PaymentConsumer, OrderPublishException -> PublishException ✅. Leftovers: unused KafkaTemplate field in OrderProducer; publisher param/log names still order-specific (orderId/orderData) -> key/value + log the topic
- Cleanup 1 DONE: generic publish(topic,key,value), logs with topic=/key=/partition/offset (no value logged), PublishException(topic,key,cause). Leftover: unused import KafkaTemplate in OrderProducer. Note: 503 handler returns ex.getMessage() which now contains the internal topic name -> use a fixed client message
- Cleanup 1 verified: orders 26 (PAID) / 27 (FAILED). Same KafkaPublisher used from HTTP thread [nio-8080-exec-x] (orders) and listener thread [ntainer#2] (payments) -> thread name in log shows the caller
- Cleanup 2 DONE: consumers under app.kafka, BUILD SUCCESS, 3 groups assigned. Only ERROR = embedded Kafka shutdown 'Error deleting ...\Temp\kafka-xxx ... being used by another process' -> Windows can't delete files still open (memory-mapped index files). Harmless, test passed. Leftover folders in %TEMP%\kafka-* can be deleted manually
- Full code review (25 Oct):
  1. Two config styles: @Value topic names in OrderProducer/PaymentConsumer while AppKafkaProperties exists -> inject the record
  2. KafkaConfig: 3 identical NewTopic beans -> private helper newTopic(Topic t)
  3. OrderConsumer still saves keyless records (noKey...) + status CREATED, while PaymentConsumer skips them -> inconsistent; skip in both
  4. Redis prefixes "order:" / "order-status:" repeated (cleanup 3)
  5. Order + PaymentConsumer log full value at INFO (may be sensitive); NotificationConsumer uses debug -> make consistent (debug)
  6. GET /status returns Order{key,value} -> a small response record (orderId, status) is clearer
  7. Unused: Consumers record (nothing reads it), @Slf4j in OrderProducer
- Review fix 1 DONE: OrderConsumer skips keyless (offset 17 test: both consumers warn+skip, nothing saved/paid). Leftover: duplicate warn 'Key was null or Blank' + new skip warn -> keep one. PaymentConsumer record log still INFO
- Review fix 2 DONE: prefixes as constants. Polish: make them private (internal detail of the repository), names ORDER_PREFIX/STATUS_PREFIX, same param name everywhere (orderId), unused @Slf4j
- Annotation attributes (@KafkaListener topics/groupId) must be COMPILE-TIME CONSTANTS -> can't call props.topics()... there. Only string literals with ${...} placeholders (resolved by Spring at startup). Listeners keep placeholders; AppKafkaProperties only in normal code (OrderProducer, PaymentConsumer publish)
- Field initializer 'private final String topicName = props.topics()...' runs BEFORE the constructor sets props -> compile error 'variable props might not have been initialized' (or NPE). Read props inside the method instead
- Review fix 3 DONE: OrderProducer/PaymentConsumer read topic names from AppKafkaProperties inside the method. Order 28 full chain OK, record logs now DEBUG in all 3 consumers (fix 5 also done)
- Review fix 4 DONE: private newTopic(Topic) helper, each @Bean is 1 line
- Review fix 6 DONE: record OrderStatusResponse(orderId, status) -> JSON {"orderId":..,"status":..}. Records serialize to JSON with Jackson out of the box (component names = JSON fields)
- Logging additions (my idea):
  - Repository: keep @Slf4j, log.debug key + operation (not the value) e.g. "Redis SET order:24", "Redis GET order-status:24 -> found/missing"
  - Incoming requests, 3 options:
    a) log.info at start of each controller method (simple, repeats)
    b) Spring's built-in CommonsRequestLoggingFilter bean + logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter: debug (no code beyond 1 bean, logs method/URI/query)
    c) custom OncePerRequestFilter: log method, URI, response status, duration in ms in ONE line (best for production)
  - Don't log request bodies / sensitive values
- Repository logging added (Claude, one-time on request): log.debug per operation with Redis key; order VALUE never logged (may be customer data), status values logged (not sensitive); SETNX logs written vs skipped -> shows the race protection working
- Repository logs now key-only (no status values). Logs are placed AFTER the Redis call -> if the call throws, the line is never reached -> 'SET' log only means success. Failures are logged by the caller's handler (ApiErrorHandler / DefaultErrorHandler)
- RequestLoggingFilter review: structure ✅ (OncePerRequestFilter, try/finally, doFilter). Bugs: duration not logged (start unused); getParameterNames().toString() prints the Enumeration OBJECT (class@hash), not the names -> Collections.list(...) gives a List with readable toString
- Request logging (option C) DONE: "Method POST, URI /orders, params [orderId, orderData], Status 200 took 266ms" (param NAMES only, no values)
  - First request slower (266 ms): DispatcherServlet init + first Kafka metadata fetch. Later requests faster
  - Redis lines ~600 ms later on first use: Lettuce opens the Redis connection lazily on the FIRST command
  - "Redis SETNX order-status:24 -> skipped (status already exists)" -> PAID was already there (order 24 reused + notification wrote first). setIfAbsent protection visible in the log
  - Request log line comes AFTER the consumer lines: filter logs in finally, i.e. after the whole request finished (incl. the sync Kafka send)

## Step 15.1 Postgres in Docker
- Image postgres:18.6-alpine3.24 (pinned). PG18 data path = /var/lib/postgresql (not .../data)
- Empty `pgdata:` under top-level volumes = named volume with defaults; survives down, removed by down -v
- Container exited -> typos POSTGRESS_USER / POSTGRES_PASSOWRD; image exits if POSTGRES_PASSWORD missing. Check `docker logs postgres`

## Step 15.2 JPA + Postgres
- Deps: spring-boot-starter-data-jpa + org.postgresql:postgresql (runtime), versions from Boot (Hibernate 7.4.5, driver 42.7.13)
- "Unable to determine Dialect without JDBC metadata" = Hibernate could NOT connect (here: username/password missing). Real cause is in an earlier WARN line
- Success signs: "HikariPool-1 - Start completed", "Database dialect: PostgreSQLDialect", "Database version: 18.6"
- spring.jpa.open-in-view: false -> REST API has no views; keeps DB connection from being held for the whole request
- "Multiple Spring Data modules found, strict mode" (JPA + Redis): JPA repos must be for @Entity classes
- Open-in-view (OSIV): keeps JPA session + DB connection open for whole HTTP request so lazy fields load during JSON writing. Off -> session closes after @Transactional method; touching lazy field later = LazyInitializationException -> fix with DTO built in service or @EntityGraph. For REST: set spring.jpa.open-in-view: false (frees connections sooner)
- App timezone: TimeZone.setDefault(GMT) in main() -> logs end with Z. Instant is always UTC anyway; avoid LocalDateTime. Tests don't run main() -> use -Duser.timezone=UTC in surefire argLine if needed

## Step 15.3 Entities + repositories
- Entity: @Entity @Table, @Id, @GeneratedValue(IDENTITY) -> Postgres "generated by default as identity"
- Lombok on entities: @Getter @Setter @NoArgsConstructor, NOT @Data (equals/hashCode/toString issues)
- String -> varchar(255) by default; @Column(columnDefinition = "text") for unbounded text
- @Column(nullable = false) -> NOT NULL; columns nullable by default
- ddl-auto update: adds tables/columns only, never alters types/constraints -> drop table + restart
- Derived query: findTop100ByPublishedAtIsNullOrderByIdAsc() -> WHERE published_at IS NULL ORDER BY id ASC LIMIT 100
- @Repository not needed on JpaRepository interfaces
```
docker exec -it postgres psql -U app -d orders_db -c "\d orders" -c "\d outbox"
docker exec -it postgres psql -U app -d orders_db -c "drop table orders, outbox;"
```

## Step 15.4 OrderService @Transactional
- Checked exception (extends Throwable/Exception) forces try/catch; wrapping in RuntimeException hides it from @ExceptionHandler -> 500. Use RuntimeException
- No try/catch in @Transactional service: RuntimeException escaping -> rollback
- Use org.springframework.transaction.annotation.Transactional; only works when called from another bean (proxy)
- save() with assigned @Id = merge -> extra SELECT + silent overwrite; existsById + 409. Race -> PK violation -> DataIntegrityViolationException -> 409
- Postgres down -> CannotCreateTransactionException -> 503; Hikari waits connection-timeout (default 30s) -> set 2000
- Never return/log ex.getMessage() of DB exceptions (contains SQL + values)
- Timezone: TimeZone.setDefault in main did not change Logback timestamps; reliable = -Duser.timezone=UTC (JVM flag / Docker ENTRYPOINT)
```
curl -i -X POST "http://localhost:8080/orders?orderId=101&orderData=Phone"   # 200, again -> 409
docker stop postgres   # POST -> 503
docker start postgres
docker exec -it postgres psql -U app -d orders_db -c "select * from orders" -c "select id, topic, message_key, published_at from outbox"
```

## Step 15.5 Why outbox + relay
- Dual-write problem: DB save + Kafka send are 2 systems, no shared transaction -> crash/outage in between = lost or out-of-sync
- Outbox: order row + outbox row in ONE DB transaction (both or none); request never touches Kafka -> works when Kafka down
- Relay = background job (@Scheduled) passing outbox rows to Kafka, then marking published_at. Kafka down -> rows wait, retried next run
- Analogy: letter in post-office outbox tray (receipt given), courier collects tray periodically
- Side effect: crash after send, before mark -> sent again (at-least-once) -> consumers must be idempotent (15.6)
- 15.5 verified: Kafka down -> POST 200 (26 ms, no Kafka in request), relay warn every ~6 s (5 s max.block + 1 s poll), Kafka up -> "Published key=104" -> consumers -> PAID. eventId header visible with print.headers=true
- @EnableScheduling needs @Configuration class (else silently ignored)
- @Scheduled(fixedDelayString="${...}") + plain for-loop + try/catch PublishException + break (forEach lambda cannot break -> ordering)
- Spring Kafka LoggingProducerListener logs key + PAYLOAD at ERROR on send failure -> customer data in logs
```
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders --from-beginning --property print.key=true --property print.headers=true
```

## Step 15.6 Idempotent consumer (PaymentConsumer)
- Problem: relay is at-least-once -> same order can arrive twice -> double payment
- eventId header (= outbox id) read with @Header(name=EVENT_ID, required=false); null for old messages -> process normally
- Order: key check -> if processed (Redis hasKey processed:<group>:<eventId>) skip -> pay + publish -> markProcessed (SET with TTL 7 days)
- Mark AFTER success (mark before + failure = payment lost on retry)
- OrderConsumer already idempotent (SET / SETNX give same result twice)
- RedisTemplate<Object,Object> = Java serialization -> unreadable keys; use StringRedisTemplate
- @Value on final field + @RequiredArgsConstructor does not inject -> non-final field
```
docker exec -it redis redis-cli KEYS "processed:*"
docker exec -it redis redis-cli TTL processed:payment-service:<eventId>
```
- 15.7 replay test: `update outbox set published_at = null where message_key='105'` -> relay resends (new offset 9) -> PaymentConsumer "Redis Exists ... found" -> "Skip duplicate eventId=4"; OrderConsumer re-SET harmless, SETNX skipped; no 2nd PAID/notify

## Multi-instance (2 pods) - notes
- Works: HTTP via load balancer (stateless), same group-id -> partitions split (max parallelism = partitions), same key -> same partition -> same pod, PK race -> 409, idempotency keys shared in Redis
- Breaks/changes: OutboxRelay runs in every pod (duplicates) -> ShedLock or FOR UPDATE SKIP LOCKED or Debezium; DB connections = pool x pods; ddl-auto update unsafe -> Flyway/Liquibase; outbox grows -> cleanup job; config/secrets via env; central logs + trace ID; health probes (Actuator)
- Flyway: versioned SQL files (V1__create_orders.sql) in db/migration, applied once, tracked in flyway_schema_history, DB lock so one pod migrates; Hibernate ddl-auto validate
- ShedLock: @SchedulerLock(name=...) + shedlock table; one pod gets the lock per run; lockAtMostFor = takeover if pod dies

## Next plan: 2 local instances (steps 1-3 verified)
- 2nd IntelliJ run config (Ctrl+D copy) + VM option -Dserver.port=8081; both share Kafka/Redis/Postgres
- 2nd instance joins -> rebalance: 8080 "partitions revoked" (all) -> "partitions assigned" (subset)
- Result: payment-service 8080=[orders-0,1] 8081=[orders-2]; order-service 8080=[orders-2] 8081=[orders-0,1]; notification-service 8080=[payments-0] 8081=[payments-1]
- Each GROUP splits independently -> order and payment of same orderId may run in different instances
- describe: CONSUMER-ID unique per instance (CLIENT-ID same name in both), LAG 0
```
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group payment-service
```
- clientIdPrefix = "payment-${server.port:8080}" on each @KafkaListener -> CLIENT-ID payment-8080-0 / payment-8081-0 (-0 = consumer number). K8s: use ${HOSTNAME}. Not global spring.kafka.client-id (duplicate client registration warning)
- describe columns: GROUP | TOPIC | PARTITION | CURRENT-OFFSET (next offset group will read = committed) | LOG-END-OFFSET (next offset to be written = total msgs) | LAG (end - current = unread) | CONSUMER-ID (client-id + unique suffix per join) | HOST (consumer IP as seen by broker; 172.18.0.1 = Docker gateway = your PC) | CLIENT-ID (readable name)
- Step 4 result: POST 201 to 8080 -> BOTH relays read eventId 5 before it was marked (8080 first send ~270ms) -> Kafka offsets 19 + 20 = duplicate. OrderConsumer: same SET twice harmless, SETNX skipped. PaymentConsumer: 2nd -> "found" -> Skip duplicate eventId=5 (paid once)
- 202 posted to 8081, partition 2 owned by 8081; its payments-0 owned by 8080 -> notification logged in 8080 (one order across both instances)
- Assignment can change after each restart/rebalance

## Redis role (flow)
1. POST -> Postgres only  2. Relay -> Kafka, no Redis
3. OrderConsumer: SET order:<id>, SETNX order-status:<id> CREATED
4. PaymentConsumer: EXISTS processed:payment-service:<eventId> (skip dup) -> pay -> SET processed... "1" TTL 7d
5. NotificationConsumer: SET order-status:<id> PAID/FAILED
6. GET /orders/{id} + /status read Redis
Roles: read store, status tracker, idempotency memory. Postgres = permanent record

## ShedLock on OutboxRelay
- Deps shedlock-spring + shedlock-provider-jdbc-template 7.10.1 (tested with Boot 4 / Spring 7)
- Table created manually (ddl-auto validate): CREATE TABLE shedlock(name VARCHAR(64) PK, lock_until, locked_at, locked_by)
- LockProvider bean: JdbcTemplateLockProvider + usingDbTime() (DB clock, not instance clocks); @EnableSchedulerLock(defaultLockAtMostFor="30s"); @SchedulerLock(name="outboxRelay", lockAtMostFor="30s") - must exceed slowest run (~5s Kafka timeout)
- Own DataSource (option 2): app.datasource.postgres (url/user/pass) -> @Primary DataSourceProperties bean; app.datasource.postgres.configuration (Hikari) -> @Bean("postgresDataSource") @Primary HikariDataSource via initializeDataSourceBuilder() (url -> jdbc-url). Boot 4 package org.springframework.boot.jdbc.autoconfigure.DataSourceProperties. LockProvider uses @Qualifier("postgresDataSource")
- Multiple DataSources: by-type injection -> NoUniqueBeanDefinitionException unless @Primary/@Qualifier; 2nd DB beans without @Primary (defaultCandidate=false)
- shedlock row: lock_until ~= locked_at after run -> lock released right after the run (lockAtLeastFor = 0); times in UTC (usingDbTime)
- Postgres down: lock call fails BEFORE method -> scheduler logs full stack trace each run (method try/catch can't catch it)
```
docker exec -it postgres psql -U app -d orders_db -c "select * from shedlock"
```
- ShedLock 2-instance test: POST 302 -> only 8080 relay published (offset 21, once), no Skip duplicate; 8081 only did notification (payments-1)
- ShedLock Kafka-down test: relay runs ALTERNATE between 8080/8081 (never overlap) -> ShedLock = no concurrent run, not "always same pod". Kafka back -> 303 published ONCE (8080, offset 22), no Skip duplicate
- After Kafka restart, payment-service got partitions before order-service -> PAID written first -> SETNX CREATED skipped (race protection again)

## Outbox cleanup job
- service/OutboxCleanupService (@Service: no Kafka -> not messaging). OutboxRelay moved to service/OutboxRelayService, lock name kept "outboxRelay"
- yaml app.outbox.cleanup-cron (6 fields: sec min hour day month weekday, JVM tz = UTC) + retention (Duration: 1m, 7d)
- Repo: @Modifying @Query("delete from OutboxEvent e where e.publishedAt < :cutOff") int deletePublishedBefore(@Param("cutOff") Instant) -> one SQL DELETE (derived deleteBy loads rows first). JPQL uses entity/field names; param names case-sensitive; interface method = declaration only
- Method: @Scheduled(cron) + @SchedulerLock(name="outboxCleanup", lockAtMostFor="10m") + @Transactional (needed for @Modifying); cutoff = Instant.now().minus(retention) (NOT toDays(): 1m/12h -> 0 days -> deletes all)
- @Scheduled ignores return value -> void
- Verified: "Outbox cleanup deleted 8 rows older than ..." -> published 0 / pending 0; shedlock rows outboxCleanup + outboxRelay

## Scheduler error handler
- @Bean ThreadPoolTaskScheduler taskScheduler() (name taskScheduler -> used by @EnableScheduling): setPoolSize(2) (relay + cleanup), setThreadNamePrefix("scheduling-"), setErrorHandler(t -> log.warn("Scheduled task failed: {}", t.getMessage())); Spring calls initialize()
- Postgres down: before = 60-line stack trace per run; after = 1 WARN line every ~3s (2s Hikari timeout + 1s poll). Threads scheduling-1/-2 = pool of 2
- 10x "Failed to validate connection" once = Hikari discarding its 10 dead pooled connections

## Central logging
- grafana/otel-lgtm:0.40.0 = Grafana(3000, admin/admin) + Loki(logs) + Tempo(traces) + Prometheus(metrics) + OTel collector (4317 gRPC, 4318 HTTP)
- spring-boot-starter-opentelemetry + management.tracing.sampling.probability: 1.0 (default 0.1) -> [traceId-spanId] in log lines
- Windows cmd: cls (not clear)
```
docker compose up -d otel-lgtm
docker logs otel-lgtm --tail 5
```
- spring.kafka.template.observation-enabled + spring.kafka.listener.observation-enabled = true (under spring.kafka, NOT app.kafka) -> trace context in Kafka headers
- Result: POST trace A (a18d...), relay trace B (3976...) = separate background job (outbox breaks the chain by design); OrderConsumer/PaymentConsumer/NotificationConsumer all trace B, each its own span ID
- Log format [traceId-spanId]: traceId = whole flow, spanId = one step
- protobuf "sun.misc.Unsafe" WARNING on JDK 25 = from OTLP exporter lib, harmless (optional JVM flag --sun-misc-unsafe-memory-access=allow)
- OTel logback appender version must match Boot's OTel SDK: Boot 4.1.1 -> SDK 1.62.0 -> appender 2.28.0-alpha (2.21.0-alpha = SDK 1.55 -> "method does not exist" on startup)
- logback-spring.xml: include base.xml (keeps Boot console format + logging.level) + OTEL appender + CONSOLE on root
- yaml trap: management.opentelemetry.logging.export.otlp.endpoint must be indented under management (root-level "opentelemetry:" ignored -> no logs in Loki, metrics still arrive)
- OpenTelemetryAppenderInstaller (InitializingBean): OpenTelemetryAppender.install(openTelemetry)
- Loki (Explore, Code mode): {service_name="kafka-redis-demo"} |= "405"
- Outbox splits traces: POST = trace A, relay + Kafka + consumers = trace B. Link options: 1) log business key (orderId/eventId) in both; 2) store trace parent in outbox -> relay continues trace A; 3) OTel span link
- CL5: OrderService logs "Order saved orderId=407 eventId=15" in trace A (92c5...); relay/consumers in trace B (ec87...) show key=407 + processed:payment-service:15 -> one Loki search |= "407" shows both
- CL6: outbox.trace_parent VARCHAR(55) (manual ALTER, ddl validate). W3C traceparent = "00-" + traceId(32 hex) + "-" + spanId(16 hex) + "-01"(sampled). OrderService: tracer.currentSpan() (may be null) -> span.context().traceId()/spanId(). Verified: row 16 trace_parent 00-13827c36...-98d02b46...-01 = POST log trace/span
- Mistake to avoid: concatenating object toString() (span.context(), tracer) -> >55 chars -> insert fails -> whole order rolled back
- CL6 verified: one trace cbd7b4de... from POST "Order saved 409" -> relay Published -> OrderConsumer/PaymentConsumer/payments/NotificationConsumer. Only "Relaying N events" keeps relay's own trace
- Relay: parse trace_parent -> tracer.traceContextBuilder().traceId/spanId/sampled(true) -> tracer.spanBuilder().setParent(ctx).name("outbox.publish").start() -> try (SpanInScope = tracer.withSpan(span)) publish -> finally span.end(); catch span.error(e)
- Never return null span: fallback tracer.nextSpan().name(...).start() (old rows / malformed value)
- getSpan null-safe verified: 411 (no trace_parent) + 412 ('bad-value', 2 parts) -> relay's own trace (real IDs, not zeros), no "Scheduled task failed". Before fix: withSpan(null) -> zeros trace + NPE in finally (row was saved as published, but loop aborted)
- Traces need their own endpoint: management.opentelemetry.tracing.export.otlp.endpoint: http://localhost:4318/v1/traces (no default; metrics default 4318, logs need endpoint). Sampling stays at management.tracing.sampling.probability (NOT under opentelemetry)
- Tempo node graph (order 414): http post /orders -> outbox.publish -> orders send -> orders process x2 (order-service, payment-service) -> Redis set/exists spans (Lettuce auto-instrumented) -> payments send -> payments process -> set
- orders process self time ~420ms while Redis spans ~2ms = lazy first Redis connection (seen earlier in logs). "orders send" negative self time = async children end after the send span (normal for messaging)
- Tempo: use "Trace ID" tab for an ID (TraceQL tab expects a query)
