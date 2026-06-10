package github.com.gengyoubo.replayneo.platform.input;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.InputConstants;
import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.api.events.KeyBindingEventCallback;
import github.com.gengyoubo.replayneo.api.events.KeyEventCallback;
import github.com.gengyoubo.replayneo.api.events.PreRenderCallback;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.api.function.KeyInput;
import github.com.gengyoubo.replayneo.platform.versions.LangResourcePack;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.identifier;

public class ForgeKeyBindingRegistry extends EventRegistrations implements ReplayKeyBindingRegistry {
    private static final String CATEGORY = "replaymod.title";
    private static final List<KeyMapping> PENDING_KEY_MAPPINGS = new ArrayList<>();

    private final Map<String, Binding> bindings = new LinkedHashMap<>();
    private final Set<KeyMapping> onlyInReplay = new HashSet<>();
    private final Multimap<Integer, Function<KeyInput, Boolean>> rawHandlers = ArrayListMultimap.create();

    @Override
    public Binding registerKeyBinding(String name, int keyCode, Runnable whenPressed, boolean onlyInReplay) {
        Binding binding = registerKeyBinding(name, keyCode, onlyInReplay);
        binding.handlers.add(whenPressed);
        return binding;
    }

    @Override
    public Binding registerRepeatedKeyBinding(String name, int keyCode, Runnable whenPressed, boolean onlyInReplay) {
        Binding binding = registerKeyBinding(name, keyCode, onlyInReplay);
        binding.repeatedHandlers.add(whenPressed);
        return binding;
    }

    private Binding registerKeyBinding(String name, int keyCode, boolean onlyInReplay) {
        Binding binding = bindings.get(name);
        if (binding == null) {
            if (keyCode == 0) {
                keyCode = -1;
            }
            ResourceLocation id = identifier(RePlayNeo.RESOURCE_NAMESPACE, name.substring(LangResourcePack.LEGACY_KEY_PREFIX.length()));
            String key = String.format("key.%s.%s", id.getNamespace(), id.getPath());
            KeyMapping keyBinding = new KeyMapping(key, InputConstants.Type.KEYSYM, keyCode, CATEGORY);
            PENDING_KEY_MAPPINGS.add(keyBinding);
            binding = new Binding(name, keyBinding);
            bindings.put(name, binding);
            if (onlyInReplay) {
                this.onlyInReplay.add(keyBinding);
            }
        } else if (!onlyInReplay) {
            this.onlyInReplay.remove(binding.keyBinding);
        }
        return binding;
    }

    @Override
    public void registerRaw(int keyCode, Function<KeyInput, Boolean> whenPressed) {
        rawHandlers.put(keyCode, whenPressed);
    }

    @Override
    public Map<String, ReplayKeyBindingRegistry.Binding> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public Set<KeyMapping> getOnlyInReplay() {
        return Collections.unmodifiableSet(onlyInReplay);
    }

    { on(PreRenderCallback.EVENT, this::handleRepeatedKeyBindings); }

    public void handleRepeatedKeyBindings() {
        for (Binding binding : bindings.values()) {
            if (binding.keyBinding.isDown()) {
                invokeKeyBindingHandlers(binding, binding.repeatedHandlers);
            }
        }
    }

    { on(KeyBindingEventCallback.EVENT, this::handleKeyBindings); }
    private void handleKeyBindings() {
        for (Binding binding : bindings.values()) {
            while (binding.keyBinding.consumeClick()) {
                invokeKeyBindingHandlers(binding, binding.handlers);
                invokeKeyBindingHandlers(binding, binding.repeatedHandlers);
                drainConflictingReplayKeyClicks(binding);
            }
        }
    }

    private void drainConflictingReplayKeyClicks(Binding handledBinding) {
        for (Binding binding : bindings.values()) {
            if (binding == handledBinding) {
                continue;
            }
            if (binding.keyBinding.same(handledBinding.keyBinding)) {
                while (binding.keyBinding.consumeClick()) {
                    // Drain duplicate replay-only clicks so one physical key does not trigger multiple replay actions.
                }
            }
        }
    }

    private void invokeKeyBindingHandlers(Binding binding, Collection<Runnable> handlers) {
        for (final Runnable runnable : handlers) {
            try {
                runnable.run();
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.forThrowable(cause, "Handling Key Binding");
                CrashReportCategory category = crashReport.addCategory("Key Binding");
                category.setDetail("Key Binding", binding.name::toString);
                category.setDetail("Handler", runnable::toString);
                throw new ReportedException(crashReport);
            }
        }
    }

    { on(KeyEventCallback.EVENT, this::handleRaw); }
    private boolean handleRaw(KeyInput keyInput, int action) {
        if (action != KeyEventCallback.ACTION_PRESS) return false;
        for (final Function<KeyInput, Boolean> handler : rawHandlers.get(keyInput.key())) {
            try {
                if (handler.apply(keyInput)) {
                    return true;
                }
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.forThrowable(cause, "Handling Raw Key Binding");
                CrashReportCategory category = crashReport.addCategory("Key Binding");
                category.setDetail("Key Code", () -> "" + keyInput.key());
                category.setDetail("Handler", handler::toString);
                throw new ReportedException(crashReport);
            }
        }
        return false;
    }

    public class Binding implements ReplayKeyBindingRegistry.Binding {
        public final String name;
        public final KeyMapping keyBinding;
        private final List<Runnable> handlers = new ArrayList<>();
        private final List<Runnable> repeatedHandlers = new ArrayList<>();
        private boolean autoActivation;
        private Consumer<Boolean> autoActivationUpdate;

        public Binding(String name, KeyMapping keyBinding) {
            this.name = name;
            this.keyBinding = keyBinding;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String getBoundKey() {
            try {
                return keyBinding.getTranslatedKeyMessage().getString();
            } catch (ArrayIndexOutOfBoundsException e) {
                return "Unknown";
            }
        }

        @Override
        public boolean isBound() {
            return !keyBinding.isUnbound();
        }

        @Override
        public boolean isDown() {
            return keyBinding.isDown();
        }

        @Override
        public void trigger() {
            keyBinding.clickCount++;
            handleKeyBindings();
        }

        @Override
        public void registerAutoActivationSupport(boolean active, Consumer<Boolean> update) {
            this.autoActivation = active;
            this.autoActivationUpdate = update;
        }

        @Override
        public boolean supportsAutoActivation() {
            return autoActivationUpdate != null;
        }

        @Override
        public boolean isAutoActivating() {
            return supportsAutoActivation() && autoActivation;
        }

        @Override
        public void setAutoActivating(boolean active) {
            if (this.autoActivation == active) {
                return;
            }
            this.autoActivation = active;
            this.autoActivationUpdate.accept(active);
        }
    }

    @Mod.EventBusSubscriber(modid = RePlayNeo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ForgeKeyMappings {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            PENDING_KEY_MAPPINGS.forEach(event::register);
        }
    }
}
