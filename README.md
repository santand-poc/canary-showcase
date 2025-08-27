# Canary Lite Showcase 🚦🐤

> **Cel:** Pokazać developerom w prosty sposób **jak działają feature flagi + canary release + shadow traffic** na żywym przykładzie.  
> **Dla kogo:** developerzy (Java/Spring), którzy nigdy nie bawili się Site Reliability Engineering, ale chcą ogarnąć jak wygląda *stabilne wdrażanie produkcji*.

---

## 🔑 Najważniejsze pojęcia (po ludzku)

- **Feature flag** – przełącznik w kodzie.  
  Możesz włączyć nowy kawałek logiki tylko dla części userów (np. 1%).  
  Dzięki temu nie musisz robić wielkiego „deploy all or nothing”.

- **Canary release** – stopniowe wpuszczanie ruchu do nowej wersji („kanarek w kopalni”).  
  Zaczynasz od 1%, potem 5%, 25%, aż do 100% – tylko jeśli jest zdrowo.

- **Shadow traffic** – wysyłasz żądanie równolegle do „starej” i „nowej” ścieżki, ale tylko obserwujesz różnice.  
  Użytkownik dostaje wynik „starej” wersji, a Ty w tle porównujesz decyzje.

- **Error rate** – jaki % żądań kończy się błędem (np. wyjątek 500).  
  Wzór: błędy / wszystkie żądania × 100%.

- **p95 latency** – „95. percentyl czasu odpowiedzi”.  
  Mówiąc po ludzku: 95% requestów działa szybciej niż ta wartość.  
  Jeśli p95 = 400 ms → oznacza, że 19 na 20 zapytań mieści się w 400 ms.

- **Diff rate** – ile % odpowiedzi różni się między starym i shadow’owym systemem.  
  Idealnie powinno być blisko 0% (systemy dają te same decyzje).

- **Autopilot** – mechanizm, który sam pilnuje czy można zwiększyć % ruchu na canary.  
  Jeśli warunki są OK → podnosi z 1% → 5% → 25% → 100%.  
  Jeśli jest źle → robi rollback do poprzedniego progu.

---

## 🔑 Co robi Site Reliability Engineering (w uproszczeniu):

### Pomiary i SLA/SLO
Nie „aplikacja działa albo nie działa”, tylko np. „95% requestów ma być obsłużonych w < 400ms w ostatnich 30s”.
Jak nie wyrabiasz — znaczy, że system jest „niespełniający umowy”.

### Error budget
Świadomie zakładasz ile awarii możesz mieć. Np. SLO = 99.9% uptime → budżet błędów to 0.1%.
Jak go przepalisz — stop z nowymi feature’ami, najpierw stabilność.

### Automatyzacja
Reguły typu canary, p95, error_rate → to jest klasyczny SRE vibe. Masz automaty, które zamiast człowieka decydują: „puszczamy więcej ruchu / rollback”.

### Monitoring + alerting
Nie czekasz aż klient zgłosi, że jest źle. Masz metryki, logi i alarmy.

### Blameless postmortems
Po awarii analizujesz przyczyny systemowe, a nie szukasz winnego człowieka.

---

## 📂 Struktura projektu

