package com.quitsq.oprclock

import org.junit.Assert.assertEquals
import org.junit.Test

class RssClientTest {
    private fun parse(xml: String) = RssClient().parse(xml.byteInputStream())

    @Test fun rssReadsOnlyArticleTitlesAndNormalizesDuplicates() {
        val xml = """<rss><channel><title>Channel</title>
            <item><title><![CDATA[ News   & weather ]]></title></item>
            <item><title>News &amp; weather</title></item>
            <item><title> </title></item>
            <item><description>Missing title</description></item>
            <item><title>Second</title></item>
            </channel></rss>"""
        assertEquals(listOf("News & weather", "Second"), parse(xml))
    }

    @Test fun atomSupportsNamespaces() {
        assertEquals(listOf("Article"), parse("""
            <feed xmlns="http://www.w3.org/2005/Atom"><title>Feed</title>
            <entry><title>Article</title></entry></feed>
        """))
    }

    @Test(expected = IllegalStateException::class)
    fun emptyFeedIsAnError() {
        parse("<rss><channel><title>Channel</title></channel></rss>")
    }

    @Test fun externalEntitiesAreNotLoaded() {
        assertEquals(listOf("Safe"), parse("""
            <!DOCTYPE rss [<!ENTITY external SYSTEM "file:///nonexistent-secret">]>
            <rss><channel><item><title>Safe&external;</title></item></channel></rss>
        """))
    }
}
