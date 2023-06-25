package uk.me.paraskos.web.crawler

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

fun main(args: Array<String>) = runBlocking {
    val parser = ArgParser("example")
    val startingUrl by parser.argument(ArgType.String, description = "Starting URL")
    parser.parse(args)
    Crawler(URL(startingUrl)).crawl()
}

class Crawler(
    private val startingUrl: URL,
    private val botName: String = "ExampleBot",
    private val version: String = "0.0.1",
    private val userAgent: String = "$botName/$version"
) {
    private val robots = Robots().load(URL(startingUrl, "/robots.txt"))
    suspend fun crawl() = coroutineScope {
        val crawled = HashSet<URL>()
        val linksToCrawl = ConcurrentLinkedQueue<URL>()
        val openJobs = AtomicInteger();


        linksToCrawl.add(startingUrl)
        do {
            val link = linksToCrawl.poll()
            if (link == null) {
                delay(1);
                continue;
            }
            // If we haven't already crawled this link then do so now.
            if (!crawled.contains(link)) {
                crawled.add(link)

                openJobs.incrementAndGet() // Remember we've started a job so we wait for it later.
                launch(Dispatchers.Default) { // Kick it off in a worker thread asynchronously
                    linksToCrawl.addAll(fetchPage(link)) // Get the page and queue all the links (order is not guaranteed)
                    openJobs.decrementAndGet() // decrement job counter so we dont wait any longer for it.
                }
            }
            // If there are no more links and no more jobs which might produce links then we're done.
        } while (!linksToCrawl.isEmpty() || openJobs.get() > 0)
    }

    suspend fun fetchPage(
        link: URL
    ) = coroutineScope {
        try {

            if (robots.forbids(link)) return@coroutineScope emptyList();

            val connection = Jsoup.connect(link.toString())
                .method(Connection.Method.GET)
                .userAgent(userAgent)
                .header("Accept", "text/html")
                .followRedirects(false)
                .execute()

            // if X-Robots-Tag is set to "noindex" then respect it and stop crawling this page.
            if (connection.headerEquals("X-Robots-Tag", "noindex", true)) {
                return@coroutineScope emptyList()
            }

            val doc = connection.parse()

            // If there's a <meta name="robots" content="noindex" /> then respect it and stop crawling this page.
            if (doc.getElementsByTag("meta")
                    .filter { it: Element -> isMatchingRobotsMetaTag(it) }
                    .any { it.attrEquals("content", "noindex", true) }
            ) {
                return@coroutineScope emptyList()
            }

            // BaseURL to resolve relative URLs is normally the current page URL, but may be overridden by a <base> element on the page.
            // base can also be relative to current page. (e.g. /url)
            val baseUrl = doc.getElementsByTag("base")
                .firstNotNullOfOrNull { URL(link, it.attr("href")) } ?: link

            // Find all the outgoing links to other pages on the same site.
            val outgoingLinks = doc.body().getElementsByTag("a")
                .asSequence()
                .mapNotNull { it.attr("href") }
                .filter { !isMailtoStyleLink(it) }
                .filter { isHypertext(it) }
                .mapNotNull { it.split('#').first() } // Drop the fragment (assume same page different anchors)
                .mapNotNull { it.toUrlOrNull(baseUrl) }
                .filter { isSameOrigin(it, link) }
                .toSet().toList()

            // Handle the current page
            val links = outgoingLinks.joinToString("; ") { "\"$it\"" }
            println("\"${link}\" -> {$links}")
            return@coroutineScope outgoingLinks
        } catch (e: Exception) {
            System.err.println("Could retrieve data from URL [$link]: ${e.message}")
        }
        return@coroutineScope emptyList()
    }

    private fun isMatchingRobotsMetaTag(it: Element) = it.attrEquals("name", "robots", true)
            || it.attrEquals("name", botName, true)
}

private fun Connection.Response.headerEquals(headerName: String, value: String, ignoreCase: Boolean): Boolean =
    header(headerName).equals(value, ignoreCase)

private fun Element.attrEquals(attrName: String, value: String, ignoreCase: Boolean): Boolean =
    attr(attrName).equals(value, ignoreCase)