```

canary-showcase/
├── canary-lite/
│    ├── canary-core      # Silnik guardów (SpEL + Micrometer)
│    └── canary-spring    # Auto-config do Spring Boot
├── producer-app/         # „System zewnętrzny” do którego dzwonimy (może psuć odpowiedzi)
├── consumer-app/         # Klient, Feign + feature flagi + autopilot
└── README.md             # ten plik

````

---

## 🚀 Jak uruchomić

1. **Zbuduj wszystko**
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. **Odpal Producer POWERSHELL**

   ```bash
    .\mvnw -pl producer-app spring-boot:run
   ```

   Producer działa na **[http://localhost:9090/actuator/health](http://localhost:9090/actuator/health)**

3. **Odpal Consumer**

   ```bash
    .\mvnw -pl consumer-app spring-boot:run
   ```

   Consumer działa na **[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)**


## **Wejdź w UI**
   Otwórz przeglądarkę: [http://localhost:8080/index.html](http://localhost:8080/index.html)
   Zobaczysz dashboard z przyciskami (start/stop, autopilot, inject errors).

---

## 🖼️ UI — co widzisz?

Na stronie są dwa panele:

### Panel 1 — sterowanie

* **Start traffic** – generuje stały ruch klientów.
* **Stop** – zatrzymuje ruch.
* **Canary %** – możesz ręcznie ustawić ile userów trafia do nowej logiki.
* **Autopilot ON/OFF** – włącza/wyłącza automatyczne sterowanie.

### Panel 2 — status

* **Error rate (30s)** – % błędów w ostatnich 30 sekundach.
* **p95 latency (30s)** – 95. percentyl czasu odpowiedzi.
* **Diff rate (30s)** – % rozbieżnych odpowiedzi shadow vs primary.
* **Guard** – status reguły (PASS/FAIL/WAIT).
* **Canary %** – aktualny procent ruchu na nowej wersji.

---

## 🧪 Eksperymenty

1. **Normalny ruch**

    * Kliknij **Start traffic**.
    * Canary startuje z 1%.
    * Widzisz stabilne metryki: error \~0%, p95 niskie, diff \~0%.

2. **Wstrzyknięcie błędów**

    * Użyj panelu „Inject”: np. `err=0.2` (20% błędów).
    * W error rate pojawi się wzrost.
    * Autopilot cofnie % ruchu.

3. **Spowolnienie**

    * Inject `latency=1000` (1 sekunda).
    * p95 wystrzeli → guard FAIL.
    * Autopilot cofnie się.

4. **Rozbieżne decyzje**

    * Inject `diff=0.5` (50% różnic).
    * Diff rate pokaże \~50%.
    * Guard FAIL.

---

## ⚙️ Jak to działa pod spodem?

### 🔄 Diagram przepływu

```text
[ User ]
   │
   ▼
[ ConsumerApp (/consumer/do) ]
   │   ├── FeatureFlagService -> decyduje: control vs canary
   │   ├── ProducerClient (Feign) -> dzwoni do Producer
   │   ├── Zlicza metryki (Micrometer):
   │   │     - feature_flag_requests_total (ok/error)
   │   │     - shadow_diff_total (same/diff)
   │   │     - http.server.requests (latency)
   │   │     - http.client.requests (latency Feign)
   │   └── Zwraca JSON do UI
   │
   ▼
[ ProducerApp (/api/producer/process) ]
   │   ├── Może wstrzykiwać błędy (err%)
   │   ├── Może spowalniać odpowiedzi (latencyMs)
   │   └── Może wprowadzać różnice (diff%)
```

### 🔧 Guard Engine

Reguła w `application.yml`:

```yaml
canary:
  expr: >
    error_rate('consumer-service','release.new_rules_v2','canary','30s') < 0.8
    && p95('consumer-service','/consumer/do','30s') < 400
    && diff_rate('decision_delta','30s') < 10
  ladder: [1,5,25,100]
  passStreak: 2
  timeoutMs: 50
```

* Sprawdza co 2 sekundy (`@Scheduled`).
* PASS 2× pod rząd → zwiększa canary % (np. z 1 do 5).
* FAIL → rollback do niższego %.
* WAIT → nic nie robi (np. timeout albo brak danych).

---

## 📊 Metryki w Actuatorze

Dostępne endpointy:

* `http://localhost:8080/actuator/metrics` – lista metryk
  
Error rate:
* `http://localhost:8080/actuator/metrics/feature_flag_requests_total?tag=service:consumer-service&tag=flag:release.new_rules_v2&tag=segment:canary&tag=outcome:ok` - OK
* `http://localhost:8080/actuator/metrics/feature_flag_requests_total?tag=service:consumer-service&tag=flag:release.new_rules_v2&tag=segment:canary&tag=outcome:error` - ERROR

P95:
* `http://localhost:8080/actuator/metrics/http.server.requests.percentile?tag=uri:/consumer/do&tag=service:consumer-service&tag=phi:0.95`

Diff rate:
* `http://localhost:8080/actuator/metrics/shadow_diff_total?&tag=service:consumer-service&tag=metricKey:decision_delta&tag=outcome:diff` - DIFF
* `http://localhost:8080/actuator/metrics/shadow_diff_total?&tag=service:consumer-service&tag=metricKey:decision_delta&tag=outcome:same` - SAME

---

## 📚 Dalsze kroki

* [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html)
* [Micrometer](https://micrometer.io/docs)
* [Canary release (Martin Fowler)](https://martinfowler.com/bliki/CanaryRelease.html)

---
