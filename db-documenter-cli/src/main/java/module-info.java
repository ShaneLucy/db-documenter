import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.shanelucy.dbdocumenter.cli {
  requires dev.shanelucy.dbdocumenter;
  requires info.picocli;
  requires java.logging;
  requires java.sql;
  requires static org.jspecify;

  opens dev.shanelucy.dbdocumenter.cli to
      info.picocli;
}
