/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.play;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import javax.inject.Inject;

public class DaggerBasicDemo {

  // task interface
  public interface Task {
    public void run();
  }

  // base task implementation
  public static abstract class TaskImpl implements Task {
    private String name;

    public TaskImpl(String name){
      this.name = name;
    }

    @Override
    public void run() {
      System.out.println(String.format("  %s.run()", name));
    }
  }

  // One task implementation
  public static class TaskImplA extends TaskImpl {

    // Even if we don't have a anything to inject, we need to define an
    // injectable constructor for Dagger to pick it up
    @Inject
    public TaskImplA(){
      super(TaskImplA.class.getSimpleName());
    }
  }

  // Another task implementation
  public static class TaskImplB extends TaskImpl {

    // Even if we don't have a anything to inject, we need to define an
    // injectable constructor for Dagger to pick it up
    @Inject
    public TaskImplB(){
      super(TaskImplB.class.getSimpleName());
    }
  }

  // application interface
  public interface App {
    public void run();
  }

  // application implementation
  public static class AppImpl implements App {
    @Inject Task task;

    @Override
    public void run() {
      System.out.println("AppImpl.run()");
      task.run();
    }
  }

  // Dagger module, defines the DI rules
  // Module:inject defines the list of external classes (external to the module) that can be injected
  // Obvious note: we need to inject classes, cannot be an interface
  @Module(injects = AppImpl.class)
  public static class AppModuleA {

    // provides an Task using an TaskImplA, this is the interface to impl wiring definition
    // (TaskImplA will be magically injected because it has an Inject constructor)
    @Provides Task provideRunnable(TaskImplA task) {
      return task;
    }

  }

  // Dagger module, defines the DI rules
  // Module:inject defines the list of external classes (external to the module) that can be injected
  // Obvious note: we need to inject classes, cannot be an interface
  @Module(injects = AppImpl.class)
  public static class AppModuleB {

    // provides an Task using an TaskImplA, this is the interface to impl wiring definition
    // (TaskImplB will be magically injected because it has an Inject constructor)
    @Provides Task provideRunnable(TaskImplB task) {
      return task;
    }

  }

  public static void main(String[] args) throws Exception {

    // creating the DI dagger graph using the AppModuleA
    ObjectGraph dagger1 = ObjectGraph.create(new AppModuleA());
    // creating the app instance
    App app1 = new AppImpl();
    // injecting the app instance with dagger (we are injecting the task to use)
    dagger1.inject(app1);
    // running the app
    app1.run();

    // same same but using AppModuleB
    ObjectGraph dagger2 = ObjectGraph.create(new AppModuleB());
    // small variant of the app instantiation & injection, letting Dagger do it all
    App app2 = dagger2.get(AppImpl.class);
    app2.run();
  }

}
