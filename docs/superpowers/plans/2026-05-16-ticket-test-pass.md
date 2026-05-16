# Ticket Test Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add service-first then controller coverage for the `ticket` module, starting with `TicketServiceTests` and then `TicketControllerTests`.

**Architecture:** Focus the service tests on QR generation, ticket validation, giveaway/feedback rules, reminder dispatching, and organization authorization checks. Follow with thin MVC tests that verify the ticket routes, request binding, and response envelopes without duplicating service behavior.

**Tech Stack:** Java 17, JUnit 5, Mockito, AssertJ, Spring MockMvc, Spring test utilities.

---

### Task 1: Add `TicketServiceTests`

**Files:**
- Create: `src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java`

- [ ] **Step 1: Write the failing test**

Add a focused test class that covers:

```java
package com.ht.eventbox.modules.ticket;

import com.corundumstudio.socketio.SocketIOServer;
import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.entities.TicketItemTrace;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.FeedbackSentimentType;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.auth.AuthService;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.event.EventService;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.modules.order.OrderRepository;
import com.ht.eventbox.modules.order.TicketItemRepository;
import com.ht.eventbox.modules.organization.OrganizationRepository;
import com.ht.eventbox.modules.sentiment.SentimentAnalystService;
import com.ht.eventbox.modules.ticket.dtos.FeedbackTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.GiveawayTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.ValidateTicketItemDto;
import com.ht.eventbox.modules.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTests {
    // tests for:
    // - getTicketItemById missing/ok
    // - getTicketItemQrCode before start, after end, success
    // - validateTicketItem invalid token, not member, success
    // - createTicketItemTrace first trace vs toggle trace
    // - getTicketItemByShowId unauthorized and success
    // - createTicketItemFeedback not used, not ended, success
    // - giveawayTicketItem self-gift, already-used, ended, bad password, single-item order, multi-item order
    // - triggerReminder missing item, mail failure, push failure, success
    // - getLatestTicketItemFeedbackByOrganizationId missing org and success
    // - getTicketItemFeedbackByEventId unauthorized and success
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=TicketServiceTests test`

Expected: FAIL because `TicketServiceTests` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement the service tests with compact helper builders for event, show, ticket, order, ticket item, organization, and membership graphs.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=TicketServiceTests test`

Expected: PASS with all assertions green.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/ht/eventbox/modules/ticket/TicketServiceTests.java docs/superpowers/plans/2026-05-16-ticket-test-pass.md
git commit -m "test(ticket): add ticket service coverage"
```

### Task 2: Add `TicketControllerTests`

**Files:**
- Create: `src/test/java/com/ht/eventbox/modules/ticket/TicketControllerTests.java`

- [ ] **Step 1: Write the failing test**

Add a controller test class that covers the ticket routes:

```java
package com.ht.eventbox.modules.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.modules.ticket.dtos.FeedbackTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.GiveawayTicketItemDto;
import com.ht.eventbox.modules.ticket.dtos.ValidateTicketItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.PublicKey;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "paypal.checkout.webhook.id=checkout-webhook",
        "paypal.payment.webhook.id=payment-webhook"
})
class TicketControllerTests {
    // tests for getMyTicketItems, getTicketItemById, getTicketItemQrCode,
    // validateTicketItem, createTicketItemTrace, getTicketItemByShowId,
    // createTicketItemFeedback, giveawayTicketItem, latest feedback by organization,
    // feedback by event, and reminder trigger
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=TicketControllerTests test`

Expected: FAIL because `TicketControllerTests` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement only route mapping, request binding, status codes, and response envelope assertions.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=TicketControllerTests test`

Expected: PASS with all assertions green.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/ht/eventbox/modules/ticket/TicketControllerTests.java docs/superpowers/plans/2026-05-16-ticket-test-pass.md
git commit -m "test(ticket): add ticket controller coverage"
```

### Task 3: Update coverage docs after ticket pass

**Files:**
- Modify: `docs/testing-coverage-map.md`
- Modify: `docs/testing-module-roadmap.md` only if the ticket pass changes the agreed sequencing

- [ ] **Step 1: Write the failing edit**

Record the new ticket service and controller test files and summarize the covered branches.

- [ ] **Step 2: Run a narrow verification**

Run: `./mvnw -Dtest=TicketServiceTests,TicketControllerTests test`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add docs/testing-coverage-map.md docs/testing-module-roadmap.md
git commit -m "docs(testing): record ticket coverage"
```

**Execution note:** Do not move beyond the ticket module until the service and controller suites are green and documented.
