import org.jspecify.annotations.NullMarked;

@NullMarked
module db.documenter {
  requires java.sql;
  requires java.logging;
  requires org.jspecify;
  requires info.picocli;

  exports db.documenter;

  opens db.documenter.cli to
      info.picocli;
}
