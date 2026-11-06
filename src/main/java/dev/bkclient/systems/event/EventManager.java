package dev.bkclient.systems.event;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import dev.bkclient.BKClientInit;

public class EventManager {
   private final ConcurrentHashMap<Type, CopyOnWriteArrayList<EventListener<?>>> listenerMap = new ConcurrentHashMap<>();
   private final Map<Class<?>, Field[]> declaredFieldsCache = new HashMap<>();
   private final Comparator<EventListener<?>> priorityOrder = Comparator.<EventListener<?>>comparingInt(listener -> listener.getPriority()).reversed();
   private final BiConsumer<List<EventListener<?>>, Comparator<EventListener<?>>> sortCallback = List::sort;
   /**
    * Listener failures, rate-limited: the first one per (listener, exception, throw site) is logged with
    * its stack trace, repeats are only counted and summarised at most every 30 s. A listener that throws
    * every frame used to print a full stack trace every frame.
    */
   private static final long ERROR_REPEAT_LOG_MS = 30_000L;
   private final ConcurrentHashMap<String, long[]> errorCounts = new ConcurrentHashMap<>();

   private void reportListenerError(EventListener<?> listener, Throwable error) {
      StackTraceElement[] trace = error.getStackTrace();
      String key = listener.getClass().getName() + '|' + error.getClass().getName() + '|' + (trace.length > 0 ? trace[0] : "");
      long now = System.currentTimeMillis();
      long[] state = this.errorCounts.computeIfAbsent(key, k -> new long[3]); // repeats, last log, logged once
      long repeats;
      synchronized (state) {
         if (state[2] == 0L) {
            state[2] = 1L;
            state[1] = now;
            repeats = 0L;
         } else {
            state[0]++;
            if (now - state[1] < ERROR_REPEAT_LOG_MS) return;
            repeats = state[0];
            state[0] = 0L;
            state[1] = now;
         }
      }
      if (repeats == 0L) BKClientInit.LOGGER.error("Event listener failed", error);
      else BKClientInit.LOGGER.error("Event listener failed {} more times: {}", repeats, error.toString());
   }

   public void subscribe(Object subscriber) {
      this.modifyEventListenerState(subscriber, (type, listener) -> {
         this.listenerMap.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
         this.sortCallback.accept(this.listenerMap.get(type), this.priorityOrder);
      });
   }

   public void unsubscribe(Object subscriber) {
      this.modifyEventListenerState(subscriber, (type, listener) -> {
         CopyOnWriteArrayList<EventListener<?>> listeners = this.listenerMap.get(type);
         if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
               this.listenerMap.remove(type);
            }
         }
      });
   }

   /** Cheap pre-check for hot call sites: lets them skip building an event nobody listens to. */
   public boolean hasListeners(Class<? extends Event> eventType) {
      return this.listenerMap.containsKey(eventType) && !BKClientInit.INSTANCE.isPanic();
   }

   public <T extends Event> void triggerEvent(T event) {
      Type eventType = event.getClass();
      List<EventListener<?>> listeners = this.listenerMap.get(eventType);
      if (listeners != null && !BKClientInit.INSTANCE.isPanic()) {
         for (EventListener<?> listener : listeners) {
            if (BKClientInit.INSTANCE.isPanic()) break;
            try {
               ((EventListener<T>)listener).onEvent(event);
            } catch (Throwable var7) {
               this.reportListenerError(listener, var7);
            }
         }
      }
   }

   private void modifyEventListenerState(Object o, BiConsumer<Type, EventListener<?>> action) {
      for (Field field : this.getCachedDeclaredFields(o.getClass())) {
         if (field.getType() == EventListener.class) {
            EventListener<?> eventListener = this.getEventListener(o, field);
            if (eventListener != null) {
               Type eventType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
               action.accept(eventType, eventListener);
            }
         }
      }
   }

   private Field[] getCachedDeclaredFields(Class<?> clazz) {
      return this.declaredFieldsCache.computeIfAbsent(clazz, Class::getDeclaredFields);
   }

   private EventListener<?> getEventListener(Object o, Field field) {
      boolean accessible = field.canAccess(o);
      field.setAccessible(true);

      Object var5;
      try {
         return (EventListener<?>)field.get(o);
      } catch (IllegalAccessException var9) {
         BKClientInit.LOGGER.error("Event listener field not readable: {}", field, var9);
         var5 = null;
      } finally {
         field.setAccessible(accessible);
      }

      return (EventListener<?>)var5;
   }
}
