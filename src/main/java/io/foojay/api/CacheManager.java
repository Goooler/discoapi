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

package io.foojay.api;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import eu.hansolo.jdktools.scopes.BuildScope;
import eu.hansolo.jdktools.util.OutputFormat;
import io.foojay.api.mqtt.MqttEvt;
import io.foojay.api.mqtt.MqttEvtObserver;
import io.foojay.api.mqtt.MqttManager3;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import io.foojay.api.util.JsonCache;
import io.foojay.api.util.PkgCache;
import io.foojay.api.util.State;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.foojay.api.util.Constants.API_VERSION_V3;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_OPEN;


@Requires(notEnv = Environment.TEST) // Don't run in tests
public enum CacheManager {
    INSTANCE;

    private static final Logger                       LOGGER                      = LoggerFactory.getLogger(CacheManager.class);
    public final         MqttManager3                 mqttManager                 = new MqttManager3();
    public final         MqttEvtObserver              mqttEvtObserver             = evt -> handleMqttEvt(evt);
    public final         PkgCache<String, Pkg>        pkgCache                    = new PkgCache<>();
    public final         JsonCache<String, String>    jsonCacheV2                 = new JsonCache<>();
    public final         JsonCache<String, String>    jsonCacheV3                 = new JsonCache<>();
    public final         JsonCache<String, String>    jsonCacheMinimizedV3        = new JsonCache<>();
    public final         Map<Integer, Boolean>        maintainedMajorVersions     = new ConcurrentHashMap<>() {{
        put(1, false);
        put(2, false);
        put(3, false);
        put(4, false);
        put(5, false);
        put(6, false);
        put(7, true);
        put(8, true);
        put(9, false);
        put(10, false);
        put(11, true);
        put(12, false);
        put(13, false);
        put(14, false);
        put(15, false);
        put(16, false);
        put(17, true);
        put(18, false);
        put(19, false);
        put(20, true);
        put(21, true);
        put(22, true);
    }};
    public final         AtomicBoolean                syncWithDatabaseInProgress  = new AtomicBoolean(false);
    public final         AtomicLong                   msToFillCacheWithPkgsFromDB = new AtomicLong(-1);
    public final         AtomicLong                   numberOfPackages            = new AtomicLong(-1);
    public final         AtomicReference<Instant>     lastSync                    = new AtomicReference<>(Instant.MIN);
    private final        List<MajorVersion>           majorVersions               = new LinkedList<>();
    private final        List<MajorVersion>           graalvmMajorVersions        = new LinkedList<>();


    CacheManager() {
        mqttManager.subscribe(Constants.MQTT_PKG_UPDATE_TOPIC, MqttQos.EXACTLY_ONCE);
        mqttManager.subscribe(Constants.MQTT_EPHEMERAL_ID_UPDATE_TOPIC, MqttQos.EXACTLY_ONCE);
        mqttManager.subscribe(Constants.MQTT_UPDATER_STATE_TOPIC, MqttQos.EXACTLY_ONCE);
        mqttManager.addMqttObserver(mqttEvtObserver);
        maintainedMajorVersions.entrySet().forEach(entry-> majorVersions.add(new MajorVersion(entry.getKey(), Helper.getTermOfSupport(entry.getKey()), entry.getValue())));
    }


    public void updateMajorVersions() {
        StateManager.INSTANCE.setState(State.UPDATING, "Updating major versions");
        List<MajorVersion> majorVersionsFromDb = MongoDbManager.INSTANCE.getMajorVersions();
        if (null == majorVersionsFromDb || majorVersionsFromDb.isEmpty()) {
            LOGGER.error("Error updating major versions from mongodb");
            Set<MajorVersion> mv = new HashSet<>();
            pkgCache.getPkgs().forEach(pkg -> mv.add(pkg.getMajorVersion()));
            majorVersions.clear();
            majorVersions.addAll(mv);
        } else {
        majorVersions.clear();
            majorVersions.addAll(majorVersionsFromDb);
            LOGGER.debug("Successfully updated major versions");
        }
        
        Set<MajorVersion> mvgvm = new HashSet<>();
        pkgCache.getPkgs().stream().filter(pkg -> Distro.isBasedOnGraalVM(pkg.getDistribution().getDistro())).forEach(pkg -> mvgvm.add(new MajorVersion(pkg.getFeatureVersion().getAsInt())));
        graalvmMajorVersions.clear();
        graalvmMajorVersions.addAll(mvgvm);
        
        updateMaintainedMajorVersions();
    }

