import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.shanelucy.dbdocumenter {
  requires java.sql;
  requires java.logging;
  requires static org.jspecify;
  requires info.picocli;

  exports dev.shanelucy.dbdocumenter;

  opens dev.shanelucy.dbdocumenter.cli to
      info.picocli;
}
