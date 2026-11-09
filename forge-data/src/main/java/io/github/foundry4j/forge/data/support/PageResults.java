package io.github.foundry4j.forge.data.support;

import io.github.foundry4j.forge.core.result.PageResult;
import org.springframework.data.domain.Page;

/// Converts Spring Data pages into framework neutral [PageResult] records.
public final class PageResults {

    private PageResults() {
    }

    /// Converts `page` into a [PageResult].
    public static <T> PageResult<T> of(Page<T> page) {
        return PageResult.of(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
