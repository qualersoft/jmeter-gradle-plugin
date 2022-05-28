package de.qualersoft.jmeter.gradleplugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * Shortcut for [ObjectFactory.property]
 */
internal inline fun <reified T> ObjectFactory.property(): Property<T> = this.property(T::class.java)

/**
 * Shortcut for [ObjectFactory.listProperty]
 */
internal inline fun <reified T> ObjectFactory.listProperty(): ListProperty<T> = this.listProperty(T::class.java)

/**
 * Shortcut for [ObjectFactory.mapProperty]
 */
internal inline fun <reified K, reified V> ObjectFactory.mapProperty(): MapProperty<K, V> =
  this.mapProperty(K::class.java, V::class.java)
