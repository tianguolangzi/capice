package org.molgenis.capice.vcf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.molgenis.capice.vcf.TsvUtils.TSV_FORMAT;

import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder.OutputType;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Iterator;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class TsvToVcfMapperImpl implements TsvToVcfMapper {
  private static final String INFO_ID_CAPICE = "CAP";

  @Override
  public void map(Path sortedTsvPath, Path outputVcfPath, Settings settings) {

    VariantContextWriter variantContextWriter =
        new VariantContextWriterBuilder()
            .setOutputFile(outputVcfPath.toFile())
            .setOutputFileType(OutputType.BLOCK_COMPRESSED_VCF)
            .build();
    try {
      VCFHeaderLineCount vcfHeaderLineCount = VCFHeaderLineCount.A;
      VCFHeaderLineType vcfHeaderLineType = VCFHeaderLineType.Float;
      String description = "CAPICE pathogenicity prediction";
      VCFInfoHeaderLine capiceVcfHeaderLine =
          new VCFInfoHeaderLine(INFO_ID_CAPICE, vcfHeaderLineCount, vcfHeaderLineType, description);
      VCFHeaderLine appVcfHeaderLine = new VCFHeaderLine("CAP", settings.getAppVersion());
      VCFHeader vcfHeader = new VCFHeader();
      vcfHeader.addMetaDataLine(appVcfHeaderLine);
      vcfHeader.addMetaDataLine(capiceVcfHeaderLine);

      variantContextWriter.writeHeader(vcfHeader);

      try(Reader in = new InputStreamReader(new FileInputStream(sortedTsvPath.toFile()), UTF_8);
          CSVParser csvParser = TSV_FORMAT.parse(in)) {
        Iterator<CSVRecord> iterator = csvParser.iterator();
        iterator.next(); // skip header line (TSV_FORMAT.withSkipHeaderLine doesn't seem to work)
        while (iterator.hasNext()) {
          CSVRecord record = iterator.next();
          String chrPosRefAlt = record.get(0);
          String[] tokens = chrPosRefAlt.split("_");
          String chr = tokens[0];
          String pos = tokens[1];
          String ref = tokens[2];
          String alt = tokens[3];
          float prediction = getPrediction(record);

          long start = Long.parseLong(pos);
          long stop = Long.parseLong(pos) + (ref.length() - 1); // correct ??
          VariantContextBuilder variantContextBuilder = new VariantContextBuilder();
          variantContextBuilder.chr(chr);
          variantContextBuilder.start(start);
          variantContextBuilder.stop(stop);
          variantContextBuilder.alleles(ref, alt);
          variantContextBuilder.attribute(INFO_ID_CAPICE, singletonList(prediction));
          variantContextWriter.add(variantContextBuilder.make());
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    } finally {
      variantContextWriter.close();
    }
  }

  private float getPrediction(CSVRecord record) {
    try{
    return Float.parseFloat(record.get(4));
    }
    catch(NumberFormatException e){
      throw new IllegalStateException(e);
    }
  }
}
