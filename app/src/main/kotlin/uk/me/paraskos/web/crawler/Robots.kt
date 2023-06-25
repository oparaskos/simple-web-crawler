package uk.me.paraskos.web.crawler

import java.net.URL
import java.nio.charset.Charset

/**
 * There's a lot more to robots than this.
 * There is a library by google I could use here but it seems well out of scope for this.
 * I just want to show I thought about it.
 */
class Robots {
    private var disallowLines: List<String> = emptyList();

    fun load(url: URL): Robots {
        try {
            val robotsTxt = url.readText(Charset.defaultCharset());
            disallowLines = robotsTxt.lineSequence().filter { it.startsWith("Disallow:") }.toList()
        } catch (e: Exception) {
            System.err.println("Could not load robots.txt ${e.message}")
            disallowLines = emptyList()
        }
        return this
    }

    fun forbids(link: URL): Boolean {
        // Assume all the disallow lines are for us and ignore everything else
        for (line in disallowLines) {
            // do a simple starts-with and pretend operators like '*' are just normal text with no special meaning.
            val disallowedPath = line.split("Disallow:").last().trim()
            if (link.path.startsWith(disallowedPath, true)) return true
        }
        return false;
    }
}