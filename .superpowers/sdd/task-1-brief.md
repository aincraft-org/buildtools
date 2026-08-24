### Task 1: Add `isReplaceable` port to `WorldAccess`

**Files:**
- Modify: `api/src/main/java/dev/mintychochip/buildtools/api/service/WorldAccess.java`
- Modify: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperWorldAccess.java`
- Modify: `common/src/test/java/dev/mintychochip/buildtools/common/support/InMemoryWorldAccess.java`

**Interfaces:**
- Consumes: `BlockPosition` from `api`
- Produces: `boolean isReplaceable(BlockPosition position)` on `WorldAccess`

- [ ] **Step 1: Declare `isReplaceable` in the `WorldAccess` interface**

```java
/**
 * @param position block coordinate
 * @return {@code true} if the block can be replaced by another block without being broken first
 */
boolean isReplaceable(BlockPosition position);
```

- [ ] **Step 2: Implement in `PaperWorldAccess`**

In `PaperWorldAccess` add:

```java
@Override
public boolean isReplaceable(BlockPosition position) {
    return blockAt(position).getBlockData().isReplaceable();
}
```

- [ ] **Step 3: Implement in `InMemoryWorldAccess` with a configurable set**

Add fields and helpers:

```java
private final Set<String> replaceableNames = new HashSet<>();

public InMemoryWorldAccess withReplaceable(String namespacedKey) {
    replaceableNames.add(namespacedKey);
    return this;
}

@Override
public boolean isReplaceable(BlockPosition position) {
    BlockState state = getBlock(position);
    return state.isAir() || replaceableNames.contains(state.namespacedKey());
}
```

- [ ] **Step 4: Run the `api` and `common` tests to verify the interface still compiles**

```bash
./gradlew :api:compileJava :common:compileTestJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/dev/mintychochip/buildtools/api/service/WorldAccess.java \
        paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperWorldAccess.java \
        common/src/test/java/dev/mintychochip/buildtools/common/support/InMemoryWorldAccess.java
git commit -m "api: add WorldAccess.isReplaceable port"
```

---

