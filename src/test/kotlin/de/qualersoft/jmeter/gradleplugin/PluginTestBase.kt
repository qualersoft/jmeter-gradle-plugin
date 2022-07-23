package de.qualersoft.jmeter.gradleplugin

import io.kotest.assertions.print.print
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult

object PluginTestBase {
  const val PLUGIN_ID = "de.qualersoft.jmeter"
}

internal fun <T : CharSequence, C : Collection<T>> matchingEntry(regex: Regex) = object : Matcher<C> {
  override fun test(value: C) = MatcherResult(
    value.any { it.matches(regex) },
    { "Collection should contain element matching ${regex.print().value}; listing some elements ${value.take(5)}" },
    { "Collection should not contain element matching ${regex.print().value}" }
  )
}

internal fun <T : CharSequence, C : Collection<T>> entryStartsWith(t: T) = object : Matcher<C> {
  override fun test(value: C) = MatcherResult(
    value.any { it.startsWith(t) },
    { "Collection should contain element starting with ${t.print().value}; listing some elements ${value.take(5)}" },
    { "Collection should not contain element starting with ${t.print().value}" }
  )
}

internal fun <T : CharSequence, C : Collection<T>> entryEndsWith(t: T) = object : Matcher<C> {
  override fun test(value: C) = MatcherResult(
    value.any { it.endsWith(t) },
    { "Collection should contain element ending with ${t.print().value}; listing some elements ${value.take(5)}" },
    { "Collection should not contain element ending with ${t.print().value}" }
  )
}
