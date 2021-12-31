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

package de.dytanic.cloudnet.driver.event;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.dytanic.cloudnet.driver.event.invoker.ListenerInvokerGenerator;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import lombok.NonNull;

public class DefaultEventManager implements EventManager {

  /**
   * Holds all registered events mapped to all registered events listeners
   */
  protected final Lock lock = new ReentrantLock();
  protected final ListMultimap<Class<?>, RegisteredEventListener> listeners = ArrayListMultimap.create();

  @Override
  public @NonNull EventManager unregisterListeners(@NonNull ClassLoader classLoader) {
    this.safeRemove(value -> value.instance().getClass().getClassLoader().equals(classLoader));
    // for chaining
    return this;
  }

  @Override
  public @NonNull EventManager unregisterListener(Object @NonNull ... listeners) {
    var listenerList = Arrays.asList(listeners);
    this.safeRemove(value -> listenerList.contains(value.instance()));
    // for chaining
    return this;
  }

  @Override
  public <T extends Event> @NonNull T callEvent(@NonNull String channel, @NonNull T event) {
    this.lock.lock();
    try {
      // get all registered listeners of the event
      var listeners = this.listeners.get(event.getClass());
      if (!listeners.isEmpty()) {
        // check if there is only one listener
        if (listeners.size() == 1) {
          var listener = listeners.get(0);
          // check if the event gets called on the same channel as the listener is listening to
          if (listener.channel().equals(channel)) {
            listener.fireEvent(event);
          }
        } else {
          // post the event to the listeners
          for (var listener : listeners) {
            // check if the event gets called on the same channel as the listener is listening to
            if (listener.channel().equals(channel)) {
              listener.fireEvent(event);
            }
          }
        }
      }
    } finally {
      this.lock.unlock();
    }
    // for chaining
    return event;
  }

  @Override
  public @NonNull EventManager registerListener(@NonNull Object listener) {
    // get all methods of the listener
    for (var method : listener.getClass().getDeclaredMethods()) {
      // check if the method can be used
      var annotation = method.getAnnotation(EventListener.class);
      if (annotation != null && method.getParameterCount() == 1) {
        // check the parameter type
        var eventClass = method.getParameterTypes()[0];
        if (!Event.class.isAssignableFrom(eventClass)) {
          throw new IllegalStateException(String.format(
            "Parameter type %s (index 0) of listener method %s in %s is not a subclass of Event",
            eventClass.getName(),
            method.getName(),
            listener.getClass().getName()));
        }
        // bring the information together
        var eventListener = new DefaultRegisteredEventListener(
          listener,
          method.getName(),
          eventClass,
          annotation,
          ListenerInvokerGenerator.generate(listener, method, eventClass));
        // bake an event listener from the information
        this.lock.lock();
        try {
          var listeners = this.listeners.get(eventClass);
          listeners.add(eventListener);
          // sort now - we don't need to sort lather then
          Collections.sort(listeners);
        } finally {
          this.lock.unlock();
        }
      }
    }
    // for chaining
    return this;
  }

  protected void safeRemove(@NonNull Predicate<RegisteredEventListener> predicate) {
    this.lock.lock();
    try {
      // prevents concurrency issues
      this.listeners.values().removeIf(predicate);
    } finally {
      this.lock.unlock();
    }
  }
}
