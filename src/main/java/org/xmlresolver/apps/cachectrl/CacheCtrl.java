/*
 * CacheCtrl.java
 *
 */

package org.xmlresolver.apps.cachectrl;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.xmlresolver.ResolverFeature;
import org.xmlresolver.XMLResolverConfiguration;
import org.xmlresolver.cache.CacheEntry;
import org.xmlresolver.cache.CacheInfo;
import org.xmlresolver.cache.CacheParser;
import org.xmlresolver.cache.ResourceCache;
import org.xmlresolver.catalog.entry.Entry;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** An application to display information about items in the cache.
 *
 * @author ndw
 */
public class CacheCtrl {
    // Hack to print the cache directory at most once.
    private static boolean showCacheDirectory = true;

    public static void main(String[] args) {
        CacheCtrl app = new CacheCtrl();
        app.process(args);
    }

    private void process(String[] args) {
        CommandMain cmain = new CommandMain();
        CommandShow cshow = new CommandShow();
        CommandInspect cinspect = new CommandInspect();
        CommandCreate ccreate = new CommandCreate();
        CommandUpdate cupdate = new CommandUpdate();
        CommandDelete cdelete = new CommandDelete();
        CommandFlush cflush = new CommandFlush();
        JCommander jc = JCommander.newBuilder()
                .addObject(cmain)
                .addCommand("show", cshow)
                .addCommand("inspect", cinspect)
                .addCommand("create", ccreate)
                .addCommand("update", cupdate)
                .addCommand("delete", cdelete)
                .addCommand("flush", cflush)
                .build();

        jc.setProgramName("CacheCtrl");

        try {
            jc.parse(args);
            if (cmain.help || jc.getParsedCommand() == null) {
                usage(jc, true);
            } else {
                cmain.command = jc.getParsedCommand();

                switch (jc.getParsedCommand()) {
                    case "show":
                        show(cmain, cshow);
                        break;
                    case "inspect":
                        inspect(cmain);
                        break;
                    case "create":
                        create(cmain, ccreate);
                        break;
                    case "update":
                        update(cmain, cupdate);
                        break;
                    case "delete":
                        delete(cmain, cdelete);
                        break;
                    case "flush":
                        flush(cmain, cflush);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unexpected command: " + jc.getParsedCommand());
                }
            }
        } catch (ParameterException pe) {
            System.err.println(pe.getMessage());
            usage(pe.getJCommander(), false);
        }
    }

