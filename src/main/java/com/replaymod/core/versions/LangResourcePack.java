package com.replaymod.core.versions;

import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static de.johni0702.minecraft.gui.versions.MCVer.identifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import java.util.Objects;


/**
 * Resource pack which on-the-fly converts pre-1.13 language files into 1.13 json format.
 * Also duplicates `replaymod.input.*` bindings to `key.replaymod.*` as convention on Fabric.
 */
public class LangResourcePack extends AbstractPackResources {
    private static final Gson GSON = new Gson();
    public static final String NAME = "replaymod_lang";
    private static final Pattern JSON_FILE_PATTERN = Pattern.compile("^assets/" + ReplayMod.MOD_ID + "/lang/([a-z][a-z])_([a-z][a-z]).json$");
    private static final Pattern LANG_FILE_NAME_PATTERN = Pattern.compile("^([a-z][a-z])_([a-z][a-z]).lang$");

    public static final String LEGACY_KEY_PREFIX = "replaymod.input.";
    private static final String FABRIC_KEY_FORMAT = "key." + ReplayMod.MOD_ID + ".%s";

    private final Path basePath;
    public LangResourcePack() {
        super(NAME, true);

        try {
            this.basePath = Paths.get(ReplayMod.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to locate ReplayMod resources", e);
        }
    }

    private String langName(String path) {
        Matcher matcher = JSON_FILE_PATTERN.matcher(path);
        if (!matcher.matches()) return null;
        return String.format("%s_%s.lang", matcher.group(1), matcher.group(2).toUpperCase());
    }

    private Path baseLangPath() {
        return basePath.resolve("assets").resolve(ReplayMod.MOD_ID).resolve("lang");
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
    public IoSupplier<InputStream> getRootResource(String... segments) {
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
            return "{\"pack\": {\"description\": \"ReplayMod language files\", \"pack_format\": 4}}".getBytes(StandardCharsets.UTF_8);
        }

        Path langPath = langPath(path);
        if (langPath == null) return null;

        List<String> langFile;
        try (InputStream in = Files.newInputStream(langPath)) {
            langFile = IOUtils.readLines(in, StandardCharsets.UTF_8);
        }

        Map<String, String> properties = new HashMap<>();
        for (String line : langFile) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            int i = line.indexOf('=');
            String key = line.substring(0, i);
            String value = line.substring(i + 1);
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
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput consumer) {
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
                    .map(LANG_FILE_NAME_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> String.format("%s_%s.json", matcher.group(1), matcher.group(1)))
                    .map(name -> identifier(ReplayMod.MOD_ID, "lang/" + name))
                    .forEach(consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getNamespaces(PackType resourcePackType) {
        if (resourcePackType == PackType.CLIENT_RESOURCES) {
            return Collections.singleton("replaymod");
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {}

    // Not needed on fabric, using MixinModResourcePackUtil instead.
}
