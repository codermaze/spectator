/*
 * Copyright 2014-2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spectator.atlas;

import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Statistic;
import com.netflix.spectator.impl.AtomicDouble;
import com.netflix.spectator.impl.StepDouble;

import java.util.Collections;

/**
 * <p><b>Experimental:</b> This type may be removed in a future release.</p>
 *
 * Gauge that reports the maximum value submitted during an interval to Atlas. Main use-case
 * right now is for allowing the max stat used internally to AtlasDistributionSummary and
 * AtlasTimer to be transferred to a remote AtlasRegistry.
 */
class AtlasMaxGauge extends AtlasMeter implements MaxGauge {

  private final StepDouble value;
  private final Id stat;

  /** Create a new instance. */
  AtlasMaxGauge(Id id, Clock clock, long ttl, long step) {
    super(id, clock, ttl);
    this.value = new StepDouble(0.0, clock, step);
    // Add the statistic for typing. Re-adding the tags from the id is to retain
    // the statistic from the id if it was already set
    this.stat = id.withTag(Statistic.max).withTags(id.tags()).withTag(DsType.gauge);
  }

  @Override public Iterable<Measurement> measure() {
    final Measurement m = new Measurement(stat, clock.wallTime(), value());
    return Collections.singletonList(m);
  }

  @Override public void set(double v) {
    AtomicDouble current = value.getCurrent();
    double max = current.get();
    while (v > max && !current.compareAndSet(max, v)) {
      max = current.get();
    }
    updateLastModTime();
  }

  @Override public double value() {
    return value.poll();
  }
}
