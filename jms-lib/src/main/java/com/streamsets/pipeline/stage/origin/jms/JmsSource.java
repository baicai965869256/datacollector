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
package com.streamsets.pipeline.stage.origin.jms;

import com.streamsets.pipeline.api.BatchMaker;
import com.streamsets.pipeline.api.OffsetCommitter;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseSource;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.stage.origin.lib.BasicConfig;
import com.streamsets.pipeline.stage.origin.lib.CredentialsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JmsSource extends BaseSource implements OffsetCommitter {
  private static final Logger LOG = LoggerFactory.getLogger(JmsSource.class);

  private final BasicConfig basicConfig;
  private final CredentialsConfig credentialsConfig;
  private final JmsConfig jmsConfig;
  private final JmsMessageConsumerFactory jmsMessageConsumerFactory;
  private final JmsMessageConverter jmsMessageConverter;
  private final InitialContextFactory initialContextFactory;
  private JmsMessageConsumer jmsMessageConsumer;
  private InitialContext initialContext;
  private ConnectionFactory connectionFactory;
  private long messagesConsumed;

  public JmsSource(BasicConfig basicConfig, CredentialsConfig credentialsConfig, JmsConfig jmsConfig,
                   JmsMessageConsumerFactory jmsMessageConsumerFactory,
                   JmsMessageConverter jmsMessageConverter, InitialContextFactory initialContextFactory) {
    this.basicConfig = basicConfig;
    this.credentialsConfig = credentialsConfig;
    this.jmsConfig = jmsConfig;
    this.jmsMessageConsumerFactory = jmsMessageConsumerFactory;
    this.jmsMessageConverter = jmsMessageConverter;
    this.initialContextFactory = initialContextFactory;
    this.messagesConsumed = 0;
  }

  @Override
  protected List<ConfigIssue> init() {
    List<ConfigIssue> issues = new ArrayList<>();
    try {
      Properties contextProperties = new Properties();
      contextProperties.setProperty(
        javax.naming.Context.INITIAL_CONTEXT_FACTORY,
        jmsConfig.initialContextFactory);
      contextProperties.setProperty(
        javax.naming.Context.PROVIDER_URL, jmsConfig.providerURL);
      initialContext = initialContextFactory.create(contextProperties);
    } catch (NamingException ex) {
      LOG.info(Utils.format(JmsErrors.JMS_00.getMessage(), jmsConfig.initialContextFactory,
        jmsConfig.providerURL, ex.toString()), ex);
      issues.add(getContext().createConfigIssue(JmsGroups.JMS.name(), "jmsConfig.initialContextFactory", JmsErrors.JMS_00,
        jmsConfig.initialContextFactory, jmsConfig.providerURL, ex.toString()));
    }
    if (issues.isEmpty()) {
      try {
        connectionFactory = (ConnectionFactory) initialContext.lookup(jmsConfig.connectionFactory);
      } catch (NamingException ex) {
        LOG.info(Utils.format(JmsErrors.JMS_01.getMessage(), jmsConfig.initialContextFactory, ex.toString()), ex);
        issues.add(getContext().createConfigIssue(JmsGroups.JMS.name(), "jmsConfig.initialContextFactory", JmsErrors.JMS_01,
          jmsConfig.connectionFactory, ex.toString()));
      }
    }
    if (issues.isEmpty()) {
      jmsMessageConsumer = jmsMessageConsumerFactory.create(initialContext, connectionFactory, basicConfig,
        credentialsConfig, jmsConfig, jmsMessageConverter);
      issues.addAll(jmsMessageConsumer.init(getContext()));
    }
    // no dependencies on the above for initialization
    issues.addAll(jmsMessageConverter.init(getContext()));
    return issues;
  }

  @Override
  public void destroy() {
    if (jmsMessageConsumer != null) {
      jmsMessageConsumer.close();
    }
    super.destroy();
  }

  @Override
  public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws StageException {
    int batchSize = Math.max(basicConfig.maxBatchSize, maxBatchSize);
    messagesConsumed += jmsMessageConsumer.take(batchMaker, getContext(), batchSize, messagesConsumed);
    return Utils.format("{}::{}", jmsConfig.destinationName, messagesConsumed);
  }

  @Override
  public void commit(String offset) throws StageException {
    jmsMessageConsumer.commit();
  }

  public void rollback() {
    try {
      if (jmsMessageConsumer != null) {
        jmsMessageConsumer.rollback();
      }
    } catch (Exception ex) {
      LOG.warn("Rollback failed: {}", ex.toString(), ex);
    }
  }
}
