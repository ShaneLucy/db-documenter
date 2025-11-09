package db.documenter.internal.renderer.api;

public interface PumlRenderer<T> {

    String render(T input);
}
