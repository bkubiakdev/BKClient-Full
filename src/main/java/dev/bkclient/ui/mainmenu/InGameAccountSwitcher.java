package dev.bkclient.ui.mainmenu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.file.SafeJsonFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.session.Session;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

/**
 * Optional access to the account manager supplied by In-Game Account Switcher.
 * IAS 9 has no auto-login and launchers start the game as an offline nick, so BK-Client remembers the last IAS
 * account itself (name only, never credentials) and logs it back in once per game start through the public IAS API.
 */
public final class InGameAccountSwitcher {
    /**
     * Restore progress for other mods (BK Menu): "working", "done", "skipped" or "failed".
     * While it is "working" nobody else should start a second login.
     */
    public static final String RESTORE_PROPERTY = "bkclient.ias.restore";
    private static final String STORE_FILE = "ias-account.json";
    private static final Constructor<? extends Screen> ACCOUNT_SCREEN = findAccountScreen();
    private static boolean restoreAttempted;
    private static volatile boolean restoring;
    private static String seenName;
    private static UUID seenId;

    private InGameAccountSwitcher() {
    }

    private static Constructor<? extends Screen> findAccountScreen() {
        if (!FabricLoader.getInstance().isModLoaded("ias")) return null;
        try {
            // Keep IAS optional, and use Screen.class so the signature survives remapping.
            return Class.forName("ru.vidtu.ias.screen.AccountScreen", false,
                    InGameAccountSwitcher.class.getClassLoader())
                    .asSubclass(Screen.class).getConstructor(Screen.class);
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            BKClientInit.LOGGER.warn("Cannot initialize In-Game Account Switcher integration", e);
            return null;
        }
    }

    public static boolean isAvailable() {
        return ACCOUNT_SCREEN != null;
    }

    /** Client tick: remembers account switches and covers the vanilla title screen when the BK menu is off. */
    public static void tick(MinecraftClient client) {
        if (!isAvailable()) return;
        Session session = client.getSession();
        if (session == null) return;
        if (seenName == null) {
            // The launch session is the launcher's offline nick; remembering it would undo the last real choice.
            seenName = session.getUsername();
            seenId = session.getUuidOrNull();
        } else if (!restoring && (!session.getUsername().equals(seenName) || !Objects.equals(session.getUuidOrNull(), seenId))) {
            seenName = session.getUsername();
            seenId = session.getUuidOrNull();
            String name = iasName(seenName);
            if (name != null && !name.equals(storedLastAccount())) storeLastAccount(name);
        }
        if (client.currentScreen instanceof TitleScreen title) restoreLastAccount(title);
    }

    /** Called from the title screen tick; logs the remembered IAS account back in once per game start. */
    public static void restoreLastAccount(Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (restoreAttempted || !isAvailable() || client.getOverlay() != null
                || client.currentScreen != parent || client.world != null) return;
        restoreAttempted = true;
        String name = "";
        try {
            Class<?> ias = Class.forName("ru.vidtu.ias.IAS");
            if ((boolean) ias.getMethod("disabled").invoke(null)) {
                signal("skipped");
                return;
            }
            Session session = client.getSession();
            name = storedLastAccount();
            if (name.isBlank()) name = iasConfigLastAccount();
            // Started on a real Microsoft session, nothing remembered, or already on that account: never log in twice.
            if (name.isBlank() || session != null && (realMicrosoftSession(session)
                    || session.getUsername().equalsIgnoreCase(name))) {
                signal("skipped");
                return;
            }
            Object account = findAccount(name);
            if (account == null) {
                BKClientInit.LOGGER.info("The last IAS account is no longer saved in IAS, keeping the launcher nick");
                signal("skipped");
                return;
            }
            Class<?> accountType = Class.forName("ru.vidtu.ias.account.Account");
            String accountName = (String) accountType.getMethod("name").invoke(account);
            restoring = true;
            signal("working");
            BKClientInit.LOGGER.info("Restoring the last IAS account ({})", accountType.getMethod("type").invoke(account));
            if (!(boolean) accountType.getMethod("canLogin").invoke(account)) {
                // Offline accounts need no credentials; IAS builds the same login data for them.
                install(client, offlineData(accountName), accountName);
                return;
            }
            Class<?> handlerType = Class.forName("ru.vidtu.ias.auth.handlers.LoginHandler");
            Method login = loginMethod(accountType, handlerType);
            Handler handler = new Handler(client, accountName);
            Object proxy = Proxy.newProxyInstance(handlerType.getClassLoader(), new Class<?>[]{handlerType}, handler);
            Object[] args = login.getParameterCount() == 2 ? new Object[]{proxy, (Runnable) () -> { }} : new Object[]{proxy};
            Executor executor = (Executor) ias.getMethod("executor").invoke(null);
            executor.execute(() -> {
                try {
                    // IAS refreshes the credentials itself, exactly like its own login screen.
                    login.invoke(account, args);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    handler.fail(unwrap(failure));
                }
            });
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            // Only the exception type: IAS messages could quote server responses.
            BKClientInit.LOGGER.warn("This In-Game Account Switcher version is not supported for automatic login ({})",
                    e.getClass().getSimpleName());
            finish("failed");
            if (!name.isBlank()) toast(name, false, "");
        }
    }

