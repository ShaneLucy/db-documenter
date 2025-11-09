package db.documenter.internal.writer;

import java.io.FileWriter;

public record PumlWriter(FileWriter fileWriter) {

  public void writeLine() {}
}
