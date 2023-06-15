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
 *     You should have received a copy of the GNU General Public License
 *     along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.distribution;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.HashAlgorithm;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.SignatureType;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.GithubTokenPool;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.ArchiveType.getFromFileName;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.ReleaseStatus.GA;


public class Mandrel implements Distribution {
    private static final Logger        LOGGER                  = LoggerFactory.getLogger(Mandrel.class);

    private static final String        GITHUB_USER             = "graalvm";
    private static final String        PACKAGE_URL             = "https://api.github.com/repos/" + GITHUB_USER + "/mandrel/releases";
    private static final Pattern       FILENAME_PATTERN        = Pattern.compile("^(mandrel-java)([0-9]{2,3})(.*)(Final\\.tar\\.gz|\\.zip)$");
    private static final Matcher       FILENAME_MATCHER        = FILENAME_PATTERN.matcher("");

    // URL parameters
    private static final String        ARCHITECTURE_PARAM      = "";
    private static final String        OPERATING_SYSTEM_PARAM  = "";
    private static final String        ARCHIVE_TYPE_PARAM      = "";
    private static final String        PACKAGE_TYPE_PARAM      = "";
    private static final String        RELEASE_STATUS_PARAM    = "";
    private static final String        SUPPORT_TERM_PARAM      = "";
    private static final String        BITNESS_PARAM           = "";

    private static final HashAlgorithm HASH_ALGORITHM          = HashAlgorithm.NONE;
    private static final String        HASH_URI                = "";
    private static final SignatureType SIGNATURE_TYPE          = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM     = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI           = "";
    private static final String        OFFICIAL_URI            = "https://developers.redhat.com/blog/2021/04/14/mandrel-a-specialized-distribution-of-graalvm-for-quarkus#";


    @Override public Distro getDistro() { return Distro.MANDREL; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return SUPPORT_TERM_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }

    @Override public HashAlgorithm getHashAlgorithm() { return HASH_ALGORITHM; }

    @Override public String getHashUri() { return HASH_URI; }

    @Override public SignatureType getSignatureType() { return SIGNATURE_TYPE; }

    @Override public HashAlgorithm getSignatureAlgorithm() { return SIGNATURE_ALGORITHM; }

    @Override public String getSignatureUri() { return SIGNATURE_URI; }

    @Override public String getOfficialUri() { return OFFICIAL_URI; }

    @Override public List<String> getSynonyms() {
        return List.of("mandrel", "MANDREL", "Mandrel");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.MANDREL.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {

        LOGGER.debug("Query string for {}: {}", this.getName(), PACKAGE_URL);
        return PACKAGE_URL;
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        VersionNumber vNumber = null;
        String tag = jsonObj.get("tag_name").getAsString();
        if (tag.contains("vm-")) {
            tag = tag.substring(tag.lastIndexOf("vm-")).replace("vm-", "");
            vNumber = VersionNumber.fromText(tag);
        } else if (tag.startsWith("mandrel")) {
            tag = tag.replace("mandrel-", "");
            vNumber = VersionNumber.fromText(tag);
        }

        boolean prerelease = false;
        if (jsonObj.has("prerelease")) {
            prerelease = jsonObj.get("prerelease").getAsBoolean();
        }
        if (prerelease) { return pkgs; }

        JsonArray assets = jsonObj.getAsJsonArray("assets");
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     filename     = assetJsonObj.get("name").getAsString();
            if (filename.endsWith(Constants.FILE_ENDING_TXT) || filename.endsWith(Constants.FILE_ENDING_JAR) ||
                filename.endsWith(Constants.FILE_ENDING_SHA1) || filename.endsWith(Constants.FILE_ENDING_SHA256) ||
                filename.endsWith(Constants.FILE_ENDING_SOURCE_TAR_GZ)) { continue; }

            FILENAME_MATCHER.reset(filename);
            if (!FILENAME_MATCHER.matches()) { continue; }

            String[] filenameParts         = filename.split("-");
            String   strippedFilename = filename.replaceFirst("mandrel-java[0-9]+-", "").replaceAll("\\.Final.*", "");
            String[] strippedFilenameParts = strippedFilename.split("-");

            String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
            }

            Pkg pkg = new Pkg();

            pkg.setDistribution(Distro.MANDREL.get());
            pkg.setFileName(filename);
            pkg.setDirectDownloadUri(downloadLink);

            ArchiveType ext = getFromFileName(filename);
            pkg.setArchiveType(ext);

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> strippedFilename.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);
            if (Architecture.NONE == arch) {
                LOGGER.debug("Architecture not found in Mandrel for filename: {}", filename);
                continue;
            }

            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());

            if (null == vNumber && strippedFilenameParts.length > 2) {
                vNumber = VersionNumber.fromText(strippedFilenameParts[2]);
            }

            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);

            if (filenameParts.length > 1) {
                String part = filenameParts[1].replace("java", "");
                try {
                    int jdkVersion = Integer.parseInt(part);
                    pkg.setJdkVersion(new MajorVersion(jdkVersion));
                } catch (Exception e) {
                    LOGGER.error("Error parsing jdk version from filename in Mandrel {}", filename);
                    continue;
                }
            }

            TermOfSupport supTerm = Helper.getTermOfSupport(pkg.getJdkVersion().getAsInt());
            supTerm = TermOfSupport.MTS == supTerm ? TermOfSupport.STS : supTerm;
            pkg.setTermOfSupport(supTerm);

            pkg.setPackageType(JDK);

            pkg.setReleaseStatus(GA);

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                  .filter(entry -> strippedFilename.contains(entry.getKey()))
                                                                  .findFirst()
                                                                  .map(Entry::getValue)
                                                                  .orElse(OperatingSystem.NONE);

            if (OperatingSystem.NONE == os) {
                switch (pkg.getArchiveType()) {
                    case DEB:
                    case RPM:
                    case TAR_GZ:
                        os = LINUX;
                        break;
                    case MSI:
                    case ZIP:
                        os = WINDOWS;
                        break;
                    case DMG:
                    case PKG:
                        os = MACOS;
                        break;
                    default: continue;
                }
            }
            if (OperatingSystem.NONE == os) {
                LOGGER.debug("Operating System not found in Mandrel for filename: {}", filename);
                continue;
            }
            pkg.setOperatingSystem(os);

            pkg.setFreeUseInProduction(Boolean.TRUE);

            pkg.setSize(Helper.getFileSize(downloadLink));

            pkgs.add(pkg);
        }

        return pkgs;
    }

    public List<Pkg> getAllPkgs(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        final String pkgUrl = new StringBuilder(PACKAGE_URL).append("?per_page=100").toString();

        try {
            // Get all packages from github
            try {
                HttpResponse<String> response = Helper.get(pkgUrl, Map.of("accept", "application/vnd.github.v3+json",
                                                                          "authorization", GithubTokenPool.INSTANCE.next()));
                if (response.statusCode() == 200) {
                    String      bodyText = response.body();
                    Gson        gson     = new Gson();
                    JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                    if (element instanceof JsonArray) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        for (JsonElement jsonElement : jsonArray) {
                            JsonObject jsonObj = jsonElement.getAsJsonObject();
                            pkgs.addAll(getPkgFromJson(jsonObj, null, true, null, null, null, null, null, false, null, null, onlyNewPkgs));
                        }
                    }
                } else {
                    // Problem with url request
                    LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
                }
            } catch (CompletionException e) {
                LOGGER.error("Error fetching packages for distribution {} from {}", getName(), pkgUrl);
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages from {}. {}", getName(), e);
        }
        return pkgs;
    }
}
