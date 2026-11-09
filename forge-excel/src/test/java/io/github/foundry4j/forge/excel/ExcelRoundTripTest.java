package io.github.foundry4j.forge.excel;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/// Round trip tests of the streaming exporter and importer.
class ExcelRoundTripTest {

    private record Person(String name, int age, String city) {
    }

    private static final ExcelExportSpec<Person> SPEC = ExcelExportSpec.<Person>builder("People")
            .column("Name", Person::name)
            .column("Age", Person::age)
            .column("City", Person::city)
            .build();

    private static final List<Person> PEOPLE = List.of(
            new Person("Ada, Lovelace", 36, "London"),
            new Person("Grace \"Amazing\" Hopper", 85, "New York"));

    @Test
    void exportsAndImportsCsvWithQuotes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelExporter.write(out, ExcelFormat.CSV, SPEC, PEOPLE);

        List<Map<String, String>> rows = read(out, ExcelFormat.CSV);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()).containsEntry("Name", "Ada, Lovelace")
                .containsEntry("Age", "36")
                .containsEntry("City", "London");
        assertThat(rows.get(1)).containsEntry("Name", "Grace \"Amazing\" Hopper");
    }

    @Test
    void exportsAndImportsXlsx() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelExporter.write(out, ExcelFormat.XLSX, SPEC, PEOPLE);

        List<Map<String, String>> rows = read(out, ExcelFormat.XLSX);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()).containsEntry("Name", "Ada, Lovelace")
                .containsEntry("Age", "36")
                .containsEntry("City", "London");
    }

    @Test
    void reportsRowErrorsWithoutAbortingTheImport() {
        String csv = "name,age\nAda,36\n,unknown\n";
        ImportResult result = ExcelImporter.importData(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                ExcelFormat.CSV,
                ExcelImportSpec.<Person>builder()
                        .mapper(row -> new Person(row.cell("name"), Integer.parseInt(row.cell("age")), null))
                        .validator((person, ctx) -> {
                            if (person.name().isBlank()) {
                                ctx.reject("name", person.name(), "Name must not be blank");
                            }
                        })
                        .build());

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.successRows()).isEqualTo(1);
        assertThat(result.failedRows()).isEqualTo(1);
        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.rowNumber()).isEqualTo(3));
    }

    private static List<Map<String, String>> read(ByteArrayOutputStream out, ExcelFormat format) {
        List<Map<String, String>> rows = new ArrayList<>();
        ExcelReader.read(new ByteArrayInputStream(out.toByteArray()), format, RowReadOptions.defaults(), row ->
                rows.add(IntStream.range(0, row.headers().size())
                        .boxed()
                        .collect(Collectors.toMap(row.headers()::get, row::cell))));
        return rows;
    }
}
