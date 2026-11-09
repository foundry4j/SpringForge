package io.github.foundry4j.forge.core.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the paged result envelope.
class PageResultTest {

    @Test
    void computesPagesAndMapsRecords() {
        var page = PageResult.of(List.of("a", "b"), 5, 0, 2);
        assertThat(page.pages()).isEqualTo(3);
        assertThat(page.records()).containsExactly("a", "b");

        var mapped = page.map(String::toUpperCase);
        assertThat(mapped.records()).containsExactly("A", "B");
        assertThat(mapped.total()).isEqualTo(5);
        assertThat(mapped.page()).isZero();
        assertThat(mapped.size()).isEqualTo(2);
        assertThat(mapped.pages()).isEqualTo(3);
    }

    @Test
    void handlesEmptyResultsAndZeroSize() {
        var empty = PageResult.of(List.of(), 0, 0, 0);
        assertThat(empty.pages()).isZero();
        assertThat(empty.records()).isEmpty();
    }
}
