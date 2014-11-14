// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.skyframe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.Action;
import com.google.devtools.build.lib.blaze.BlazeDirectories;
import com.google.devtools.build.lib.packages.RuleVisibility;
import com.google.devtools.build.lib.skyframe.SkyframeActionExecutor.ConflictException;
import com.google.devtools.build.lib.view.TopLevelArtifactContext;
import com.google.devtools.build.lib.view.WorkspaceStatusAction;
import com.google.devtools.build.lib.view.buildinfo.BuildInfoFactory;
import com.google.devtools.build.lib.view.buildinfo.BuildInfoFactory.BuildInfoKey;
import com.google.devtools.build.skyframe.Injectable;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * A value that represents something computed outside of the skyframe framework. These values are
 * "precomputed" from skyframe's perspective and so the graph needs to be prepopulated with them
 * (e.g. via injection).
 */
public class PrecomputedValue implements SkyValue {

  static final Precomputed<String> DEFAULTS_PACKAGE_CONTENTS =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "default_pkg"));

  static final Precomputed<RuleVisibility> DEFAULT_VISIBILITY =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "default_visibility"));

  static final Precomputed<UUID> BUILD_ID =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "build_id"));

  static final Precomputed<WorkspaceStatusAction> WORKSPACE_STATUS_KEY =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "workspace_status_action"));

  static final Precomputed<TopLevelArtifactContext> TOP_LEVEL_CONTEXT =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "top_level_context"));

  static final Precomputed<Map<BuildInfoKey, BuildInfoFactory>> BUILD_INFO_FACTORIES =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "build_info_factories"));

  static final Precomputed<Map<String, String>> TEST_ENVIRONMENT_VARIABLES =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "test_environment"));

  static final Precomputed<BlazeDirectories> BLAZE_DIRECTORIES =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "blaze_directories"));

  static final Precomputed<ImmutableMap<Action, ConflictException>> BAD_ACTIONS =
      new Precomputed<>(new SkyKey(SkyFunctions.PRECOMPUTED, "bad_actions"));

  private final Object value;

  public PrecomputedValue(Object value) {
    this.value = Preconditions.checkNotNull(value);
  }

  /**
   * Returns the value of the variable.
   */
  public Object get() {
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PrecomputedValue)) {
      return false;
    }
    PrecomputedValue other = (PrecomputedValue) obj;
    return value.equals(other.value);
  }

  @Override
  public String toString() {
    return "<BuildVariable " + value + ">";
  }

  public static final void dependOnBuildId(SkyFunction.Environment env) {
    BUILD_ID.get(env);
  }

  /**
   * A helper object corresponding to a variable in Skyframe.
   *
   * <p>Instances do not have internal state.
   */
  static final class Precomputed<T> {
    private final SkyKey key;

    private Precomputed(SkyKey key) {
      this.key = key;
    }

    @VisibleForTesting
    SkyKey getKeyForTesting() {
      return key;
    }

    /**
     * Retrieves the value of this variable from Skyframe.
     *
     * <p>If the value was not set, an exception will be raised.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    T get(SkyFunction.Environment env) {
      PrecomputedValue value = (PrecomputedValue) env.getValue(key);
      if (value == null) {
        return null;
      }
      return (T) value.get();
    }

    /**
     * Injects a new variable value.
     */
    void set(Injectable injectable, T value) {
      injectable.inject(ImmutableMap.of(key, new PrecomputedValue(value)));
    }
  }
}