package com.ddalkkak.date.batch;

import com.ddalkkak.date.service.place.PlaceCurationImporter;
import com.ddalkkak.date.service.place.PlaceCurationImporter.ImportResult;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 수작업 큐레이션 CSV import 도구 (--app.batch.place-import.file=/path/to/curation.csv)
@Component
@ConditionalOnProperty(prefix = "app.batch.place-import", name = "file")
public class PlaceImportRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PlaceImportRunner.class);

  private final PlaceCurationImporter importer;
  private final Path file;

  public PlaceImportRunner(
      PlaceCurationImporter importer, @Value("${app.batch.place-import.file}") Path file) {
    this.importer = importer;
    this.file = file;
  }

  @Override
  public void run(ApplicationArguments args) throws IOException {
    ImportResult result = importer.importCsv(file);
    log.info("큐레이션 import 완료: {}곳 반영, 오류 {}건", result.updated(), result.errors().size());
    result.errors().forEach(error -> log.warn("import 오류: {}", error));
    if (!result.errors().isEmpty()) {
      throw new IllegalStateException("import에 오류가 있습니다. 위 로그를 확인하세요.");
    }
  }
}
