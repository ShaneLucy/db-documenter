import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.shanelucy.dbdocumenter {
  requires java.sql;
  requires java.logging;
  requires static com.github.spotbugs.annotations;
  requires static org.jspecify;

  exports dev.shanelucy.dbdocumenter;

  // TODO(Phase 3): remove once DbDocumenterException is public
  exports dev.shanelucy.dbdocumenter.internal.exceptions to
      dev.shanelucy.dbdocumenter.cli;
  // TODO(Phase 3): remove once DbDocumenterException is public
  exports dev.shanelucy.dbdocumenter.internal.utils to
      dev.shanelucy.dbdocumenter.cli;
  // TODO(Phase 3): remove once DbDocumenterException is public
  exports dev.shanelucy.dbdocumenter.internal.validation to
      dev.shanelucy.dbdocumenter.cli;
}
