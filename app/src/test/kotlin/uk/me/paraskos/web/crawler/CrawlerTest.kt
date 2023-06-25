package uk.me.paraskos.web.crawler

import com.github.stefanbirkner.systemlambda.SystemLambda
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CrawlerTest {
    @Test
    fun shouldReturnNoLinksForXRobotsNoIndex() = runBlocking {
        val crawler = Crawler(URL("https://crawler-test.com"))
        val links = crawler.fetchPage(URL("https://crawler-test.com/robots_protocol/x_robots_tag_noindex"))
        assertTrue(links.isEmpty())
    }

    @Test
    fun shouldReturnNoLinksForSimpleRobotsStatement() = runBlocking {
        val crawler = Crawler(URL("https://crawler-test.com"))
        val links = crawler.fetchPage(URL("https://crawler-test.com/robots_protocol/robots_excluded_1"))
        assertTrue(links.isEmpty())
    }

    @Test
    fun shouldReturnNoLinksForMetaNoindex() = runBlocking {
        val crawler = Crawler(URL("https://crawler-test.com"))
        val links = crawler.fetchPage(URL("https://crawler-test.com/robots_protocol/meta_noindex"))
        assertTrue(links.isEmpty())
    }

    @Test
    fun shouldNotFollowExternalLinks() = runBlocking {
        val base = URL("https://crawler-test.com/")
        val crawler = Crawler(base)
        val links = crawler.fetchPage(URL("https://crawler-test.com/links/max_external_links")).toMutableList()
        links.remove(base) // this site links back to its landing page everywhere, all other links on this page are external.
        assertTrue(links.isEmpty())
    }

    @Test
    fun shouldReturnRelativeLinks() = runBlocking {
        val crawler = Crawler(URL("https://crawler-test.com/"))
        val links = crawler.fetchPage(URL("https://crawler-test.com/links/relative_link/a/b"))
        assertEquals(4, links.count(), "Expected only 4 links on the page")
    }
    @Test
    fun shouldNotReturnWeirdLinksOrDeepLinks() = runBlocking {
        val base = URL("https://crawler-test.com/")
        val crawler = Crawler(base)
        val links = crawler.fetchPage(URL("https://crawler-test.com/links/non_standard_links")).toMutableList()
        links.remove(base) // this site links back to its landing page everywhere, all other links on this page are external.
        assertTrue(links.isEmpty())
    }

    @Test
    fun shouldReturnEmptyListFromErrorPages() = runBlocking {
        val crawler = Crawler(URL("https://crawler-test.com/"))
        val links = crawler.fetchPage(URL("https://crawler-test.com/status_codes/status_500"))
        assertTrue(links.isEmpty())
    }


    @Test
    fun shouldCrawlFromStartingUrl() {
        // docs.monzo.com is mostly on 1 page, only 1 other page on the domain so is a nice simple test case
        val initialUrl = "https://docs.monzo.com/";
        val output = SystemLambda.tapSystemOut {
            runBlocking {
                val crawler = Crawler(URL(initialUrl))
                crawler.crawl()
            }
        }.trim()
        println("CAPTURED: $output")
        assertEquals(2, output.lines().count())
        var arrowSplit = output.lines()[0].split("->", limit = 2)
        assertContains(arrowSplit[0], initialUrl)
        assertContains(arrowSplit[1], initialUrl)
        assertContains(arrowSplit[1], "https://docs.monzo.com/cdn-cgi/l/email-protection")

        arrowSplit = output.lines()[1].split("->", limit = 2)
        assertContains(arrowSplit[0], "https://docs.monzo.com/cdn-cgi/l/email-protection")
        assertContains(arrowSplit[1], "{}")
    }

    @Test
    fun shouldTakeStartingUrlFromProgramArguments() {
        // This URL only links to itself
        val initialUrl = "https://subdomain.crawler-test.com/";
        val output = SystemLambda.tapSystemOut {
            runBlocking {
                main(arrayOf(initialUrl))
            }
        }
        println("CAPTURED: $output")
        assertEquals(1, output.trim().lines().count())
        val arrowSplit = output.split("->", limit = 2)
        assertContains(arrowSplit[0], initialUrl)
        assertEquals("{\"$initialUrl\"}", arrowSplit[1].trim())
    }

}