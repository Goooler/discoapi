/*
 * Copyright (c) 2021.
 *
 * This file is part of DiscoAPI.
 *
 *     DiscoAPI is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     DiscoAPI is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public enum Config {
    INSTANCE;

    private static final Logger LOGGER                     = LoggerFactory.getLogger(Config.class);

    public static final String FOOJAY_API_BASE_URL         = "FOOJAY_API_BASE_URL";
    public static final String FOOJAY_API_ENVIRONMENT      = "FOOJAY_API_ENVIRONMENT";
    public static final String FOOJAY_API_MONGODB_URL      = "FOOJAY_API_MONGODB_URL";
    public static final String FOOJAY_API_MONGODB_PORT     = "FOOJAY_API_MONGODB_PORT";
    public static final String FOOJAY_API_MONGODB_DATABASE = "FOOJAY_API_MONGODB_DATABASE";
    public static final String FOOJAY_API_MONGODB_USER     = "FOOJAY_API_MONGODB_USER";
    public static final String FOOJAY_API_MONGODB_PASSWORD = "FOOJAY_API_MONGODB_PASSWORD";

    public static final String FOOJAY_MQTT_BROKER          = "FOOJAY_MQTT_BROKER";
    public static final String FOOJAY_MQTT_PORT            = "FOOJAY_MQTT_PORT";
    public static final String FOOJAY_MQTT_USER            = "FOOJAY_MQTT_USER";
    public static final String FOOJAY_MQTT_PASSWORD        = "FOOJAY_MQTT_PASSWORD";


    public String getFoojayApiBaseUrl() {
        final String baseUrl = System.getenv(FOOJAY_API_BASE_URL);
        if (null == baseUrl) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_API_BASE_URL);
            return null;
        } else {
            return baseUrl;
        }
    }


    public String getFoojayApiEnvironment() {
        final String environment = System.getenv(FOOJAY_API_ENVIRONMENT);
        if (null == environment) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_API_ENVIRONMENT);
            return "production";
        } else {
            return environment;
        }
    }


    public String getFoojayMongoDbUrl() {
        final String urlString = System.getenv(FOOJAY_API_MONGODB_URL);
        if (null == urlString) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_API_MONGODB_URL);
            return null;
        } else {
            return urlString;
        }
    }

    public Integer getFoojayMongoDbPort() {
        final String portString = System.getenv(FOOJAY_API_MONGODB_PORT);
        if (null == portString) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_API_MONGODB_PORT);
            return 27017;
        } else {
            try {
                Integer port = Integer.valueOf(portString);
                return port;
            } catch (NumberFormatException e) {
                LOGGER.warn("Environment variable {} contains wrong value.", FOOJAY_API_MONGODB_PORT);
                return 27017;
            }
        }
    }

    public String getFoojayMongoDbDatabase() {
        final String mongoDbDatabase = System.getenv(FOOJAY_API_MONGODB_DATABASE);
        if (null == mongoDbDatabase) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_API_MONGODB_DATABASE);
            return null;
        } else {
            return mongoDbDatabase;
        }
    }

    public String getFoojayMongoDbUser() {
        final String mongoDbUser = System.getenv(FOOJAY_API_MONGODB_USER);
        if (null == mongoDbUser) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_API_MONGODB_USER);
            return null;
        } else {
            return mongoDbUser;
        }
    }

    public String getFoojayMongoDbPassword() {
        final String mongoDbPassword = System.getenv(FOOJAY_API_MONGODB_PASSWORD);
        if (null == mongoDbPassword) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_API_MONGODB_PASSWORD);
            return null;
        } else {
            return mongoDbPassword;
        }
    }


    public String getFoojayMqttBroker() {
        final String mqttBroker = System.getenv(FOOJAY_MQTT_BROKER);
        if (null == mqttBroker) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_MQTT_BROKER);
            return null;
        } else {
            return mqttBroker;
        }
    }

    public Integer getFoojayMqttPort() {
        final String portString = System.getenv(FOOJAY_MQTT_PORT);
        if (null == portString) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_MQTT_PORT);
            return 8883;
        } else {
            try {
                Integer port = Integer.valueOf(portString);
                return port;
            } catch (NumberFormatException e) {
                LOGGER.warn("Environment variable {} contains wrong value.", FOOJAY_MQTT_PORT);
                return 8883;
            }
        }
    }

    public String getFoojayMqttUser() {
        final String mqttUser = System.getenv(FOOJAY_MQTT_USER);
        if (null == mqttUser) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_MQTT_USER);
            return null;
        } else {
            return mqttUser;
        }
    }

    public String getFoojayMqttPassword() {
        final String mqttPassword = System.getenv(FOOJAY_MQTT_PASSWORD);
        if (null == mqttPassword) {
            LOGGER.warn("No environment variable {} found.", FOOJAY_MQTT_PASSWORD);
            return null;
        } else {
            return mqttPassword;
        }
    }
}

