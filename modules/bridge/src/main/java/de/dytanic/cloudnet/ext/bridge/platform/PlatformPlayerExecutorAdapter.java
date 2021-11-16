/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.bridge.platform;

import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.jetbrains.annotations.NotNull;

public abstract class PlatformPlayerExecutorAdapter implements PlayerExecutor {

  @Override
  public void sendTitle(@NotNull Title title) {
    // get the title times
    Times times = title.times();
    if (times == null) {
      times = Title.DEFAULT_TIMES;
    }
    // send the title
    this.sendTitle(
      title.title(),
      title.subtitle(),
      (int) times.fadeIn().toMillis() / 50,
      (int) times.stay().toMillis() / 50,
      (int) times.fadeOut().toMillis() / 50);
  }

  protected void sendTitle(@NotNull Component title, @NotNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    // no-op
  }
}