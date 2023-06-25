# Web Crawler
A simple web crawler.

Given a starting URL, the crawler visits each URL it finds on the same domain and prints each URL visited with a list of links found on that page.

The crawler is limited to a single subdomain - so when you start with https://example.com/, it will crawl all pages on the example.com website, but not follow external links, for example to othersite.com or links to other subdomains e.g. forum.example.com or www.example.com.

The crawler wont crawl pages with X-Robot-Tag: noindex, or a `<meta name=robots value=noindex />` in the HTML body.

## Prerequisites

Assuming you have a working Java runtime installed the gradle wrapper (`./gradlew`) should handle the rest.

## Running the Application

```
./gradlew run --args='[OPTIONS] https://example.com'
```

## Running Tests

```
./gradlew test
```

## Caveats

* This isnt rate-limited, if you run it against something protected by a CDN (e.g. CloudFlare) you'll likely get banned
* Pages with dynamically loaded content (e.g. those which require javascript to be enabled) won't work properly unless they are also rendered server side.
* The crawler only has a very simple interpretation of robots.txt
* Error pages arent crawled, this is by design. but does mean any links the error pages take you to are also not crawled unless linked elsewhere
* non-HTML pages (e.g. pdf downloads) aren't crawled but leave litter in the error logs. 
* in theory you could take the stdout and dump it into a graphviz 'dot' digraph, but these graphs quickly get too big to render properly.

## Other Notes

* java.util.URL is marked deprecated, I'm still using it here because it handles "malformed" URLs better than the suggested replacement java.util.URI. for instance URLs with spaces in it are considered invalid by URI, but behave more as expected using URL.

----

## Outline

Illustrative psuedocode outline

```pascal
program Crawler;
// push to a queue of urls still to check, and to a set of already checked urls
function queue_push();

// return true if url in already queued list
function already_queued();

// return true if url host part matches
function same_host();

// return true if url in robots exclusion list (á la RFC-9309)
function matches_robots();

// Write something to the console for each page we find.
function emit(url: string, document: HTMLDocument, anchors: List):
    write(url)
    write(' : ')
    write(anchors)
    write('\n')


function crawl(url: string):
    html := get_page(url);
    document := parse_html(html);
    anchors := find_elements_by_tag_name(document, 'a')

    emit(url, document, anchors)
    
    for i := 0 to len(anchors) do
    begin
        if (already_queued(anchors[i])) then continue;
        if (!has_href(anchors[i])) then continue;
        if (!same_host(anchors[i])) then continue;
        if (matches_robots(anchors[i])) then continue;
        queue_push(anchors[i])
    end;

begin
    crawl(first_url)
    while !queue_empty() do
        crawl(queue_pop())
    end;
end.
```
