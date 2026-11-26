package dev.shanelucy.dbdocumenter.internal.renderer.api;

@FunctionalInterface
public interface PumlRenderer<T> {

  String render(T input);
}
