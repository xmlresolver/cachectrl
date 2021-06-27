# cachectrl

A tool to inspect and update an XML Resolver document cache.

The [XML Resolver](https://github.com/xmlresolver/xmlresolver) can be configured to
automatically cache resources that it accesses on the web. This tool allows you to
inspect and update the cache.

It has six commands: show, inspect, create, update, delete, and flush.
These are accessed from the command line:

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

    delete      Create a new cache configuration
      Usage: delete [options]
        Options:
        * -pattern
            The URI regular expression pattern

    flush      Flush the cache
      Usage: flush [options]
        Options:
        * -regex, -r
            A regular expression to filter the entries shown
```

## Commands

### show

The `show` command will show you what is in the cache.

### inspect

The `inspect` command will show you how the cache is configured. The
cache control parameters can be configured on a per-URI basis. For
example, you could cache resources from some sites longer than others,
or allow some to have larger caches.

### create and update

The `create` and `update` commands modify the cache control parameters
for a particular pattern. They are the same except that the pattern
you specify for `create` must not exist and the pattern you specify
for `update` must exist.

### delete

The `delete` command removes a cache pattern.

### flush

The `flush` command will delete resources from the cache.

## Examples

The cache directory can be configured in a `.xmlresolver.properties` file or via
system properties. In these examples, we’ve got the cache in `$HOME/.xmlresolver/cache`.

First, we can inspect the default settings:

```
$ java -jar xmlresolver-cachectrl.jar inspect
Cache directory: /usr/local/share/xmlresolver/cache
Cache exclude ^file:
Cache exclude ^jar:file:
Cache exclude ^classpath:
Cache include .*
  Files: 0.00 bytes in 0 entries
  Limits: 10.00mb, 1000 entries, delete wait 7d
```

The last pattern `.*` is the default pattern. You can’t delete that one, but you can
add your own pattern matching `.*` if you wish to provide different defaults.

Next, let’s update the cache so that it will store DocBook and JATS resources
differently:

```
$ java -jar xmlresolver-cachectrl.jar create -pattern:"https://.*docbook\.org" -size 10k -space 100m
Cache directory: /usr/local/share/xmlresolver/cache
Cache exclude ^file:
Cache exclude ^jar:file:
Cache exclude ^classpath:
Cache include https://.*docbook\.org/
  Files: 0.00 bytes in 0 entries
  Limits: 100.00mb, 10240 entries, delete wait 7d
Cache include .*
  Files: 0.00 bytes in 0 entries
  Limits: 10.00mb, 1000 entries, delete wait 7d
```

This allows up to 10,240 resources occupying at most 100mb of space to be cached
from URIs that match “https://.*docbook\.org”. That will include both docbook.org
and cdn.docbook.org.

For JATS, let’s try this:

```
$ java -jar xmlresolver-cachectrl.jar create -pattern:https://jats.nlm.nih.gov/ -size 2k -space 20m -age 30d -delete 1d
Cache directory: /usr/local/share/xmlresolver/cache
Cache exclude ^file:
Cache exclude ^jar:file:
Cache exclude ^classpath:
Cache include https://.*docbook\.org/
  Files: 0.00 bytes in 0 entries
  Limits: 100.00mb, 10240 entries, delete wait 7d
Cache include https://jats.nlm.nih.gov/
  Files: 0.00 bytes in 0 entries
  Limits: 20.00mb, 2048 entries, delete wait 1d, max age: 30d
Cache include .*
  Files: 0.00 bytes in 0 entries
  Limits: 10.00mb, 1000 entries, delete wait 7d
```

The JATS portion of the cache will only keep deleted resources for 1 day and will
only keep any resource for 30 days.

Let’s use this cache for a while and accumulate some documents in it. An easy way to do
this is with the [SampleApp](https://github.com/xmlresolver/sampleapp).

After parsing and styling a few documents, we see:

```
$ java -jar xmlresolver-cachectrl.jar inspect
Cache directory: /usr/local/share/xmlresolver/cache
Cache exclude ^file:
Cache exclude ^jar:file:
Cache exclude ^classpath:
Cache include https://.*docbook\.org/
  Files: 0.00 bytes in 0 entries
  Limits: 100.00mb, 10240 entries, delete wait 7d
Cache include https://jats.nlm.nih.gov/
  Files: 0.00 bytes in 0 entries
  Limits: 20.00mb, 2048 entries, delete wait 1d, max age: 30d
Cache include .*
  Files: 3.19mb in 126 entries
  Limits: 10.00mb, 1000 entries, delete wait 7d
```

It’s worth noting that these statistics are imperfect. The cache doesn’t
really keep track of which pattern matched, so these statistics are created by
testing the cached documents against the patterns.

In the case of “.*”, everything matches. In the case of “public”
entries, there isn’t a URI to match against.

To see what files have been cached, you can show them:

```
$ java -jar xmlresolver-cachectrl.jar show
Cache directory: /usr/local/share/xmlresolver/cache
system https://jats.nlm.nih.gov/articleauthoring/1.2/JATS-articleauthoring1.dtd
system https://jats.nlm.nih.gov/articleauthoring/1.2/JATS-articleauthcustom-modules1.ent
public -//NLM//DTD JATS (Z39.96) Article Authoring DTD-Specific Modules v1.2 20190208//EN
system https://jats.nlm.nih.gov/articleauthoring/1.2/JATS-modules1.ent
…
uri https://cdn.docbook.org/release/xsltng/current/xslt/locale/cs.xml
uri https://cdn.docbook.org/release/xsltng/current/xslt/transforms/60-annotations.xsl
uri https://cdn.docbook.org/release/xsltng/current/xslt/transforms/70-xlinkbase.xsl
uri https://cdn.docbook.org/release/xsltng/current/xslt/modules/templates.xml
126 entries
```

You can limit the results with a regular expression:

```
$ java -jar xmlresolver-cachectrl.jar show -r:".*\.dtd"
Cache directory: /usr/local/share/xmlresolver/cache
Showing cache entries matching '.*\.dtd'
system https://jats.nlm.nih.gov/articleauthoring/1.2/JATS-articleauthoring1.dtd
1 of 126 entries match
```

The regular expression is applied to the summary line, not just the URI.
Consequently, you can match on the entry type:

```
$ java -jar xmlresolver-cachectrl.jar show -r:"^public "
Cache directory: /usr/local/share/xmlresolver/cache
Showing cache entries matching '^public '
public -//NLM//DTD JATS (Z39.96) Article Authoring DTD-Specific Modules v1.2 20190208//EN
public -//NLM//DTD JATS (Z39.96) JATS DTD Suite Module of Modules v1.2 20190208//EN
…
public -//NLM//DTD JATS (Z39.96) JATS DTD Suite Notation Declarations v1.2 20190208//EN
34 of 126 entries match
```

To get rid of cache entries, you can flush them:

```
$ java -jar xmlresolver-cachectrl.jar flush -r:"https://cdn\.docbook"
Cache directory: /usr/local/share/xmlresolver/cache
Flushed 57 entries
```
