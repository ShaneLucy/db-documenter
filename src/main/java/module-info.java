import org.jspecify.annotations.NullMarked;

@NullMarked
module db.documenter {
  requires java.sql;
  requires java.logging;
  requires org.jspecify;

  exports db.documenter;
}
