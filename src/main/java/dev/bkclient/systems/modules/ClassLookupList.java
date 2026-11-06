package dev.bkclient.systems.modules;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A mutable registration list with cached first-match class lookups.
 * AbstractList routes iterator, sublist, sort and replaceAll edits through the
 * same mutation methods, so callers holding getModules() cannot stale the cache.
 */
final class ClassLookupList<T> extends AbstractList<T> implements RandomAccess {
   private final ArrayList<T> entries = new ArrayList<>();
   private final ConcurrentHashMap<String, T> firstByClassName = new ConcurrentHashMap<>();

   T firstOfClass(Class<?> type) {
      String name = type.getName();
      T found = this.firstByClassName.get(name);
      if (found != null) return found;
      // Cache hits need no lock. Serialize cache fills with edits so a slow lookup
      // cannot put an old registration back into the cache after it was removed.
      synchronized (this) {
         found = this.firstByClassName.get(name);
         if (found != null) return found;
         for (T entry : this.entries) {
            if (entry.getClass().getName().equals(name)) {
               this.firstByClassName.put(name, entry);
               return entry;
            }
         }
         return null;
      }
   }

   @Override
   public T get(int index) {
      return this.entries.get(index);
   }

   @Override
   public int size() {
      return this.entries.size();
   }

   @Override
   public synchronized T set(int index, T element) {
      T previous = this.entries.set(index, element);
      this.firstByClassName.clear();
      return previous;
   }

   @Override
   public synchronized void add(int index, T element) {
      this.entries.add(index, element);
      this.modCount++;
      this.firstByClassName.clear();
   }

   @Override
   public synchronized T remove(int index) {
      T previous = this.entries.remove(index);
      this.modCount++;
      this.firstByClassName.clear();
      return previous;
   }

   @Override
   protected synchronized void removeRange(int fromIndex, int toIndex) {
      this.entries.subList(fromIndex, toIndex).clear();
      this.modCount++;
      this.firstByClassName.clear();
   }
}
