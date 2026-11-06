package dev.bkclient.utility.game;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public final class BaritoneBridge {
    private static String lastError = "";

    // The mod classpath is fixed for the lifetime of the client. In particular,
    // do not search every JAR again for each XRay setting when Baritone is absent.
    // Resolve without initialization: provider readiness is still checked live.
    private static final class ApiLookup {
        private static final Class<?> TYPE;
        private static final String ERROR;

        static {
            Class<?> type = null;
            String error = "";
            try {
                type = Class.forName("baritone.api.BaritoneAPI", false, BaritoneBridge.class.getClassLoader());
            } catch (ClassNotFoundException | LinkageError exception) {
                error = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            }
            TYPE = type;
            ERROR = error;
        }
    }

    public static boolean isAvailable() {
        if (ApiLookup.TYPE == null) {
            lastError = ApiLookup.ERROR;
            return false;
        }
        try {
            getPrimaryBaritone();
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static boolean gotoBlock(BlockPos pos) {
        return gotoGoal("baritone.api.pathing.goals.GoalBlock", pos);
    }

    public static boolean gotoInteractableBlock(BlockPos pos) {
        return gotoGoal("baritone.api.pathing.goals.GoalGetToBlock", pos);
    }

    public static boolean farm(int range, BlockPos center) {
        try {
            Object baritone = getPrimaryBaritone();
            Object farmProcess = invoke(baritone, "getFarmProcess");
            Method method = findCompatibleMethod(farmProcess.getClass(), "farm", int.class, center);
            method.invoke(farmProcess, range, center);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static boolean setReplantCrops(boolean enabled) {
        return setSettingValue("replantCrops", enabled);
    }

    public static boolean setReplantNetherWart(boolean enabled) {
        return setSettingValue("replantNetherWart", enabled);
    }

    public static boolean setFarmMaxScanSize(int size) {
        return setSettingValue("farmMaxScanSize", size);
    }

    public static boolean setFarmAllowedTargets(Collection<String> targetNames) {
        try {
            Object baritone = getPrimaryBaritone();
            Object farmProcess = invoke(baritone, "getFarmProcess");
            Method method = findMethod(farmProcess.getClass(), "setAllowedTargetNames", String[].class);
            method.invoke(farmProcess, (Object) targetNames.toArray(new String[0]));
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static boolean protectBlocksFromBreaking(Collection<Block> blocks) {
        return addToSettingList("blocksToDisallowBreaking", blocks)
                && addToSettingList("blocksToAvoidBreaking", blocks);
    }

    public static boolean restrictBreakingTo(Collection<Block> blocks) {
        return setSettingValue("allowBreak", false)
                && setSettingList("allowBreakAnyway", blocks);
    }

    public static boolean setAllowBreak(boolean enabled) {
        return setSettingValue("allowBreak", enabled);
    }

    private static boolean gotoGoal(String goalClassName, BlockPos pos) {
        try {
            Object baritone = getPrimaryBaritone();
            Object customGoalProcess = invoke(baritone, "getCustomGoalProcess");
            Object goal = createGoal(goalClassName, pos);
            invokeOneArg(customGoalProcess, "setGoalAndPath", goal);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static boolean mineByName(int quantity, Collection<String> blockNames) {
        if (blockNames.isEmpty()) {
            lastError = "No block names";
            return false;
        }

        try {
            Object baritone = getPrimaryBaritone();
            Object mineProcess = invoke(baritone, "getMineProcess");
            Method method = findMethod(mineProcess.getClass(), "mineByName", int.class, String[].class);
            method.invoke(mineProcess, quantity, (Object) blockNames.toArray(new String[0]));
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static boolean mineAtPositions(int quantity, Collection<String> blockNames, Collection<BlockPos> positions) {
        if (blockNames.isEmpty()) {
            lastError = "No block names";
            return false;
        }
        if (positions.isEmpty()) {
            lastError = "No positions";
            return false;
        }

        try {
            Object baritone = getPrimaryBaritone();
            Object mineProcess = invoke(baritone, "getMineProcess");
            Method method = findMethod(mineProcess.getClass(), "mineAtPositions", int.class, String[].class, BlockPos[].class);
            method.invoke(mineProcess, quantity, (Object) blockNames.toArray(new String[0]),
                    (Object) positions.toArray(new BlockPos[0]));
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    private static boolean setSettingList(String settingName, Collection<?> entries) {
        try {
            Object settings = getSettings();
            Object setting = settings.getClass().getField(settingName).get(settings);
            Field value = setting.getClass().getField("value");
            List<Object> updated = new ArrayList<>();
            for (Object entry : entries) {
                if (entry != null && !updated.contains(entry)) {
                    updated.add(entry);
                }
            }
            value.set(setting, updated);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    private static boolean addToSettingList(String settingName, Collection<?> additions) {
        try {
            Object settings = getSettings();
            Object setting = settings.getClass().getField(settingName).get(settings);
            Field value = setting.getClass().getField("value");
            Object current = value.get(setting);
            List<Object> updated = new ArrayList<>();
            if (current instanceof Collection<?> collection) {
                updated.addAll(collection);
            }
            for (Object addition : additions) {
                if (addition != null && !updated.contains(addition)) {
                    updated.add(addition);
                }
            }
            value.set(setting, updated);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static boolean setAllowOnlyExposedOres(boolean enabled) {
        return setSettingValue("allowOnlyExposedOres", enabled);
    }

    public static boolean setMineScanDroppedItems(boolean enabled) {
        return setSettingValue("mineScanDroppedItems", enabled);
    }

    public static boolean setMineDropLoiterDuration(long durationMs) {
        return setSettingValue("mineDropLoiterDurationMSThanksLouca", durationMs);
    }

    /**
     * Kara za chodzenie po wodzie przy liczeniu trasy (domyslnie 3). Podbicie sprawia, ze baritone
     * trasuje WOKOL wody zamiast przez nia \u00E2\u20AC\u201D gracz nie wchodzi do wody i nie drga/spamuje pakietami.
     */
    public static boolean setWalkOnWaterOnePenalty(double penalty) {
        return setSettingValue("walkOnWaterOnePenalty", penalty);
    }

    public static boolean setAvoidWaterBlocks(boolean enabled) {
        try {
            Object settings = getSettings();
            Object setting = settings.getClass().getField("blocksToAvoid").get(settings);
            Field value = setting.getClass().getField("value");
            Object current = value.get(setting);
            List<Object> updated = new ArrayList<>();
            if (current instanceof Collection<?> collection) {
                for (Object entry : collection) {
                    if (entry != Blocks.WATER && entry != Blocks.LAVA && entry != Blocks.BUBBLE_COLUMN
                            && !updated.contains(entry)) {
                        updated.add(entry);
                    }
                }
            }
            if (enabled) {
                updated.add(Blocks.WATER);
                updated.add(Blocks.LAVA);
                updated.add(Blocks.BUBBLE_COLUMN);
            }
            value.set(setting, updated);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static boolean pickupAll() {
        try {
            Object baritone = getPrimaryBaritone();
            Object followProcess = invoke(baritone, "getFollowProcess");
            Method method = findMethod(followProcess.getClass(), "pickup", Predicate.class);
            Predicate<Object> allItems = stack -> true;
            method.invoke(followProcess, allItems);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static boolean stop() {
        try {
            Object baritone = getPrimaryBaritone();
            Object pathingBehavior = invoke(baritone, "getPathingBehavior");
            invoke(pathingBehavior, "cancelEverything");
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    public static String getLastError() {
        return lastError;
    }

    private static Object getPrimaryBaritone() throws ReflectiveOperationException {
        Class<?> api = getApiClass();
        Object provider = api.getMethod("getProvider").invoke(null);
        return invoke(provider, "getPrimaryBaritone");
    }

    private static Object getSettings() throws ReflectiveOperationException {
        Class<?> api = getApiClass();
        return api.getMethod("getSettings").invoke(null);
    }

    private static Class<?> getApiClass() throws ClassNotFoundException {
        if (ApiLookup.TYPE == null) throw new ClassNotFoundException(ApiLookup.ERROR);
        return ApiLookup.TYPE;
    }

    private static boolean setSettingValue(String settingName, Object newValue) {
        try {
            Object settings = getSettings();
            Object setting = settings.getClass().getField(settingName).get(settings);
            Field value = setting.getClass().getField("value");
            value.set(setting, newValue);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return false;
        }
    }

    private static Object createGoal(String className, BlockPos pos) throws ReflectiveOperationException {
        Class<?> goalClass = Class.forName(className);
        for (Constructor<?> constructor : goalClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 1) {
                try {
                    return constructor.newInstance(pos);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return goalClass.getConstructor(int.class, int.class, int.class).newInstance(pos.getX(), pos.getY(), pos.getZ());
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Method method = findMethod(target.getClass(), methodName);
        return method.invoke(target);
    }

    private static Object invokeOneArg(Object target, String methodName, Object argument)
            throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(argument.getClass())) {
                return method.invoke(target, argument);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static Method findCompatibleMethod(Class<?> owner, String name, Class<?> firstParamType, Object secondArg)
            throws NoSuchMethodException {
        for (Method method : owner.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals(name) && parameterTypes.length == 2 && parameterTypes[0] == firstParamType
                    && (secondArg == null || parameterTypes[1].isAssignableFrom(secondArg.getClass()))) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = owner.getMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private BaritoneBridge() {
    }
}

