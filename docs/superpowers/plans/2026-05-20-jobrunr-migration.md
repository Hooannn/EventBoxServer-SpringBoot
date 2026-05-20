# CompletableFuture to JobRunr Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current fire-and-forget `CompletableFuture.runAsync` usage with durable JobRunr background jobs.

**Architecture:** Add the JobRunr Spring Boot starter, configure the app to run a background job server in the main deployment, and move each async concern into a dedicated background-job service. Business services should only enqueue work; the worker services should own the email, socket, and push-notification execution paths and reload any needed data by ID inside the job.

**Tech Stack:** Spring Boot 3.2, JobRunr, Maven, PostgreSQL, JUnit 5, Mockito, Java 17.

---

### Task 1: Add JobRunr bootstrap configuration

**Files:**
- Modify: `pom.xml`
- Create: `src/main/resources/application.properties`
- Modify: `src/test/resources/application-test.properties`
- Create: `src/test/java/com/ht/eventbox/config/JobRunrBootstrapTests.java`

- [ ] **Step 1: Write the failing test**

Create a bootstrap test that expects the JobRunr scheduler bean to exist in the Spring context:

```java
package com.ht.eventbox.config;

import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ht.eventbox.support.AbstractSpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

class JobRunrBootstrapTests extends AbstractSpringBootTest {

    @Autowired
    private JobScheduler jobScheduler;

    @Test
    void jobSchedulerBeanIsAvailable() {
        assertThat(jobScheduler).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=JobRunrBootstrapTests test`

Expected: FAIL because the JobRunr starter and configuration are not present yet.

- [ ] **Step 3: Write minimal implementation**

Add the dependency and the config files:

```xml
<!-- pom.xml -->
<properties>
    <java.version>17</java.version>
    <jobrunr.version>8.5.1</jobrunr.version>
</properties>

<dependency>
    <groupId>org.jobrunr</groupId>
    <artifactId>jobrunr-spring-boot-3-starter</artifactId>
    <version>${jobrunr.version}</version>
</dependency>
```

```properties
# src/main/resources/application.properties
jobrunr.background-job-server.enabled=true
jobrunr.dashboard.enabled=true
jobrunr.database.skip-create=false
```

```properties
# src/test/resources/application-test.properties
jobrunr.background-job-server.enabled=false
jobrunr.dashboard.enabled=false
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=JobRunrBootstrapTests test`

Expected: PASS with a resolvable `JobScheduler` bean and no JobRunr server startup in the test profile.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application.properties src/test/resources/application-test.properties src/test/java/com/ht/eventbox/config/JobRunrBootstrapTests.java
git commit -m "refactor: add JobRunr bootstrap"
```

### Task 2: Create the mail job service

**Files:**
- Create: `src/main/java/com/ht/eventbox/modules/backgroundjobs/MailJobService.java`
- Create: `src/test/java/com/ht/eventbox/modules/backgroundjobs/MailJobServiceTests.java`

- [ ] **Step 1: Write the failing test**

Create a unit test that expects the new mail job service to enqueue work through `JobScheduler` and still delegate execution to `MailService`:

```java
package com.ht.eventbox.modules.backgroundjobs;

