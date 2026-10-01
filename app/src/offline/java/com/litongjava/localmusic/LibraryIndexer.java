package com.litongjava.localmusic;

import java.util.*;

/** Traversal is independent of Android so overlap, nesting and failures can be tested. */
public final class LibraryIndexer {
    public interface Node {
        String id();
        String uri();
        String name();
        boolean directory();
        List<Node> children() throws Exception;
    }
    public static final class Result {
        public final List<Track> tracks = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
    }
    public static boolean isAudio(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        return Arrays.asList("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "oga", "amr", "3gp")
                .contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
    public Result scan(List<Node> roots) {
        Result result = new Result();
        Set<String> seenFiles = new HashSet<>();
        for (Node root : roots) {
            if (Thread.currentThread().isInterrupted()) break;
            try {
                Set<String> seenDirectories = new HashSet<>();
                seenDirectories.add(root.id());
                for (Node child : sorted(root.children())) {
                    if (child.directory()) walk(child, child, root.name(), seenDirectories, seenFiles, result);
                    else add(child, root, root.name(), seenFiles, result);
                }
            } catch (Exception e) {
                result.warnings.add("ROOT:" + root.name());
            }
        }
        Collections.sort(result.tracks, (a, b) -> {
            int order = a.album.compareToIgnoreCase(b.album);
            if (order == 0) order = a.albumId.compareTo(b.albumId);
            return order == 0 ? a.title.compareToIgnoreCase(b.title) : order;
        });
        return result;
    }
    private void walk(Node node, Node album, String source, Set<String> dirs, Set<String> files, Result result) {
        // Iterative traversal avoids stack overflow for deeply nested document providers.
        Deque<Node> pending = new ArrayDeque<>();
        pending.add(node);
        while (!pending.isEmpty() && !Thread.currentThread().isInterrupted()) {
            Node next = pending.removeFirst();
            if (!dirs.add(next.id())) continue;
            try {
                for (Node child : sorted(next.children())) {
                    if (child.directory()) pending.addLast(child);
                    else add(child, album, source, files, result);
                }
            } catch (Exception e) {
                result.warnings.add("FOLDER:" + next.name());
            }
        }
    }
    private List<Node> sorted(List<Node> nodes) {
        List<Node> copy = new ArrayList<>(nodes);
        Collections.sort(copy, (a, b) -> {
            int order = a.name().compareToIgnoreCase(b.name());
            return order == 0 ? a.id().compareTo(b.id()) : order;
        });
        return copy;
    }
    private void add(Node file, Node album, String source, Set<String> seen, Result result) {
        if (isAudio(file.name()) && seen.add(file.id())) {
            result.tracks.add(new Track(file.id(), file.uri(), file.name(), album.id(), album.name(), source));
        }
    }
}
