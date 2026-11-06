package dev.bkclient.combatengine.api.events;

import dev.bkclient.combatengine.api.utils.chat.ChatUtils;
import dev.bkclient.combatengine.client.bots.core.BotContext;
import dev.bkclient.combatengine.client.modules.Module;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;

public class EventInvoker {
   private static final ConcurrentHashMap<Class<?>, Object> recoveredField1 = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<Class<? extends Event>, List<EventInvoker.Invocation>> recoveredField2 = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Long> recoveredField3 = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Long> recoveredField4 = new ConcurrentHashMap<>();
   private static final boolean recoveredField5 = Boolean.parseBoolean(System.getProperty("aura.perf.debug", "false"));
   private static final long recoveredField6 = Long.getLong("aura.perf.handlerMs", 8L) * 1000000L;
   private static final long recoveredField7 = Long.getLong("aura.perf.eventMs", 18L) * 1000000L;
   private static final long recoveredField8 = Long.getLong("aura.perf.cooldownMs", 1000L) * 1000000L;
   private static volatile boolean recoveredField9 = true;

   public static void register(Object obj) {
      recoveredField1.putIfAbsent(obj.getClass(), obj);
      recoveredField9 = true;
   }

   public static void unregister(Object obj) {
      recoveredField1.remove(obj.getClass());
      recoveredField9 = true;
   }

   public static void clean() {
      recoveredField1.clear();
      recoveredField2.clear();
      recoveredField9 = false;
   }

   public static void invoke(Event event) throws IllegalAccessException, InvocationTargetException, InstantiationException {
      if (!BotContext.isBot()) {
         long var1 = recoveredField5 ? System.nanoTime() : 0L;
         if (recoveredField9) {
            recoveredMethod1();
         }

         List<EventInvoker.Invocation> var3 = recoveredField2.get(event.getClass());
         if (var3 != null && !var3.isEmpty()) {
            for (EventInvoker.Invocation var5 : var3) {
               if (recoveredField1.containsKey(var5.listener().getClass())) {
                  Method var6 = var5.method();
                  long var7 = recoveredField5 ? System.nanoTime() : 0L;

                  try {
                     var6.invoke(var5.listener(), event);
                  } catch (InvocationTargetException var17) {
                     recoveredMethod2(var5, event, var17.getCause() == null ? var17 : var17.getCause());
                  } catch (Throwable var18) {
                     recoveredMethod2(var5, event, var18);
                  } finally {
                     if (recoveredField5) {
                        long var12 = System.nanoTime() - var7;
                        if (var12 >= recoveredField6) {
                           recoveredMethod3(event, var5, var12);
                        }
                     }
                  }
               }
            }

            if (recoveredField5) {
               long var20 = System.nanoTime() - var1;
               if (var20 >= recoveredField7) {
                  recoveredMethod4(event, var20, var3.size());
               }
            }
         }
      }
   }

   public static boolean hasListeners(Class<? extends Event> eventClass) {
      if (recoveredField9) {
         recoveredMethod1();
      }

      List var1 = recoveredField2.get(eventClass);
      return var1 != null && !var1.isEmpty();
   }

   private static synchronized void recoveredMethod1() {
      if (recoveredField9) {
         ConcurrentHashMap<Class<? extends Event>, List<EventInvoker.Invocation>> var0 = new ConcurrentHashMap<>();

         for (Object var2 : recoveredField1.values()) {
            for (Method var6 : var2.getClass().getDeclaredMethods()) {
               if (var6.isAnnotationPresent(EventLink.class)) {
                  Class[] var7 = var6.getParameterTypes();
                  if (var7.length == 1 && Event.class.isAssignableFrom(var7[0])) {
                     Class<? extends Event> var8 = var7[0].asSubclass(Event.class);
                     var6.setAccessible(true);
                     var0.computeIfAbsent(var8, key -> new ArrayList<>())
                        .add(new EventInvoker.Invocation(var2, var6, var6.getAnnotation(EventLink.class).priority()));
                  }
               }
            }
         }

         for (List<EventInvoker.Invocation> var10 : var0.values()) {
            var10.sort((a, b) -> {
               int var2x = Integer.compare(b.priority(), a.priority());
               if (var2x != 0) {
                  return var2x;
               }

               int var3 = a.listener().getClass().getName().compareTo(b.listener().getClass().getName());
               return var3 != 0 ? var3 : a.method().getName().compareTo(b.method().getName());
            });
         }

         recoveredField2.clear();
         recoveredField2.putAll(var0);
         recoveredField9 = false;
      }
   }

   private static void recoveredMethod2(EventInvoker.Invocation invocation, Event event, Throwable error) {
      if (error instanceof OutOfMemoryError var8) {
         throw var8;
      } else {
         Object var3 = invocation.listener();
         String var4 = var3.getClass().getSimpleName();
         System.err
            .println("[aura] " + var4 + "#" + invocation.method().getName() + " failed on " + event.getClass().getSimpleName() + " - handler disabled");
         error.printStackTrace();
         if (var3 instanceof Module var5) {
            try {
               var5.setEnabled(false);
            } catch (Throwable var7) {
            }

            unregister(var5);
            MinecraftClient var6 = MinecraftClient.getInstance();
            if (var6 != null && var6.isOnThread()) {
               ChatUtils.sendMessage(
                  "§c"
                     + var5.getName()
                     + " disabled after an error: "
                     + error.getClass().getSimpleName()
                     + (error.getMessage() == null ? "" : " (" + error.getMessage() + ")")
               );
            }
         }
      }
   }

   private static void recoveredMethod3(Event event, EventInvoker.Invocation invocation, long elapsedNanos) {
      String var4 = invocation.listener().getClass().getSimpleName();
      String var5 = invocation.method().getName();
      String var6 = event.getClass().getSimpleName();
      String var7 = "handler:" + var6 + ":" + var4 + "#" + var5;
      if (recoveredMethod5(recoveredField3, var7)) {
         System.out.println(String.format(Locale.ROOT, "[PerfDebug] Slow handler: %s -> %s#%s took %.2f ms", var6, var4, var5, elapsedNanos / 1000000.0));
      }
   }

   private static void recoveredMethod4(Event event, long elapsedNanos, int invocationCount) {
      String var4 = event.getClass().getSimpleName();
      String var5 = "event:" + var4;
      if (recoveredMethod5(recoveredField4, var5)) {
         System.out
            .println(String.format(Locale.ROOT, "[PerfDebug] Slow event: %s took %.2f ms for %d handlers", var4, elapsedNanos / 1000000.0, invocationCount));
      }
   }

   private static boolean recoveredMethod5(ConcurrentHashMap<String, Long> warnings, String key) {
      long var2 = System.nanoTime();
      Long var4 = (Long)warnings.get(key);
      if (var4 != null && var2 - var4 < recoveredField8) {
         return false;
      }

      warnings.put(key, var2);
      return true;
   }

   private record Invocation(Object listener, Method method, int priority) {

      private Invocation(Object listener, Method method, int priority) {
         this.listener = listener;
         this.method = method;
         this.priority = priority;
      }

      public Object listener() {
         return this.listener;
      }

      public Method method() {
         return this.method;
      }

      public int priority() {
         return this.priority;
      }
   }
}
