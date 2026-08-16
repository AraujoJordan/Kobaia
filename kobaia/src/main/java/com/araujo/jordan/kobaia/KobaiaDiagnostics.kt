package com.araujo.jordan.kobaia

import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * What was on screen instead, for the moment an assertion fails.
 *
 * `"SIGN IN" should be visible` names what was wanted and nothing about what was there, which for
 * a UI test is the half that tells you whether the button is missing, misspelled, or spelled
 * exactly right one screen further back. This adds that half.
 *
 * Everything here runs **only on the failure path**, so a passing suite never pays for it, and
 * everything here swallows its own failures: a diagnostic that throws would replace the assertion
 * the test actually failed on, which is the one thing worse than no diagnostic at all.
 */
internal object ScreenReport {

    /**
     * The screen as a suffix for an assertion message, or an empty string if it could not be read.
     *
     * @param target what the assertion was looking for, to point out the near misses. Null when
     * the target is not a plain string — a [java.util.regex.Pattern] has no useful near miss.
     */
    fun explain(target: String? = null): String =
        try {
            val screen = read()
            when {
                screen.isEmpty() -> ""
                else -> buildString {
                    append("\n\nOn screen now:")
                    if (screen.texts.isNotEmpty()) {
                        append("\n  text: ")
                        append(screen.texts.joinToString(", ") { "\"$it\"" })
                    }
                    if (screen.tags.isNotEmpty()) {
                        append("\n  tags: ")
                        append(screen.tags.joinToString(", "))
                    }
                    target?.let { wanted ->
                        nearMisses(wanted, screen.texts + screen.tags).forEach { (candidate, why) ->
                            append("\n\nDid you mean \"$candidate\"? ($why)")
                        }
                    }
                }
            }
        } catch (couldNotRead: Throwable) {
            // Deliberately Throwable, and deliberately swallowed: see the class comment.
            Log.w(KOBAIA_TAG, "Could not describe the screen for a failing assertion", couldNotRead)
            ""
        }

    /**
     * The texts and tags currently on screen.
     *
     * Read from a single [android.app.UiAutomation] hierarchy dump rather than by walking
     * `UiObject2`s: every `UiObject2.getText()` opens with a `waitForIdle()`, so reading a hundred
     * nodes one at a time would charge the failing test a hundred idle waits on exactly the kind of
     * busy screen that made it fail. The dump is one traversal, done natively.
     */
    private fun read(): Screen {
        val hierarchy = ByteArrayOutputStream().use { dump ->
            Kobaia.device().dumpWindowHierarchy(dump)
            dump.toString(Charsets.UTF_8.name())
        }
        return Screen(
            texts = TEXT_ATTRIBUTE.findAll(hierarchy).extract(),
            tags = RESOURCE_ID_ATTRIBUTE.findAll(hierarchy).extract().map { it.substringAfterLast('/') }
        )
    }

    /**
     * The distinct, non-blank, length-capped values of an attribute, and no more of them than a
     * failure message can usefully carry — a list screen has hundreds.
     */
    private fun Sequence<MatchResult>.extract(): List<String> =
        map { it.groupValues[1].unescaped().trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(REPORTED_LIMIT)
            .map { if (it.length > VALUE_LIMIT) it.take(VALUE_LIMIT) + "…" else it }
            .toList()

    /**
     * The candidates that are close enough to the target to be the thing the test meant.
     *
     * Deliberately not a fuzzy distance: the misses worth reporting are the ones with a cause a
     * reader can act on — the label is cased differently, it has padding the test did not expect,
     * or the test asked for an exact text where only a substring match exists.
     *
     * @return each candidate with the reason it nearly matched, closest first
     */
    fun nearMisses(target: String, candidates: List<String>): List<Pair<String, String>> {
        val wanted = target.trim()
        if (wanted.isEmpty()) return emptyList()
        return candidates
            .filterNot { it == target }
            .mapNotNull { candidate ->
                val why = when {
                    candidate.equals(wanted, ignoreCase = true) -> "differs by case"
                    candidate.trim() == wanted -> "differs by surrounding whitespace"
                    candidate.collapsed() == wanted.collapsed() -> "differs by whitespace"
                    candidate.contains(wanted, ignoreCase = true) -> "contains it — try clickContaining"
                    wanted.contains(candidate, ignoreCase = true) -> "is only part of it"
                    else -> null
                }
                why?.let { candidate to it }
            }
            .take(NEAR_MISS_LIMIT)
    }

    private fun String.collapsed() = replace(WHITESPACE_RUN, " ").trim()

    /**
     * The five entities [android.view.accessibility.AccessibilityNodeInfo] dumps escape. Nothing
     * else appears, so nothing else is decoded.
     */
    private fun String.unescaped() = replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")

    private data class Screen(val texts: List<String>, val tags: List<String>) {
        fun isEmpty() = texts.isEmpty() && tags.isEmpty()
    }

    private val TEXT_ATTRIBUTE = Regex(" text=\"([^\"]*)\"")
    private val RESOURCE_ID_ATTRIBUTE = Regex(" resource-id=\"([^\"]*)\"")
    private val WHITESPACE_RUN = Regex("\\s+")
    private const val REPORTED_LIMIT = 40
    private const val VALUE_LIMIT = 60
    private const val NEAR_MISS_LIMIT = 3
}
