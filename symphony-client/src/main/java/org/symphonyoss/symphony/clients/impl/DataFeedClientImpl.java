/*
 *
 * Copyright 2016 The Symphony Software Foundation
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.symphonyoss.symphony.clients.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.common.Constants;
import org.symphonyoss.client.exceptions.DataFeedException;
import org.symphonyoss.client.exceptions.SymFault;
import org.symphonyoss.client.model.SymAuth;
import org.symphonyoss.symphony.agent.api.DatafeedApi;
import org.symphonyoss.symphony.agent.invoker.ApiClient;
import org.symphonyoss.symphony.agent.invoker.ApiException;
import org.symphonyoss.symphony.agent.model.Datafeed;
import org.symphonyoss.symphony.agent.model.V2BaseMessage;
import org.symphonyoss.symphony.agent.model.V4Event;
import org.symphonyoss.symphony.clients.DataFeedClient;
import org.symphonyoss.client.events.SymEvent;
import org.symphonyoss.symphony.clients.model.ApiVersion;

import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.List;


/**
 * Provides access to datafeed in order to stream all message events (messages) through blocking calls.
 *
 * @author Frank Tarsillo
 */
public class DataFeedClientImpl implements DataFeedClient {

    private final ApiClient apiClient;
    private final SymAuth symAuth;
    private Logger logger = LoggerFactory.getLogger(DataFeedClientImpl.class);


    public DataFeedClientImpl(SymAuth symAuth, String agentUrl) {

        this.symAuth = symAuth;


        //Get Service client to query for userID.
        apiClient = org.symphonyoss.symphony.agent.invoker.Configuration.getDefaultApiClient();
        apiClient.setBasePath(agentUrl);

    }

    /**
     * If you need to override HttpClient.  Important for handling individual client certs.
     *
     * @param symAuth    Authorization model containing session and key tokens
     * @param podUrl Service URL used to access API
     * @param httpClient Custom HTTP client
     */
    public DataFeedClientImpl(SymAuth symAuth, String podUrl, Client httpClient) {
        this.symAuth = symAuth;

        //Get Service client to query for userID.
        apiClient = org.symphonyoss.symphony.agent.invoker.Configuration.getDefaultApiClient();
        apiClient.setHttpClient(httpClient);
        apiClient.setBasePath(podUrl);

    }



    @Override
    public Datafeed createDatafeed(ApiVersion apiVersion) throws DataFeedException {


        DatafeedApi datafeedApi = new DatafeedApi(apiClient);


        try {


                return datafeedApi.v4DatafeedCreatePost(symAuth.getSessionToken().getToken(), symAuth.getKeyToken().getToken());


        } catch (ApiException e) {
            throw new DataFeedException("Could not start datafeed..",
                    datafeedApi.getApiClient().getBasePath(), e.getCode(), e);
        } catch(RuntimeException ef){
            throw new DataFeedException("Could not start datafeed due to network issue..",
                    datafeedApi.getApiClient().getBasePath(),500, ef);
        }
    }



    @Override
    public List<SymEvent> getEventsFromDatafeed(Datafeed datafeed, int maxMessages) throws DataFeedException {

        DatafeedApi datafeedApi = new DatafeedApi(apiClient);

        if (datafeed == null) {
            throw new NullPointerException("Datafeed was not provided and null..");
        }


        try {
            List<V4Event> v4Events;

            v4Events = datafeedApi.v4DatafeedIdReadGet(datafeed.getId(), symAuth.getSessionToken().getToken(), symAuth.getKeyToken().getToken(), maxMessages);

            return SymEvent.toSymEvent(v4Events);
        } catch (ApiException e) {
            throw new DataFeedException("Failed to retrieve messages from datafeed...",
                    datafeedApi.getApiClient().getBasePath(), e.getCode(), e);
        }catch(RuntimeException ef){
            throw new DataFeedException("Failed to retrieve messages due to network issue..",
                    datafeedApi.getApiClient().getBasePath(),500, ef);
        }


    }


    @Override
    public List<SymEvent> getEventsFromDatafeed(Datafeed datafeed) throws DataFeedException {

        return getEventsFromDatafeed(datafeed, Integer.parseInt(System.getProperty(Constants.DATAFEED_MAX_MESSAGES, "100")));
    }


}
