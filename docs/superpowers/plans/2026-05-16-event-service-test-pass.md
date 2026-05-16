# Event Service Test Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add focused service-layer test coverage for the `event` module, starting with `EventServiceTests` and then `VoucherServiceTests`, without touching controller tests yet.

**Architecture:** Keep each test file isolated to one service class and build fixtures around the service's branching logic, authorization checks, and repository interactions. Use Mockito-based unit tests so the coverage is fast, deterministic, and easy to extend later with controller mapping tests.

**Tech Stack:** Java 17, JUnit 5, Mockito, AssertJ, Spring test utilities.

---

### Task 1: Add `EventServiceTests`

**Files:**
- Create: `src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java`

- [ ] **Step 1: Write the failing test**

Add a focused service test class that covers the highest-risk `EventService` branches:

```java
package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Asset;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.entities.UserOrganization;
import com.ht.eventbox.enums.AssetUsage;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.category.CategoryRepository;
import com.ht.eventbox.modules.event.dtos.CreateEventDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventTagsDto;
import com.ht.eventbox.modules.keyword.KeywordRepository;
import com.ht.eventbox.modules.messaging.PushNotificationService;
import com.ht.eventbox.modules.order.CurrencyConverterServiceV2;
import com.ht.eventbox.modules.order.OrderRepository;
import com.ht.eventbox.modules.order.PayPalService;
import com.ht.eventbox.modules.order.TicketItemRepository;
import com.ht.eventbox.modules.organization.OrganizationRepository;
import com.ht.eventbox.modules.storage.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTests {
    // mocks, fixture helpers, and tests for:
    // - getById missing event
    // - getDiscovery query branches
    // - search with and without province
    // - create / update / publish / archive / inactive / active
    // - updateTags status guard
    // - getByIdAndStatusIsNot / getWithRealStockByIdAndStatusIsNot
    // - getAllByStatusIn
    // - eventPayout failure and success branches
    // - isMember role checks
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=EventServiceTests test`

Expected: FAIL because `EventServiceTests` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement the test class with one behavior per test method, using Mockito stubs for repository and service collaborators and explicit fixture helpers for event, organization, ticket, and user-organization graphs.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=EventServiceTests test`

Expected: PASS with all assertions green.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/ht/eventbox/modules/event/EventServiceTests.java docs/superpowers/plans/2026-05-16-event-service-test-pass.md
git commit -m "test(event): add event service coverage"
```

### Task 2: Add `VoucherServiceTests`

**Files:**
- Create: `src/test/java/com/ht/eventbox/modules/event/VoucherServiceTests.java`

- [ ] **Step 1: Write the failing test**

Add a service test class that covers the main `VoucherService` flows:

```java
package com.ht.eventbox.modules.event;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.entities.Order;
import com.ht.eventbox.entities.Ticket;
import com.ht.eventbox.entities.Voucher;
import com.ht.eventbox.enums.DiscountType;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.event.dtos.ApplyVoucherDto;
import com.ht.eventbox.modules.event.dtos.CreateVoucherDto;
import com.ht.eventbox.modules.order.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTests {
    // mocks, helper fixtures, and tests for:
    // - member authorization on list/create/update/delete/getUsage
    // - duplicate code rejection
    // - voucher used / not found / ownership checks
    // - apply voucher time window, conditions, usage limits, per-user limits
    // - remove voucher forbidden for fulfilled orders
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=VoucherServiceTests test`

Expected: FAIL because `VoucherServiceTests` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement the test class with compact fixtures for an event, show, ticket, order, and voucher chain so `applyByOrderId` and `removeByOrderId` can exercise real branch conditions.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=VoucherServiceTests test`

Expected: PASS with all assertions green.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/ht/eventbox/modules/event/VoucherServiceTests.java docs/superpowers/plans/2026-05-16-event-service-test-pass.md
git commit -m "test(event): add voucher service coverage"
```

### Task 3: Update coverage docs after service pass

**Files:**
- Modify: `docs/testing-coverage-map.md`
- Modify: `docs/testing-module-roadmap.md` if the event service pass changes sequencing notes

- [ ] **Step 1: Write the failing edit**

Record the new event service test files and the covered behaviors in the coverage map.

- [ ] **Step 2: Run a narrow verification**

Run: `./mvnw -Dtest=EventServiceTests,VoucherServiceTests test`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add docs/testing-coverage-map.md docs/testing-module-roadmap.md
git commit -m "docs(testing): record event service coverage"
```

**Execution note:** Do not start controller tests until the service suite is green and the coverage map reflects the new files.
