package uk.me.paraskos.web.crawler

import java.net.URL
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinksTest {

    @Test
    fun shouldNotErrorForBadUrlsJustReturnNull() {
        assertNull("about:blank".toUrlOrNull(URL("https://example.com")))
    }

    @Test
    fun sameOriginShouldReturnTrueForSameOrigin() {
        listOf(
            URL("http://store.company.com/dir2/other.html"),// 	Same origin 	Only the path differs
            URL("http://store.company.com/dir/inner/another.html"),// 	Same origin 	Only the path differs
        ).forEach {
            assertTrue { isSameOrigin(it, URL("http://store.company.com")) }
        }

        listOf(
            URL("https://store.company.com/page.html"), // 	Failure 	Different protocol
            URL("http://store.company.com:81/dir/page.html"), // 	Failure 	Different port (http:// is port 80 by default)
            URL("http://news.company.com/dir/page.html") // 	Failure 	Different host
        ).forEach{
            assertFalse { isSameOrigin(it, URL("http://store.company.com")) }
        }
    }

    @Test
    fun isHypertextShouldFilterOnlyRelativeOrAbsoluteHttpLinks() {
        listOf(
            "https://xyz",
            "/absolute/path",
            "/absolute/path:with:colon",
            "relative/path"
        ).forEach {
            assertTrue(isHypertext(it), "$it should be recognised as a followable link")
        }

        listOf(
            "sms://xyz",
            "chrome://xyz",
            "mid://xyz",
            "callto://+1234567",
            "fb-messenger://share?link=encodedLink",
            "itms-apps://itunes.apple.com/us/app/pages/id361309726?mt=8&uo=4"
        )
            .forEach {
                assertFalse(isHypertext(it), "$it should not be recognised as a followable link")
            }
    }
}
