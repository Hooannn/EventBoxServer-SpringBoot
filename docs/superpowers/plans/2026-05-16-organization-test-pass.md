# Organization Test Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add service-first then controller coverage for the `organization` module, starting with `OrganizationServiceTests` and then `OrganizationControllerTests`.

**Architecture:** Keep the service tests focused on authorization checks, ownership checks, membership mutation, and Cloudinary-backed asset flows. Follow with thin MVC tests that only verify request binding, status codes, response envelopes, and the path-to-service wiring for the organization routes.

**Tech Stack:** Java 17, JUnit 5, Mockito, AssertJ, Spring MockMvc, Spring test utilities.

---

### Task 1: Add `OrganizationServiceTests`

**Files:**
- Create: `src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java`

- [ ] **Step 1: Write the failing test**

Add a focused test class that covers:

```java
package com.ht.eventbox.modules.organization;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.entities.UserOrganization;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.asset.AssetRepository;
import com.ht.eventbox.modules.event.EventRepository;
import com.ht.eventbox.modules.mail.MailService;
import com.ht.eventbox.modules.organization.dtos.AddMemberDto;
import com.ht.eventbox.modules.organization.dtos.CreateOrganizationDto;
import com.ht.eventbox.modules.organization.dtos.RemoveMemberDto;
import com.ht.eventbox.modules.organization.dtos.UpdateMemberDto;
import com.ht.eventbox.modules.organization.dtos.UpdateOrganizationDto;
import com.ht.eventbox.modules.storage.CloudinaryService;
import com.ht.eventbox.modules.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTests {
    // tests for:
    // - getById missing organization
    // - getDetailsById with subscriber/event counts
    // - create with and without logo
    // - update with remove-logo and replacement-logo branches
    // - deleteById blocked by existing events and allowed delete
    // - addMember duplicate, missing user, and success paths
    // - updateMember missing target and success path
    // - removeMember missing target and success path
    // - subscribe toggling add/remove behavior
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=OrganizationServiceTests test`

Expected: FAIL because `OrganizationServiceTests` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement the test class with small helper builders for organizations, users, and memberships so each branch is isolated and readable.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=OrganizationServiceTests test`

Expected: PASS with all assertions green.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/ht/eventbox/modules/organization/OrganizationServiceTests.java docs/superpowers/plans/2026-05-16-organization-test-pass.md
git commit -m "test(organization): add organization service coverage"
```

### Task 2: Add `OrganizationControllerTests`

**Files:**
- Create: `src/test/java/com/ht/eventbox/modules/organization/OrganizationControllerTests.java`

- [ ] **Step 1: Write the failing test**

Add a controller test class that covers the organization routes:

```java
package com.ht.eventbox.modules.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.eventbox.config.GlobalExceptionHandler;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.organization.dtos.AddMemberDto;
import com.ht.eventbox.modules.organization.dtos.CreateOrganizationDto;
import com.ht.eventbox.modules.organization.dtos.RemoveMemberDto;
import com.ht.eventbox.modules.organization.dtos.UpdateMemberDto;
import com.ht.eventbox.modules.organization.dtos.UpdateOrganizationDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "paypal.checkout.webhook.id=checkout-webhook",
        "paypal.payment.webhook.id=payment-webhook"
})
class OrganizationControllerTests {
    // tests for getAll, getMyOwn, getMy, getDetailsById, getById,
    // updateById, deleteById, create, addMember, updateMember, removeMember, subscribe
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=OrganizationControllerTests test`

Expected: FAIL because `OrganizationControllerTests` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement only route mapping, response envelope, and request-attribute/body binding assertions.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=OrganizationControllerTests test`

Expected: PASS with all assertions green.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/ht/eventbox/modules/organization/OrganizationControllerTests.java docs/superpowers/plans/2026-05-16-organization-test-pass.md
git commit -m "test(organization): add organization controller coverage"
```

### Task 3: Update coverage docs after organization pass

**Files:**
- Modify: `docs/testing-coverage-map.md`
- Modify: `docs/testing-module-roadmap.md` only if the organization pass changes the agreed sequencing

- [ ] **Step 1: Write the failing edit**

Record the new organization service and controller test files and summarize the covered branches.

- [ ] **Step 2: Run a narrow verification**

Run: `./mvnw -Dtest=OrganizationServiceTests,OrganizationControllerTests test`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add docs/testing-coverage-map.md docs/testing-module-roadmap.md
git commit -m "docs(testing): record organization coverage"
```

**Execution note:** Do not start ticket tests until the organization service and controller suites are green and documented.