    public void updateMaintainedMajorVersions() {
        LOGGER.debug("Updating maintained major versions");
        final Properties            maintainedProperties       = new Properties();
        final Map<Integer, Boolean> tmpMaintainedMajorVersions = new HashMap<>();
        try {
            HttpResponse<String> response = Helper.get(Constants.MAINTAINED_PROPERTIES_URL);
            if (null == response) { return; }
            String maintainedPropertiesText = response.body();
            if (null == maintainedPropertiesText) { return; }
            maintainedProperties.load(new StringReader(maintainedPropertiesText));
            maintainedProperties.entrySet().forEach(entry -> {
                Integer majorVersion = Integer.valueOf(entry.getKey().toString().replaceAll("jdk-", ""));
                Boolean maintained   = Boolean.valueOf(entry.getValue().toString().toLowerCase());
                tmpMaintainedMajorVersions.put(majorVersion, maintained);
            });
            maintainedMajorVersions.clear();
            maintainedMajorVersions.putAll(tmpMaintainedMajorVersions);
            LOGGER.debug("Successfully updated maintained major versions");
        } catch (Exception e) {
            LOGGER.error("Error loading maintained version properties from github. {}", e);
        }
    }

    public void updateJsonCacheV2() {
        StateManager.INSTANCE.setState(State.UPDATING, "Updating Json Cache V2");
        pkgCache.getEntrySet().parallelStream().forEach(entry -> jsonCacheV2.put(entry.getKey(), entry.getValue().toString(OutputFormat.REDUCED_COMPRESSED, Constants.API_VERSION_V2)));
        final List<String> keysToRemove = jsonCacheV2.getEntrySet().parallelStream().filter(entry -> !pkgCache.containsKey(entry.getKey())).map(entry -> entry.getKey()).collect(Collectors.toList());
        jsonCacheV2.remove(keysToRemove);
    }
    public void updateJsonCacheV3() {
        StateManager.INSTANCE.setState(State.UPDATING, "Updating Json Cache V3");
        pkgCache.getEntrySet().parallelStream().forEach(entry -> jsonCacheV3.put(entry.getKey(), entry.getValue().toString(OutputFormat.REDUCED_COMPRESSED, Constants.API_VERSION_V3)));
        final List<String> keysToRemove = jsonCacheV3.getEntrySet().parallelStream().filter(entry -> !pkgCache.containsKey(entry.getKey())).map(entry -> entry.getKey()).collect(Collectors.toList());
        jsonCacheV3.remove(keysToRemove);
    }
    public void updateJsonCacheMinimizedV3() {
        StateManager.INSTANCE.setState(State.UPDATING, "Updating Json Cache Reduced V3");
        pkgCache.getEntrySet().parallelStream().forEach(entry -> jsonCacheMinimizedV3.put(entry.getKey(), entry.getValue().toString(OutputFormat.MINIMIZED, Constants.API_VERSION_V3)));
        final List<String> keysToRemove = jsonCacheMinimizedV3.getEntrySet().parallelStream().filter(entry -> !pkgCache.containsKey(entry.getKey())).map(entry -> entry.getKey()).collect(Collectors.toList());
        jsonCacheMinimizedV3.remove(keysToRemove);
    }

    public List<MajorVersion> getMajorVersions() {
        return getMajorVersions(BuildScope.BUILD_OF_OPEN_JDK);
    }
    public List<MajorVersion> getMajorVersions(final BuildScope scope) {
        if (majorVersions.isEmpty() || graalvmMajorVersions.isEmpty()) { updateMajorVersions(); }
        switch(scope) {
            case BUILD_OF_GRAALVM  : return graalvmMajorVersions;
            case BUILD_OF_OPEN_JDK :
            default                : return majorVersions.stream().filter(majorVersion -> majorVersion.getScope() == scope).collect(Collectors.toList());
        }
    }

