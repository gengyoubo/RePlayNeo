package github.com.gengyoubo.replayneo.platform.versions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.RePlayNeo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.identifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/**
 * Resource pack which on-the-fly converts pre-1.13 language files into 1.13 json format.
 * Also duplicates legacy `replaymod.input.*` bindings to the active mod namespace.
 */
public class LangResourcePack extends AbstractPackResources {
    private static final Gson GSON = new Gson();
    private static final Type LANG_MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
    public static final String NAME = RePlayNeo.MODID + "_lang";
    private static final Pattern JSON_FILE_PATTERN = Pattern.compile("^assets/" + RePlayNeo.RESOURCE_NAMESPACE + "/lang/([a-z][a-z])_([a-z][a-z]).json$");
    private static final Pattern JSON_FILE_NAME_PATTERN = Pattern.compile("^([a-z][a-z])_([a-z][a-z]).json$");

    public static final String LEGACY_KEY_PREFIX = "replaymod.input.";
    private static final String FABRIC_KEY_FORMAT = "key." + RePlayNeo.RESOURCE_NAMESPACE + ".%s";

    private final Path basePath;
    public LangResourcePack() {
        super(NAME, true);

        try {
            this.basePath = Paths.get(RePlayCore.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to locate RePlayNeo resources", e);
        }
    }

    private String langName(String path) {
        Matcher matcher = JSON_FILE_PATTERN.matcher(path);
        if (!matcher.matches()) return null;
        return String.format("%s_%s.json", matcher.group(1), matcher.group(2));
    }

    private Path baseLangPath() {
        return basePath.resolve("assets").resolve(RePlayNeo.RESOURCE_NAMESPACE).resolve("lang");
    }

    private Path langPath(String path) {
        String langName = langName(path);
        if (langName == null) return null;
        Path basePath = baseLangPath();
        return basePath.resolve(langName);
    }

    private String convertValue(String value) {
        return value;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String @NotNull ... segments) {
        byte[] bytes;
        try {
            bytes = readFile(String.join("/", segments));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (bytes == null) {
            return null;
        }
        return () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        return getRootResource(type.getDirectory(), id.getNamespace(), id.getPath());
    }

    private byte[] readFile(String path) throws IOException {
        if ("pack.mcmeta".equals(path)) {
            return "{\"pack\": {\"description\": \"RePlayNeo language files\", \"pack_format\": 15}}".getBytes(StandardCharsets.UTF_8);
        }

        Path langPath = langPath(path);
        if (langPath == null) return null;

        Map<String, String> source;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(langPath), StandardCharsets.UTF_8)) {
            source = GSON.fromJson(reader, LANG_MAP_TYPE);
        }

        Map<String, String> properties = new LinkedHashMap<>();
        if (source == null) {
            source = Collections.emptyMap();
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            value = convertValue(value);
            if (key.startsWith(LEGACY_KEY_PREFIX)) {
                // Duplicating instead of just remapping as some other part of the UI may still rely on the old key
                properties.put(key, value);
                key = String.format(FABRIC_KEY_FORMAT, key.substring(LEGACY_KEY_PREFIX.length()));
            }
            properties.put(key, value);
        }

        return GSON.toJson(properties).getBytes(StandardCharsets.UTF_8);
    }



    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String prefix, @NotNull ResourceOutput consumer) {
        findResources(type, prefix, id -> consumer.accept(id, () -> new ByteArrayInputStream(Objects.requireNonNull(readFile(id.getPath())))));
    }

    private void findResources(PackType type, String path, Consumer<ResourceLocation> consumer) {
        if (type != PackType.CLIENT_RESOURCES) return;
        if (!"lang".equals(path)) return;
        Path base = baseLangPath();
        try (Stream<Path> stream = Files.walk(base, 1)) {
            stream
                    .skip(1)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName).map(Path::toString)
                    .map(JSON_FILE_NAME_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> String.format("%s_%s.json", matcher.group(1), matcher.group(2)))
                    .map(name -> identifier(RePlayNeo.RESOURCE_NAMESPACE, "lang/" + name))
                    .forEach(consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType resourcePackType) {
        if (resourcePackType == PackType.CLIENT_RESOURCES) {
            return Collections.singleton(RePlayNeo.RESOURCE_NAMESPACE);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {}

    // Not needed on fabric, using MixinModResourcePackUtil instead.
}
