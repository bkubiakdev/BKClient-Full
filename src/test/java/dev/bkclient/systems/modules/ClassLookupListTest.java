package dev.bkclient.systems.modules;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

public final class ClassLookupListTest {
   public static void main(String[] args) throws Exception {
      ClassLookupList<Object> list = new ClassLookupList<>();
      String first = new String("first"), second = new String("second");
      check(list.firstOfClass(String.class) == null, "missing registration");
      list.addAll(List.of(1, first, second));
      check(list.firstOfClass(String.class) == first, "first registration wins");
      check(list.firstOfClass(Number.class) == null, "exact class, not assignability");
      list.remove(first);
      check(list.firstOfClass(String.class) == second, "removal reveals duplicate");
      list.add(0, first);
      check(list.firstOfClass(String.class) == first, "insertion before cached entry");
      list.set(0, 2);
      check(list.firstOfClass(String.class) == second, "replacement invalidates cache");
      list.subList(1, list.size()).clear();
      check(list.firstOfClass(String.class) == null, "sublist clear removes cached entry");

      list.clear(); list.addAll(List.of(first, second));
      check(list.firstOfClass(String.class) == first, "cache before iterator edit");
      var iterator = list.listIterator(); iterator.next(); iterator.set(1);
      check(list.firstOfClass(String.class) == second, "list iterator replacement");
      iterator.next(); iterator.remove();
      check(list.firstOfClass(String.class) == null, "iterator removal");
      iterator.add(first);
      check(list.firstOfClass(String.class) == first, "list iterator insertion");
      list.replaceAll(value -> value instanceof String ? second : value);
      check(list.firstOfClass(String.class) == second, "replaceAll invalidates cache");
      list.removeIf(value -> value instanceof String);
      check(list.firstOfClass(String.class) == null, "removeIf invalidates cache");

      list.clear(); list.addAll(List.of(first, second));
      check(list.firstOfClass(String.class) == first, "cache before reordering");
      Collections.swap(list, 0, 1);
      check(list.firstOfClass(String.class) == second, "reordering changes first match");
      list.sort(Comparator.comparing(Object::toString));
      check(list.firstOfClass(String.class) == first, "sort changes first match");
      list.subList(0, 1).set(0, second);
      check(list.firstOfClass(String.class) == second, "sublist replacement");
      list.removeAll(List.of(second));
      check(list.firstOfClass(String.class) == null, "removeAll clears cache");
      list.add(first); check(list.firstOfClass(String.class) == first, "register after removal");
      list.retainAll(List.of(1));
      check(list.firstOfClass(String.class) == null, "retainAll clears cache");

      // Preserve the existing name-based lookup even across class loaders.
      byte[] bytes;
      try (var input = Sample.class.getResourceAsStream("ClassLookupListTest$Sample.class")) {
         bytes = input.readAllBytes();
      }
      Class<?> foreignType = new ClassLoader(null) {
         Class<?> define() { return defineClass(Sample.class.getName(), bytes, 0, bytes.length); }
      }.define();
      Object foreign = foreignType.getConstructor().newInstance();
      list.add(foreign);
      check(list.firstOfClass(Sample.class) == foreign, "same binary name across loaders");
      list.clear(); list.add(first);
      try (var workers = Executors.newFixedThreadPool(4)) {
         var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
         for (int i = 0; i < 4; i++) futures.add(workers.submit(() -> {
            for (int n = 0; n < 10_000; n++) check(list.firstOfClass(String.class) == first, "concurrent read");
         }));
         for (var future : futures) future.get();
      }
      list.clear();
      check(list.firstOfClass(String.class) == null, "clear after concurrent reads");
      System.out.println("Module class lookup: duplicates, live list edits, loaders and concurrent reads passed");
   }

   public static final class Sample { public Sample() {} }
   private static void check(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
}
