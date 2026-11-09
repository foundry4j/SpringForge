package io.github.foundry4j.forge.excel;

/// Receives rows while a file is streamed; the whole file is never held in memory.
@FunctionalInterface
public interface RowConsumer {

    /// Consumes one row.
    void accept(RowData row);
}
