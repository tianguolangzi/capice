package org.molgenis.capice.vcf;

import com.google.code.externalsorting.csv.CsvExternalSort;
import com.google.code.externalsorting.csv.CsvSortOptions;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;

@Component
public class TsvSorterImpl implements TsvSorter {
  private static final CSVFormat TSV_FORMAT =
      CSVFormat.DEFAULT.withDelimiter('\t').withRecordSeparator('\n');

  @Override
  public void sortTsv(Path inputTsv, Path outputTsv) {
    CsvSortOptions sortOptions =
        new CsvSortOptions.Builder(
                CsvExternalSort.DEFAULTMAXTEMPFILES,
                new TsvRecordComparator(),
                1,
                CsvExternalSort.estimateAvailableMemory())
            .charset(StandardCharsets.UTF_8)
            .distinct(false)
            .numHeader(1)
            .skipHeader(false)
            .format(TSV_FORMAT)
            .build();

    try {
      List<File> sortInBatch = CsvExternalSort.sortInBatch(inputTsv.toFile(), null, sortOptions);
      CsvExternalSort.mergeSortedFiles(sortInBatch, outputTsv.toFile(), sortOptions, true);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}