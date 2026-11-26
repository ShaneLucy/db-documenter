package db.documenter.internal.renderer.api;

@FunctionalInterface
public interface PumlRenderer<T> {

  String render(T input);
}