    public static boolean open(Screen parent) {
        if (!isAvailable()) return false;
        try {
            MinecraftClient.getInstance().setScreen(ACCOUNT_SCREEN.newInstance(parent));
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            BKClientInit.LOGGER.warn("Cannot open In-Game Account Switcher", e);
            return false;
        }
    }

    /**
     * A signed-in Microsoft session from a real launcher. BK-Launcher starts the offline nick with userType "msa"
     * and the access token "0", so the account type alone does not tell them apart.
     */
    private static boolean realMicrosoftSession(Session session) {
        String token = session.getAccessToken();
        return session.getAccountType() == Session.AccountType.MSA && token != null && token.length() > 1;
    }

    private static Method loginMethod(Class<?> accountType, Class<?> handlerType) throws NoSuchMethodException {
        for (Method method : accountType.getMethods()) {
            Class<?>[] params = method.getParameterTypes();
            if (method.getName().equals("login") && params.length >= 1 && params[0] == handlerType
                    && (params.length == 1 || params.length == 2 && params[1] == Runnable.class)) return method;
        }
        throw new NoSuchMethodException("Account.login");
    }

    private static Object offlineData(String name) throws ReflectiveOperationException {
        Object uuid = Class.forName("ru.vidtu.ias.account.OfflineAccount").getMethod("uuid", String.class).invoke(null, name);
        return Class.forName("ru.vidtu.ias.auth.LoginData").getConstructor(String.class, UUID.class, String.class, boolean.class)
                .newInstance(name, uuid, "ias:offline", false);
    }

    /** IASMinecraft.account(MinecraftClient, LoginData) installs the session exactly like IAS's own login screen. */
    @SuppressWarnings("unchecked")
    private static void install(MinecraftClient client, Object data, String name) {
        CompletableFuture<Void> future;
        try {
            Method account = null;
            for (Method method : Class.forName("ru.vidtu.ias.IASMinecraft").getMethods()) {
                if (method.getName().equals("account") && method.getParameterCount() == 2) account = method;
            }
            if (account == null) throw new NoSuchMethodException("IASMinecraft.account");
            future = (CompletableFuture<Void>) account.invoke(null, client, data);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            future = CompletableFuture.failedFuture(unwrap(e));
        }
        future.whenComplete((ignored, failure) -> {
            if (failure == null) {
                BKClientInit.LOGGER.info("Logged back into the last IAS account");
                finish("done");
            } else {
                report(failure, name, false);
            }
        });
    }

    private static void report(Throwable failure, String name, boolean needsPassword) {
        if (needsPassword) {
            BKClientInit.LOGGER.info("The last IAS account is protected with an IAS password, waiting for the player");
            finish("failed");
            toast(name, true, "");
            return;
        }
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
        String key = friendlyKey(cause);
        BKClientInit.LOGGER.warn("Automatic IAS login failed ({}{})", cause.getClass().getSimpleName(), key == null ? "" : ", " + key);
        finish("failed");
        toast(name, false, key == null ? "" : Text.translatable(key).getString());
    }

