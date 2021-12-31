/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.command;

import de.dytanic.cloudnet.common.Nameable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * The commandInfo class allows to easily serialize the command information
 */
public record CommandInfo(
  @NonNull String name,
  @NonNull Set<String> aliases,
  @NonNull String permission,
  @NonNull String description,
  @NonNull List<String> usage
) implements Nameable {

  @Contract("_ -> new")
  public static @NonNull CommandInfo empty(@NonNull String name) {
    return new CommandInfo(name, Collections.emptySet(), "", "", Collections.emptyList());
  }

  public @NonNull String joinNameToAliases(@NonNull String separator) {
    var result = this.name;
    if (!this.aliases.isEmpty()) {
      result += separator + String.join(separator, this.aliases);
    }

    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommandInfo that)) {
      return false;
    }
    return this.name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name);
  }
}
