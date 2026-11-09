package io.github.foundry4j.forge.web.paging;

/// Paging query parameters of a REST endpoint.
///
/// Bind it with `@ModelAttribute` to read `page` and `size` from the query string:
///
/// ```java
/// @GetMapping
/// public PageResult<OrderView> list(@ModelAttribute PageQuery query) {
///     return service.find(query.page(), query.size());
/// }
/// ```
///
/// The values are normalized: the page index starts at one and the page size is
/// clamped to `[1, 100]`.
public class PageQuery {

    /// First page index.
    public static final int DEFAULT_PAGE = 1;

    /// Page size used when the caller does not send one.
    public static final int DEFAULT_SIZE = 20;

    /// Largest accepted page size, protects the database from huge result sets.
    public static final int MAX_SIZE = 100;

    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_SIZE;

    /// Raw page query parameter.
    public int getPage() {
        return page;
    }

    /// Sets the raw page query parameter.
    public void setPage(int page) {
        this.page = page;
    }

    /// Raw size query parameter.
    public int getSize() {
        return size;
    }

    /// Sets the raw size query parameter.
    public void setSize(int size) {
        this.size = size;
    }

    /// Normalized page index, at least one.
    public int page() {
        return Math.max(page, DEFAULT_PAGE);
    }

    /// Normalized page size, between one and [MAX_SIZE].
    public int size() {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /// Zero based row offset, ready for `setFirstResult` or a SQL `OFFSET`.
    public int offset() {
        return (page() - 1) * size();
    }

    /// Number of rows to read.
    public int limit() {
        return size();
    }

    /// The same value Spring Data expects in `PageRequest.of(...)`.
    public int pageIndex() {
        return page() - 1;
    }
}
