package com.quitsq.oprclock

import org.w3c.dom.Element
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class RssClient {
    // Call from a worker thread; both network timeouts are bounded.
    fun fetch(url: String): List<String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
            check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.use { parse(it) }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(input: InputStream): List<String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // Android's DOM factory does not support SAX external-entity features.
            // Disable expansion where supported; the resolver below blocks external input.
            isExpandEntityReferences = false
        }
        val builder = factory.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> org.xml.sax.InputSource(java.io.StringReader("")) }
        val document = builder.parse(input)
        val items = document.getElementsByTagNameNS("*", "item")
        val entries = if (items.length > 0) items else document.getElementsByTagNameNS("*", "entry")
        return (0 until entries.length).mapNotNull { index ->
            val children = (entries.item(index) as Element).childNodes
            (0 until children.length).map { children.item(it) }
                .firstOrNull { it.localName == "title" }
                ?.textContent?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotEmpty() }
        }.distinct().take(30).also { check(it.isNotEmpty()) { "No headlines in feed" } }
    }
}