    public void syncCacheWithDatabase() {
        if (syncWithDatabaseInProgress.get()) { return; }

        syncWithDatabaseInProgress.set(true);
        StateManager.INSTANCE.setState(State.SYNCHRONIZING, "Synchronizing cache with db");

        final long startSyncronizingCache = System.currentTimeMillis();
        LOGGER.debug("Get last updates per distro from mongodb");
        Map<Distro, Instant> lastUpdates = MongoDbManager.INSTANCE.getLastUpdatesForDistros();
        Distro.getAsListWithoutNoneAndNotFound().forEach(distro -> distro.lastUpdate.set(lastUpdates.get(distro)));

        LOGGER.debug("Fill cache with packages from mongodb");
        final long      startRetrievingPkgFromMongodb = System.currentTimeMillis();
        final List<Pkg> pkgsFromMongoDb               = MongoDbManager.INSTANCE.getPkgs();
        LOGGER.debug("Got all pkgs from mongodb in {} ms", (System.currentTimeMillis() - startRetrievingPkgFromMongodb));

        Map<String, Pkg> patch = pkgsFromMongoDb.parallelStream().collect(Collectors.toMap(Pkg::getId, pkg -> pkg));
        pkgCache.setAll(patch);

        numberOfPackages.set(pkgCache.size());
        msToFillCacheWithPkgsFromDB.set(System.currentTimeMillis() - startSyncronizingCache);

        // Update all available major versions and maintained major versions
        updateMajorVersions();

        lastSync.set(Instant.now());
        syncWithDatabaseInProgress.set(false);
        }


    // ******************** MQTT Message handling *****************************
    public void handleMqttEvt(final MqttEvt evt) {
        final String topic = evt.getTopic();
        final String msg   = evt.getMsg();

        if (topic.equals(Constants.MQTT_PKG_UPDATE_TOPIC)) {
            switch(msg) {
                case Constants.MQTT_PKG_UPDATE_FINISHED_EMPTY_MSG -> {
                    if (!pkgCache.isEmpty()) { return; }
                        try {
                        LOGGER.debug("PkgCache is empty -> syncCacheWithDatabase(). MQTT event: {}", evt);

                            // Update cache with pkgs from mongodb
                            syncCacheWithDatabase();

                            // Update json cache
                            updateJsonCacheV2();
                        updateJsonCacheV3();
                        updateJsonCacheMinimizedV3();
                        } catch (Exception e) {
                            syncWithDatabaseInProgress.set(false);
                        }
                    }
                case Constants.MQTT_PKG_UPDATE_FINISHED_MSG -> {
                    try {
                        LOGGER.debug("Database updated -> syncCacheWithDatabase(). MQTT event: {}", evt);
                        mqttManager.publish(Constants.MQTT_API_STATE_TOPIC, "Database updated -> syncCacheWithDatabase");
                        // Update cache with pkgs from mongodb
                        syncCacheWithDatabase();

                        // Update json cache
                        updateJsonCacheV2();
                        updateJsonCacheV3();
                        updateJsonCacheMinimizedV3();
                    } catch (Exception e) {
                        syncWithDatabaseInProgress.set(false);
                    }
                }
                case Constants.MQTT_FORCE_PKG_UPDATE_MSG -> {
                    try {
                        LOGGER.debug("Force pkg update -> syncCacheWithDatabase(). MQTT event: {}", evt);
                        mqttManager.publish(Constants.MQTT_API_STATE_TOPIC, "Force pkg update -> syncCacheWithDatabase");

                        // Update cache with pkgs from mongodb
                        syncCacheWithDatabase();

                        // Update json cache
                        updateJsonCacheV2();
                        updateJsonCacheV3();
                        updateJsonCacheMinimizedV3();
                    } catch (Exception e) {
                        syncWithDatabaseInProgress.set(false);
                    }
                }
            }
        }
    }
}