import com.ht.eventbox.modules.mail.MailService;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.mail.MessagingException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailJobServiceTests {

    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private MailService mailService;

    @InjectMocks
    private MailJobService mailJobService;

    @Test
    void enqueueRegistrationEmailSchedulesAJob() {
        mailJobService.enqueueRegistrationEmail("user@example.com", "User Example", "123456");
        verify(jobScheduler).enqueue(any());
    }

    @Test
    void sendRegistrationEmailDelegatesToMailService() throws MessagingException {
        mailJobService.sendRegistrationEmail("user@example.com", "User Example", "123456");
        verify(mailService).sendRegistrationEmail("user@example.com", "User Example", "123456");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=MailJobServiceTests test`

Expected: FAIL because `MailJobService` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement `MailJobService` as a thin scheduler plus worker wrapper around `MailService`. The class should expose enqueue methods for these flows:

```java
enqueueRegistrationEmail(String to, String name, String otp)
enqueueResendVerifyEmail(String to, String name, String otp)
enqueueForgotPasswordEmail(String to, String otp)
enqueueMemberAddedEmail(String to, String name, String orgName)
enqueueMemberRemovedEmail(String to, String name, String orgName)
enqueueOrderPaidEmail(long orderId)
enqueueOrderRefundedEmail(long orderId)
enqueueGiveawayNotificationEmail(long ticketItemId, String fromEmail)
```

Each enqueue method should call `jobScheduler.enqueue(() -> sendRegistrationEmail(to, name, otp))`, `jobScheduler.enqueue(() -> sendForgotPasswordEmail(to, otp))`, or the matching worker method for the other flow. The worker methods for the order and giveaway flows should reload the order or ticket item from the database, then call the matching `MailService` method. All worker methods should rethrow failures as runtime exceptions so JobRunr can retry them.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=MailJobServiceTests test`

Expected: PASS with the enqueue path and worker delegation both covered.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ht/eventbox/modules/backgroundjobs/MailJobService.java src/test/java/com/ht/eventbox/modules/backgroundjobs/MailJobServiceTests.java
git commit -m "refactor: add mail job service"
```

### Task 3: Create the socket job service

**Files:**
- Create: `src/main/java/com/ht/eventbox/modules/backgroundjobs/SocketJobService.java`
- Create: `src/test/java/com/ht/eventbox/modules/backgroundjobs/SocketJobServiceTests.java`

- [ ] **Step 1: Write the failing test**

Create a unit test that expects socket broadcasts to be queued through JobRunr instead of running inline:

```java
package com.ht.eventbox.modules.backgroundjobs;

import com.corundumstudio.socketio.SocketIOServer;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocketJobServiceTests {

    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private SocketIOServer socketIOServer;

    @InjectMocks
    private SocketJobService socketJobService;

    @Test
    void enqueueStockUpdatedSchedulesAJob() {
        socketJobService.enqueueStockUpdated(42L);
        verify(jobScheduler).enqueue(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=SocketJobServiceTests test`

Expected: FAIL because `SocketJobService` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement `SocketJobService` with enqueue and worker methods for these flows:

```java
enqueueStockUpdated(long eventId)
enqueueOrderApproved(long orderId)
enqueueOrderFulfilled(long orderId)
enqueueOrderRefunded(long orderId)
enqueueTicketTracesUpdated(long ticketItemId, long eventId)
```

The worker methods should use `SocketIOServer` to emit the same events the code currently sends with `CompletableFuture.runAsync`. Keep the payloads unchanged so the front end does not need to change.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=SocketJobServiceTests test`

Expected: PASS with the job scheduling path covered.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ht/eventbox/modules/backgroundjobs/SocketJobService.java src/test/java/com/ht/eventbox/modules/backgroundjobs/SocketJobServiceTests.java
git commit -m "refactor: add socket job service"
```

### Task 4: Create the notification job service

**Files:**
- Create: `src/main/java/com/ht/eventbox/modules/backgroundjobs/NotificationJobService.java`
- Create: `src/test/java/com/ht/eventbox/modules/backgroundjobs/NotificationJobServiceTests.java`

- [ ] **Step 1: Write the failing test**

Create a unit test that expects the notification job service to enqueue work and then call `PushNotificationService` from the worker method:

```java
package com.ht.eventbox.modules.backgroundjobs;

import com.ht.eventbox.modules.messaging.PushNotificationService;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationJobServiceTests {

    @Mock
    private JobScheduler jobScheduler;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private NotificationJobService notificationJobService;

    @Test
    void enqueueEventPublishedSchedulesAJob() {
        notificationJobService.enqueueEventPublished(99L);
        verify(jobScheduler).enqueue(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=NotificationJobServiceTests test`

Expected: FAIL because `NotificationJobService` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement `NotificationJobService` with enqueue and worker methods for these flows:

```java
enqueueEventPublished(long eventId)
enqueueOrderFulfilled(long orderId)
enqueueOrderRefunded(long orderId)
```

The worker methods should load the needed event or order data by ID, build the existing Firebase notification payloads, and push them through `PushNotificationService`. Keep the worker arguments small and serializable.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=NotificationJobServiceTests test`

Expected: PASS with the JobScheduler enqueue path covered.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ht/eventbox/modules/backgroundjobs/NotificationJobService.java src/test/java/com/ht/eventbox/modules/backgroundjobs/NotificationJobServiceTests.java
git commit -m "refactor: add notification job service"
```

### Task 5: Migrate auth and organization email call sites

**Files:**
- Modify: `src/main/java/com/ht/eventbox/modules/auth/AuthService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java`
- Modify: `src/test/java/com/ht/eventbox/modules/auth/AuthServiceTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java`

- [ ] **Step 1: Write the failing test**

Update the service tests so they expect the new `MailJobService` dependency instead of direct `MailService` scheduling. Add assertions that the business methods still return the same results while calling the enqueue API:

```java
// AuthServiceTests
verify(mailJobService).enqueueRegistrationEmail("user@example.com", "User Example", "123456");
verify(mailJobService).enqueueForgotPasswordEmail("user@example.com", "123456");
verify(mailJobService).enqueueResendVerifyEmail("user@example.com", "User Example", "123456");

// OrganizationServiceTests
verify(mailJobService).enqueueMemberAddedEmail("member@example.com", "Member Name", "Organization Name");
verify(mailJobService).enqueueMemberRemovedEmail("member@example.com", "Member Name", "Organization Name");
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=AuthServiceTests,OrganizationServiceTests test`

Expected: FAIL because the production code still uses `CompletableFuture.runAsync` and the tests still target the old dependency shape.

- [ ] **Step 3: Write minimal implementation**

Inject `MailJobService` into both services and replace these `CompletableFuture.runAsync` blocks:

```java
// AuthService
register(RegisterDto registerDto)
resendVerify(ResendVerifyDto resendVerifyDto)
forgotPassword(ForgotPasswordDto forgotPasswordDto)

// OrganizationService
addMember(Long userId, Long orgId, AddMemberDto addMemberDto)
removeMember(Long userId, Long orgId, RemoveMemberDto removeMemberDto)
```

Each of those methods should call the matching enqueue method on `MailJobService` after the database/Redis work is complete. Do not pass JPA entities into the job layer; pass only the primitive values and strings needed to render the mail.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=AuthServiceTests,OrganizationServiceTests test`

Expected: PASS with the new job service dependency and no direct async mail execution.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ht/eventbox/modules/auth/AuthService.java src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java src/test/java/com/ht/eventbox/modules/auth/AuthServiceTests.java src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java
git commit -m "refactor(auth): route mail work through JobRunr"
```

### Task 6: Migrate order, event, and ticket async call sites

**Files:**
- Modify: `src/main/java/com/ht/eventbox/modules/order/OrderService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/event/EventService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/ticket/TicketService.java`
- Modify: `src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java`

- [ ] **Step 1: Write the failing test**

Update the three service test classes so they expect the new job services to be called. Add assertions for the exact migrations that matter:

```java
// OrderServiceTests
verify(socketJobService).enqueueStockUpdated(eventId);
verify(mailJobService).enqueueOrderPaidEmail(orderId);
verify(mailJobService).enqueueOrderRefundedEmail(orderId);
verify(notificationJobService).enqueueOrderFulfilled(orderId);
verify(notificationJobService).enqueueOrderRefunded(orderId);

// EventServiceTests
verify(notificationJobService).enqueueEventPublished(eventId);

// TicketServiceTests
verify(socketJobService).enqueueTicketTracesUpdated(ticketItemId, eventId);
verify(mailJobService).enqueueGiveawayNotificationEmail(ticketItemId, fromEmail);
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=OrderServiceTests,EventServiceTests,TicketServiceTests test`

Expected: FAIL because the production code still uses `CompletableFuture.runAsync`.

- [ ] **Step 3: Write minimal implementation**

Inject `MailJobService`, `SocketJobService`, and `NotificationJobService` into the three services and replace every remaining `CompletableFuture.runAsync` call:

```java
// OrderService
onStockUpdated(long eventId)
cancelReservation(Long userId, Long orderId)
cancelReservation(Long userId)
onOrderRefunded(Order order)
onOrderApproved(Order order)
onOrderFulfilled(Order order)

// EventService
publishByAdmin(Long eventId)

// TicketService
createTicketItemTrace(Long userId, ValidateTicketItemDto dto)
giveawayTicketItem(Long userId, Long ticketItemId, GiveawayTicketItemDto dto)
```

Keep the current event payloads, but move the actual execution into the JobRunr worker services. For the order and notification jobs, reload any entity data needed for email templates or push payloads inside the worker methods.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=OrderServiceTests,EventServiceTests,TicketServiceTests test`

Expected: PASS with the async call sites replaced by job enqueues.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ht/eventbox/modules/order/OrderService.java src/main/java/com/ht/eventbox/modules/event/EventService.java src/main/java/com/ht/eventbox/modules/ticket/TicketService.java src/test/java/com/ht/eventbox/modules/order/OrderServiceTests.java src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java
git commit -m "refactor(order): migrate async work to JobRunr"
```

### Task 7: Remove the old async executor and verify there are no remaining runAsync call sites

**Files:**
- Delete: `src/main/java/com/ht/eventbox/config/AsyncConfiguration.java`

- [ ] **Step 1: Write the failing edit**

Delete the old async configuration file so the project no longer advertises `SimpleAsyncTaskExecutor` as the background mechanism:

```java
// src/main/java/com/ht/eventbox/config/AsyncConfiguration.java
// delete the file entirely
```

- [ ] **Step 2: Run a narrow verification**

Run: `rg -n "CompletableFuture\\.runAsync|@EnableAsync" src/main/java src/test/java`

Expected: no matches.

- [ ] **Step 3: Run the full service and application test sweep**

Run: `./mvnw -Dtest=JobRunrBootstrapTests,MailJobServiceTests,SocketJobServiceTests,NotificationJobServiceTests,AuthServiceTests,OrganizationServiceTests,OrderServiceTests,EventServiceTests,TicketServiceTests test`

Expected: PASS.

Run: `./mvnw test`

Expected: PASS for the full repository test suite.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ht/eventbox/config/AsyncConfiguration.java
git commit -m "refactor: remove CompletableFuture async config"
```