    /** IAS's translatable reason (FriendlyException.key) anywhere in the cause chain, found without naming its class. */
    private static String friendlyKey(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (!t.getClass().getSimpleName().equals("FriendlyException")) continue;
            try {
                Object key = t.getClass().getMethod("key").invoke(t);
                if (key instanceof String text && !text.isBlank()) return text;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static void finish(String state) {
        restoring = false;
        MinecraftClient client = MinecraftClient.getInstance();
        // The restored session is not a "switch" by the player, but remembering it again is harmless.
        client.execute(() -> {
            Session session = client.getSession();
            if (session != null) {
                seenName = session.getUsername();
                seenId = session.getUuidOrNull();
            }
            signal(state);
        });
    }

    private static void signal(String state) {
        System.setProperty(RESTORE_PROPERTY, state);
    }

    private static void toast(String name, boolean password, String reason) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            boolean pl = client.getLanguageManager().getLanguage().startsWith("pl");
            Text title = Text.literal(password
                    ? (pl ? "Konto " + name + " ma hasło IAS" : "Account " + name + " has an IAS password")
                    : (pl ? "Nie udało się zalogować na " + name : "Could not log in as " + name));
            Text description = Text.literal(password
                    ? (pl ? "Zaloguj się w Kontach (IAS)" : "Log in from Accounts (IAS)")
                    : !reason.isBlank() ? reason : (pl ? "Spróbuj ponownie w Kontach (IAS)" : "Try again from Accounts (IAS)"));
            SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION, title, description);
        });
    }

    private static Throwable unwrap(Throwable failure) {
        return failure instanceof InvocationTargetException wrapped && wrapped.getCause() != null ? wrapped.getCause() : failure;
    }

    private static Object findAccount(String name) throws ReflectiveOperationException {
        Class<?> accountType = Class.forName("ru.vidtu.ias.account.Account");
        Method accountName = accountType.getMethod("name");
        for (Object account : (List<?>) Class.forName("ru.vidtu.ias.config.IASStorage").getField("ACCOUNTS").get(null)) {
            if (name.equalsIgnoreCase((String) accountName.invoke(account))) return account;
        }
        return null;
    }

    /** IAS name of the given session nick, or null when it is not an IAS account. */
    private static String iasName(String sessionName) {
        try {
            Object account = findAccount(sessionName);
            return account == null ? null : (String) Class.forName("ru.vidtu.ias.account.Account").getMethod("name").invoke(account);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Path storeFile() {
        return FileManager.DIRECTORY.toPath().resolve(STORE_FILE);
    }

    private static String storedLastAccount() {
        Path file = storeFile();
        try {
            if (!Files.exists(file) && !Files.exists(SafeJsonFile.backup(file))) return "";
            JsonObject json = SafeJsonFile.read(file);
            return json.has("lastAccount") && json.get("lastAccount").isJsonPrimitive()
                    ? json.get("lastAccount").getAsString().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static void storeLastAccount(String name) {
        JsonObject json = new JsonObject();
        json.addProperty("lastAccount", name);
        try {
            SafeJsonFile.write(storeFile(), json);
        } catch (Exception e) {
            BKClientInit.LOGGER.warn("Cannot remember the last IAS account ({})", e.getClass().getSimpleName());
        }
    }

    /** The "lastAccount" name kept in config/ias.json by older IAS versions (IAS 9.0.8 no longer updates it). */
    private static String iasConfigLastAccount() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("ias.json");
        try {
            if (!Files.isRegularFile(file)) return "";
            var json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!json.isJsonObject() || !json.getAsJsonObject().has("lastAccount")) return "";
            var value = json.getAsJsonObject().get("lastAccount");
            return value.isJsonPrimitive() ? value.getAsString().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** LoginHandler without a screen: a password-protected account is reported instead of asking for the password. */
    private static final class Handler implements java.lang.reflect.InvocationHandler {
        private final MinecraftClient client;
        private final String name;
        private volatile boolean passwordRequested;
        private volatile boolean done;

        Handler(MinecraftClient client, String name) {
            this.client = client;
            this.name = name;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "cancelled":
                    return false;
                case "password":
                    this.passwordRequested = true;
                    return CompletableFuture.failedFuture(new IllegalStateException("IAS password required"));
                case "success":
                    if (args != null && args.length >= 2 && Boolean.TRUE.equals(args[1])) saveIasStorage();
                    if (!this.done && args != null && args.length >= 1) {
                        this.done = true;
                        install(this.client, args[0], this.name);
                    }
                    return null;
                case "error":
                    this.fail(args != null && args.length >= 1 && args[0] instanceof Throwable t ? t : new IllegalStateException());
                    return null;
                case "stage":
                    return null;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return args != null && args.length == 1 && proxy == args[0];
                case "toString":
                    return "BKClient IAS login";
                default:
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
            }
        }

        void fail(Throwable error) {
            if (this.done) return;
            this.done = true;
            report(error, this.name, this.passwordRequested);
        }

        /** IAS keeps the refreshed tokens itself, exactly like its own login screen does. */
        private static void saveIasStorage() {
            try {
                Class<?> ias = Class.forName("ru.vidtu.ias.IAS");
                ias.getMethod("disclaimersStorage").invoke(null);
                ias.getMethod("saveStorage").invoke(null);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                BKClientInit.LOGGER.warn("IAS could not save its account storage ({})", e.getClass().getSimpleName());
            }
        }
    }
}
