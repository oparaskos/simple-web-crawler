package uk.me.paraskos.web.crawler

import java.net.URL

/**
 * Treat different origins as external links not to be follwed.
 */
internal fun isSameOrigin(it: URL, link: URL) =
    it.host == link.host && it.port == link.port && it.protocol == link.protocol

/**
 * So that I can skip 'tel:0123', 'mailto:abc@def' links, don't treat them as relative
 * it's not easy to enumerate all the (tel:, skype:, mailto:, fax:) prefixes so instead I'm going to assume (probably wrongly) that people don't use colons in relative URLs without escaping.
 */
internal fun isMailtoStyleLink(it: String) = it.matches(Regex("^[a-z_\\-]+:[^\\/]+$", RegexOption.IGNORE_CASE))

/**
 * don't bother trying to parse URLs with non HTTP schemes.
 * filter out non http(s) protocols, don't follow links to FTP, app deeplinks; i.e. it starts 'some-protocol://' but not http(s):// (https://regexr.com/7g0c6)
 */
internal fun isHypertext(it: String) = it.startsWith("http://") || it.startsWith("https://") || !it.contains("://");

internal fun String.toUrlOrNull(baseUrl: URL?): URL? {
    try {
        if(baseUrl == null) return URL(this)
        return URL(baseUrl, this)
    } catch (e: Exception) {
        System.err.println("Could not convert URL [$this] with context [$baseUrl]: ${e.message}")
        return null;
    }
}