    private void usage(JCommander jc, boolean help) {
        if (jc != null) {
            DefaultUsageFormatter formatter = new DefaultUsageFormatter(jc);
            StringBuilder sb = new StringBuilder();
            formatter.usage(sb);
            System.err.println(sb);
        }
        if (help) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    private void show(CommandMain main, CommandShow command) {
        ResourceCache cache = getResourceCache(main);

        long count = 0;
        long match = 0;

        String regex = ".*";
        if (command.regex != null) {
            regex = command.regex;
            System.out.println("Showing cache entries matching '" + regex + "'");
        }

        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        for (CacheEntry entry : cache.entries()) {
            // Hack out the arrow pointer
            String entrystr = entry.entry.toString();
            int pos = entrystr.indexOf(Entry.rarr);
            entrystr = entrystr.substring(0, pos);

            count += 1;

            Matcher matcher = pattern.matcher(entrystr);
            if (matcher.find()) {
                if (main.verbose) {
                    System.out.println(entry);
                    System.out.println("  " + entrystr);
                    if (entry.expired) {
                        System.out.println("  EXPIRED (cached " + formatTime(entry.time) + ")");
                    } else {
                        System.out.println("  Cached " + formatTime(entry.time));
                    }

                    if (entry.contentType() != null) {
                        System.out.println("  Content-type: " + entry.contentType());
                    }
                } else {
                    System.out.println(entrystr);
                }

                match += 1;
            }
        }

        if (match == count) {
            System.out.println(count + " " + (count == 1 ? "entry" : "entries"));
        } else {
            System.out.println(match + " of " + count + " entries match");
        }
    }

    private void inspect(CommandMain main) {
        ResourceCache cache = getResourceCache(main);

        long count = 0;
        long space = 0;

        // Make a list of cache entries that includes the defaultCacheInfo
        ArrayList<CacheInfo> cacheInfoList = new ArrayList<>();
        boolean explicitDefault = false;
        for (CacheInfo info : cache.getCacheInfoList()) {
            explicitDefault = explicitDefault || (ResourceCache.defaultPattern.equals(info.pattern));
            cacheInfoList.add(info);
        }
        if (!explicitDefault) {
            cacheInfoList.add(cache.getDefaultCacheInfo());
        }

        HashMap<String, CacheStatistics> stats = new HashMap<>();
        ArrayList<CacheInfo> caching = new ArrayList<>();
        for (CacheInfo info : cacheInfoList) {
            if (info.cache) {
                caching.add(info);
                stats.put(info.pattern, new CacheStatistics(info));
            }
        }

        for (CacheEntry entry : cache.entries()) {
            CacheInfo info = null;
            for (CacheInfo cached : caching) {
                if (info == null && cached.uriPattern.matcher(entry.uri.toString()).find()) {
                    info = cached;
                }
            }
            if (info == null) {
                throw new IllegalArgumentException("Cache is invalid");
            }
            stats.get(info.pattern).count += 1;
            stats.get(info.pattern).space += entry.file.length();
        }

        for (CacheInfo info : cacheInfoList) {
            System.out.println(info);
            if (info.cache) {
                count = stats.get(info.pattern).count;
                space = stats.get(info.pattern).space;
                System.out.println("  Files: " + formatSpace(space) + " in " + count + " "
                        + (count == 1 ? "entry" : "entries"));
                System.out.print("  Limits: " + formatSpace(info.cacheSpace) + ", " + info.cacheSize + " "
                        + (info.cacheSize == 1 ? "entry" : "entries"));
                System.out.print(", delete wait " + formatDuration(info.deleteWait));
                if (info.maxAge >= 0) {
                    System.out.print(", max age: " + formatDuration(info.maxAge));
                }
                System.out.println();
                stats.put(info.pattern, new CacheStatistics(info));
            }
        }
    }

    private String formatDuration(long seconds) {
        if (seconds < 0) {
            return "" + seconds;
        } else if (seconds == 0) {
            return "0s";
        }

        long d = 0;
        long h = 0;
        long m = 0;

        if (seconds >= 3600 * 24) {
            long msec = seconds;
            seconds = seconds % (3600 * 24);
            msec = msec - seconds;
            d = msec / (3600 * 24);
        }

        if (seconds >= 3600) {
            long hsec = seconds;
            seconds = seconds % 3600;
            hsec = hsec - seconds;
            h = hsec / 3600;
        }

        if (seconds >= 60) {
            long msec = seconds;
            seconds = seconds % 60;
            msec = msec - seconds;
            m = msec / 60;
        }

        StringBuilder sb = new StringBuilder();
        if (d != 0) {
            sb.append(d);
            sb.append("d");
        }

        if (h != 0 || m != 0 || seconds != 0) {
            if (h != 0) {
                sb.append(h);
                sb.append("h");
            }
            if (m != 0) {
                sb.append(m);
                sb.append("m");
            }
            if (seconds != 0 || m != 0) {
                sb.append(seconds);
                sb.append("s");
            }
        }

        return sb.toString();
    }

    private void create(CommandMain main, CommandCreate command) {
        ResourceCache cache = getResourceCache(main);
        for (CacheInfo info : cache.getCacheInfoList()) {
            if (info.pattern.equals(command.pattern)) {
                throw new ParameterException("Cannot create '" + command.pattern + "', it already exists");
            }
        }
        createOrUpdate(cache, main, command);
        inspect(main);
    }

    private void update(CommandMain main, CommandUpdate command) {
        ResourceCache cache = getResourceCache(main);
        boolean found = false;
        for (CacheInfo info : cache.getCacheInfoList()) {
            found = found || info.pattern.equals(command.pattern);
        }
        if (!found) {
            throw new ParameterException("Cannot update '" + command.pattern + "', it doesn't exist");
        }
        createOrUpdate(cache, main, command);
        inspect(main);
    }

    private void createOrUpdate(ResourceCache cache, CommandMain main, CommandCreate command) {
        long deleteWait = CacheParser.parseTimeLong(command.deleteWait, ResourceCache.deleteWait);
        long cacheSize = CacheParser.parseSizeLong(command.size, ResourceCache.cacheSize);
        long cacheSpace = CacheParser.parseSizeLong(command.space, ResourceCache.cacheSpace);
        long maxAge = CacheParser.parseTimeLong(command.age, ResourceCache.maxAge);

        cache.removeCacheInfo(command.pattern);
        cache.addCacheInfo(command.pattern, command.include, deleteWait, cacheSize, cacheSpace, maxAge);
    }

    private void delete(CommandMain main, CommandDelete command) {
        ResourceCache cache = getResourceCache(main);
        boolean found = false;
        for (CacheInfo info : cache.getCacheInfoList()) {
            found = found || info.pattern.equals(command.pattern);
        }
        if (!found) {
            throw new ParameterException("Cannot delete '" + command.pattern + "', it doesn't exist");
        }
        cache.removeCacheInfo(command.pattern);
        inspect(main);
    }

    private void flush(CommandMain main, CommandFlush command) {
        ResourceCache cache = getResourceCache(main);

        String regex = ".*";
        if (command.regex != null) {
            regex = command.regex;
        }

        long count = 0;
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        for (CacheEntry entry : cache.entries()) {
            // Hack out the arrow pointer
            String entrystr = entry.entry.toString();
            int pos = entrystr.indexOf(Entry.rarr);
            entrystr = entrystr.substring(0, pos);

            Matcher matcher = pattern.matcher(entrystr);
            if (matcher.find()) {
                File centry = new File(entry.entry.baseURI.getPath());
                if (centry.exists()) {
                    if (!centry.delete()) {
                        System.err.println("Failed to delete: " + centry.getAbsolutePath());
                    }
                } else {
                    System.err.println("Entry did not exist: " + centry.getAbsolutePath());
                }

                if (!centry.exists()) {
                    if (entry.file.delete()) {
                        count += 1;
                    } else {
                        System.err.println("Failed to delete: " + centry.getAbsolutePath());
                    }
                }
            }
        }
        System.out.println("Flushed " + count + " entries");
    }

    private String formatSpace(long space) {
        float gigabyte = 1024 * 1000 * 1000;
        float megabyte = 1024 * 1000;
        float kilobyte = 1024;

        String units = " bytes";
        float fspace = space;
        if (fspace > gigabyte) {
            fspace = fspace / gigabyte;
            units = "gb";
        } else if (fspace > megabyte) {
            fspace = fspace / megabyte;
            units = "mb";
        } else if (fspace > kilobyte) {
            fspace = fspace / kilobyte;
            units = "kb";
        }

        return String.format("%8.2f%s", fspace, units).trim();
    }

    private String formatTime(long time) {
        Date date = new Date(time);
        String tz = new SimpleDateFormat("Z").format(date);
        DateFormat simple = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simple.format(date) + tz.substring(0, 3) + ":" + tz.substring(3);
    }

    private ResourceCache getResourceCache(CommandMain main) {
        XMLResolverConfiguration config = new XMLResolverConfiguration();
        if (main.cacheDirectory != null) {
            config.setFeature(ResolverFeature.CACHE_DIRECTORY, main.cacheDirectory);
        }
        ResourceCache cache = config.getFeature(ResolverFeature.CACHE);
        if (cache.directory() == null) {
            if (main.cacheDirectory == null) {
                throw new ParameterException("Failed to initialize cache");
            } else {
                throw new ParameterException("Failed to initialize cache: " + main.cacheDirectory);
            }
        }

        if (CacheCtrl.showCacheDirectory) {
            System.out.println("Cache directory: " + cache.directory());
            CacheCtrl.showCacheDirectory = false;
        }

        return cache;
    }

    // ============================================================

    @Parameters(separators = ":", commandDescription = "Global options")
    private static class CommandMain {
        private String command = null;

        @Parameter(names = {"-help", "-h", "--help"}, help = true, description = "Display help")
        private boolean help = false;

        @Parameter(names = {"-cache-directory", "-cache-dir"}, description = "Cache directory")
        private String cacheDirectory;

        @Parameter(names = "-verbose", description = "Verbose output")
        private boolean verbose = false;
    }

    @Parameters(separators = ":", commandDescription = "Show the contents of the cache")
    private class CommandShow {
        @Parameter(names = {"-regex", "-r"}, description = "A regular expression to filter the entries shown")
        private String regex;
    }

    @Parameters(separators = ":", commandDescription = "Inspect the cache configuration")
    private class CommandInspect {
    }

    @Parameters(separators = ":", commandDescription = "Create a new cache configuration")
    private class CommandCreate {
        @Parameter(names = "-pattern", description = "The URI regular expression pattern")
        public String pattern;

        @Parameter(names = "-include", description = "Include matching pattern in cache?", arity = 1)
        public boolean include = true;

        @Parameter(names = "-size", description = "The maximum number of entries allowed")
        public String size;

        @Parameter(names = "-space", description = "The maximum space the entries are allowed")
        public String space;

        @Parameter(names = "-age", description = "The maximum age each entry is allowed to be")
        public String age;

        @Parameter(names = "-delete", description = "The amount of time to wait before flushing a deleted entry")
        public String deleteWait;
    }

    @Parameters(separators = ":", commandDescription = "Update a cache configuration")
    private class CommandUpdate extends CommandCreate {
        // The same parameters, the only difference is whether or not it must exist or must not exist
    }

    @Parameters(separators = ":", commandDescription = "Create a new cache configuration")
    private class CommandDelete {
        @Parameter(names = "-pattern", description = "The URI regular expression pattern", required = true)
        public String pattern;
    }

    @Parameters(separators = ":", commandDescription = "Flush the cache")
    private class CommandFlush {
        @Parameter(names = {"-regex", "-r"}, description = "A regular expression to filter the entries shown", required = true)
        private String regex;
    }

    private static class CacheStatistics {
        private final CacheInfo info;
        private long count = 0;
        private long space = 0;

        private CacheStatistics(CacheInfo info) {
            this.info = info;
        }
    }

}
