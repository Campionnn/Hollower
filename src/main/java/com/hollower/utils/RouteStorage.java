package com.hollower.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.hollower.Hollower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Persists routes as one JSON file per route, organized into real, nestable directories under
// config/hollower/routes/. Folders and route files are addressed by paths relative to that root; every
// method here resolves and validates those paths before touching disk.
@Environment(EnvType.CLIENT)
public final class RouteStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("hollower").resolve("routes");
    private static final String EXTENSION = ".json";

    private RouteStorage() {
    }

    // Thrown for problems the screen should show to the user rather than just log, such as name clashes.
    public static final class RouteStorageException extends Exception {
        public RouteStorageException(String message) {
            super(message);
        }
    }

    public record Entry(Path path, String name, boolean folder) {
    }

    private static final class NodeDto {
        int x;
        int y;
        int z;

        NodeDto(BlockPos pos) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }

        BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final class RouteFile {
        String name;
        long savedAt;
        List<NodeDto> positions;
    }

    public record SavedRoute(String name, List<BlockPos> positions) {
    }

    // ---------------------------------------------------------------- paths

    // Resolves a folder path relative to the root, and refuses anything that would escape it.
    private static Path resolveFolder(Path relFolder) {
        Path resolved = ROOT.resolve(relFolder).normalize();
        if (!resolved.equals(ROOT) && !resolved.startsWith(ROOT)) {
            throw new IllegalArgumentException("Refusing to resolve folder outside the routes directory: "
                    + relFolder);
        }
        return resolved;
    }

    // Strips characters that would be unsafe or confusing in a file/directory name, trims it, and caps its
    // length. Returns "route"/"folder" style callers pass as a fallback if nothing usable is left.
    private static String sanitize(String rawName, String fallback) {
        String cleaned = rawName == null ? "" : rawName.strip();
        cleaned = cleaned.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").replaceAll("\\.+$", "").strip();
        if (cleaned.isEmpty() || cleaned.equals(".") || cleaned.equals("..")) cleaned = fallback;
        if (cleaned.length() > 64) cleaned = cleaned.substring(0, 64).strip();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    // Resolves the file a route Entry's relative path points to (relFile's parent may be null at root).
    private static Path resolveFile(Path relFile) {
        Path relParent = relFile.getParent();
        return resolveFolder(relParent == null ? Path.of("") : relParent).resolve(relFile.getFileName());
    }

    private static Path uniquePath(Path folder, String sanitizedBase, boolean asFolder)
            throws RouteStorageException {
        Path candidate = folder.resolve(asFolder ? sanitizedBase : sanitizedBase + EXTENSION);
        if (Files.exists(candidate)) {
            throw new RouteStorageException(
                    "A " + (asFolder ? "folder" : "route") + " named \"" + sanitizedBase + "\" already exists here");
        }
        return candidate;
    }

    // ---------------------------------------------------------------- listing

    public static List<Entry> list(Path relFolder) {
        Path folder = resolveFolder(relFolder);
        List<Entry> folders = new ArrayList<>();
        List<Entry> routes = new ArrayList<>();

        if (!Files.isDirectory(folder)) return List.of();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    folders.add(new Entry(ROOT.relativize(entry), entry.getFileName().toString(), true));
                } else if (entry.getFileName().toString().endsWith(EXTENSION)) {
                    String displayName = readDisplayName(entry);
                    if (displayName != null) routes.add(new Entry(ROOT.relativize(entry), displayName, false));
                }
            }
        } catch (IOException e) {
            Hollower.LOGGER.warn("Failed to list route folder {}", relFolder, e);
            return List.of();
        }

        folders.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        routes.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        List<Entry> all = new ArrayList<>(folders);
        all.addAll(routes);
        return all;
    }

    // Reads just enough of a route file to show its name in a listing, skipping it (with a logged warning)
    // if it isn't readable, so one corrupt file doesn't break browsing.
    private static String readDisplayName(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            RouteFile data = GSON.fromJson(reader, RouteFile.class);
            if (data == null || data.name == null || data.name.isBlank()) {
                return stripExtension(file.getFileName().toString());
            }
            return data.name;
        } catch (IOException | JsonSyntaxException e) {
            Hollower.LOGGER.warn("Failed to read route file {}", file, e);
            return null;
        }
    }

    private static String stripExtension(String fileName) {
        return fileName.endsWith(EXTENSION) ? fileName.substring(0, fileName.length() - EXTENSION.length()) : fileName;
    }

    // ---------------------------------------------------------------- routes

    public static void saveRoute(Path relFolder, String name, List<BlockPos> positions) throws RouteStorageException {
        Path folder = resolveFolder(relFolder);
        String sanitized = sanitize(name, "route");
        Path file = uniquePath(folder, sanitized, false);

        RouteFile data = new RouteFile();
        data.name = name == null || name.isBlank() ? sanitized : name.strip();
        data.savedAt = System.currentTimeMillis();
        data.positions = positions.stream().map(NodeDto::new).toList();

        try {
            Files.createDirectories(folder);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            Hollower.LOGGER.warn("Failed to save route {}", file, e);
            throw new RouteStorageException("Failed to save route: " + e.getMessage());
        }
    }

    public static SavedRoute loadRoute(Path relFile) throws RouteStorageException {
        Path file = resolveFile(relFile);
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            RouteFile data = GSON.fromJson(reader, RouteFile.class);
            if (data == null || data.positions == null) {
                throw new RouteStorageException("Route file is empty or malformed");
            }
            List<BlockPos> positions = data.positions.stream().map(NodeDto::toBlockPos).toList();
            String name = data.name == null || data.name.isBlank()
                    ? stripExtension(file.getFileName().toString())
                    : data.name;
            return new SavedRoute(name, positions);
        } catch (IOException | JsonSyntaxException e) {
            Hollower.LOGGER.warn("Failed to load route {}", file, e);
            throw new RouteStorageException("Failed to load route: " + e.getMessage());
        }
    }

    public static void renameRoute(Path relFile, String newName) throws RouteStorageException {
        Path file = resolveFile(relFile);
        RouteFile data;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data = GSON.fromJson(reader, RouteFile.class);
        } catch (IOException | JsonSyntaxException e) {
            throw new RouteStorageException("Failed to read route: " + e.getMessage());
        }
        if (data == null) data = new RouteFile();

        String sanitized = sanitize(newName, "route");
        Path folder = file.getParent();
        Path target = folder.resolve(sanitized + EXTENSION);
        if (!target.equals(file) && Files.exists(target)) {
            throw new RouteStorageException("A route named \"" + sanitized + "\" already exists here");
        }

        data.name = newName == null || newName.isBlank() ? sanitized : newName.strip();
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            throw new RouteStorageException("Failed to rename route: " + e.getMessage());
        }
        if (!target.equals(file)) {
            try {
                Files.move(file, target);
            } catch (IOException e) {
                throw new RouteStorageException("Failed to rename route: " + e.getMessage());
            }
        }
    }

    public static void moveRoute(Path relFile, Path relNewFolder) throws RouteStorageException {
        Path file = resolveFile(relFile);
        Path newFolder = resolveFolder(relNewFolder);
        Path target = newFolder.resolve(file.getFileName());
        if (Files.exists(target)) {
            throw new RouteStorageException(
                    "A route named \"" + stripExtension(file.getFileName().toString()) + "\" already exists there");
        }
        try {
            Files.createDirectories(newFolder);
            Files.move(file, target);
        } catch (IOException e) {
            throw new RouteStorageException("Failed to move route: " + e.getMessage());
        }
    }

    public static void deleteRoute(Path relFile) throws RouteStorageException {
        Path file = resolveFile(relFile);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RouteStorageException("Failed to delete route: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- folders

    public static void createFolder(Path relParent, String name) throws RouteStorageException {
        Path parent = resolveFolder(relParent);
        String sanitized = sanitize(name, "folder");
        Path target = uniquePath(parent, sanitized, true);
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            throw new RouteStorageException("Failed to create folder: " + e.getMessage());
        }
    }

    public static void renameFolder(Path relFolder, String newName) throws RouteStorageException {
        Path folder = resolveFolder(relFolder);
        String sanitized = sanitize(newName, "folder");
        Path target = folder.getParent().resolve(sanitized);
        if (!target.equals(folder) && Files.exists(target)) {
            throw new RouteStorageException("A folder named \"" + sanitized + "\" already exists here");
        }
        try {
            Files.move(folder, target);
        } catch (IOException e) {
            throw new RouteStorageException("Failed to rename folder: " + e.getMessage());
        }
    }

    public static void moveFolder(Path relFolder, Path relNewParent) throws RouteStorageException {
        Path folder = resolveFolder(relFolder);
        Path newParent = resolveFolder(relNewParent);
        if (newParent.equals(folder) || newParent.startsWith(folder)) {
            throw new RouteStorageException("Can't move a folder into itself or one of its own subfolders");
        }
        Path target = newParent.resolve(folder.getFileName());
        if (Files.exists(target)) {
            throw new RouteStorageException(
                    "A folder named \"" + folder.getFileName() + "\" already exists there");
        }
        try {
            Files.createDirectories(newParent);
            Files.move(folder, target);
        } catch (IOException e) {
            throw new RouteStorageException("Failed to move folder: " + e.getMessage());
        }
    }

    public static void deleteFolder(Path relFolder) throws RouteStorageException {
        Path folder = resolveFolder(relFolder);
        try (var paths = Files.walk(folder)) {
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : ordered) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new RouteStorageException("Failed to delete folder: " + e.getMessage());
        }
    }
}
