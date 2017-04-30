/**
 * Copyright 2017 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
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
package com.streamsets.datacollector.bundles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Writer for content generator classes.
 *
 * Subclasses OutputStream by adding adding methods that are relevant to bundles creation (stores whole file, ...)
 */
public interface BundleWriter {

  /**
   * Create new file in the bundle.
   *
   * @param name Path and name of the file that should be started.
   */
  public abstract void markStartOfFile(String name) throws IOException;

  /**
   * Needs to be called after each file is done writing.
   */
  public abstract void markEndOfFile() throws IOException;

  /**
   * Write out string with platform default encoding.
   * @param str String to be written out
   */
  public abstract void write(String str);

  /**
   * Write out string with platform default encoding and end with new line character.
   * @param str String to be written out
   */
  public abstract void writeLn(String str);

  /**
   * Write given properties file.
   *
   * @param fileName Path and file name for the file in the bundle
   * @param properties Properties to be serialized
   */
  public abstract void write(String fileName, Properties properties) throws IOException;

  /**
   * Copy file at given path to given directory, the file will be redacted line by line.
   *
   * @param bundleDirectory Directory inside bundle where the file should be stored (file name will be kept)
   * @param path Path on local filesystem for the source file.
   * @throws IOException
   */
  public abstract void write(String bundleDirectory, Path path) throws IOException;
}