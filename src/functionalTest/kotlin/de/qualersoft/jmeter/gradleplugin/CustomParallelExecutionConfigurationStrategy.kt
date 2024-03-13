package de.qualersoft.jmeter.gradleplugin

import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy.CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfiguration
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy
import org.junit.platform.commons.util.Preconditions
import org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy.CONFIG_FIXED_PARALLELISM_PROPERTY_NAME
import java.math.BigDecimal
import java.util.concurrent.ForkJoinPool
import java.util.function.Predicate

/**
 * This is required to workaround JUNIT5 issue https://github.com/junit-team/junit5/issues/1858
 */
class CustomParallelExecutionConfigurationStrategy : ParallelExecutionConfigurationStrategy {

  override fun createConfiguration(configurationParameters: ConfigurationParameters): ParallelExecutionConfiguration {

    val factor: BigDecimal = configurationParameters
      .get(CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME) { BigDecimal(it) }
      .orElse(BigDecimal.ONE)
    val maxThreads = configurationParameters.get(CONFIG_FIXED_PARALLELISM_PROPERTY_NAME) { BigDecimal(it).toInt() }

    Preconditions.condition(factor > BigDecimal.ZERO) {
      """Factor '$factor' specified via configuration parameter 
        |'$CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME' must be greater than 0""".trimMargin().replace("\n", "")
    }

    var parallelism = 1.coerceAtLeast(
      factor.multiply(BigDecimal.valueOf(Runtime.getRuntime().availableProcessors().toLong())).toInt()
    )
    if (maxThreads.isPresent) {
      parallelism = maxThreads.get().coerceAtMost(parallelism)
    }

    return CustomParallelExecutionConfiguration(parallelism, parallelism, parallelism, parallelism, KEEP_ALIVE_SECONDS)
  }

  data class CustomParallelExecutionConfiguration(
    private val parallelism: Int,
    private val minimumRunnable: Int,
    private val maxPoolSize: Int,
    private val corePoolSize: Int,
    private val keepAliveSeconds: Int
  ) : ParallelExecutionConfiguration {
    override fun getParallelism(): Int = parallelism
    override fun getMinimumRunnable(): Int = minimumRunnable
    override fun getMaxPoolSize(): Int = maxPoolSize
    override fun getCorePoolSize(): Int = corePoolSize
    override fun getKeepAliveSeconds(): Int = keepAliveSeconds
    override fun getSaturatePredicate(): Predicate<in ForkJoinPool> {
      return Predicate { true }
    }
  }

  companion object {
    const val KEEP_ALIVE_SECONDS = 5
  }
}
