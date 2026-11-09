package io.github.foundry4j.forge.excel;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/// Streams `.xlsx` and `.csv` files row by row with a constant memory footprint.
///
/// - Excel goes through the POI event model (SAX) and never builds a workbook
/// - CSV is parsed by a character level state machine that supports quoted commas,
///   embedded newlines, `""` escaping and a UTF-8 BOM
///
/// ```java
/// ExcelReader.read(in, ExcelFormat.XLSX, RowReadOptions.defaults(), row ->
///         System.out.println(row.cell("name")));
/// ```
public final class ExcelReader {

    private ExcelReader() {
    }

    /// Reads a file using the format implied by the file name suffix.
    public static void read(InputStream in, String filename, RowReadOptions options, RowConsumer consumer) {
        read(in, ExcelFormat.fromFilename(filename), options, consumer);
    }

    /// Streams a file, invoking `consumer` once per data row.
    public static void read(InputStream in, ExcelFormat format, RowReadOptions options, RowConsumer consumer) {
        if (in == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Missing input stream");
        }
        if (format == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Missing file format");
        }
        RowReadOptions opts = options == null ? RowReadOptions.defaults() : options;
        RowConsumer target = consumer == null ? _ -> {
        } : consumer;
        try {
            switch (format) {
                case XLSX -> readXlsx(in, opts, target);
                case CSV -> readCsv(in, opts, target);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST,
                    "Failed to read " + format + " file", ex);
        }
    }

    private static void readXlsx(InputStream in, RowReadOptions options, RowConsumer consumer) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(in)) {
            XSSFReader reader = new XSSFReader(pkg);
            SharedStrings sharedStrings = reader.getSharedStringsTable();
            XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(
                    reader.getStylesTable(), sharedStrings,
                    new SheetHandler(options, consumer), false);

            XMLReader parser = XMLHelper.newXMLReader();
            parser.setContentHandler(handler);

            try (InputStream sheet = openSheet(reader, options.sheetIndex())) {
                parser.parse(new InputSource(sheet));
            }
        }
    }

    private static InputStream openSheet(XSSFReader reader, int sheetIndex) throws Exception {
        Iterator<InputStream> sheets = reader.getSheetsData();
        for (int i = 0; i < sheetIndex && sheets.hasNext(); i++) {
            InputStream skipped = sheets.next();
            skipped.close();
            // Close the skipped sheet so that no file handle leaks.
        }
        if (!sheets.hasNext()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST,
                    "Worksheet does not exist, index: " + sheetIndex);
        }
        return sheets.next();
    }

    /// POI SAX callback assembling one row of text and honouring header and empty row rules.
    private static final class SheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final RowReadOptions options;
        private final RowConsumer consumer;
        private final List<String> headers = new ArrayList<>();
        private List<String> current = new ArrayList<>();
        private int lastColumn;

        private SheetHandler(RowReadOptions options, RowConsumer consumer) {
            this.options = options;
            this.consumer = consumer;
        }

        @Override
        public void startRow(int rowNum) {
            current = new ArrayList<>();
            lastColumn = -1;
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int column = columnIndex(cellReference, current.size());
            while (current.size() <= column) {
                current.add("");
            }
            current.set(column, formattedValue == null ? "" : formattedValue);
            lastColumn = column;
        }

        @Override
        public void endRow(int rowNum) {
            int expectedWidth = Math.max(lastColumn + 1, current.size());
            while (current.size() < expectedWidth) {
                current.add("");
            }
            if (options.headerRowIndex() >= 0 && rowNum == options.headerRowIndex()) {
                headers.clear();
                headers.addAll(current);
                return;
            }
            if (options.headerRowIndex() >= 0 && rowNum < options.headerRowIndex()) {
                return;
            }
            if (options.skipEmptyRows() && isEmpty(current)) {
                return;
            }
            consumer.accept(new RowData(rowNum + 1, headers, current));
        }

        private static boolean isEmpty(List<String> values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // Header and footer text is irrelevant for tabular imports.
        }
    }

    /// Resolves `A1` to 0 and `AB12` to 27, falling back to the current width on failure.
    private static int columnIndex(String cellReference, int fallback) {
        if (cellReference == null || cellReference.isEmpty()) {
            return fallback;
        }
        int column = 0;
        int seenLetter = 0;
        for (int i = 0; i < cellReference.length(); i++) {
            char c = cellReference.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                column = column * 26 + (c - 'A' + 1);
                seenLetter++;
            } else if (c >= 'a' && c <= 'z') {
                column = column * 26 + (c - 'a' + 1);
                seenLetter++;
            } else {
                break;
            }
        }
        return seenLetter == 0 ? fallback : Math.max(0, column - 1);
    }

    private static void readCsv(InputStream in, RowReadOptions options, RowConsumer consumer) throws IOException {
        try (PushbackReader reader = new PushbackReader(new InputStreamReader(in, options.charset()), 2)) {
            int first = reader.read();
            if (first != 0xFEFF && first != -1) {
                reader.unread(first);
            }
            new CsvParser(options, consumer).parse(reader);
        }
    }

    /// Character level CSV state machine supporting multi-line quoted fields and BOM.
    private static final class CsvParser {

        private final RowReadOptions options;
        private final RowConsumer consumer;
        private final List<String> headers = new ArrayList<>();

        private List<String> fields = new ArrayList<>();
        private final StringBuilder field = new StringBuilder();
        private boolean inQuotes;
        private boolean recordStarted;
        private int recordIndex;

        private CsvParser(RowReadOptions options, RowConsumer consumer) {
            this.options = options;
            this.consumer = consumer;
        }

        private void parse(PushbackReader reader) throws IOException {
            int c;
            while ((c = reader.read()) != -1) {
                char ch = (char) c;
                if (inQuotes) {
                    if (ch == '"') {
                        int next = reader.read();
                        if (next == '"') {
                            field.append('"');
                            recordStarted = true;
                        } else {
                            inQuotes = false;
                            if (next != -1) {
                                reader.unread(next);
                            }
                        }
                    } else {
                        field.append(ch);
                    }
                } else if (ch == '"') {
                    inQuotes = true;
                    recordStarted = true;
                } else if (ch == ',') {
                    fields.add(field.toString());
                    field.setLength(0);
                    recordStarted = true;
                } else if (ch == '\n') {
                    emitRecord();
                } else if (ch == '\r') {
                    int next = reader.read();
                    if (next != '\n' && next != -1) {
                        reader.unread(next);
                    }
                    emitRecord();
                } else {
                    field.append(ch);
                    recordStarted = true;
                }
            }
            if (inQuotes) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Malformed CSV: unclosed quote");
            }
            if (recordStarted || !fields.isEmpty() || !field.isEmpty()) {
                emitRecord();
            }
        }

        private void emitRecord() {
            fields.add(field.toString());
            field.setLength(0);
            int logicalRow = recordIndex + 1;

            if (options.headerRowIndex() >= 0 && recordIndex == options.headerRowIndex()) {
                headers.clear();
                headers.addAll(fields);
            } else if (options.headerRowIndex() < 0 || recordIndex > options.headerRowIndex()) {
                if (!(options.skipEmptyRows() && isEmpty(fields))) {
                    consumer.accept(new RowData(logicalRow, headers, fields));
                }
            }

            fields = new ArrayList<>();
            recordIndex++;
            recordStarted = false;
        }

        private static boolean isEmpty(List<String> values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
            return true;
        }
    }
}
