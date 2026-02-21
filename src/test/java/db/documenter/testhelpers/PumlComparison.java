package db.documenter.testhelpers;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Objects;

public class PumlComparison {

  public static void comparePumlLineByLine(final String output, final List<String> expectedOutput) {
    final var resultLines = output.lines().toList();

    int max = Math.max(resultLines.size(), expectedOutput.size());

    for (int i = 0; i < max; i++) {
      String resultLine = (i < resultLines.size()) ? resultLines.get(i) : "<missing>";
      String expectedLine = (i < expectedOutput.size()) ? expectedOutput.get(i) : "<missing>";

      if (!Objects.equals(resultLine, expectedLine)) {
        fail(
            """
                                              PUML mismatch at line %d:
                                              Expected: %s
                                              Actual  : %s
                                              """
                .formatted(i + 1, expectedLine, resultLine));
      }
    }
  }
}
