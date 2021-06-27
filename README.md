# cachectrl

A tool to inspect and update an XML Resolver document cache.

The [XML Resolver](https://github.com/xmlresolver/xmlresolver) can be configured to
automatically cache resources that it accesses on the web. This tool allows you to
inspect and update the cache.

It has five commands: show, inspect, create, update, and flush. These are accessed
from the command line:

```
Usage: CacheCtrl [options] [command] [command options]
  Options:
    -help, -h, --help
      Display help
    -cache-directory, -cache-dir
      Cache directory
    -verbose
      Verbose output
      Default: false
  Commands:
    show      Show the contents of the cache
      Usage: show [options]
        Options:
          -regex, -r
            A regular expression to filter the entries shown

    inspect      Inspect the cache configuration
      Usage: inspect

    create      Create a new cache configuration
      Usage: create [options]
        Options:
          -age
            The maximum age each entry is allowed to be
          -delete
            The amount of time to wait before flushing a deleted entry
          -include
            Include matching pattern in cache?
            Default: true
          -pattern
            The URI regular expression pattern
          -size
            The maximum number of entries allowed
          -space
            The maximum space the entries are allowed

    update      Update a cache configuration
      Usage: update [options]
        Options:
          -age
            The maximum age each entry is allowed to be
          -delete
            The amount of time to wait before flushing a deleted entry
          -include
            Include matching pattern in cache?
            Default: true
          -pattern
            The URI regular expression pattern
          -size
            The maximum number of entries allowed
          -space
            The maximum space the entries are allowed

    flush      Flush the cache
      Usage: flush [options]
        Options:
          -pattern
            The URI regular expression pattern
          -regex, -r
            A regular expression to filter the entries shown
```

## show

The `show` command will show you what is in the cache.

## inspect

The `inspect` command will show you how the cache is configured. The
cache control parameters can be configured on a per-URI basis. For
example, you could cache resources from some sites longer than others,
or allow some to have larger caches.

## create and update

The `create` and `update` commands modify the cache control parameters
for a particular pattern. They are the same except that the pattern
you specify for `create` must not exist and the pattern you specify
for `update` must exist.

## flush

The `flush` command will delete resources from the cache.

