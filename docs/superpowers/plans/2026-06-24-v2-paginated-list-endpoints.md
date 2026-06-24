# V2 Paginated List Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/api/v2/...` versions of the main list endpoints that use Spring `Pageable` with `page` and `size`, while keeping every existing v1 endpoint unchanged.

**Architecture:** Keep v1 controllers and service methods intact. Add small v2-only list endpoints for users, events, categories, and organizations that accept `Pageable` directly and return a `Response<Page<T>>`, so Spring Data supplies the slice and total count in one query response. Back the new endpoints with repository methods that preserve the current ID-ascending ordering.

**Tech Stack:** Java 17, Spring Boot Web, Spring Data JPA `Pageable`/`Page`, JUnit 5, MockMvc, Mockito

---

### Task 1: Add paged repository/service methods

**Files:**
- Modify: `src/main/java/com/ht/eventbox/modules/user/UserRepository.java`
- Modify: `src/main/java/com/ht/eventbox/modules/category/CategoryRepository.java`
- Modify: `src/main/java/com/ht/eventbox/modules/organization/OrganizationRepository.java`
- Modify: `src/main/java/com/ht/eventbox/modules/event/EventRepository.java`
- Modify: `src/main/java/com/ht/eventbox/modules/user/UserService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/category/CategoryService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/organization/OrganizationService.java`
- Modify: `src/main/java/com/ht/eventbox/modules/event/EventService.java`

- [ ] **Step 1: Add repository methods that return `Page<T>` with the existing ID ordering**

```java
Page<User> findAllByOrderByIdAsc(Pageable pageable);
Page<Category> findAllByOrderByIdAsc(Pageable pageable);
Page<Organization> findAllByOrderByIdAsc(Pageable pageable);
Page<Event> findAllByStatusInOrderByIdAsc(Collection<EventStatus> status, Pageable pageable);
```

- [ ] **Step 2: Add service methods that delegate to those repository methods**

```java
public Page<User> getAll(Pageable pageable) {
    return userRepository.findAllByOrderByIdAsc(pageable);
}

public Page<Category> getAll(Pageable pageable) {
    return categoryRepository.findAllByOrderByIdAsc(pageable);
}

public Page<Organization> getAll(Pageable pageable) {
    return organizationRepository.findAllByOrderByIdAsc(pageable);
}

public Page<Event> getAllByStatusIn(List<EventStatus> statuses, Pageable pageable) {
    return eventRepository.findAllByStatusInOrderByIdAsc(statuses, pageable);
}
```

- [ ] **Step 3: Run the relevant service tests and verify the new methods are covered**

Run: `./mvnw -Dtest=UserServiceTests,CategoryServiceTests,OrganizationServiceTests,EventServiceTests test`
Expected: pass, with the new page-returning methods verified by Mockito assertions.

### Task 2: Add v2 list controllers

**Files:**
- Create: `src/main/java/com/ht/eventbox/modules/user/UserControllerV2.java`
- Create: `src/main/java/com/ht/eventbox/modules/category/CategoryControllerV2.java`
- Create: `src/main/java/com/ht/eventbox/modules/organization/OrganizationControllerV2.java`
- Create: `src/main/java/com/ht/eventbox/modules/event/EventControllerV2.java`

- [ ] **Step 1: Add v2 GET endpoints under `/api/v2/...` that accept `Pageable`**

```java
@GetMapping
public ResponseEntity<Response<Page<User>>> getAll(Pageable pageable) { ... }

@GetMapping
public ResponseEntity<Response<Page<Category>>> getAll(Pageable pageable) { ... }

@GetMapping
public ResponseEntity<Response<Page<Organization>>> getAll(Pageable pageable) { ... }

@GetMapping
public ResponseEntity<Response<Page<Event>>> getAll(Pageable pageable) { ... }
```

- [ ] **Step 2: Return the `Page<T>` directly inside the existing `Response<T>` envelope**

```java
return ResponseEntity.ok(
        new Response<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                res
        )
);
```

- [ ] **Step 3: Keep all existing v1 controllers unchanged**

Run: `git diff -- src/main/java/com/ht/eventbox/modules/*/*Controller.java`
Expected: only new v2 controller files are added; v1 controller paths and methods remain untouched.

### Task 3: Add controller tests for v2 paging responses

**Files:**
- Modify: `src/test/java/com/ht/eventbox/modules/event/EventControllerTests.java`
- Modify: `src/test/java/com/ht/eventbox/modules/organization/OrganizationControllerTests.java`
- Create: `src/test/java/com/ht/eventbox/modules/category/CategoryControllerV2Tests.java`
- Create: `src/test/java/com/ht/eventbox/modules/user/UserControllerV2Tests.java`

- [ ] **Step 1: Add MockMvc assertions for `page`, `size`, and `totalElements`**

```java
mockMvc.perform(get("/api/v2/users").param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(25))
        .andExpect(jsonPath("$.data.content[0].id").value(9L));
```

- [ ] **Step 2: Verify the v2 controllers call the new paged service methods**

```java
when(userService.getAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(sampleUser(9L)), PageRequest.of(1, 10), 25));
```

- [ ] **Step 3: Run the controller tests for the affected modules**

Run: `./mvnw -Dtest=EventControllerTests,OrganizationControllerTests,CategoryControllerV2Tests,UserControllerV2Tests test`
Expected: pass.

### Task 4: Full verification

**Files:**
- None

- [ ] **Step 1: Run the focused Maven test set**

Run: `./mvnw test`
Expected: pass.

- [ ] **Step 2: Inspect the final diff for isolation**

Run: `git diff --stat`
Expected: only v2 paging files and their related tests are changed, plus any small repository/service updates.

